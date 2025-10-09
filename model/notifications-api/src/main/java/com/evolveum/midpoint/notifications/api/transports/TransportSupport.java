/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.api.transports;

import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;

/**
 * SPI support for {@link Transport} providing important dependencies.
 */
public interface TransportSupport {
    PrismContext prismContext();
    ExpressionFactory expressionFactory();
    RepositoryService repositoryService();
    Protector protector();

    // Available to cover other cases
    ApplicationContext applicationContext();
}
