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

import java.nio.channels.SelectionKey;

/**
 * * Type of I/O event notifications.
 * <p/>
 * Created by ribeirux on 8/11/14.
 */
public enum EventKey {

    /**
     * Interest in data input.
     */
    READ(SelectionKey.OP_READ),
    /**
     * Interest in data output.
     */
    WRITE(SelectionKey.OP_WRITE),
    /**
     * Interest in data input/output.
     */
    READ_WRITE(SelectionKey.OP_READ | SelectionKey.OP_WRITE);

    private final int interestOps;

    private EventKey(final int interestOps) {
        this.interestOps = interestOps;
    }

    public int interestOps() {
        return interestOps;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EventKey{");
        sb.append("interestOps=").append(interestOps);
        sb.append('}');
        return sb.toString();
    }
}
