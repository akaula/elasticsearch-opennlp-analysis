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

import static org.apache.lucene.analysis.BaseTokenStreamTestCase.assertTokenStreamContents;

public class OpenNLPTokenizerTests extends OpenNLPTestCase {

    public void testBasicUsage() throws Exception {
        IndexAnalyzers analyzers = getIndexAnalyzers(
            Settings.builder()
                .put(tokenizer("en_opennlp", "en", "ewt"))
                .put("index.analysis.analyzer.en_opennlp.tokenizer", "en_opennlp")
                .build()
        );
        assertTokenStreamContents(
            analyzers.get("en_opennlp").tokenStream("", "I'd far rather be happy than right any day."),
            new String[] { "I'd", "far", "rather", "be", "happy", "than", "right", "any", "day", "." },
            new String[] { "word", "word", "word", "word", "word", "word", "word", "word", "word", "word" }
        );
    }

    public void testMissingModel() {

        NullPointerException ex = expectThrows(
            NullPointerException.class,
            () -> getIndexAnalyzers(
                Settings.builder()
                    .put("index.analysis.tokenizer.en_opennlp.type", "opennlp")
                    .put("index.analysis.analyzer.en_opennlp.tokenizer", "en_opennlp")
                    .put("index.analysis.tokenizer.en_opennlp.tokenizer_model_path", getModelPath("en", "ewt", "tokens"))
                    .build()
            )
        );
        assertEquals("opennlp tokenizer [en_opennlp] requires non-empty sentence model", ex.getMessage());

        ex = expectThrows(
            NullPointerException.class,
            () -> getIndexAnalyzers(
                Settings.builder()
                    .put("index.analysis.tokenizer.en_opennlp.type", "opennlp")
                    .put("index.analysis.tokenizer.en_opennlp.sentence_model_path", getModelPath("en", "ewt", "sentence"))
                    .put("index.analysis.analyzer.en_opennlp.tokenizer", "en_opennlp")
                    .build()
            )
        );
        assertEquals("opennlp tokenizer [en_opennlp] requires non-empty tokenizer model", ex.getMessage());
    }

}
