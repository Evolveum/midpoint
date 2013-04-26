/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @
 */
public class ExecuteChangeOptionsPanel  extends SimplePanel<ExecuteChangeOptionsDto> {

    private static final String ID_FORCE = "force";
    private static final String ID_RECONCILE = "reconcile";
    private static final String ID_EXECUTE_AFTER_ALL_APPROVALS = "executeAfterAllApprovals";

    public ExecuteChangeOptionsPanel(String id, IModel<ExecuteChangeOptionsDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        CheckBox force = new CheckBox(ID_FORCE,
                new PropertyModel<Boolean>(getModel(), ExecuteChangeOptionsDto.F_FORCE));
        add(force);

        CheckBox reconcile = new CheckBox(ID_RECONCILE,
                new PropertyModel<Boolean>(getModel(), ExecuteChangeOptionsDto.F_RECONCILE));
        add(reconcile);

        CheckBox executeAfterAllApprovals = new CheckBox(ID_EXECUTE_AFTER_ALL_APPROVALS,
                new PropertyModel<Boolean>(getModel(), ExecuteChangeOptionsDto.F_EXECUTE_AFTER_ALL_APPROVALS));
        add(executeAfterAllApprovals);
    }
}
