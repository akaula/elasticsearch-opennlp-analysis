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

import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.lemmatizer.LemmatizerModel;
import studio.akaula.utils.CachedResourceLoader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.opennlp.OpenNLPLemmatizerFilter;
import org.apache.lucene.analysis.opennlp.tools.NLPLemmatizerOp;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import java.io.IOException;
import java.nio.file.Path;

import static studio.akaula.utils.SettingsUtils.resolvePath;

public class OpenNLPLemmatizerFilterFactory extends AbstractTokenFilterFactory {
    private final Path lemmatizerModelPath;

    private final Path dictionaryPath;

    private final CachedResourceLoader<DictionaryLemmatizer> dictionaryLemmatizerCache;
    private final CachedResourceLoader<LemmatizerModel> lemmatizerModelCache;

    public OpenNLPLemmatizerFilterFactory(
        CachedResourceLoader<DictionaryLemmatizer> dictionaryLemmatizerCache,
        CachedResourceLoader<LemmatizerModel> lemmatizerModelCache,
        Environment environment,
        String name,
        Settings settings
    ) {
        super(name, settings);
        this.dictionaryLemmatizerCache = dictionaryLemmatizerCache;
        this.lemmatizerModelCache = lemmatizerModelCache;

        dictionaryPath = resolvePath(environment, settings, "dictionary");
        lemmatizerModelPath = resolvePath(environment, settings, "lemmatizer_model");
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        if (dictionaryPath == null && lemmatizerModelPath == null) {
            throw new NullPointerException(
                "Either lemmatizer model or dictionary paths have to be specified to build opennlp_lemmatizer [" + this.name() + "]"
            );
        }

        DictionaryLemmatizer dictionaryLemmatizer = null;
        if (dictionaryPath != null) {
            try {
                dictionaryLemmatizer = dictionaryLemmatizerCache.getOrLoadResource(dictionaryPath, DictionaryLemmatizer::new);
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
            lemmatizerOp = new NLPLemmatizerOp(dictionaryLemmatizer, lemmatizerModel);
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
