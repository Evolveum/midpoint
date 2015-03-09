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

package com.evolveum.midpoint.web.page.admin.reports.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.io.Serializable;

import org.apache.commons.codec.binary.Base64;

/**
 * @author lazyman
 */
public class ReportDto implements Serializable {

    public static final String F_PARENT = "parent";
    public static final String F_OID = "oid";
    public static final String F_XML = "xml";
    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_EXPORT_TYPE = "exportType";

    private boolean parent;
    private String oid;
//    private String xml;
    private String name;
    private String description;
    private boolean searchOnResource;
    private ExportType exportType;
    private JasperReportDto jasperReportDto;
    private byte[] templateStyle;
    
//    private PrismObject<ReportType> object;
    

    private ReportType reportType;
    
    public ReportDto() {
    }
    
    public ReportDto(ReportType reportType){
    	this.oid = reportType.getOid();
    	this.name = reportType.getName().getOrig();
    	this.exportType = reportType.getExport();
    	this.searchOnResource = false;
    	this.description = reportType.getDescription();
//    	this.xml = new String(Base64.decodeBase64(reportType.getTemplate()));
    	this.jasperReportDto = new JasperReportDto(reportType.getTemplate());
    	this.templateStyle = reportType.getTemplateStyle();
    	this.parent = reportType.isParent();
    	this.reportType = reportType;
    }

    public ReportDto(String name, String description) {
        this.description = description;
        this.name = name;
    }

    public ReportDto(String name, String description, ExportType export, boolean parent) {
        this.name = name;
        this.description = description;
//        this.xml = xml;
        this.exportType = export;
        this.parent = parent;
    }
    
    

    public boolean isParent() {
        return parent;
    }

    public void setParent(boolean parent) {
        this.parent = parent;
    }

    public PrismObject<ReportType> getObject() {
    	reportType.setName(new PolyStringType(name));
    	reportType.setExport(exportType);
    	reportType.setTemplate(jasperReportDto.getTemplate());
    	reportType.setTemplateStyle(templateStyle);
    	reportType.setDescription(description);
        return reportType.asPrismObject();
    }

    public void setObject(PrismObject<ReportType> object) {
       this.reportType = object.asObjectable();
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

//    public String getXml() {
//        return xml;
//    }
//
//    public void setXml(String xml) {
//        this.xml = xml;
//    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExportType getExportType() {
        return exportType;
    }

    public void setExportType(ExportType exportType) {
        this.exportType = exportType;
    }
    
    public JasperReportDto getJasperReportDto() {
		return jasperReportDto;
	}
}
