/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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
