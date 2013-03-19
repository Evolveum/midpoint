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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lazyman
 */
public class CheckBoxPanel extends Panel {

    private static final String ID_CHECK = "check";

    public CheckBoxPanel(String id, IModel<Boolean> model) {
        this(id, model, new Model<Boolean>(true));
    }

    public CheckBoxPanel(String id, IModel<Boolean> model, final IModel<Boolean> enabled) {
        super(id);
        AjaxCheckBox check = new AjaxCheckBox(ID_CHECK, model) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                CheckBoxPanel.this.onUpdate(target);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                CheckBoxPanel.this.updateAjaxAttributes(attributes);
            }
        };
        check.setOutputMarkupId(true);
        check.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isEnabled() {
                return enabled.getObject();
            }
        });

        add(check);
    }

    public AjaxCheckBox getPanelComponent() {
        return (AjaxCheckBox) get(ID_CHECK);
    }

    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
    }

    public void onUpdate(AjaxRequestTarget target) {
    }
}
