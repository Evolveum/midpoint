/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.composite;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorFactory;
import com.evolveum.midpoint.model.api.correlator.CorrelatorFactoryRegistry;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;

/**
 * Factory for {@link CompositeCorrelator} instances.
 */
@Component
public class CompositeCorrelatorFactory implements CorrelatorFactory<CompositeCorrelator, CompositeCorrelatorType> {

    private static final QName CONFIGURATION_ITEM_NAME = SchemaConstantsGenerated.C_COMPOSITE_CORRELATOR;

    @Autowired CorrelatorFactoryRegistry registry;
    @Autowired ModelBeans beans;

    @PostConstruct
    public void register() {
        registry.registerFactory(CONFIGURATION_ITEM_NAME, this);
    }

    @Override
    public @NotNull Class<CompositeCorrelatorType> getConfigurationBeanType() {
        return CompositeCorrelatorType.class;
    }

    @Override
    public @NotNull CompositeCorrelator instantiate(
            @NotNull CorrelatorContext<CompositeCorrelatorType> context,
            @NotNull Task task,
            @NotNull OperationResult result) throws ConfigurationException {
        return new CompositeCorrelator(context, beans);
    }
}
