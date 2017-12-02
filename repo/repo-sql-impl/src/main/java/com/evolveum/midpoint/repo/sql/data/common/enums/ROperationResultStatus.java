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

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * @author lazyman
 */
@JaxbType(type = OperationResultStatusType.class)
public enum ROperationResultStatus implements SchemaEnum<OperationResultStatusType> {

    SUCCESS(OperationResultStatusType.SUCCESS),
    WARNING(OperationResultStatusType.WARNING),
    PARTIAL_ERROR(OperationResultStatusType.PARTIAL_ERROR),
    FATAL_ERROR(OperationResultStatusType.FATAL_ERROR),
    NOT_APPLICABLE(OperationResultStatusType.NOT_APPLICABLE),
    IN_PROGRESS(OperationResultStatusType.IN_PROGRESS),
    UNKNOWN(OperationResultStatusType.UNKNOWN),
    HANDLED_ERROR(OperationResultStatusType.HANDLED_ERROR);

    private OperationResultStatusType status;

    private ROperationResultStatus(OperationResultStatusType status) {
        this.status = status;
    }

    public OperationResultStatus getStatus(){
    	return OperationResultStatus.parseStatusType(status);
    }

    @Override
    public OperationResultStatusType getSchemaValue() {
        return status;
    }

    public static ROperationResultStatus toRepo(OperationResultStatusType jaxb) {
        if (jaxb == null) {
            return null;
        }
        for (ROperationResultStatus st : values()) {
            if (jaxb == st.status) {
                return st;
            }
        }
        throw new IllegalArgumentException("Unknown operation result state '" + jaxb + "'.");
    }
}
