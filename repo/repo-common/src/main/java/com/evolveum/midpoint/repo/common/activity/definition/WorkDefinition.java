/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCompositionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import org.jetbrains.annotations.Nullable;

/**
 * Defines the work that is to be done within an activity.
 */
public interface WorkDefinition extends ActivityTypeNameAware, AffectedObjectsProvider, DebugDumpable, Cloneable {

    /** Creates "parsed" work definition from the activity definition bean. */
    static @Nullable <WD extends AbstractWorkDefinition> WD getWorkDefinitionFromBean(@NotNull ActivityDefinitionType bean)
            throws SchemaException, ConfigurationException {
        ActivityCompositionType compositionBean = bean.getComposition();
        if (compositionBean != null) {
            //noinspection unchecked
            return (WD) new CompositeWorkDefinition(compositionBean, ActivityDefinitionType.F_COMPOSITION);
        }

        WorkDefinitionsType singleWorkBean = bean.getWork();
        if (singleWorkBean != null) {
            //noinspection unchecked
            return (WD) CommonTaskBeans.get().workDefinitionFactory.getWorkFromBean(singleWorkBean); // returned value can be null
        }

        return null;
    }

    // TODO decide on this
    @NotNull ActivityTailoring getActivityTailoring();

    WorkDefinition clone();
}
