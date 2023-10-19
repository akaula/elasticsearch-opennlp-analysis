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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.IndexAnalyzers;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.nio.file.Path;

public class OpenNLPTestCase extends ESTestCase {
    public Path getModelPath(String language, String dataSet, String modelType) {
        return getDataPath(
            "/" + language + "-ud-" + dataSet + "-2.12/opennlp-" + language + "-ud-" + dataSet + "-" + modelType + "-2.12-1.9.4.bin"
        );
    }

    public IndexAnalyzers getIndexAnalyzers(Settings settings) throws IOException {
        TestAnalysis analysis = createTestAnalysis(new Index("test", "_na_"), settings, new OpenNLPAnalysisPlugin());
        return analysis.indexAnalyzers;
    }

    public Settings tokenizer(String name, String language, String dataSet) {
        return Settings.builder()
            // Setup en_opennlp tokenizer
            .put("index.analysis.tokenizer." + name + ".type", "opennlp")
            .put("index.analysis.tokenizer." + name + ".sentence_model_path", getModelPath(language, dataSet, "sentence"))
            .put("index.analysis.tokenizer." + name + ".tokenizer_model_path", getModelPath(language, dataSet, "tokens"))
            .build();
    }

    public Settings pos_filter(String name, String language, String dataSet) {
        return Settings.builder()
            // Setup en_opennlp tokenizer
            .put("index.analysis.filter." + name + ".type", "opennlp_pos")
            .put("index.analysis.filter." + name + ".pos_model_path", getModelPath(language, dataSet, "pos"))
            .build();
    }

    public Settings lemmatizer_filter(String name, String language, String dataSet) {
        return Settings.builder()
            // Setup en_opennlp tokenizer
            .put("index.analysis.filter." + name + ".type", "opennlp_lemmatizer")
            .put("index.analysis.filter." + name + ".lemmatizer_model_path", getModelPath(language, dataSet, "lemmatizer"))
            .build();
    }
}
