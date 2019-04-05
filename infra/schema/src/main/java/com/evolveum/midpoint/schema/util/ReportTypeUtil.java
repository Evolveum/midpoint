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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.jfree.data.gantt.Task;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRTemplate;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignReportTemplate;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

/**
 * @author lazyman
 */
public class ReportTypeUtil {

	public static String FILENAMEPARAMETER = "fname";
	public static final String HEADER_USERAGENT = "mp-cluster-peer-client";
	public static String URLENCODING = "UTF-8";
	
	public static final String REPORT_LANGUAGE = "midPoint";
	
	
	public static final String PARAMETER_TEMPLATE_STYLES = "baseTemplateStyles";
	public static final String PARAMETER_REPORT_OID = "midpointReportOid";
	public static final String PARAMETER_REPORT_OBJECT = "midpointReportObject";
	public static final String PARAMETER_TASK = "midpointTask";
	public static final String PARAMETER_OPERATION_RESULT = "midpointOperationResult";
	
	
	 public static JasperDesign loadJasperDesign(byte[] template) throws SchemaException{
	    	try	 {
	    	byte[] reportTemplate = Base64.decodeBase64(template);
		
		 	InputStream inputStreamJRXML = new ByteArrayInputStream(reportTemplate);
		 	JasperDesign jasperDesign = JRXmlLoader.load(inputStreamJRXML);
//		 	LOGGER.trace("load jasper design : {}", jasperDesign);
		 	return jasperDesign;
	    	} catch (JRException ex){
	    		throw new SchemaException(ex.getMessage(), ex.getCause());
	    	}
	    }


	public static JasperReport loadJasperReport(ReportType reportType) throws SchemaException{

		if (reportType.getTemplate() == null) {
			throw new IllegalStateException("Could not create report. No jasper template defined.");
		}
		try	 {
	    	 	JasperDesign jasperDesign = loadJasperDesign(reportType.getTemplate());
//	    	 	LOGGER.trace("load jasper design : {}", jasperDesign);
	    	 	jasperDesign.setLanguage(REPORT_LANGUAGE);

			 if (reportType.getTemplateStyle() != null){
				JRDesignReportTemplate templateStyle = new JRDesignReportTemplate(new JRDesignExpression("$P{" + PARAMETER_TEMPLATE_STYLES + "}"));
				jasperDesign.addTemplate(templateStyle);
				
				jasperDesign.addParameter(createParameter(PARAMETER_TEMPLATE_STYLES, JRTemplate.class));
				
			 }

			 jasperDesign.addParameter(createParameter("finalQuery", Object.class));
			 jasperDesign.addParameter(createParameter(PARAMETER_REPORT_OID, String.class));
			 //TODO is this right place, we don't see e.g. task
//			 jasperDesign.addParameter(createParameter(PARAMETER_TASK, Object.class));
			 jasperDesign.addParameter(createParameter(PARAMETER_OPERATION_RESULT, OperationResult.class));
			 
			 //TODO maybe other paramteres? sunch as PARAMETER_REPORT_OBJECT PARAMETER_REPORT_SERVICE ???

			 JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
			 return jasperReport;
		 } catch (JRException ex){
//			 LOGGER.error("Couldn't create jasper report design {}", ex.getMessage());
			 throw new SchemaException(ex.getMessage(), ex.getCause());
		 }


}
	
	private static JRDesignParameter createParameter(String paramName, Class<?> valueClass) {
		JRDesignParameter param = new JRDesignParameter();
		param.setName(paramName);
		param.setValueClass(valueClass);
		param.setForPrompting(false);
		param.setSystemDefined(true);
		return param;
		
	}

    public static PrismSchema parseReportConfigurationSchema(PrismObject<ReportType> report, PrismContext context)
            throws SchemaException {

        PrismContainer xmlSchema = report.findContainer(ReportType.F_CONFIGURATION_SCHEMA);
        Element xmlSchemaElement = ObjectTypeUtil.findXsdElement(xmlSchema);
        if (xmlSchemaElement == null) {
            //no schema definition available
            return null;
        }

        return PrismSchemaImpl.parse(xmlSchemaElement, true, "schema for " + report, context);
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

        PrismContainer<ReportConfigurationType> configuration = report.findContainer(ReportType.F_CONFIGURATION);
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

    public static void applyConfigurationDefinition(PrismObject<ReportType> report, ObjectDelta delta, PrismContext prismContext)
            throws SchemaException {

    	PrismSchema schema = ReportTypeUtil.parseReportConfigurationSchema(report, prismContext);
        PrismContainerDefinition<ReportConfigurationType> definition =  ReportTypeUtil.findReportConfigurationDefinition(schema);
        if (definition == null) {
            //no definition found for container
            throw new SchemaException("Couldn't find definitions for report type " + report + ".");
        }
        Collection<ItemDelta> modifications = delta.getModifications();
        for (ItemDelta itemDelta : modifications){
        	if (itemDelta.hasCompleteDefinition()){
        		continue;
        	}
        	ItemDefinition def = definition.findItemDefinition(itemDelta.getPath().rest());
        	if (def != null){
        		itemDelta.applyDefinition(def);
        	}
        }




    }
}
