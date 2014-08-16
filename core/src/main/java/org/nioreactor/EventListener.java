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

/**
 * Event listener.
 */
// TODO add method: exception(session, Throwable cause)
public interface EventListener {

    /**
     * Triggered after the given session has been just created.
     *
     * @param session the I/O session.
     */
    void connected(SessionContext session);

    /**
     * Triggered when the given session has input pending.
     *
     * @param session the I/O session.
     */
    void inputReady(SessionContext session);

    /**
     * Triggered when the given session is ready for output.
     *
     * @param session the I/O session.
     */
    void outputReady(SessionContext session);

    /**
     * Triggered when the given session has been terminated.
     *
     * @param session the I/O session.
     */
    void disconnected(SessionContext session);

}
