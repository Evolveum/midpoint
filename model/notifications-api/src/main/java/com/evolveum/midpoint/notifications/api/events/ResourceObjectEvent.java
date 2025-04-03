/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

/**
 * Event about resource object (account) creation, modification, or deletion.
 * It is emitted when midPoint carries out the respective action.
 * Not when account is changed on resource (by some other way).
 */
@SuppressWarnings("unused")
public interface ResourceObjectEvent extends Event {

    @NotNull
    ResourceOperationDescription getOperationDescription();

    @NotNull
    ChangeType getChangeType();

    @NotNull
    OperationStatus getOperationStatus();

    boolean isShadowKind(ShadowKindType shadowKindType);

    ShadowType getShadow();

    boolean isShadowIntent(String intent);

    ObjectDelta<ShadowType> getShadowDelta();

    String getShadowName();

    PolyStringType getResourceName();

    String getResourceOid();

    String getPlaintextPassword();

    /**
     * It may be used from scripts
     */
    String getContentAsFormattedList();

    String getContentAsFormattedList(Task task, OperationResult result);

    String getContentAsFormattedList(boolean showSynchronizationItems, boolean showAuxiliaryAttributes, Task task,
            OperationResult result);

    boolean hasContentToShow();

    boolean hasContentToShow(boolean watchSynchronizationAttributes, boolean watchAuxiliaryAttributes);
}
