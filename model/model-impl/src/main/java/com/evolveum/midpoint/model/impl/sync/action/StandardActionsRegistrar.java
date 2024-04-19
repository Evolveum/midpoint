/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.action;

import com.evolveum.midpoint.model.impl.sync.reactions.SynchronizationActionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Registers standard midPoint synchronization actions in {@link SynchronizationActionFactory}.
 */
@Component
public class StandardActionsRegistrar {

    @Autowired private SynchronizationActionFactory synchronizationActionFactory;

    @PostConstruct
    public void register() {
        synchronizationActionFactory.register(AddFocusAction.class, AddFocusAction::new);
        synchronizationActionFactory.register(DeleteFocusAction.class, DeleteFocusAction::new);
        synchronizationActionFactory.register(DeleteResourceObjectAction.class, DeleteResourceObjectAction::new);
        synchronizationActionFactory.register(SynchronizeAction.class, SynchronizeAction::new);
        synchronizationActionFactory.register(InactivateFocusAction.class, InactivateFocusAction::new);
        synchronizationActionFactory.register(InactivateResourceObjectAction.class, InactivateResourceObjectAction::new);
        synchronizationActionFactory.register(LinkAction.class, LinkAction::new);
        synchronizationActionFactory.register(UnlinkAction.class, UnlinkAction::new);
        synchronizationActionFactory.register(CreateCorrelationCaseAction.class, CreateCorrelationCaseAction::new);
    }
}
