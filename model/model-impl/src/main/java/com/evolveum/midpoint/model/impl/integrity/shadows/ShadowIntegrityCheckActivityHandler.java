/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity.shadows;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowIntegrityCheckWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * Task handler for "Shadow integrity check" task.
 *
 * The purpose of this task is to detect and optionally delete duplicate shadows, i.e. distinct shadows that
 * correspond to the same resource object.
 *
 *  * Task handler for "Normalize attribute/property data" task.
 *
 * The purpose of this task is to normalize data stored in repository when the corresponding matching rule changes
 * (presumably from non-normalizing to normalizing one, e.g. from case sensitive to case insensitive).
 *
 * The reason is that if the data in the repository would be stored in non-normalized form, the would be
 * effectively hidden for any search on that particular attribute.
 */
@Component
public class ShadowIntegrityCheckActivityHandler
        extends ModelActivityHandler<ShadowIntegrityCheckWorkDefinition, ShadowIntegrityCheckActivityHandler> {

    private static final String LEGACY_HANDLER_URI = ModelPublicConstants.SHADOW_INTEGRITY_CHECK_TASK_HANDLER_URI;
    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                ShadowIntegrityCheckWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                ShadowIntegrityCheckWorkDefinition.class, ShadowIntegrityCheckWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ShadowIntegrityCheckWorkDefinitionType.COMPLEX_TYPE,
                LEGACY_HANDLER_URI,
                ShadowIntegrityCheckWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<ShadowIntegrityCheckWorkDefinition, ShadowIntegrityCheckActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<ShadowIntegrityCheckWorkDefinition, ShadowIntegrityCheckActivityHandler> context,
            @NotNull OperationResult result) {
        return new ShadowIntegrityCheckActivityRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "shadow-integrity-check";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
