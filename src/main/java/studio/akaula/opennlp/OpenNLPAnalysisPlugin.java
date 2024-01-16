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
import opennlp.tools.postag.POSModel;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;
import studio.akaula.utils.CachedResourceLoader;

import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;

import java.util.Map;

public class OpenNLPAnalysisPlugin extends Plugin implements AnalysisPlugin {

    private final CachedResourceLoader<POSModel> posModelCache = new CachedResourceLoader<>();
    private final CachedResourceLoader<SentenceModel> sentenceModelCache = new CachedResourceLoader<>();
    private final CachedResourceLoader<TokenizerModel> tokenizerModelCache = new CachedResourceLoader<>();
    private final CachedResourceLoader<DictionaryLemmatizer> dictionaryLemmatizerCache = new CachedResourceLoader<>();
    private final CachedResourceLoader<LemmatizerModel> lemmatizerModelCache = new CachedResourceLoader<>();

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        return Map.of(
            "opennlp_lemmatizer",
            (indexSettings, environment, name, settings) -> new OpenNLPLemmatizerFilterFactory(
                dictionaryLemmatizerCache,
                lemmatizerModelCache,
                environment,
                name,
                settings
            ),
            "opennlp_pos",
            (indexSettings, environment, name, settings) -> new OpenNLPPOSFilterFactory(posModelCache, environment, name, settings)
        );
    }

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> getTokenizers() {
        return Map.of(
            "opennlp",
            (indexSettings, environment, name, settings) -> new OpenNLPTokenizerFactory(
                sentenceModelCache,
                tokenizerModelCache,
                indexSettings,
                environment,
                name,
                settings
            )
        );
    }
}
