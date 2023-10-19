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

import opennlp.tools.postag.POSModel;
import studio.akaula.utils.CachedResourceLoader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilter;
import org.apache.lucene.analysis.opennlp.tools.NLPPOSTaggerOp;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import static studio.akaula.utils.SettingsUtils.resolvePath;

public class OpenNLPPOSFilterFactory extends AbstractTokenFilterFactory {
    private final Path posModelPath;

    private final CachedResourceLoader<POSModel> posModelCache;

    public OpenNLPPOSFilterFactory(CachedResourceLoader<POSModel> posModelCache, Environment environment, String name, Settings settings) {
        super(name, settings);
        this.posModelCache = posModelCache;
        posModelPath = resolvePath(environment, settings, "pos_model");
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        Objects.requireNonNull(posModelPath, "opennlp_pos filter [" + this.name() + "] requires non-empty pos_model_path");
        NLPPOSTaggerOp nlpposTaggerOp;
        try {
            nlpposTaggerOp = new NLPPOSTaggerOp(posModelCache.getOrLoadResource(posModelPath, POSModel::new));
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Cannot load model from [" + posModelPath + "] for opennlp_pos filter [" + this.name() + "]",
                exception
            );
        }

        return new OpenNLPPOSFilter(tokenStream, nlpposTaggerOp);
    }
}
