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

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Isolated checkbox - checkbox that is displayed as (visually) stand-alone component.
 * This checkbox is not supposed to have any labels associated with it.
 * 
 * For checkbox in forms see com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel
 * 
 * @author lazyman
 */
public class IsolatedCheckBoxPanel extends Panel {
	private static final long serialVersionUID = 1L;

	private static final String ID_CHECK = "check";

    public IsolatedCheckBoxPanel(String id, IModel<Boolean> model) {
        this(id, model, new Model<>(true));
    }

    public IsolatedCheckBoxPanel(String id, IModel<Boolean> model, final IModel<Boolean> enabled) {
        super(id);

        AjaxCheckBox check = new AjaxCheckBox(ID_CHECK, model) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                IsolatedCheckBoxPanel.this.onUpdate(target);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                IsolatedCheckBoxPanel.this.updateAjaxAttributes(attributes);
            }
        };
        check.setOutputMarkupId(true);
        check.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

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
