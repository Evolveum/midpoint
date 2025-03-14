/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Formats object deltas and item deltas for notification purposes.
 */
public interface IDeltaFormatter {

    String formatObjectModificationDelta(ObjectDelta<? extends Objectable> objectDelta, List<ItemPath> hiddenPaths,
            boolean showOperationalAttributes, Task task, OperationResult result);

    // objectOld and objectNew are used for explaining changed container values, e.g. assignment[1]/tenantRef (see MID-2047)
    // if null, they are ignored
    String formatObjectModificationDelta(@NotNull ObjectDelta<? extends Objectable> objectDelta, Collection<ItemPath> hiddenPaths,
            boolean showOperationalAttributes, PrismObject<?> objectOld, PrismObject<?> objectNew, Task task,
            OperationResult result);

    String formatObjectAdd(PrismObject<? extends ObjectType> object, List<ItemPath> hiddenPaths,
            boolean showOperationalAttributes, Task task, OperationResult result);
}
