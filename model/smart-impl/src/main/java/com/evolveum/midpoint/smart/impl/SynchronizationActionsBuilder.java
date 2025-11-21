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
    private final SynchronizationActionsType actions = new SynchronizationActionsType();
    private int order = 1;

    SynchronizationActionsBuilder synchronize() {
        var a = new SynchronizeSynchronizationActionType();
        a.setOrder(order++);
        actions.getSynchronize().add(a);
        return this;
    }

    SynchronizationActionsBuilder link() {
        var a = new LinkSynchronizationActionType();
        a.setOrder(order++);
        actions.getLink().add(a);
        return this;
    }

    SynchronizationActionsBuilder addFocus() {
        var a = new AddFocusSynchronizationActionType();
        a.setOrder(order++);
        actions.getAddFocus().add(a);
        return this;
    }

    SynchronizationActionsBuilder deleteFocus() {
        var a = new DeleteFocusSynchronizationActionType();
        a.setOrder(order++);
        actions.getDeleteFocus().add(a);
        return this;
    }

    SynchronizationActionsBuilder inactivateFocus() {
        var a = new InactivateFocusSynchronizationActionType();
        a.setOrder(order++);
        actions.getInactivateFocus().add(a);
        return this;
    }

    SynchronizationActionsBuilder deleteResourceObject() {
        var a = new DeleteResourceObjectSynchronizationActionType();
        a.setOrder(order++);
        actions.getDeleteResourceObject().add(a);
        return this;
    }

    SynchronizationActionsBuilder inactivateResourceObject() {
        var a = new InactivateResourceObjectSynchronizationActionType();
        a.setOrder(order++);
        actions.getInactivateResourceObject().add(a);
        return this;
    }

    SynchronizationActionsBuilder createCorrelationCase() {
        var a = new CreateCorrelationCaseSynchronizationActionType();
        a.setOrder(order++);
        actions.getCreateCorrelationCase().add(a);
        return this;
    }

    SynchronizationActionsType build() {
        return actions;
    }
}
