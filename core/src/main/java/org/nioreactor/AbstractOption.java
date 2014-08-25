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

/**
 * Base option. This class implements general methods that should be used across all different implementations.
 * <p/>
 * Created by ribeirux on 8/17/14.
 */
public abstract class AbstractOption<T> {

    private final String name;
    private final Class<T> type;

    protected AbstractOption(final String name, final Class<T> type) {
        this.name = Preconditions.checkNotNull(name, "name is null");
        this.type = Preconditions.checkNotNull(type, "type is null");
    }

    public final String name() {
        return name;
    }

    public T cast(final Object o) {
        return type.cast(o);
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
