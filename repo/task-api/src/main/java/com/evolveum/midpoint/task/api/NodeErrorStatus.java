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
 *
 * Status of this node w.r.t. error conditions.
 * Generally speaking, if NodeErrorStatus != OK, tasks cannot be scheduled on this node.
 *
 * @author Pavol Mederly
 */
public enum NodeErrorStatus {

    /**
     * Everything is OK.
     */
    OK,

    /**
     * More nodes with the same ID or name (currently ID == name).
     */
    DUPLICATE_NODE_ID_OR_NAME,

    /**
     * A non-clustered node runs along other (clustered or non-clustered) nodes.
     */
    NON_CLUSTERED_NODE_WITH_OTHERS,

    /**
     * Local task manager is not configured properly, so it can be even started.
     */
    LOCAL_CONFIGURATION_ERROR,

    /**
     * Other kind of initialization error.
     */
    LOCAL_INITIALIZATION_ERROR,

    /**
     * It was not possible to register node in repository due to a permanent error.
     */
    NODE_REGISTRATION_FAILED;

}
