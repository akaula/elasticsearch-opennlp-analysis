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

import org.elasticsearch.common.CheckedFunction;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Thread safe resource loader and cache that ensure that resources are loaded only once
 * and kept until the last reference to the resource exists.
 */
public class CachedResourceLoader<R> {
    private final HashMap<Path, WeakReference<R>> cache = new HashMap<>();

    private final ReferenceQueue<R> retired = new ReferenceQueue<>();

    public synchronized R getOrLoadResource(Path path, CheckedFunction<Path, R, IOException> loader) throws IOException {
        WeakReference<R> reference = cache.get(path);
        if (reference != null) {
            R resource = reference.get();
            if (resource != null) {
                return resource;
            }
        }
        R resource = loader.apply(path);
        cache.put(path, new WeakReference<>(resource, retired));
        cleanup();
        return resource;
    }

    @SuppressWarnings("unchecked")
    private void cleanup() {
        WeakReference<R> reference;
        while ((reference = (WeakReference<R>) retired.poll()) != null) {
            final WeakReference<R> retired_reference = reference;
            cache.entrySet().removeIf(entry -> entry.getValue() == retired_reference);
        }
    }
}
