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

import java.util.concurrent.TimeUnit;

/**
 * The result of server bind operation.
 * <p>
 * Created by ribeirux on 8/17/14.
 */
public interface ServerPromise {

    ReactorStatus getStatus();

    void shutdown();

    void await() throws InterruptedException;

    void await(long timeout, TimeUnit unit) throws InterruptedException;
}
