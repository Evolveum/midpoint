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
public enum NodeExecutionStatus {

    /**
     * Node is running, task scheduler is running.
     */
    RUNNING,

    /**
     * Node is running, scheduler is paused.
     */
    PAUSED,

    /**
     * Node down (only for remote nodes, of course)
     */
    DOWN,

    /**
     * Node error (see NodeErrorStatus for more details).
     */
    ERROR,

    /**
     * Status unknown due to communication error.
     */
    COMMUNICATION_ERROR;

}
