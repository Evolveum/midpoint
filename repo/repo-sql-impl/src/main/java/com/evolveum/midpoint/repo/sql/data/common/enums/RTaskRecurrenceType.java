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

import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskRecurrenceType;

/**
 * @author lazyman
 */
public enum RTaskRecurrenceType {

    SINGLE(TaskRecurrenceType.SINGLE),
    RECURRING(TaskRecurrenceType.RECURRING);

    private TaskRecurrenceType recurrence;

    private RTaskRecurrenceType(TaskRecurrenceType recurrence) {
        this.recurrence = recurrence;
    }

    public TaskRecurrenceType getRecurrence() {
        return recurrence;
    }

    public static RTaskRecurrenceType toRepoType(TaskRecurrenceType recurrence) {
        if (recurrence == null) {
            return null;
        }

        for (RTaskRecurrenceType repo : RTaskRecurrenceType.values()) {
            if (recurrence.equals(repo.getRecurrence())) {
                return repo;
            }
        }

        throw new IllegalArgumentException("Unknown task recurrence type " + recurrence);
    }
}
