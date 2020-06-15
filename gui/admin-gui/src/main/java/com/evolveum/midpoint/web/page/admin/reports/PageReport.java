/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.ObjectBasicPanel;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.progress.ProgressPanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.reports.component.EngineReportTabPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.ReportMainPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/report", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORT_URL,
                label = "PageReport.auth.report.label",
                description = "PageReport.auth.report.description")})
public class PageReport extends PageAdminObjectDetails<ReportType> {

    private static final Trace LOGGER = TraceManager.getTrace(PageReport.class);

    private Boolean runReport = false;
    private String oid;

    public PageReport() {
        initialize(null);
    }

    public PageReport(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }

    public PageReport(final PrismObject<ReportType> userToEdit) {
        initialize(userToEdit);
    }

    public PageReport(final PrismObject<ReportType> unitToEdit, boolean isNewObject)  {
        initialize(unitToEdit, isNewObject);
    }

    public PageReport(final PrismObject<ReportType> unitToEdit, boolean isNewObject, boolean isReadonly)  {
        initialize(unitToEdit, isNewObject, isReadonly);
    }

    @Override
    public Class<ReportType> getCompileTimeClass() {
        return ReportType.class;
    }

    @Override
    protected ReportType createNewObject() {
        return new ReportType(getPrismContext());
    }

    @Override
    protected ObjectSummaryPanel<ReportType> createSummaryPanel(IModel<ReportType> summaryModel) {
        return new ReportSummaryPanel(ID_SUMMARY_PANEL, summaryModel, this);
    }

    @Override
    protected Class<? extends Page> getRestartResponsePage() {
        return PageReport.class;
    }

    @Override
    public void continueEditing(AjaxRequestTarget target) {
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, boolean returningFromAsync, OperationResult result) {
        if(runReport && !result.isError()) {
            showResult(result);
            Task task = createSimpleTask("run_task");
            try {
                PrismObject<ReportType> report = (PrismObject<ReportType>) executedDeltas.iterator().next().getObjectDelta().getObjectToAdd();
                getReportManager().runReport(report, null, task, result);
            } catch (Exception ex) {
                result.recordFatalError(ex);
            } finally {
                result.computeStatusIfUnknown();
            }

            showResult(result);
            if (!isKeepDisplayingResults()) {
                redirectBack();
            } else {
                target.add(getFeedbackPanel());
            }
            this.runReport = false;
        } else if (!isKeepDisplayingResults()) {
            showResult(result);
            redirectBack();
        }
    }

    @Override
    protected AbstractObjectMainPanel<ReportType> createMainPanel(String id) {
        return new ReportMainPanel(id, getObjectModel(), this);
    }

    public void saveAndRunPerformed(AjaxRequestTarget target){
        this.runReport = true;
        savePerformed(target);
    }

}
