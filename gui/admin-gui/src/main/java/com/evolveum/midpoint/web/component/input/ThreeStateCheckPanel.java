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

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.threeStateCheckBox.ThreeCheckState;
import com.evolveum.midpoint.web.component.threeStateCheckBox.ThreeStateCheckBox;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * @author mserbak
 */
public class ThreeStateCheckPanel extends InputPanel {

    public ThreeStateCheckPanel(String id, IModel<Boolean> model) {
        super(id);

        ThreeStateCheckBox check = new ThreeStateCheckBox("input", checkThreeState(model));
        add(check);
    }
    
    private IModel<ThreeCheckState> checkThreeState(final IModel<Boolean> model) {
    	return new AbstractReadOnlyModel<ThreeCheckState>(){

			@Override
			public ThreeCheckState getObject() {
				if(model.getObject() == null) {
					return ThreeCheckState.UNDEFINED;
				} else if(model.getObject()) {
					return ThreeCheckState.CHECKED;
				} else {
					return ThreeCheckState.UNCHECKED;
				}
			}
    	};
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get("input");
    }
}
