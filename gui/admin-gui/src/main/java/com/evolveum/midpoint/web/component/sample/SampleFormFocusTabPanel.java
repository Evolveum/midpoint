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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;

import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractFocusTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Sample showing a custom focus form that displays semi-static form.
 * 
 * @author Radovan Semancik
 *
 */
public class SampleFormFocusTabPanel<F extends FocusType> extends AbstractFocusTabPanel<F> {
	
	private static final String ID_HEADER = "header";
	private static final String ID_BODY = "body";
	private static final String ID_PROP_NAME = "propName";
	private static final String ID_PROP_FULL_NAME = "propFullName";

	public SampleFormFocusTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusModel, PageBase pageBase) {
		super(id, mainForm, focusModel, pageBase);
		initLayout(focusModel, pageBase);
	}

	private void initLayout(final LoadableModel<ObjectWrapper<F>> focusModel, PageBase pageBase) {
		add(new Label(ID_HEADER, "Object details"));
		WebMarkupContainer body = new WebMarkupContainer("body");
		add(body);
		
		addPrismPropertyPanel(body, ID_PROP_NAME, FocusType.F_NAME);
		addPrismPropertyPanel(body, ID_PROP_FULL_NAME, UserType.F_FULL_NAME);
		
	}

}
