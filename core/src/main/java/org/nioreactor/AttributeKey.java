/*
 * Copyright 2014 Pedro Ribeiro
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nioreactor;

import org.nioreactor.util.Preconditions;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Key that can be used to access {@link org.nioreactor.SessionContext} attributes.
 * <p/>
 * Created by ribeirux on 8/16/14.
 */
public final class AttributeKey<T> {

    // Avoid Thread hostility
    private static final Set<String> NAMES = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private final String name;

    public AttributeKey(final String name) {
        Preconditions.checkNotNull(name, "name is null");
        if (!NAMES.add(name)) {
            throw new IllegalArgumentException(String.format("Key %s already exists", name));
        }

        this.name = name;
    }

    public final String name() {
        return name;
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        return super.equals(obj);
    }

    @Override
    public final String toString() {
        return name();
    }
}
