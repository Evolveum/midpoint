/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.expression;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlator.CorrelatorFactory;
import com.evolveum.midpoint.model.api.correlator.CorrelatorFactoryRegistry;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionCorrelatorType;

/**
 * Factory for {@link ExpressionCorrelator} instances.
 */
@Component
public class ExpressionCorrelatorFactory implements CorrelatorFactory<ExpressionCorrelator, ExpressionCorrelatorType> {

    private static final QName CONFIGURATION_ITEM_NAME = SchemaConstantsGenerated.C_EXPRESSION_CORRELATOR;

    @Autowired CorrelatorFactoryRegistry registry;
    @Autowired ModelBeans beans;

    @PostConstruct
    public void register() {
        registry.registerFactory(CONFIGURATION_ITEM_NAME, this);
    }

    @Override
    public @NotNull Class<ExpressionCorrelatorType> getConfigurationBeanType() {
        return ExpressionCorrelatorType.class;
    }

    @Override
    public @NotNull ExpressionCorrelator instantiate(
            @NotNull CorrelatorContext<ExpressionCorrelatorType> context,
            @NotNull Task task,
            @NotNull OperationResult result) throws ConfigurationException {
        return new ExpressionCorrelator(context, beans);
    }
}
