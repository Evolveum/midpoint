/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.OwnerOptionIdentifier;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectOwnerOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectOwnerOptionsType;

/**
 * Helps with creating the {@link CorrelationResult} for some correlators.
 *
 * TODO better name
 */
@Component
public class BuiltInResultCreator {

    public <F extends FocusType> CorrelationResult createCorrelationResult(
            @NotNull List<F> candidates,
            @NotNull CorrelationContext correlationContext) throws SchemaException {

        if (candidates.isEmpty()) {
            return CorrelationResult.noOwner();
        } else if (candidates.size() == 1) {
            return CorrelationResult.existingOwner(candidates.get(0));
        } else {
            return CorrelationResult.uncertain(
                    createOwnerOptionsBean(candidates, correlationContext));
        }
    }

    private <F extends FocusType> ResourceObjectOwnerOptionsType createOwnerOptionsBean(
            List<F> candidates, CorrelationContext correlationContext) {
        ResourceObjectOwnerOptionsType options = new ResourceObjectOwnerOptionsType(PrismContext.get());
        if (correlationContext.getManualCorrelationContext().getPotentialMatches() != null) {
            options.getOption().addAll(
                    CloneUtil.cloneCollectionMembers(
                            correlationContext.getManualCorrelationContext().getPotentialMatches()));
        } else {
            createDefaultOwnerOptions(options, candidates);
        }
        return options;
    }

    private <F extends FocusType> void createDefaultOwnerOptions(
            ResourceObjectOwnerOptionsType options, List<F> candidates) {
        for (F candidate : candidates) {
            options.getOption().add(
                    createOwnerOption(candidate));
        }
        options.getOption().add(
                createOwnerOption(null));
    }

    private ResourceObjectOwnerOptionType createOwnerOption(@Nullable FocusType candidate) {
        OwnerOptionIdentifier identifier = candidate != null ?
                OwnerOptionIdentifier.forExistingOwner(candidate.getOid()) : OwnerOptionIdentifier.forNoOwner();
        return new ResourceObjectOwnerOptionType(PrismContext.get())
                .identifier(identifier.getStringValue())
                .candidateOwnerRef(ObjectTypeUtil.createObjectRef(candidate));
    }
}
