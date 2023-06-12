/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.query;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LinkedObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Translates a linked selector (LinkedObjectSelectorType) to appropriate ObjectFilter.
 * VERY EXPERIMENTAL. TO BE RECONSIDERED. E.g. should we extend SelectorToFilterTranslator instead?
 */
@Experimental
public class LinkedSelectorToFilterTranslator {

    @NotNull private final LinkedObjectSelectorType selector;
    @NotNull private final PrismReferenceValue targetObjectRef;
    @NotNull private final SelectorToFilterTranslator objectSelectorTranslator;

    public LinkedSelectorToFilterTranslator(
            @Nullable LinkedObjectSelectorType selector,
            @NotNull PrismReferenceValue targetObjectRef,
            @NotNull String contextDescription,
            @NotNull Trace logger,
            @NotNull Task task) throws ConfigurationException {
        this.selector = selector != null ? selector : new LinkedObjectSelectorType();
        this.targetObjectRef = targetObjectRef;
        this.objectSelectorTranslator =
                new SelectorToFilterTranslator(this.selector, AssignmentHolderType.class, contextDescription, logger, task);
    }

    @NotNull
    public ObjectFilter createFilter(OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        return PrismContext.get().queryFactory().createAndOptimized(
                objectSelectorTranslator.createFilter(result),
                PrismContext.get().queryFor(AssignmentHolderType.class)
                        .item(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                        .ref(getTargetObjectRefValues())
                        .buildFilter());
    }

    private List<PrismReferenceValue> getTargetObjectRefValues() {
        if (selector.getRelation().isEmpty()) {
            return List.of(targetObjectRef);
        } else {
            return selector.getRelation().stream()
                    .map(relation -> targetObjectRef.clone().relation(relation))
                    .toList();
        }
    }

    public Class<? extends ObjectType> getNarrowedTargetType() throws ConfigurationException {
        return objectSelectorTranslator.getNarrowedTargetType();
    }
}
