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

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskExclusivityStatusType;

/**
 * @author lazyman
 */
public enum RTaskExclusivityStatusType {

    CLAIMED(TaskExclusivityStatusType.CLAIMED),
    RELEASED(TaskExclusivityStatusType.RELEASED);

    private TaskExclusivityStatusType status;

    private RTaskExclusivityStatusType(TaskExclusivityStatusType status) {
        this.status = status;
    }

    public TaskExclusivityStatusType getStatus() {
        return status;
    }

    public static RTaskExclusivityStatusType toRepoType(TaskExclusivityStatusType status) {
        if (status == null) {
            return null;
        }

        for (RTaskExclusivityStatusType repo : RTaskExclusivityStatusType.values()) {
            if (status.equals(repo.getStatus())) {
                return repo;
            }
        }

        throw new IllegalArgumentException("Unknown exclusivity status type " + status);
    }
}
