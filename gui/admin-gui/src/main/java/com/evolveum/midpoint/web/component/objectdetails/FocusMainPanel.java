/**
 * Copyright (c) 2015-2016 Evolveum
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
package com.evolveum.midpoint.web.component.objectdetails;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFormType;

/**
 * @author semancik
 *
 */
public class FocusMainPanel<F extends FocusType> extends AbstractObjectMainPanel<F> {

	private LoadableModel<List<FocusProjectionDto>> projectionModel;
	private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;
	
	public FocusMainPanel(String id, LoadableModel<ObjectWrapper<F>> objectModel, 
			LoadableModel<List<AssignmentEditorDto>> assignmentsModel, LoadableModel<List<FocusProjectionDto>> projectionModel, 
			PageAdminFocus<F> parentPage) {
		super(id, objectModel, parentPage);
		Validate.notNull(projectionModel, "Null projection model");
		this.assignmentsModel = assignmentsModel;
		this.projectionModel = projectionModel;
		initLayout(parentPage);
	}

	private void initLayout(final PageAdminObjectDetails<F> parentPage) {
		getMainForm().setMultiPart(true);
	}
	
	@Override
	protected List<ITab> createTabs(final PageAdminObjectDetails<F> parentPage) {
		List<ITab> tabs = new ArrayList<>();
		
		List<ObjectFormType> objectFormTypes = parentPage.getObjectFormTypes();
		if (objectFormTypes == null || objectFormTypes.isEmpty()) {
			addDefaultTabs(parentPage, tabs);
			return tabs;
		}
		for (ObjectFormType objectFormType: objectFormTypes) {
			if (BooleanUtils.isTrue(objectFormType.isIncludeDefaultForms())) {
				addDefaultTabs(parentPage, tabs);
				break;
			}
		}
		for (ObjectFormType objectFormType: objectFormTypes) {
			final FormSpecificationType formSpecificationType = objectFormType.getFormSpecification();
			String title = formSpecificationType.getTitle();
			if (title == null) {
				title = "pageAdminFocus.extended";
			}
			tabs.add(
					new AbstractTab(parentPage.createStringResource(title)){
						@Override
						public WebMarkupContainer getPanel(String panelId) {
							return createTabPanel(panelId, formSpecificationType, parentPage); 
						}
					});
		}
				
		return tabs;
	}

	protected WebMarkupContainer createTabPanel(String panelId, FormSpecificationType formSpecificationType,
			PageAdminObjectDetails<F> parentPage) {
		String panelClassName = formSpecificationType.getPanelClass();
		if (panelClassName == null) {
			throw new SystemException("No panel class specified in admin GUI configuration");
		}
		Class<?> panelClass;
		try {
			panelClass = Class.forName(panelClassName);
		} catch (ClassNotFoundException e) {
			throw new SystemException("Panel class '"+panelClassName+"' as specified in admin GUI configuration was not found", e);
		}
		if (AbstractFocusTabPanel.class.isAssignableFrom(panelClass)) {
			Constructor<?> constructor;
			try {
				constructor = panelClass.getConstructor(String.class, Form.class, LoadableModel.class, LoadableModel.class, LoadableModel.class, PageBase.class);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new SystemException("Unable to locate constructor (String,Form,LoadableModel,LoadableModel,LoadableModel,PageBase) in "+panelClass+": "+e.getMessage(), e);
			}
			AbstractFocusTabPanel<F> tabPanel;
			try {
				tabPanel = (AbstractFocusTabPanel<F>) constructor.newInstance(panelId, getMainForm(), getObjectModel(), 
						assignmentsModel, projectionModel ,parentPage);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SystemException("Error instantiating "+panelClass+": "+e.getMessage(), e);
			}
			return tabPanel;
		} else if (AbstractObjectTabPanel.class.isAssignableFrom(panelClass)) {
			Constructor<?> constructor;
			try {
				constructor = panelClass.getConstructor(String.class, Form.class, LoadableModel.class, PageBase.class);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new SystemException("Unable to locate constructor (String,Form,LoadableModel,PageBase) in "+panelClass+": "+e.getMessage(), e);
			}
			AbstractObjectTabPanel<F> tabPanel;
			try {
				tabPanel = (AbstractObjectTabPanel<F>) constructor.newInstance(panelId, getMainForm(), getObjectModel(), parentPage);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SystemException("Error instantiating "+panelClass+": "+e.getMessage(), e);
			}
			return tabPanel;
		
		} else {
			throw new UnsupportedOperationException("Tab panels that are not subclasses of AbstractObjectTabPanel or AbstractFocusTabPanel are not supported yet (got "+panelClass+")");
		}
	}

	protected WebMarkupContainer createFocusDetailsTabPanel(String panelId, PageAdminObjectDetails<F> parentPage) {
		return new FocusDetailsTabPanel<F>(panelId, getMainForm(), getObjectModel(), assignmentsModel, projectionModel, parentPage);
	}
	
	protected WebMarkupContainer createFocusProjectionsTabPanel(String panelId, PageAdminObjectDetails<F> parentPage) {
		return new FocusProjectionsTabPanel<F>(panelId, getMainForm(), getObjectModel(), projectionModel, parentPage);
	}
	
	protected WebMarkupContainer createFocusAssignmentsTabPanel(String panelId, PageAdminObjectDetails<F> parentPage) {
		return new FocusAssignmentsTabPanel<F>(panelId, getMainForm(), getObjectModel(), assignmentsModel, parentPage);
	}

	protected void addDefaultTabs(final PageAdminObjectDetails<F> parentPage, List<ITab> tabs) {
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageAdminFocus.basic")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return createFocusDetailsTabPanel(panelId, parentPage); 
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageAdminFocus.projections")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return createFocusProjectionsTabPanel(panelId, parentPage); 
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageAdminFocus.assignments")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return createFocusAssignmentsTabPanel(panelId, parentPage); 
					}
				});
	}
}
