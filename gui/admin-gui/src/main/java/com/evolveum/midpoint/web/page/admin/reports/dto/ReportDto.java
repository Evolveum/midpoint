/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.dto;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.BooleanUtils;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

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
    public static final String F_VIRTUALIZER = "virtualizer";
    public static final String F_VIRTUALIZER_KICKON = "virtualizerKickOn";
    public static final String F_MAXPAGES = "maxPages";
    public static final String F_TIMEOUT = "timeout";
    public static final String F_DASHBOARD_REF = "dashboardRef";

    private boolean parent;
    private String oid;
//    private String xml;
    private String name;
    private String description;
    private boolean searchOnResource;
    private JasperExportType exportType;
    private JasperReportDto jasperReportDto;
    private byte[] templateStyle;
    private String virtualizer;
    private Integer virtualizerKickOn;
    private Integer maxPages;
    private Integer timeout;
//    private ReportEngineSelectionType reportEngineType;
    private IModel<PrismObjectWrapper<ReportType>> newReportModel;

//    private PrismObject<ReportType> object;
    private ReportType reportType;

    public ReportDto() {
    }

    public ReportDto(byte[] reportJrxml) {
        this.jasperReportDto = new JasperReportDto(reportJrxml);
    }

    public ReportDto(ReportType reportType, boolean onlyForPromptingParams) {
        this.oid = reportType.getOid();
        this.name = reportType.getName().getOrig();
        this.description = reportType.getDescription();
        this.reportType = reportType;
        this.searchOnResource = false;
//        this.reportEngineType = reportType.getReportEngine();
        if(reportType.getJasper() != null) {
            JasperReportEngineConfigurationType jasperConfig = reportType.getJasper();
            this.exportType = jasperConfig.getExport();
            //        this.xml = new String(Base64.decodeBase64(reportType.getTemplate()));
            this.jasperReportDto = new JasperReportDto(jasperConfig.getTemplate(), onlyForPromptingParams);
            this.templateStyle = jasperConfig.getTemplateStyle();
            this.parent = !BooleanUtils.isFalse(jasperConfig.isParent());
            this.virtualizer = jasperConfig.getVirtualizer();
            this.virtualizerKickOn = jasperConfig.getVirtualizerKickOn();
            this.maxPages = jasperConfig.getMaxPages();
            this.timeout = jasperConfig.getTimeout();

        } else {
            this.reportType = reportType;
        }
    }

    public ReportDto(ReportType reportType) {
        this(reportType, false);
    }

    public ReportDto(String name, String description) {
        this.description = description;
        this.name = name;
    }

    public ReportDto(String name, String description, JasperExportType export, boolean parent) {
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
        if (reportType == null) {
            reportType = new ReportType();
        }
        reportType.setName(new PolyStringType(name));
        reportType.setDescription(description);
//        reportType.setReportEngine(reportEngineType);
        if(reportType.getJasper() != null) {

            JasperReportEngineConfigurationType jasperConfig = new JasperReportEngineConfigurationType();
            jasperConfig.setExport(exportType);
            jasperConfig.setTemplate(jasperReportDto.getTemplate());
            jasperConfig.setTemplateStyle(templateStyle);
            jasperConfig.setVirtualizer(virtualizer);
            jasperConfig.setVirtualizerKickOn(virtualizerKickOn);
            jasperConfig.setMaxPages(maxPages);
            jasperConfig.setTimeout(timeout);
            reportType.setJasper(jasperConfig);
        }

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

//    public ReportEngineSelectionType getReportEngineType() {
//        return reportEngineType;
//    }
//
//    public void setReportEngineType(ReportEngineSelectionType reportEngineType) {
//        this.reportEngineType = reportEngineType;
//    }
//
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

    public JasperExportType getExportType() {
        return exportType;
    }

    public void setExportType(JasperExportType exportType) {
        this.exportType = exportType;
    }

    public JasperReportDto getJasperReportDto() {
        return jasperReportDto;
    }

    public String getVirtualizer() {
        return virtualizer;
    }

    public void setVirtualizer(String virtualizer) {
        this.virtualizer = virtualizer;
    }

    public Integer getVirtualizerKickOn() {
        return virtualizerKickOn;
    }

    public void setVirtualizerKickOn(Integer virtualizerKickOn) {
        this.virtualizerKickOn = virtualizerKickOn;
    }

    public Integer getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(Integer maxPages) {
        this.maxPages = maxPages;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public IModel<PrismObjectWrapper<ReportType>> getNewReportModel() {
        return newReportModel;
    }

    public void setNewReportModel(IModel<PrismObjectWrapper<ReportType>> newReportModel) {
        this.newReportModel = newReportModel;
    }
}
