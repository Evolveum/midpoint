/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.sample;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;

import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.SimpleRoleSelector;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Sample showing a custom focus form that displays semi-static form.
 *
 * @author Radovan Semancik
 */
public class SampleFormFocusTabPanel<F extends FocusType> extends AbstractObjectMainPanel<F, FocusDetailsModels<F>> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = SampleFormFocusTabPanel.class.getName() + ".";
    private static final String OPERATION_SEARCH_ROLES = DOT_CLASS + "searchRoles";

    private static final String ID_HEADER = "header";

    private static final String ID_PROP_NAME = "propName";
    private static final String ID_PROP_FULL_NAME = "propFullName";

    private static final String ID_ROLES = "roles";

    private static final Trace LOGGER = TraceManager.getTrace(SampleFormFocusTabPanel.class);

    public SampleFormFocusTabPanel(String id, FocusDetailsModels<F> model, ContainerPanelConfigurationType config) {
        super(id, model, config);

    }

    protected void initLayout() {
        add(new Label(ID_HEADER, "Object details"));
        WebMarkupContainer body = new WebMarkupContainer("body");
        add(body);

        addPrismPropertyPanel(body, ID_PROP_NAME, PolyStringType.COMPLEX_TYPE, FocusType.F_NAME);
        addPrismPropertyPanel(body, ID_PROP_FULL_NAME, PolyStringType.COMPLEX_TYPE, UserType.F_FULL_NAME);

        // TODO: create proxy for these operations
        Task task = getPageBase().createSimpleTask(OPERATION_SEARCH_ROLES);
        List<PrismObject<RoleType>> availableRoles;
        try {
            availableRoles = getPageBase().getModelService().searchObjects(RoleType.class, null, null, task, task.getResult());
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException |
                ConfigurationException | ExpressionEvaluationException e) {
            task.getResult().recordFatalError(e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load roles", e);
            availableRoles = new ArrayList<>();
            // TODO: better error reporting
        }

        PrismContainerWrapperModel<F, AssignmentType> assignmentsModel =
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), AssignmentHolderType.F_ASSIGNMENT);
        add(new SimpleRoleSelector<>(ID_ROLES, assignmentsModel, availableRoles));
    }
}
