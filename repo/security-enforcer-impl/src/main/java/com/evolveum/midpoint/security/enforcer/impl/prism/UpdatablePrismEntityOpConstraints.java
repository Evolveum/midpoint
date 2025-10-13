/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.security.enforcer.impl.prism;

import com.evolveum.midpoint.prism.PrismObjectValue;
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

        void applyAuthorization(@NotNull PrismObjectValue<?> value, @NotNull AuthorizationEvaluation evaluation)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException;
    }
}
