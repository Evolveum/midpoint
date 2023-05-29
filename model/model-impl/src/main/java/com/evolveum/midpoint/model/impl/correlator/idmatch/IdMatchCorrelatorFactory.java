/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import com.evolveum.midpoint.model.api.correlator.CorrelatorFactory;
import com.evolveum.midpoint.model.api.correlator.CorrelatorFactoryRegistry;
import com.evolveum.midpoint.model.api.correlator.idmatch.IdMatchService;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchCorrelatorType;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

/**
 * Factory for {@link IdMatchCorrelator} instances.
 */
@Component
public class IdMatchCorrelatorFactory implements CorrelatorFactory<IdMatchCorrelator, IdMatchCorrelatorType> {

    private static final QName CONFIGURATION_ITEM_NAME = SchemaConstantsGenerated.C_ID_MATCH_CORRELATOR;

    @Autowired CorrelatorFactoryRegistry registry;
    @Autowired ModelBeans beans;

    /** What service to use in the instances created. */
    @VisibleForTesting
    private IdMatchService serviceOverride;

    @PostConstruct
    public void register() {
        registry.registerFactory(CONFIGURATION_ITEM_NAME, this);
    }

    @Override
    public @NotNull Class<IdMatchCorrelatorType> getConfigurationBeanType() {
        return IdMatchCorrelatorType.class;
    }

    @Override
    public @NotNull IdMatchCorrelator instantiate(
            @NotNull CorrelatorContext<IdMatchCorrelatorType> correlatorContext,
            @NotNull Task task,
            @NotNull OperationResult result) throws ConfigurationException {
        return new IdMatchCorrelator(correlatorContext, serviceOverride, beans);
    }

    @VisibleForTesting
    public void setServiceOverride(IdMatchService serviceOverride) {
        this.serviceOverride = serviceOverride;
    }
}
