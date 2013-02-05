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

import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultStatusType;

/**
 * @author lazyman
 */
public enum ROperationResultStatusType {

    SUCCESS(OperationResultStatusType.SUCCESS),
    WARNING(OperationResultStatusType.WARNING),
    PARTIAL_ERROR(OperationResultStatusType.PARTIAL_ERROR),
    FATAL_ERROR(OperationResultStatusType.FATAL_ERROR),
    NOT_APPLICABLE(OperationResultStatusType.NOT_APPLICABLE),
    IN_PROGRESS(OperationResultStatusType.IN_PROGRESS),
    UNKNOWN(OperationResultStatusType.UNKNOWN),
    HANDLED_ERROR(OperationResultStatusType.HANDLED_ERROR);

    private OperationResultStatusType status;

    private ROperationResultStatusType(OperationResultStatusType status) {
        this.status = status;
    }

    public OperationResultStatusType getStatus() {
        return status;
    }

    public static ROperationResultStatusType toRepoType(OperationResultStatusType status) {
        if (status == null) {
            return null;
        }

        for (ROperationResultStatusType repo : ROperationResultStatusType.values()) {
            if (status.equals(repo.getStatus())) {
                return repo;
            }
        }

        throw new IllegalArgumentException("Unknown operation result status type " + status);
    }
}
