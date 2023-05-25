/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;

/**
 * @author lazyman
 */
public class ExecuteChangeOptionsPanel extends BasePanel<ExecuteChangeOptionsDto> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ExecuteChangeOptionsPanel.class);

    private static final String ID_FORCE = "force";
    private static final String ID_FORCE_CONTAINER = "forceContainer";
    private static final String ID_RECONCILE = "reconcile";
    private static final String ID_RECONCILE_CONTAINER = "reconcileContainer";
    private static final String ID_EXECUTE_AFTER_ALL_APPROVALS = "executeAfterAllApprovals";
    private static final String ID_EXECUTE_AFTER_ALL_APPROVALS_CONTAINER = "executeAfterAllApprovalsContainer";
    private static final String ID_KEEP_DISPLAYING_RESULTS = "keepDisplayingResults";
    private static final String ID_KEEP_DISPLAYING_RESULTS_CONTAINER = "keepDisplayingResultsContainer";
    private static final String ID_TRACING = "tracing";
    private static final String ID_TRACING_CONTAINER = "tracingContainer";
    private static final String ID_SAVE_IN_BACKGROUND_CONTAINER = "saveInBackgroundContainer";

    private static final String FORCE_LABEL = "ExecuteChangeOptionsPanel.label.force";
    private static final String FORCE_HELP = "ExecuteChangeOptionsPanel.label.force.help";
    private static final String RECONCILE_LABEL = "ExecuteChangeOptionsPanel.label.reconcile";
    private static final String RECONCILE_HELP = "ExecuteChangeOptionsPanel.label.reconcile.help";
    private static final String EXECUTE_AFTER_ALL_APPROVALS_LABEL = "ExecuteChangeOptionsPanel.label.executeAfterAllApprovals";
    private static final String EXECUTE_AFTER_ALL_APPROVALS_HELP = "ExecuteChangeOptionsPanel.label.executeAfterAllApprovals.help";
    private static final String KEEP_DISPLAYING_RESULTS_LABEL = "ExecuteChangeOptionsPanel.label.keepDisplayingResults";
    private static final String KEEP_DISPLAYING_RESULTS_HELP = "ExecuteChangeOptionsPanel.label.keepDisplayingResults.help";
    private static final String SAVE_IN_BACKGROUND_LABEL = "ExecuteChangeOptionsPanel.label.saveInBackgroundLabel";
    private static final String SAVE_IN_BACKGROUND_HELP = "ExecuteChangeOptionsPanel.label.saveInBackground.help";

    private final boolean showReconcile;
    private final boolean showKeepDisplayingResults;

    public ExecuteChangeOptionsPanel(String id, IModel<ExecuteChangeOptionsDto> model,
            boolean showReconcile, boolean ignored) {
        super(id, model);
        this.showReconcile = showReconcile;
        showKeepDisplayingResults = getWebApplicationConfiguration().isProgressReportingEnabled();
    }

    public ExecuteChangeOptionsPanel(String id, IModel<ExecuteChangeOptionsDto> model,
            boolean showReconcile, boolean ignored, boolean showKeepDisplayingResults) {
        super(id, model);
        this.showReconcile = showReconcile;
        this.showKeepDisplayingResults = showKeepDisplayingResults;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        createContainer(ID_FORCE_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_FORCE),
                FORCE_LABEL,
                FORCE_HELP,
                true);

        createContainer(ID_RECONCILE_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_RECONCILE),
                RECONCILE_LABEL,
                RECONCILE_HELP,
                showReconcile);

        createContainer(ID_EXECUTE_AFTER_ALL_APPROVALS_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_EXECUTE_AFTER_ALL_APPROVALS),
                EXECUTE_AFTER_ALL_APPROVALS_LABEL,
                EXECUTE_AFTER_ALL_APPROVALS_HELP,
                true);

        createContainer(ID_KEEP_DISPLAYING_RESULTS_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_KEEP_DISPLAYING_RESULTS),
                KEEP_DISPLAYING_RESULTS_LABEL,
                KEEP_DISPLAYING_RESULTS_HELP,
                showKeepDisplayingResults,
                true);

        createContainer(ID_SAVE_IN_BACKGROUND_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_SAVE_IN_BACKGROUND),
                SAVE_IN_BACKGROUND_LABEL,
                SAVE_IN_BACKGROUND_HELP,
                showKeepDisplayingResults,
                true);

        boolean canRecordTrace;
        try {
            canRecordTrace = getPageBase().isAuthorized(ModelAuthorizationAction.RECORD_TRACE.getUrl());
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check trace recording authorization", t);
            canRecordTrace = false;
        }

        WebMarkupContainer tracingContainer = new WebMarkupContainer(ID_TRACING_CONTAINER);
        tracingContainer.setVisible(canRecordTrace && WebModelServiceUtils.isEnableExperimentalFeature(getPageBase()));
        add(tracingContainer);

        DropDownChoice tracing = new DropDownChoice<>(ID_TRACING, PropertyModel.of(getModel(), ExecuteChangeOptionsDto.F_TRACING),
                PropertyModel.of(getModel(), ExecuteChangeOptionsDto.F_TRACING_CHOICES), new IChoiceRenderer<TracingProfileType>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(TracingProfileType profile) {
                if (profile == null) {
                    return "(none)";
                } else if (profile.getDisplayName() != null) {
                    return profile.getDisplayName();
                } else if (profile.getName() != null) {
                    return profile.getName();
                } else {
                    return "(unnamed profile)";
                }
            }

            @Override
            public String getIdValue(TracingProfileType object, int index) {
                return String.valueOf(index);
            }

            @Override
            public TracingProfileType getObject(String id, IModel<? extends List<? extends TracingProfileType>> choices) {
                return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
            }
        });
        tracing.setNullValid(true);
        tracingContainer.add(tracing);
    }

    private void createContainer(String containerId, IModel<Boolean> checkboxModel, String labelKey, String helpKey, boolean show) {
        createContainer(containerId, checkboxModel, labelKey, helpKey, show, false);
    }
    private void createContainer(String containerId, IModel<Boolean> checkboxModel, String labelKey, String helpKey, boolean show,
            final boolean reloadPanelOnUpdate) {
        CheckBoxPanel panel = new CheckBoxPanel(containerId, checkboxModel, createStringResource(labelKey),
                createStringResource(helpKey, WebComponentUtil.getMidpointCustomSystemName(getPageBase(), "midPoint"))){

            private static final long serialVersionUID = 1L;

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                if (reloadPanelOnUpdate){
                    target.add(ExecuteChangeOptionsPanel.this);
                    reloadPanelOnOptionsUpdate(target);
                }
            }

            @Override
            protected boolean isCheckboxEnabled(){
                return isCheckBoxEnabled(containerId);
            }

        };
        panel.setOutputMarkupId(true);
        panel.setVisible(show);
        add(panel);
    }

    private boolean isCheckBoxEnabled(String componentId){
        if (ID_KEEP_DISPLAYING_RESULTS_CONTAINER.equals(componentId)){
            return !getModelObject().isSaveInBackground();
        } else if (ID_SAVE_IN_BACKGROUND_CONTAINER.equals(componentId)){
            return !getModelObject().isKeepDisplayingResults();
        }
        return true;
    }

    protected void reloadPanelOnOptionsUpdate(AjaxRequestTarget target) {
    }

    private CheckBoxPanel getKeepDisplayingResultsPanel(){
        return (CheckBoxPanel) get(ID_KEEP_DISPLAYING_RESULTS_CONTAINER);
    }

    private CheckBoxPanel getSaveInBackgroundPanel(){
        return (CheckBoxPanel) get(ID_KEEP_DISPLAYING_RESULTS_CONTAINER);
    }
}
