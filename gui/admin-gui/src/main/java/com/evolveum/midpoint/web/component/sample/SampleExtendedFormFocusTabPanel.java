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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.SimpleParametricRoleSelector;
import com.evolveum.midpoint.web.component.assignment.SimpleRoleSelector;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractFocusTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Sample showing a custom focus form that displays semi-static form.
 * This form is using extended attributes and role parameters. It needs extension-samples.xsd.
 * 
 * @author Radovan Semancik
 *
 */
public class SampleExtendedFormFocusTabPanel<F extends FocusType> extends AbstractFocusTabPanel<F> {
	
	private static final String DOT_CLASS = SampleExtendedFormFocusTabPanel.class.getName() + ".";
	private static final String OPERATION_SEARCH_ROLES = DOT_CLASS + "searchRoles";
	
	private static final String ID_HEADER = "header";
	
	private static final String ID_PROP_NAME = "propName";
	private static final String ID_PROP_FULL_NAME = "propFullName";
	private static final String ID_PROP_SSN = "propSsn";
	
	private static final String ID_ROLES_SIMPLE = "rolesSimple";
	private static final String ID_ROLES_DOMAIN = "rolesDomain";
	
	private static final Trace LOGGER = TraceManager.getTrace(SampleExtendedFormFocusTabPanel.class);

	public SampleExtendedFormFocusTabPanel(String id, Form mainForm, 
			LoadableModel<ObjectWrapper<F>> focusWrapperModel, 
			LoadableModel<List<AssignmentEditorDto>> assignmentsModel, 
			LoadableModel<List<FocusProjectionDto>> projectionModel,
			PageBase pageBase) {
		super(id, mainForm, focusWrapperModel, assignmentsModel, projectionModel, pageBase);
		initLayout(focusWrapperModel, assignmentsModel, pageBase);
	}

	private void initLayout(final LoadableModel<ObjectWrapper<F>> focusModel, LoadableModel<List<AssignmentEditorDto>> assignmentsModel, PageBase pageBase) {
		add(new Label(ID_HEADER, "Object details"));
		WebMarkupContainer body = new WebMarkupContainer("body");
		add(body);
		
		addPrismPropertyPanel(body, ID_PROP_NAME, FocusType.F_NAME);
		addPrismPropertyPanel(body, ID_PROP_FULL_NAME, UserType.F_FULL_NAME);
		addPrismPropertyPanel(body, ID_PROP_SSN, new ItemPath(ObjectType.F_EXTENSION, SchemaConstants.SAMPLES_SSN));
		
		// TODO: create proxy for these operations
		Task task = pageBase.createSimpleTask(OPERATION_SEARCH_ROLES);
		List<PrismObject<RoleType>> availableSimpleRoles;
		try {
			ObjectQuery simpleRoleQuery = ObjectQuery.createObjectQuery(
					EqualFilter.createEqual(RoleType.F_ROLE_TYPE, RoleType.class, getPrismContext(), "simple"));
			availableSimpleRoles = pageBase.getModelService().searchObjects(RoleType.class, simpleRoleQuery, null, task, task.getResult());
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException e) {
			task.getResult().recordFatalError(e);
			LoggingUtils.logException(LOGGER, "Couldn't load roles", e);
			availableSimpleRoles = new ArrayList<>();
			// TODO: better errror reporting
		}
		
		add(new SimpleRoleSelector<F,RoleType>(ID_ROLES_SIMPLE, assignmentsModel, availableSimpleRoles));
		
		List<PrismObject<RoleType>> availableDomainRoles;
		try {
			ObjectQuery simpleRoleQuery = ObjectQuery.createObjectQuery(
					EqualFilter.createEqual(RoleType.F_ROLE_TYPE, RoleType.class, getPrismContext(), "domain"));
			availableDomainRoles = pageBase.getModelService().searchObjects(RoleType.class, simpleRoleQuery, null, task, task.getResult());
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException e) {
			task.getResult().recordFatalError(e);
			LoggingUtils.logException(LOGGER, "Couldn't load roles", e);
			availableDomainRoles = new ArrayList<>();
			// TODO: better errror reporting
		}
		
		SimpleParametricRoleSelector<F,RoleType> domainRoleSelector = new SimpleParametricRoleSelector<F,RoleType>(ID_ROLES_DOMAIN, assignmentsModel, availableDomainRoles, new ItemPath(ObjectType.F_EXTENSION, SchemaConstants.SAMPLES_DOMAIN));
		domainRoleSelector.setLabelParam("Domain");
		domainRoleSelector.setLabelRole("Active roles for selected domain");
		add(domainRoleSelector);
		
	}

}
