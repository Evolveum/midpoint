/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

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

    private final boolean showReconcile;
    private final boolean showReconcileAffected;
    private final boolean showKeepDisplayingResults;

    public ExecuteChangeOptionsPanel(String id, IModel<ExecuteChangeOptionsDto> model,
            boolean showReconcile, boolean showReconcileAffected) {
        super(id, model);
        this.showReconcile = showReconcile;
        this.showReconcileAffected = showReconcileAffected;
        showKeepDisplayingResults = getWebApplicationConfiguration().isProgressReportingEnabled();
        initLayout();
    }

    public ExecuteChangeOptionsPanel(String id, IModel<ExecuteChangeOptionsDto> model,
            boolean showReconcile, boolean showReconcileAffected, boolean showKeepDisplayingResults) {
        super(id, model);
        this.showReconcile = showReconcile;
        this.showReconcileAffected = showReconcileAffected;
        this.showKeepDisplayingResults = showKeepDisplayingResults;
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
    }

    private void createContainer(String containerId, IModel<Boolean> checkboxModel, String labelKey, String helpKey, boolean show) {
    	CheckBoxPanel panel = new CheckBoxPanel(containerId, checkboxModel, null, createStringResource(labelKey), createStringResource(helpKey));
    	panel.setVisible(show);
        add(panel);
    }
}
