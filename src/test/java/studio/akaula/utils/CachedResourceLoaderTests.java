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
package studio.akaula.utils;

import org.elasticsearch.core.CheckedFunction;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.core.IsEqual.equalTo;

public class CachedResourceLoaderTests extends ESTestCase {

    public void testBasicUsage() throws Exception {
        AtomicInteger loadCounter = new AtomicInteger(0);
        CheckedFunction<Path, String, IOException> loader = p -> {
            loadCounter.incrementAndGet();
            return p + "_resource";
        };
        CachedResourceLoader<String> cachedLoader = new CachedResourceLoader<>();
        initial_load(cachedLoader, loader, loadCounter);

        System.gc();

        Path firstPath = Path.of("first");
        String first = cachedLoader.getOrLoadResource(firstPath, loader);
        assertThat(first, equalTo("first_resource"));
        assertThat(loadCounter.get(), equalTo(3));
    }

    private void initial_load(
        CachedResourceLoader<String> cachedLoader,
        CheckedFunction<Path, String, IOException> loader,
        AtomicInteger loadCounter
    ) throws Exception {
        Path firstPath = Path.of("first");
        String first = cachedLoader.getOrLoadResource(firstPath, loader);
        assertThat(first, equalTo("first_resource"));

        String first_again = cachedLoader.getOrLoadResource(firstPath, loader);
        assertThat(first_again, equalTo("first_resource"));
        assertThat(loadCounter.get(), equalTo(1));

        Path secondPath = Path.of("second");
        String second = cachedLoader.getOrLoadResource(secondPath, loader);
        assertThat(second, equalTo("second_resource"));

        assertThat(loadCounter.get(), equalTo(2));

    }
}
