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
 * Status of an I/O reactor.
 * <p>
 * Created by ribeirux on 8/10/14.
 */
public enum ReactorStatus {

    /**
     * The reactor is inactive / has not been started
     */
    INACTIVE,

    /**
     * The reactor is active / processing I/O events.
     */
    ACTIVE,

    /**
     * The reactor is shutting down.
     */
    SHUTTING_DOWN,

    /**
     * The reactor has shut down.
     */
    SHUT_DOWN

}
