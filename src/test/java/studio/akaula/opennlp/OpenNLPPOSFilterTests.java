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
import org.elasticsearch.index.analysis.IndexAnalyzers;

import static org.apache.lucene.tests.analysis.BaseTokenStreamTestCase.assertTokenStreamContents;

public class OpenNLPPOSFilterTests extends OpenNLPTestCase {

    public void testBasicUsage() throws Exception {
        IndexAnalyzers analyzers = getIndexAnalyzers(
            Settings.builder()
                .put(tokenizer("en_opennlp", "en", "ewt"))
                .put(pos_filter("en_opennlp_pos", "en", "ewt"))
                .put("index.analysis.analyzer.en_opennlp.tokenizer", "en_opennlp")
                .putList("index.analysis.analyzer.en_opennlp.filter", "en_opennlp_pos")
                .build()
        );
        assertTokenStreamContents(
            analyzers.get("en_opennlp").tokenStream("", "I'd far rather be happy than right any day."),
            new String[] { "I'd", "far", "rather", "be", "happy", "than", "right", "any", "day", "." },
            new String[] { "PRON+AUX", "ADV", "ADV", "AUX", "ADJ", "ADP", "ADJ", "DET", "NOUN", "PUNCT" }
        );
    }

    public void testMissingModel() {

        NullPointerException ex = expectThrows(
            NullPointerException.class,
            () -> getIndexAnalyzers(
                Settings.builder()
                    .put(tokenizer("en_opennlp", "en", "ewt"))
                    .put("index.analysis.filter.en_opennlp_pos.type", "opennlp_pos")
                    .put("index.analysis.analyzer.en_opennlp.tokenizer", "en_opennlp")
                    .putList("index.analysis.analyzer.en_opennlp.filter", "en_opennlp_pos")
                    .build()
            )
        );
        assertEquals("opennlp_pos filter [en_opennlp_pos] requires non-empty pos_model_path", ex.getMessage());
    }

}
