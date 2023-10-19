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

import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;
import studio.akaula.utils.CachedResourceLoader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizer;
import org.apache.lucene.analysis.opennlp.tools.NLPSentenceDetectorOp;
import org.apache.lucene.analysis.opennlp.tools.NLPTokenizerOp;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenizerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import static studio.akaula.utils.SettingsUtils.resolvePath;

public class OpenNLPTokenizerFactory extends AbstractTokenizerFactory {
    private final Path sentenceModelPath;

    private final Path tokenizerModelPath;

    private final CachedResourceLoader<SentenceModel> sentenceModelCache;

    private final CachedResourceLoader<TokenizerModel> tokenizerModelCache;

    public OpenNLPTokenizerFactory(
        CachedResourceLoader<SentenceModel> sentenceModelCache,
        CachedResourceLoader<TokenizerModel> tokenizerModelCache,
        IndexSettings indexSettings,
        Environment environment,
        String name,
        Settings settings
    ) {
        super(indexSettings, settings, name);
        sentenceModelPath = resolvePath(environment, settings, "sentence_model");
        tokenizerModelPath = resolvePath(environment, settings, "tokenizer_model");
        this.sentenceModelCache = sentenceModelCache;
        this.tokenizerModelCache = tokenizerModelCache;
    }

    @Override
    public Tokenizer create() {
        Objects.requireNonNull(sentenceModelPath, "opennlp tokenizer [" + this.name() + "] requires non-empty sentence model");
        Objects.requireNonNull(tokenizerModelPath, "opennlp tokenizer [" + this.name() + "] requires non-empty tokenizer model");
        NLPSentenceDetectorOp nlpSentenceDetectorOp;
        try {
            nlpSentenceDetectorOp = new NLPSentenceDetectorOp(sentenceModelCache.getOrLoadResource(sentenceModelPath, SentenceModel::new));
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Cannot load sentence model from [" + sentenceModelPath + "] for opennlp tokenizer [" + this.name() + "]",
                exception
            );
        }

        NLPTokenizerOp nlpTokenizerOp;
        try {
            nlpTokenizerOp = new NLPTokenizerOp(tokenizerModelCache.getOrLoadResource(tokenizerModelPath, TokenizerModel::new));
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Cannot load tokenizer model from [" + sentenceModelPath + "] for opennlp tokenizer [" + this.name() + "]",
                exception
            );
        }

        OpenNLPTokenizer tokenizer;
        try {
            tokenizer = new OpenNLPTokenizer(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY, nlpSentenceDetectorOp, nlpTokenizerOp);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot create opennlp tokenizer [" + this.name() + "]", exception);
        }

        return tokenizer;
    }
}
