/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.noop;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;

import com.evolveum.midpoint.model.impl.ModelBeans;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlator.CorrelatorFactory;
import com.evolveum.midpoint.model.api.correlator.CorrelatorFactoryRegistry;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NoOpCorrelatorType;

/**
 * Factory for {@link NoOpCorrelator} instances.
 */
@Component
public class NoOpCorrelatorFactory implements CorrelatorFactory<NoOpCorrelator, NoOpCorrelatorType> {

    private static final QName CONFIGURATION_ITEM_NAME = SchemaConstantsGenerated.C_NO_OP_CORRELATOR;

    @Autowired CorrelatorFactoryRegistry registry;
    @Autowired ModelBeans beans;

    @PostConstruct
    public void register() {
        registry.registerFactory(CONFIGURATION_ITEM_NAME, this);
    }

    @Override
    public @NotNull Class<NoOpCorrelatorType> getConfigurationBeanType() {
        return NoOpCorrelatorType.class;
    }

    @Override
    public @NotNull NoOpCorrelator instantiate(
            @NotNull CorrelatorContext<NoOpCorrelatorType> context,
            @NotNull Task task,
            @NotNull OperationResult result) {
        return new NoOpCorrelator(context, beans);
    }
}
