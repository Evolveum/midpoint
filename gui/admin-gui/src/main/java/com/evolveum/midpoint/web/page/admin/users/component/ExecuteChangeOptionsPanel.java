/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class ExecuteChangeOptionsPanel extends BasePanel<ExecuteChangeOptionsDto> {
	private static final long serialVersionUID = 1L;

	private static final String ID_FORCE = "force";
    private static final String ID_RECONCILE = "reconcile";
    private static final String ID_RECONCILE_LABEL = "reconcileLabel";
    private static final String ID_RECONCILE_AFFECTED = "reconcileAffected";
    private static final String ID_RECONCILE_AFFECTED_LABEL = "reconcileAffectedLabel";
    private static final String ID_EXECUTE_AFTER_ALL_APPROVALS = "executeAfterAllApprovals";
    private static final String ID_KEEP_DISPLAYING_RESULTS = "keepDisplayingResults";
    private static final String ID_KEEP_DISPLAYING_RESULTS_LABEL = "keepDisplayingResultsLabel";

    private boolean showReconcile;
    private boolean showReconcileAffected;
    private boolean showKeepDisplayingResults;

    public ExecuteChangeOptionsPanel(String id, IModel<ExecuteChangeOptionsDto> model, boolean showReconcile, boolean showReconcileAffected) {
        super(id, model);
        this.showReconcile = showReconcile;
        this.showReconcileAffected = showReconcileAffected;
        showKeepDisplayingResults = getWebApplicationConfiguration().isProgressReportingEnabled();
        initLayout();
    }

    public ExecuteChangeOptionsPanel(String id, IModel<ExecuteChangeOptionsDto> model, boolean showReconcile, boolean showReconcileAffected, boolean showKeepDisplayingResults) {
        super(id, model);
        this.showReconcile = showReconcile;
        this.showReconcileAffected = showReconcileAffected;
        this.showKeepDisplayingResults = showKeepDisplayingResults;
        initLayout();
    }

    private void initLayout() {
        CheckBox force = new CheckBox(ID_FORCE,
                new PropertyModel<Boolean>(getModel(), ExecuteChangeOptionsDto.F_FORCE));
        add(force);

        WebMarkupContainer reconcileLabel = new WebMarkupContainer(ID_RECONCILE_LABEL);
        reconcileLabel.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return showReconcile;
            }

        });
        add(reconcileLabel);

        CheckBox reconcile = new CheckBox(ID_RECONCILE,
                new PropertyModel<Boolean>(getModel(), ExecuteChangeOptionsDto.F_RECONCILE));
        reconcileLabel.add(reconcile);

        WebMarkupContainer reconcileAffectedLabel = new WebMarkupContainer(ID_RECONCILE_AFFECTED_LABEL);
        reconcileAffectedLabel.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return showReconcileAffected;
            }

        });
        add(reconcileAffectedLabel);

        CheckBox reconcileAffected = new CheckBox(ID_RECONCILE_AFFECTED,
                new PropertyModel<Boolean>(getModel(), ExecuteChangeOptionsDto.F_RECONCILE_AFFECTED));
        reconcileAffectedLabel.add(reconcileAffected);

        CheckBox executeAfterAllApprovals = new CheckBox(ID_EXECUTE_AFTER_ALL_APPROVALS,
                new PropertyModel<Boolean>(getModel(), ExecuteChangeOptionsDto.F_EXECUTE_AFTER_ALL_APPROVALS));
        add(executeAfterAllApprovals);

        WebMarkupContainer keepDisplayingResultsLabel = new WebMarkupContainer(ID_KEEP_DISPLAYING_RESULTS_LABEL);
        keepDisplayingResultsLabel.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return showKeepDisplayingResults;
            }

        });
        add(keepDisplayingResultsLabel);

        CheckBox keepDisplayingResults = new CheckBox(ID_KEEP_DISPLAYING_RESULTS,
                new PropertyModel<Boolean>(getModel(), ExecuteChangeOptionsDto.F_KEEP_DISPLAYING_RESULTS));
        keepDisplayingResultsLabel.add(keepDisplayingResults);
    }
}
