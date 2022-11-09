/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ItemComparisonResult;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordCompareStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourcePasswordDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Implements the `compare` operation.
 */
@Experimental
@Component
class CompareHelper {

    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired private Protector protector;

    public <T> ItemComparisonResult compare(
            @NotNull PrismObject<ShadowType> repositoryShadow,
            ItemPath path,
            T expectedValue,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, EncryptionException {

        if (!path.equivalent(SchemaConstants.PATH_PASSWORD_VALUE)) {
            throw new UnsupportedOperationException("Only password comparison is supported");
        }

        ProvisioningContext ctx = ctxFactory.createForShadow(repositoryShadow, task, result);
        ctx.applyAttributesDefinition(repositoryShadow);

        ResourceType resource = ctx.getResource();

        PasswordCompareStrategyType passwordCompareStrategy = getPasswordCompareStrategy(ctx.getObjectDefinitionRequired());
        if (passwordCompareStrategy == PasswordCompareStrategyType.ERROR) {
            throw new UnsupportedOperationException("Password comparison is not supported on "+resource);
        }

        PrismProperty<T> repoProperty = repositoryShadow.findProperty(path);
        if (repoProperty == null) {
            if (passwordCompareStrategy == PasswordCompareStrategyType.CACHED) {
                if (expectedValue == null) {
                    return ItemComparisonResult.MATCH;
                } else {
                    return ItemComparisonResult.MISMATCH;
                }
            } else {
                // AUTO
                return ItemComparisonResult.NOT_APPLICABLE;
            }
        }

        ProtectedStringType repoProtectedString = (ProtectedStringType) repoProperty.getRealValue();
        ProtectedStringType expectedProtectedString;
        if (expectedValue instanceof ProtectedStringType) {
            expectedProtectedString = (ProtectedStringType) expectedValue;
        } else {
            expectedProtectedString = new ProtectedStringType();
            expectedProtectedString.setClearValue((String) expectedValue);
        }
        if (protector.compareCleartext(repoProtectedString, expectedProtectedString)) {
            return ItemComparisonResult.MATCH;
        } else {
            return ItemComparisonResult.MISMATCH;
        }
    }

    private PasswordCompareStrategyType getPasswordCompareStrategy(ResourceObjectDefinition definition) {
        ResourcePasswordDefinitionType passwordDefinition = definition.getPasswordDefinition();
        return passwordDefinition != null ? passwordDefinition.getCompareStrategy() : null;
    }
}
