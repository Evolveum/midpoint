/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class SynchronizationActionsBuilder {
    private static final String SYNCHRONIZE_NAME = "Synchronize";
    private static final String LINK_NAME = "Link";
    private static final String ADD_FOCUS_NAME = "Add-focus";
    private static final String DELETE_FOCUS_NAME = "Delete-focus";
    private static final String INACTIVATE_FOCUS_NAME = "Inactivate-focus";
    private static final String DELETE_RESOURCE_OBJECT_NAME = "Delete-resource-object";
    private static final String INACTIVATE_RESOURCE_OBJECT_NAME = "Inactivate-resource-object";
    private static final String CREATE_CORRELATION_CASE_NAME = "Create-correlation-case";

    private final SynchronizationActionsType actions = new SynchronizationActionsType();
    private int order = 1;

    SynchronizationActionsBuilder synchronize() {
        var a = new SynchronizeSynchronizationActionType();
        a.setOrder(order++);
        a.setName(SYNCHRONIZE_NAME);
        actions.getSynchronize().add(a);
        return this;
    }

    SynchronizationActionsBuilder link() {
        var a = new LinkSynchronizationActionType();
        a.setOrder(order++);
        a.setName(LINK_NAME);
        actions.getLink().add(a);
        return this;
    }

    SynchronizationActionsBuilder addFocus() {
        var a = new AddFocusSynchronizationActionType();
        a.setOrder(order++);
        a.setName(ADD_FOCUS_NAME);
        actions.getAddFocus().add(a);
        return this;
    }

    SynchronizationActionsBuilder deleteFocus() {
        var a = new DeleteFocusSynchronizationActionType();
        a.setOrder(order++);
        a.setName(DELETE_FOCUS_NAME);
        actions.getDeleteFocus().add(a);
        return this;
    }

    SynchronizationActionsBuilder inactivateFocus() {
        var a = new InactivateFocusSynchronizationActionType();
        a.setOrder(order++);
        a.setName(INACTIVATE_FOCUS_NAME);
        actions.getInactivateFocus().add(a);
        return this;
    }

    SynchronizationActionsBuilder deleteResourceObject() {
        var a = new DeleteResourceObjectSynchronizationActionType();
        a.setOrder(order++);
        a.setName(DELETE_RESOURCE_OBJECT_NAME);
        actions.getDeleteResourceObject().add(a);
        return this;
    }

    SynchronizationActionsBuilder inactivateResourceObject() {
        var a = new InactivateResourceObjectSynchronizationActionType();
        a.setOrder(order++);
        a.setName(INACTIVATE_RESOURCE_OBJECT_NAME);
        actions.getInactivateResourceObject().add(a);
        return this;
    }

    SynchronizationActionsBuilder createCorrelationCase() {
        var a = new CreateCorrelationCaseSynchronizationActionType();
        a.setOrder(order++);
        a.setName(CREATE_CORRELATION_CASE_NAME);
        actions.getCreateCorrelationCase().add(a);
        return this;
    }

    SynchronizationActionsType build() {
        return actions;
    }
}
