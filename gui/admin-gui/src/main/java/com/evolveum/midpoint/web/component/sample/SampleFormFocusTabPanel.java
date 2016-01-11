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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismPropertyPanel;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.FocusTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
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
		
		IModel<? extends ItemWrapper> propNameModel = new Model<PropertyWrapper>() {
			@Override
			public PropertyWrapper getObject() {
				return (PropertyWrapper) focusModel.getObject().findMainContainerWrapper().findPropertyWrapper(FocusType.F_NAME);
			}
		};
		PrismPropertyPanel propNamePanel = new PrismPropertyPanel(ID_PROP_NAME, propNameModel, getMainForm(), pageBase);
		body.add(propNamePanel);
		
		IModel<? extends ItemWrapper> propFullNameModel = new Model<PropertyWrapper>() {
			@Override
			public PropertyWrapper getObject() {
				return (PropertyWrapper) focusModel.getObject().findMainContainerWrapper().findPropertyWrapper(UserType.F_FULL_NAME);
			}
		};
		PrismPropertyPanel propFullNamePanel = new PrismPropertyPanel(ID_PROP_FULL_NAME, propFullNameModel, getMainForm(), pageBase);
		body.add(propFullNamePanel);
		
	}

}
