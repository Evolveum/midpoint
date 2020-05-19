/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.JasperExportType;

@JaxbType(type = ExportType.class)
public enum RExportType implements SchemaEnum<ExportType> {


         CSV(ExportType.CSV),

         HTML(ExportType.HTML);

        private ExportType type;

        RExportType(ExportType type) {
            this.type = type;
        }


    @Override
    public ExportType getSchemaValue() {
        return type;
    }

}
