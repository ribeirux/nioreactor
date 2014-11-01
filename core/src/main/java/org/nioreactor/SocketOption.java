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
 * Option for configuring the socket.
 * <p>
 * Created by ribeirux on 8/17/14.
 */
public final class SocketOption<T> extends AbstractOption<T> {

    public static final SocketOption<Integer> SO_TIMEOUT = new SocketOption<>("SO_TIMEOUT", Integer.class, 0);
    public static final SocketOption<Boolean> SO_REUSEADDR = new SocketOption<>("SO_REUSEADDR", Boolean.class, false);
    public static final SocketOption<Integer> SO_LINGER = new SocketOption<>("SO_LINGER", Integer.class, -1);
    public static final SocketOption<Boolean> SO_KEEPALIVE = new SocketOption<>("SO_KEEPALIVE", Boolean.class, false);
    public static final SocketOption<Boolean> TCP_NODELAY = new SocketOption<>("TCP_NODELAY", Boolean.class, true);
    public static final SocketOption<Integer> SO_SNDBUF = new SocketOption<>("SO_SNDBUF", Integer.class, 0);
    public static final SocketOption<Integer> SO_RCVBUF = new SocketOption<>("SO_RCVBUF", Integer.class, 0);

    private final T defaultValue;

    private SocketOption(final String name, final Class<T> type, final T defaultValue) {
        super(name, type);
        this.defaultValue = Preconditions.checkNotNull(defaultValue, "default value is null");
    }

    public T defaultValue() {
        return defaultValue;
    }
}
