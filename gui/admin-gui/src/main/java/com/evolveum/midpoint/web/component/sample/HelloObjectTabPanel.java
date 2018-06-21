/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.web.component.sample;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Sample showing a custom object form that displays simple greeting.
 *
 * @author Radovan Semancik
 *
 */
public class HelloObjectTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {

	private static final String ID_HELLO_LABEL = "helloLabel";

	public HelloObjectTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusModel, PageBase pageBase) {
		super(id, mainForm, focusModel, pageBase);
		initLayout();
	}

	private void initLayout() {
		add(new Label(ID_HELLO_LABEL, new Model<String>() {
			@Override
			public String getObject() {
				PrismObject<F> focus = getObjectWrapper().getObject();
				if (focus != null) {
					PolyStringType name = focus.asObjectable().getName();
					if (name != null) {
						return "Hello "+name.getOrig()+"!";
					}
				}
				return "Hello world!";
			}
		}));
	}

}
