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
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.FocusTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Sample showing a custom focus form that displays semi-static form.
 * 
 * @author Radovan Semancik
 *
 */
public class SampleFormFocusTabPanel<F extends FocusType> extends FocusTabPanel<F> {
	
	private static final String ID_HEADER = "header";
	private static final String ID_BODY = "body";

	public SampleFormFocusTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusModel, PageBase pageBase) {
		super(id, mainForm, focusModel, pageBase);
		initLayout();
	}

	private void initLayout() {
		add(new Label(ID_HEADER, "Object details"));
		WebMarkupContainer body = new WebMarkupContainer("body");
		add(body);
	}

}
