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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Key that can be used to access {@link org.nioreactor.SessionContext} attributes.
 * <p>
 * Created by ribeirux on 8/16/14.
 */
public final class AttributeKey<T> extends AbstractOption<T> {

    // Avoid Thread hostility
    private static final Set<String> NAMES = ConcurrentHashMap.newKeySet();

    public AttributeKey(final String name, final Class<T> type) {
        super(validateNameUniqueness(name), type);
    }

    private static String validateNameUniqueness(final String name) {
        Preconditions.checkNotNull(name, "name");
        if (!NAMES.add(name)) {
            throw new IllegalArgumentException("name: " + name + " already exists");
        }

        return name;
    }
}
