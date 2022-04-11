/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.correlator;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlator.CorrelatorFactory;
import com.evolveum.midpoint.model.api.correlator.CorrelatorFactoryRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

/**
 * A factory for {@link DummyCorrelator} instances.
 *
 * Not used yet.
 */
@Component
public class DummyCorrelatorFactory implements CorrelatorFactory<DummyCorrelator, AbstractCorrelatorType> {

    public static final QName CONFIGURATION_ITEM_NAME = SchemaTestConstants.DUMMY_CORRELATOR_CONFIGURATION_ITEM_NAME;

    @Autowired CorrelatorFactoryRegistry registry;

    @PostConstruct
    public void register() {
        registry.registerFactory(CONFIGURATION_ITEM_NAME, this);
    }

    @Override
    public @NotNull DummyCorrelator instantiate(
            @NotNull CorrelatorContext<AbstractCorrelatorType> configuration,
            @NotNull Task task,
            @NotNull OperationResult result) {
        return new DummyCorrelator(configuration.getConfigurationBean());
    }

    @Override
    public @NotNull Class<AbstractCorrelatorType> getConfigurationBeanType() {
        return AbstractCorrelatorType.class;
    }
}
