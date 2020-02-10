/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author lazyman
 */
public class ExecuteChangeOptionsPanel extends BasePanel<ExecuteChangeOptionsDto> {
    private static final long serialVersionUID = 1L;

    private static final String ID_FORCE = "force";
    private static final String ID_FORCE_CONTAINER = "forceContainer";
    private static final String ID_RECONCILE = "reconcile";
    private static final String ID_RECONCILE_CONTAINER = "reconcileContainer";
    private static final String ID_RECONCILE_AFFECTED = "reconcileAffected";
    private static final String ID_RECONCILE_AFFECTED_CONTAINER = "reconcileAffectedContainer";
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
    private static final String RECONCILE_AFFECTED_LABEL = "ExecuteChangeOptionsPanel.label.reconcileAffected";
    private static final String RECONCILE_AFFECTED_HELP = "ExecuteChangeOptionsPanel.label.reconcileAffected.help";
    private static final String EXECUTE_AFTER_ALL_APPROVALS_LABEL = "ExecuteChangeOptionsPanel.label.executeAfterAllApprovals";
    private static final String EXECUTE_AFTER_ALL_APPROVALS_HELP = "ExecuteChangeOptionsPanel.label.executeAfterAllApprovals.help";
    private static final String KEEP_DISPLAYING_RESULTS_LABEL = "ExecuteChangeOptionsPanel.label.keepDisplayingResults";
    private static final String KEEP_DISPLAYING_RESULTS_HELP = "ExecuteChangeOptionsPanel.label.keepDisplayingResults.help";
    private static final String SAVE_IN_BACKGROUND_LABEL = "ExecuteChangeOptionsPanel.label.saveInBackgroundLabel";
    private static final String SAVE_IN_BACKGROUND_HELP = "ExecuteChangeOptionsPanel.label.saveInBackground.help";

    private final boolean showReconcile;
    private final boolean showReconcileAffected;
    private final boolean showKeepDisplayingResults;

    public ExecuteChangeOptionsPanel(String id, IModel<ExecuteChangeOptionsDto> model,
            boolean showReconcile, boolean showReconcileAffected) {
        super(id, model);
        this.showReconcile = showReconcile;
        this.showReconcileAffected = showReconcileAffected;
        showKeepDisplayingResults = getWebApplicationConfiguration().isProgressReportingEnabled();
    }

    public ExecuteChangeOptionsPanel(String id, IModel<ExecuteChangeOptionsDto> model,
            boolean showReconcile, boolean showReconcileAffected, boolean showKeepDisplayingResults) {
        super(id, model);
        this.showReconcile = showReconcile;
        this.showReconcileAffected = showReconcileAffected;
        this.showKeepDisplayingResults = showKeepDisplayingResults;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
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

        createContainer(ID_RECONCILE_AFFECTED_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_RECONCILE_AFFECTED),
                RECONCILE_AFFECTED_LABEL,
                RECONCILE_AFFECTED_HELP,
                showReconcileAffected);

        createContainer(ID_EXECUTE_AFTER_ALL_APPROVALS_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_EXECUTE_AFTER_ALL_APPROVALS),
                EXECUTE_AFTER_ALL_APPROVALS_LABEL,
                EXECUTE_AFTER_ALL_APPROVALS_HELP,
                true);

        createContainer(ID_KEEP_DISPLAYING_RESULTS_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_KEEP_DISPLAYING_RESULTS),
                KEEP_DISPLAYING_RESULTS_LABEL,
                KEEP_DISPLAYING_RESULTS_HELP,
                showKeepDisplayingResults);

        createContainer(ID_SAVE_IN_BACKGROUND_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_SAVE_IN_BACKGROUND),
                SAVE_IN_BACKGROUND_LABEL,
                SAVE_IN_BACKGROUND_HELP,
                showKeepDisplayingResults);

        WebMarkupContainer tracingContainer = new WebMarkupContainer(ID_TRACING_CONTAINER);
        tracingContainer.setVisible(WebModelServiceUtils.isEnableExperimentalFeature(getPageBase()));
        add(tracingContainer);

        DropDownChoice tracing = new DropDownChoice<>(ID_TRACING, PropertyModel.of(getModel(), ExecuteChangeOptionsDto.F_TRACING),
                PropertyModel.of(getModel(), ExecuteChangeOptionsDto.F_TRACING_CHOICES), new IChoiceRenderer<TracingProfileType>() {
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
        CheckBoxPanel panel = new CheckBoxPanel(containerId, checkboxModel, null, createStringResource(labelKey),
                createStringResource(helpKey, WebComponentUtil.getMidpointCustomSystemName(getPageBase(), "midPoint")));
        panel.setVisible(show);
        add(panel);
    }
}
