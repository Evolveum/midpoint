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

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @lazyman
 */
public class ExecuteChangeOptionsPanel extends SimplePanel<ExecuteChangeOptionsDto> {

    private static final String ID_FORCE = "force";
    private static final String ID_RECONCILE = "reconcile";
    private static final String ID_RECONCILE_LABEL = "reconcileLabel";
    private static final String ID_EXECUTE_AFTER_ALL_APPROVALS = "executeAfterAllApprovals";
    private static final String ID_KEEP_DISPLAYING_RESULTS = "keepDisplayingResults";

    private boolean showReconcile;

    public ExecuteChangeOptionsPanel(String id, IModel<ExecuteChangeOptionsDto> model, boolean showReconcile) {
        super(id, model);
        this.showReconcile = showReconcile;
    }

    @Override
    protected void initLayout() {
        CheckBox force = new CheckBox(ID_FORCE,
                new PropertyModel<Boolean>(getModel(), ExecuteChangeOptionsDto.F_FORCE));
        add(force);

        WebMarkupContainer reconcileLabel = new WebMarkupContainer(ID_RECONCILE_LABEL);
        reconcileLabel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return showReconcile;
            }

        });
        add(reconcileLabel);

        CheckBox reconcile = new CheckBox(ID_RECONCILE,
                new PropertyModel<Boolean>(getModel(), ExecuteChangeOptionsDto.F_RECONCILE));
        reconcileLabel.add(reconcile);

        CheckBox executeAfterAllApprovals = new CheckBox(ID_EXECUTE_AFTER_ALL_APPROVALS,
                new PropertyModel<Boolean>(getModel(), ExecuteChangeOptionsDto.F_EXECUTE_AFTER_ALL_APPROVALS));
        add(executeAfterAllApprovals);

        CheckBox keepDisplayingResults = new CheckBox(ID_KEEP_DISPLAYING_RESULTS,
                new PropertyModel<Boolean>(getModel(), ExecuteChangeOptionsDto.F_KEEP_DISPLAYING_RESULTS));
        add(keepDisplayingResults);
    }
}
