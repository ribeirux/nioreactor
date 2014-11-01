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
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link org.nioreactor.SocketConfig}.
 * <p>
 * Created by ribeirux on 8/23/14.
 */
public final class DefaultSocketConfig implements SocketConfig {

    private final Map<SocketOption<?>, Object> socketOptions;

    private DefaultSocketConfig(final Builder builder) {
        this.socketOptions = Collections.unmodifiableMap(new HashMap<>(builder.socketOptions));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> T option(final SocketOption<T> key) {
        Preconditions.checkNotNull(key, "key is null");

        final Object o = this.socketOptions.get(key);
        return o == null ? key.defaultValue() : key.cast(o);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DefaultSocketConfig{");
        sb.append("socketOptions=").append(socketOptions);
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {

        private final Map<SocketOption<?>, Object> socketOptions = new HashMap<>();

        private Builder() {
        }

        public <T> Builder option(final SocketOption<T> key, final T value) {
            Preconditions.checkNotNull(key, "key is null");
            Preconditions.checkNotNull(value, "value is null");

            this.socketOptions.put(key, value);
            return this;
        }

        public SocketConfig build() {
            return new DefaultSocketConfig(this);
        }
    }
}
