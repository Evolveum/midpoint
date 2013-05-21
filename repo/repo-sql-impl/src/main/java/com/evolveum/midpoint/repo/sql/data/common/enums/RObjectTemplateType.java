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

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public enum RObjectTemplateType implements SchemaEnum<QName>{

    OBJECT(SchemaConstantsGenerated.C_OBJECT_TEMPLATE),

    USER(SchemaConstantsGenerated.C_USER_TEMPLATE);

    private QName elementName;

    private RObjectTemplateType(QName elementName) {
        this.elementName = elementName;
    }


    @Override
    public QName getSchemaValue() {
        return elementName;
    }
}
