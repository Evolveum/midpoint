/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.reports.component.AuditLogViewerPanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;

import com.evolveum.midpoint.web.session.AuditLogStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CollectionRefSpecificationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.checkerframework.common.value.qual.StringVal;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/admin/auditLogViewer", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL, label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_AUDIT_LOG_VIEWER_URL, label = "PageAuditLogViewer.auth.auditLogViewer.label", description = "PageAuditLogViewer.auth.auditLogViewer.description") })
public class PageAuditLogViewer extends PageBase {

    private static final transient Trace LOGGER = TraceManager.getTrace(PageAuditLogViewer.class);

    private static final long serialVersionUID = 1L;
    private static final String ID_PANEL = "auditLogViewerPanel";

    public static final String PARAM_DASHBOARD = "dashboard";
    public static final String PARAM_DASHBOARD_WIDGET = "widget";

    private static final String OPERATION_LOAD_DASHBOARD = PageAuditLogViewer.class.getSimpleName() + ".loadDashboard";

    private IModel<CollectionRefSpecificationType> collectionRefSpecificationModel;

    public PageAuditLogViewer() {
        this(null);
    }

    public PageAuditLogViewer(PageParameters params) {
        if (params != null) {
            getPageParameters().overwriteWith(params);
        }

        collectionRefSpecificationModel = new LoadableModel<CollectionRefSpecificationType>(false) {

            @Override
            protected CollectionRefSpecificationType load() {
                String dasbordOid = getParamValue(PARAM_DASHBOARD);
                if (StringUtils.isBlank(dasbordOid)) {
                    return null;
                }

                String widgetName = getParamValue(PARAM_DASHBOARD_WIDGET);
                if (StringUtils.isBlank(widgetName)) {
                    return null;
                }

                Task task = PageAuditLogViewer.this.createSimpleTask(OPERATION_LOAD_DASHBOARD);
                OperationResult result = task.getResult();
                PrismObject<DashboardType> dashboard = WebModelServiceUtils.loadObject(DashboardType.class, dasbordOid, PageAuditLogViewer.this, task, result);
                result.computeStatusIfUnknown();

                if (dashboard == null) {
                    showResult(result, false);
                    return null;
                }

                DashboardType dashboardType = dashboard.asObjectable();
                List<DashboardWidgetType> dashboardWidgetTypeList = dashboardType.getWidget()
                        .stream()
                        .filter(widget -> widget.getIdentifier().equalsIgnoreCase(widgetName))
                        .collect(Collectors.toList());
                if (dashboardWidgetTypeList.size() != 1) {
                    LOGGER.error("Found {} widgets with name {}", dashboardWidgetTypeList.size(), widgetName);
                    result.recordFatalError("Found " + dashboardWidgetTypeList.size() + " widgets with name " + widgetName);
                    showResult(result);
                    return null;
                }

                DashboardWidgetType widgetType = dashboardWidgetTypeList.iterator().next();
                CollectionRefSpecificationType collectionRefSpecificationType = getDashboardService().getCollectionRefSpecificationType(widgetType, task, result);
                result.computeStatusIfUnknown();
                showResult(result, false);
                return  collectionRefSpecificationType;
            }
        };
    }

    private String getParamValue(String paramName) {
        StringValue paramValue = getPageParameters().get(paramName);
        if (paramValue == null) {
            return null;
        }
        return paramValue.toString();
    }
    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        AuditLogViewerPanel panel = new AuditLogViewerPanel(ID_PANEL, new IModel<AuditSearchDto>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AuditSearchDto getObject() {
                initDefaultSearchDto();
                return getAuditLogStorage().getSearchDto();
            }

            @Override
            public void setObject(AuditSearchDto auditSearchDto) {
                getAuditLogStorage().setSearchDto(auditSearchDto);
            }

            @Override
            public void detach() {

            }
        }, false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected AuditLogStorage getAuditLogStorage(){
                return PageAuditLogViewer.this.getAuditLogStorage();
            }

            @Override
            protected void updateAuditSearchStorage(AuditSearchDto searchDto) {
                getAuditLogStorage().setSearchDto(searchDto);
                getAuditLogStorage().setPageNumber(0);
            }

            @Override
            protected void resetAuditSearchStorage() {
                getAuditLogStorage().setSearchDto(new AuditSearchDto());

            }

        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    private void initDefaultSearchDto(){
        AuditSearchDto auditSearchDto = getSessionStorage().getAuditLog().getSearchDto();
        XMLGregorianCalendar searchFromDate = auditSearchDto.getFrom();
        if (searchFromDate == null){
            Date todayDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
            getSessionStorage().getAuditLog().getSearchDto().setFrom(MiscUtil.asXMLGregorianCalendar(todayDate));
        }
        auditSearchDto.setCollectionRef(collectionRefSpecificationModel.getObject());
    }

    private AuditLogStorage getAuditLogStorage(){
        return getSessionStorage().getAuditLog();
    }


}
