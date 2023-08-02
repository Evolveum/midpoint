/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetBasedWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

public class ObjectSetUtil {

    /**
     * Fills-in the expected type or checks that provided one is not contradicting it.
     */
    public static void assumeObjectType(@NotNull ObjectSetType set, @NotNull QName superType) {
        if (superType.equals(set.getType())) {
            return;
        }
        if (set.getType() == null || QNameUtil.match(set.getType(), superType)) {
            set.setType(superType);
            return;
        }
        argCheck(PrismContext.get().getSchemaRegistry().isAssignableFrom(superType, set.getType()),
                "Activity requires object type of %s, but %s was provided in the work definition",
                superType, set.getType());
    }

    public static void applyDefaultObjectType(@NotNull ObjectSetType set, @NotNull QName type) {
        if (set.getType() == null) {
            set.setType(type);
        }
    }

    public static @NotNull ObjectSetType emptyIfNull(ObjectSetType configured) {
        return configured != null ? configured : new ObjectSetType();
    }

    public static ObjectSetBasedWorkDefinitionType getObjectSetDefinitionFromTask(TaskType task) {
        if (java.util.Objects.isNull(task) || java.util.Objects.isNull(task.getActivity()) || Objects.isNull(task.getActivity().getWork())) {
            return null;
        }
        WorkDefinitionsType work = task.getActivity().getWork();
        if (!Objects.isNull(work.getIterativeScripting())){
            return work.getIterativeScripting();
        }
        if (!Objects.isNull(work.getReindexing())){
            return work.getReindexing();
        }
        if (!Objects.isNull(work.getIterativeChangeExecution())){
            return work.getIterativeChangeExecution();
        }
        if (!Objects.isNull(work.getTriggerScan())){
            return work.getTriggerScan();
        }
        if (!Objects.isNull(work.getRecomputation())){
            return work.getRecomputation();
        }
        if (!Objects.isNull(work.getObjectIntegrityCheck())){
            return work.getObjectIntegrityCheck();
        }
        if (!Objects.isNull(work.getFocusValidityScan())){
            return work.getFocusValidityScan();
        }
        if (!Objects.isNull(work.getDeletion())){
            return work.getDeletion();
        }
        return null;
    }
}
