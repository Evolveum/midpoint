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

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.threeStateCheckBox.ThreeCheckState;
import com.evolveum.midpoint.web.component.threeStateCheckBox.ThreeStateCheckBox;

/**
 * @author mserbak
 */
public class ThreeStateCheckPanel extends InputPanel {

	public ThreeStateCheckPanel(String id, IModel<Boolean> model) {
		super(id);

		ThreeStateCheckBox check = new ThreeStateCheckBox("input", checkThreeState(model));
		add(check);
	}

	private IModel<String> checkThreeState(final IModel<Boolean> model) {
		return new Model<String>() {
			@Override
			public String getObject() {
				String object = "";
				if (model.getObject() == null) {
					object = ThreeCheckState.UNDEFINED.toString();
				} else if (model.getObject()) {
					object = ThreeCheckState.CHECKED.toString();
				} else {
					object = ThreeCheckState.UNCHECKED.toString();
				}
				return object;
			}
		};
	}

	@Override
	public FormComponent getBaseFormComponent() {
		return (FormComponent) get("input");
	}
}
