/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;

/**
 * Factory for {@link ItemsCorrelator} instances.
 */
@Component
public class ItemsCorrelatorFactory implements CorrelatorFactory<ItemsCorrelator, ItemsCorrelatorType> {

    private static final QName CONFIGURATION_ITEM_NAME = SchemaConstantsGenerated.C_ITEMS_CORRELATOR;

    @Autowired CorrelatorFactoryRegistry registry;
    @Autowired ModelBeans beans;

    @PostConstruct
    public void register() {
        registry.registerFactory(CONFIGURATION_ITEM_NAME, this);
    }

    @Override
    public @NotNull Class<ItemsCorrelatorType> getConfigurationBeanType() {
        return ItemsCorrelatorType.class;
    }

    @Override
    public @NotNull ItemsCorrelator instantiate(
            @NotNull CorrelatorContext<ItemsCorrelatorType> context,
            @NotNull Task task,
            @NotNull OperationResult result) throws ConfigurationException {
        return new ItemsCorrelator(context, beans);
    }
}
