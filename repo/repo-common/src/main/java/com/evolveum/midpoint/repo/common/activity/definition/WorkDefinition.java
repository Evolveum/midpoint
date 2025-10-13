/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCompositionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import javax.xml.namespace.QName;

/**
 * Defines the work that is to be done within an activity.
 */
public interface WorkDefinition extends AffectedObjectSetProvider, DebugDumpable, Cloneable {

    /**
     * Returns the activity type name, currently the same as the name of the respective configuration item e.g. `c:recomputation`.
     */
    @NotNull QName getActivityTypeName();

    /** Creates "parsed" work definition from the activity definition bean. */
    static @Nullable <WD extends AbstractWorkDefinition> WD fromBean(
            @NotNull ActivityDefinitionType bean, @NotNull ConfigurationItemOrigin origin)
            throws SchemaException, ConfigurationException {
        ActivityCompositionType compositionBean = bean.getComposition();
        if (compositionBean != null) {
            //noinspection unchecked
            return (WD) new CompositeWorkDefinition(compositionBean, ActivityDefinitionType.F_COMPOSITION, origin);
        }

        WorkDefinitionsType singleWorkBean = bean.getWork();
        if (singleWorkBean != null) {
            //noinspection unchecked
            return (WD) CommonTaskBeans.get().workDefinitionFactory.getWorkFromBean(singleWorkBean, origin);
            // returned value can be null
        }

        return null;
    }

    // TODO decide on this
    @NotNull ActivityTailoring getActivityTailoring();

    WorkDefinition clone();

    @NotNull ConfigurationItemOrigin getOrigin();
}
