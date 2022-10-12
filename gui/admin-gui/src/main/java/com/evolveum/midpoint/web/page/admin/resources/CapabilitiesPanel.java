/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class CapabilitiesPanel extends BasePanel<CapabilitiesDto> {

    private static final String ID_ACTIVATION = "activation";
    private static final String ID_ACTIVATION_LOCKOUT_STATUS = "activationLockoutStatus";
    private static final String ID_ACTIVATION_STATUS = "activationStatus";
    private static final String ID_ACTIVATION_VALIDITY = "activationValidity";
    private static final String ID_AUXILIARY_OBJECT_CLASSES = "auxiliaryObjectClasses";
    private static final String ID_CREDENTIALS = "credentials";
    private static final String ID_LIVE_SYNC = "liveSync";
    private static final String ID_TEST = "testConnection";
    private static final String ID_CREATE = "create";
    private static final String ID_UPDATE = "update";
    private static final String ID_COUNT_OBJECTS = "countObjects";
    private static final String ID_PAGED_SEARCH = "pagedSearch";
    private static final String ID_PASSWORD = "password";
    private static final String ID_ADD_ATTRIBUTE_VALUES = "addRemoveAttributeValues";
    private static final String ID_DELETE = "delete";
    private static final String ID_READ = "read";
    private static final String ID_CONNECTOR_SCRIPT = "script";
    private static final String ID_RUN_AS = "runAs";
    private static final String ID_LABEL = "label";

    private static final long serialVersionUID = 1L;

    public CapabilitiesPanel(String id, IModel<CapabilitiesDto> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "card card-outline card-primary"));

        createCapabilityButton(ID_ACTIVATION);
        createCapabilityButton(ID_CREDENTIALS);
        createCapabilityButton(ID_LIVE_SYNC);
        createCapabilityButton(ID_TEST);
        createCapabilityButton(ID_CREATE);
        createCapabilityButton(ID_UPDATE);
        createCapabilityButton(ID_ADD_ATTRIBUTE_VALUES);
        createCapabilityButton(ID_DELETE);
        createCapabilityButton(ID_READ);
        createCapabilityButton(ID_CONNECTOR_SCRIPT);
        createCapabilityButton(ID_PASSWORD);
        createCapabilityButton(ID_PAGED_SEARCH);
        createCapabilityButton(ID_AUXILIARY_OBJECT_CLASSES);
        createCapabilityButton(ID_ACTIVATION_VALIDITY);
        createCapabilityButton(ID_ACTIVATION_STATUS);
        createCapabilityButton(ID_ACTIVATION_LOCKOUT_STATUS);
        createCapabilityButton(ID_COUNT_OBJECTS);
        createCapabilityButton(ID_RUN_AS);
    }

    private void createCapabilityButton(String id) {
        AjaxLink<Boolean> button = new AjaxLink<>(id, new PropertyModel<>(getModel(), id)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                // TODO:
            }
        };

        button.add(AttributeAppender.append("class", () -> button.getModelObject() ? "bg-light-blue" : "bg-gray text-light-blue"));
        button.add(new Label(ID_LABEL, new ResourceModel("CapabilitiesType." + id)));

        add(button);
    }
}
