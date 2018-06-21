/**
 * Copyright (c) 2016-2018 Evolveum
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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.SimpleRoleSelector;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractFocusTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import org.apache.wicket.model.Model;

/**
 * Sample showing a custom focus form that displays semi-static form.
 *
 * @author Radovan Semancik
 *
 */
public class SampleFormFocusTabPanel<F extends FocusType> extends AbstractFocusTabPanel<F> {
	private static final long serialVersionUID = 1L;
	
	private static final String DOT_CLASS = SampleFormFocusTabPanel.class.getName() + ".";
    private static final String OPERATION_SEARCH_ROLES = DOT_CLASS + "searchRoles";

    private static final String ID_HEADER = "header";

    private static final String ID_PROP_NAME = "propName";
    private static final String ID_PROP_FULL_NAME = "propFullName";

    private static final String ID_ROLES = "roles";

    private static final Trace LOGGER = TraceManager.getTrace(SampleFormFocusTabPanel.class);

    public SampleFormFocusTabPanel(String id, Form mainForm,
                                   LoadableModel<ObjectWrapper<F>> focusWrapperModel,
                                   LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel,
                                   PageBase pageBase) {
        super(id, mainForm, focusWrapperModel, projectionModel, pageBase);
        initLayout(focusWrapperModel, pageBase);
    }

    private void initLayout(final LoadableModel<ObjectWrapper<F>> focusModel, PageBase pageBase) {
        add(new Label(ID_HEADER, "Object details"));
        WebMarkupContainer body = new WebMarkupContainer("body");
        add(body);

        addPrismPropertyPanel(body, ID_PROP_NAME, FocusType.F_NAME);
        addPrismPropertyPanel(body, ID_PROP_FULL_NAME, UserType.F_FULL_NAME);

        // TODO: create proxy for these operations
        Task task = pageBase.createSimpleTask(OPERATION_SEARCH_ROLES);
        List<PrismObject<RoleType>> availableRoles;
        try {
            availableRoles = pageBase.getModelService().searchObjects(RoleType.class, null, null, task, task.getResult());
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException |
        		ConfigurationException | ExpressionEvaluationException e) {
            task.getResult().recordFatalError(e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load roles", e);
            availableRoles = new ArrayList<>();
            // TODO: better error reporting
        }

        ContainerWrapper<AssignmentType> assignmentsContainerWrapper = getObjectWrapper().findContainerWrapper(new ItemPath(FocusType.F_ASSIGNMENT));

        add(new SimpleRoleSelector<F,RoleType>(ID_ROLES, Model.of(assignmentsContainerWrapper), availableRoles));
    }

}
