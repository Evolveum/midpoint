/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.task.api;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 27.4.2012
 * Time: 20:59
 * To change this template use File | Settings | File Templates.
 */
public class TaskManagerException extends Exception {
    public TaskManagerException() {
        super();
    }

    public TaskManagerException(Throwable cause) {
        super(cause);
    }

    public TaskManagerException(String message) {
        super(message);
    }

    public TaskManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
