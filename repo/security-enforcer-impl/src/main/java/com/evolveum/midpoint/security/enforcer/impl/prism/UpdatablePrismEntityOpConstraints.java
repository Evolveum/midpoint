/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.prism;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.security.enforcer.api.PrismEntityOpConstraints;
import com.evolveum.midpoint.security.enforcer.impl.AuthorizationEvaluation;

/**
 * {@link PrismEntityOpConstraints} that can be updated with additional information coming e.g. from authorizations.
 */
public interface UpdatablePrismEntityOpConstraints extends PrismEntityOpConstraints {

    interface ForItemContent extends PrismEntityOpConstraints.ForItemContent {

        @Override
        @NotNull UpdatablePrismEntityOpConstraints.ForValueContent getValueConstraints(@NotNull PrismValue value);
    }

    interface ForValueContent extends UpdatablePrismEntityOpConstraints, PrismEntityOpConstraints.ForValueContent {

        @Override
        @NotNull UpdatablePrismEntityOpConstraints.ForItemContent getItemConstraints(@NotNull ItemName name);

        void applyAuthorization(@NotNull PrismValue object, @NotNull AuthorizationEvaluation evaluation)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException;
    }
}
