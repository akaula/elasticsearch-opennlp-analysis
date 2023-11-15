/*
 * Copyright 2023 Aka'ula Studio LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package studio.akaula.opennlp;

import opennlp.tools.lemmatizer.LemmatizerModel;
import studio.akaula.utils.CachedResourceLoader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.opennlp.OpenNLPLemmatizerFilter;
import org.apache.lucene.analysis.opennlp.tools.NLPLemmatizerOp;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.internal.io.Streams;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static studio.akaula.utils.SettingsUtils.resolvePath;

public class OpenNLPLemmatizerFilterFactory extends AbstractTokenFilterFactory {
    private final Path lemmatizerModelPath;

    private final Path dictionaryPath;

    private final CachedResourceLoader<byte[]> dictionaryLemmatizerCache;
    private final CachedResourceLoader<LemmatizerModel> lemmatizerModelCache;

    public OpenNLPLemmatizerFilterFactory(

        CachedResourceLoader<byte[]> dictionaryLemmatizerCache,
        CachedResourceLoader<LemmatizerModel> lemmatizerModelCache,
        Environment environment,
        IndexSettings indexSettings,
        String name,
        Settings settings
    ) {
        super(indexSettings, name, settings);
        this.dictionaryLemmatizerCache = dictionaryLemmatizerCache;
        this.lemmatizerModelCache = lemmatizerModelCache;

        dictionaryPath = resolvePath(environment, settings, "dictionary");
        lemmatizerModelPath = resolvePath(environment, settings, "lemmatizer_model");
    }

    private static byte[] readDictionary(Path dictionaryPath) throws IOException {
        try (
            InputStream inputStream = Files.newInputStream(dictionaryPath);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ) {
            Streams.copy(inputStream, outputStream);
            return outputStream.toByteArray();
        }
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        if (dictionaryPath == null && lemmatizerModelPath == null) {
            throw new NullPointerException(
                "Either lemmatizer model or dictionary paths have to be specified to build opennlp_lemmatizer [" + this.name() + "]"
            );
        }

        byte[] dictionaryLemmatizer = null;
        if (dictionaryPath != null) {
            try {
                dictionaryLemmatizer = dictionaryLemmatizerCache.getOrLoadResource(
                    dictionaryPath,
                    OpenNLPLemmatizerFilterFactory::readDictionary
                );
            } catch (IOException exception) {
                throw new IllegalArgumentException(
                    "Cannot load dictionary from [" + dictionaryPath + "] for opennlp_lemmatizer [" + this.name() + "]",
                    exception
                );
            }
        }

        LemmatizerModel lemmatizerModel = null;
        if (lemmatizerModelPath != null) {
            try {
                lemmatizerModel = lemmatizerModelCache.getOrLoadResource(lemmatizerModelPath, LemmatizerModel::new);
            } catch (IOException exception) {
                throw new IllegalArgumentException(
                    "Cannot load lemmatizer model from [" + dictionaryPath + "] for opennlp_lemmatizer [" + this.name() + "]",
                    exception
                );
            }
        }

        NLPLemmatizerOp lemmatizerOp;
        try {
            InputStream dictionaryInputStream = null;
            try {
                if (dictionaryLemmatizer != null) {
                    dictionaryInputStream = new ByteArrayInputStream(dictionaryLemmatizer);
                }
                lemmatizerOp = new NLPLemmatizerOp(dictionaryInputStream, lemmatizerModel);
            } finally {
                if (dictionaryInputStream != null) {
                    dictionaryInputStream.close();
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Cannot build lemmatizer model from"
                    + (dictionaryPath != null ? " dictionary [" + dictionaryPath + "]" : "")
                    + (lemmatizerModelPath != null ? " lemmatizer model [" + lemmatizerModelPath + "]" : "")
                    + " for opennlp_lemmatizer ["
                    + this.name()
                    + "]",
                exception
            );
        }
        return new OpenNLPLemmatizerFilter(tokenStream, lemmatizerOp);
    }
}
