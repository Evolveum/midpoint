/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class ReportTypeUtil {

    public static PrismSchema parseReportConfigurationSchema(PrismObject<ReportType> report, PrismContext context)
            throws SchemaException {

        PrismContainer xmlSchema = report.findContainer(ReportType.F_CONFIGURATION_SCHEMA);
        Element xmlSchemaElement = ObjectTypeUtil.findXsdElement(xmlSchema);
        if (xmlSchemaElement == null) {
            //no schema definition available
            return null;
        }

        return PrismSchema.parse(xmlSchemaElement, true, "schema for " + report, context);
    }

    public static PrismContainerDefinition<ReportConfigurationType> findReportConfigurationDefinition(PrismSchema schema) {
        if (schema == null) {
            return null;
        }

        QName configContainerQName = new QName(schema.getNamespace(), ReportType.F_CONFIGURATION.getLocalPart());
        return schema.findContainerDefinitionByElementName(configContainerQName);
    }

    public static void applyDefinition(PrismObject<ReportType> report, PrismContext prismContext)
            throws SchemaException {

        PrismContainer<Containerable> configuration = report.findContainer(ReportType.F_CONFIGURATION);
        if (configuration == null) {
            //nothing to apply definitions on
            return;
        }

        PrismContainer xmlSchema = report.findContainer(ReportType.F_CONFIGURATION_SCHEMA);
        Element xmlSchemaElement = ObjectTypeUtil.findXsdElement(xmlSchema);
        if (xmlSchemaElement == null) {
            //no schema definition available
            throw new SchemaException("Couldn't find schema for configuration in report type " + report + ".");
        }

        PrismSchema schema = ReportTypeUtil.parseReportConfigurationSchema(report, prismContext);
        PrismContainerDefinition<ReportConfigurationType> definition =  ReportTypeUtil.findReportConfigurationDefinition(schema);
        if (definition == null) {
            //no definition found for container
            throw new SchemaException("Couldn't find definitions for report type " + report + ".");
        }

        configuration.applyDefinition(definition, true);
    }
}
