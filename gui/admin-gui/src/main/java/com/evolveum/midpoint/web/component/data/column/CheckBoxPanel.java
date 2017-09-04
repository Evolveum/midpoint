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
        this(id, model, new Model<>(true));
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

    public boolean getValue() {
    	Boolean val = getPanelComponent().getModelObject();
    	if (val == null) {
    		return false;
    	}

    	return val.booleanValue();
    }
}
