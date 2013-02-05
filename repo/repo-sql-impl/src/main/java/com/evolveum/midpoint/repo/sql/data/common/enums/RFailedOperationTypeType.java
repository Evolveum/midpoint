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

import com.evolveum.midpoint.xml.ns._public.common.common_2a.FailedOperationTypeType;

/**
 * @author lazyman
 */
public enum RFailedOperationTypeType {

    DELETE(FailedOperationTypeType.DELETE),
    ADD(FailedOperationTypeType.ADD),
    MODIFY(FailedOperationTypeType.MODIFY);

    private FailedOperationTypeType operation;

    private RFailedOperationTypeType(FailedOperationTypeType operation) {
        this.operation = operation;
    }

    public FailedOperationTypeType getOperation() {
        return operation;
    }

    public static RFailedOperationTypeType toRepoType(FailedOperationTypeType operation) {
        if (operation == null) {
            return null;
        }

        for (RFailedOperationTypeType repo : RFailedOperationTypeType.values()) {
            if (operation.equals(repo.getOperation())) {
                return repo;
            }
        }

        throw new IllegalArgumentException("Unknown failed operation type " + operation);
    }
}
