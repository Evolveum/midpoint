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

@JaxbType(type = JasperExportType.class)
public enum RExportType implements SchemaEnum<JasperExportType> {

         PDF(JasperExportType.PDF),

         CSV(JasperExportType.CSV),

         XML(JasperExportType.XML),

         XML_EMBED(JasperExportType.XML_EMBED),

         HTML(JasperExportType.HTML),

         RTF(JasperExportType.RTF),

         XLS(JasperExportType.XLS),

         ODT(JasperExportType.ODT),

         ODS(JasperExportType.ODS),

         DOCX(JasperExportType.DOCX),

         XLSX(JasperExportType.XLSX),

         PPTX(JasperExportType.PPTX),

         XHTML(JasperExportType.XHTML),

         JXL(JasperExportType.JXL);

        private JasperExportType type;

        RExportType(JasperExportType type) {
            this.type = type;
        }


    @Override
    public JasperExportType getSchemaValue() {
        return type;
    }

}
