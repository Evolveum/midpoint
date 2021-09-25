/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class ExecuteChangeOptionsPanel extends BasePanel<ExecuteChangeOptionsDto> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ExecuteChangeOptionsPanel.class);

    private static final String ID_FORCE_CONTAINER = "forceContainer";
    private static final String ID_RECONCILE_CONTAINER = "reconcileContainer";
    private static final String ID_RECONCILE_AFFECTED_CONTAINER = "reconcileAffectedContainer";
    private static final String ID_EXECUTE_AFTER_ALL_APPROVALS_CONTAINER = "executeAfterAllApprovalsContainer";
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

    private final LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel;

    public ExecuteChangeOptionsPanel(String id) {
        super(id);

         executeOptionsModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ExecuteChangeOptionsDto load() {
                return ExecuteChangeOptionsDto.createFromSystemConfiguration();
            }
        };
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    @Override
    public IModel<ExecuteChangeOptionsDto> getModel() {
        return executeOptionsModel;
    }

    @Override
    public ExecuteChangeOptionsDto getModelObject() {
        return executeOptionsModel.getObject();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        createContainer(ID_FORCE_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_FORCE),
                FORCE_LABEL,
                FORCE_HELP);

        createContainer(ID_RECONCILE_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_RECONCILE),
                RECONCILE_LABEL,
                RECONCILE_HELP);

        createContainer(ID_RECONCILE_AFFECTED_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_RECONCILE_AFFECTED),
                RECONCILE_AFFECTED_LABEL,
                RECONCILE_AFFECTED_HELP);

        createContainer(ID_EXECUTE_AFTER_ALL_APPROVALS_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_EXECUTE_AFTER_ALL_APPROVALS),
                EXECUTE_AFTER_ALL_APPROVALS_LABEL,
                EXECUTE_AFTER_ALL_APPROVALS_HELP);

        createContainer(ID_KEEP_DISPLAYING_RESULTS_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_KEEP_DISPLAYING_RESULTS),
                KEEP_DISPLAYING_RESULTS_LABEL,
                KEEP_DISPLAYING_RESULTS_HELP);

        createContainer(ID_SAVE_IN_BACKGROUND_CONTAINER,
                new PropertyModel<>(getModel(), ExecuteChangeOptionsDto.F_SAVE_IN_BACKGROUND),
                SAVE_IN_BACKGROUND_LABEL,
                SAVE_IN_BACKGROUND_HELP);

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

    private void createContainer(String containerId, IModel<Boolean> checkboxModel, String labelKey, String helpKey) {

        AjaxButton button = new AjaxButton(containerId, createStringResource(labelKey)) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                checkboxModel.setObject(!checkboxModel.getObject());
                ajaxRequestTarget.add(ExecuteChangeOptionsPanel.this);
            }
        };
        button.add(AttributeAppender.append("title", createStringResource(helpKey)));
        button.add(AttributeAppender.append("class", new ReadOnlyModel<>(() -> checkboxModel.getObject() ? " active" : "")));
        button.setOutputMarkupId(true);
        add(button);
    }

    protected void reloadPanelOnOptionsUpdate(AjaxRequestTarget target) {
    }

}
