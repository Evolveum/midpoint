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

package com.evolveum.midpoint.gui.api.component.form;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Checkbox that is supposed to be used in forms - checkbox with label.
 * 
 * @author lazyman
 * @author Radovan Semancik
 */
public class CheckBoxPanel extends Panel {
	private static final long serialVersionUID = 1L;

	private static final String ID_CONTAINER = "container";
	private static final String ID_CHECK = "check";
	private static final String ID_LABEL = "label";

    public CheckBoxPanel(String id, IModel<Boolean> checkboxModel, final IModel<Boolean> visibilityModel, 
    		IModel<String> labelModel, IModel<String> tooltipModel) {
        super(id);
        
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        add(container);

        AjaxCheckBox check = new AjaxCheckBox(ID_CHECK, checkboxModel) {
        	private static final long serialVersionUID = 1L;

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
        if (visibilityModel != null) {
	        check.add(new VisibleEnableBehaviour() {
	        	private static final long serialVersionUID = 1L;
	
	            @Override
	            public boolean isEnabled() {
	                return visibilityModel.getObject();
	            }
	        });
        }
        container.add(check);
        
		Label label = new Label(ID_LABEL, labelModel);
		label.setRenderBodyOnly(true);
		container.add(label);
        
        if (tooltipModel != null) {
            container.add(new AttributeModifier("title", tooltipModel));
        }
    }

    private AjaxCheckBox getPanelComponent() {
        return (AjaxCheckBox) get(ID_CONTAINER).get(ID_CHECK);
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
