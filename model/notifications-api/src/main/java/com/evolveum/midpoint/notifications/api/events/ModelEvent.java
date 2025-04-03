/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * Event about model operation (TODO)
 */
public interface ModelEvent extends Event {

    @NotNull
    ModelContext<?> getModelContext();

    @NotNull
    ModelElementContext<?> getFocusContext();

    Collection<? extends ModelProjectionContext> getProjectionContexts();

    List<? extends ObjectDeltaOperation> getFocusExecutedDeltas();

    List<ObjectDeltaOperation> getAllExecutedDeltas();

    // a bit of hack but ...
    ChangeType getChangeType();

    ObjectDelta<?> getFocusPrimaryDelta();

    ObjectDelta<?> getFocusSecondaryDelta();

    ObjectDelta<?> getFocusSummaryDelta() throws SchemaException;

    List<ObjectDelta<AssignmentHolderType>> getFocusDeltas();

    ObjectDelta<? extends AssignmentHolderType> getSummarizedFocusDeltas() throws SchemaException;

    boolean hasFocusOfType(Class<? extends AssignmentHolderType> clazz);

    boolean hasFocusOfType(QName focusType);

    String getFocusTypeName();

    default boolean hasContentToShow() {
        return hasContentToShow(false);
    }

    boolean hasContentToShow(boolean showAuxiliaryAttributes);

    /**
     * May be used from scripts
     */
    default String getContentAsFormattedList() {
        return getContentAsFormattedList(false, null, null);
    }

    default String getContentAsFormattedList(Task task, OperationResult result) {
        return getContentAsFormattedList(false, task, result);
    }
    String getContentAsFormattedList(boolean showAuxiliaryAttributes, Task task, OperationResult result);
}
