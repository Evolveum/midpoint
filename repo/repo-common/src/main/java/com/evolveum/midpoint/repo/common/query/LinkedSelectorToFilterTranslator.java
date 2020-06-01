/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.query;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LinkedObjectSelectorType;

import org.jetbrains.annotations.Nullable;

/**
 * Translates a linked selector (LinkedObjectSelectorType) to appropriate ObjectFilter.
 * VERY EXPERIMENTAL. TO BE RECONSIDERED. E.g. should we extend SelectorToFilterTranslator instead?
 */
@Experimental
public class LinkedSelectorToFilterTranslator {

    @NotNull private final LinkedObjectSelectorType selector;
    @NotNull private final PrismReferenceValue targetObjectRef;
    @NotNull private final SelectorToFilterTranslator objectSelectorTranslator;

    @NotNull private final List<ObjectFilter> components = new ArrayList<>();

    public LinkedSelectorToFilterTranslator(@Nullable LinkedObjectSelectorType selector, @NotNull PrismReferenceValue targetObjectRef,
            @NotNull String contextDescription, @NotNull PrismContext prismContext, @NotNull ExpressionFactory expressionFactory,
            @NotNull Task task, @NotNull OperationResult result) {
        this.selector = selector != null ? selector : new LinkedObjectSelectorType(prismContext);
        this.targetObjectRef = targetObjectRef;
        this.objectSelectorTranslator = new SelectorToFilterTranslator(this.selector, AssignmentHolderType.class,
                contextDescription, prismContext, expressionFactory, task, result);
    }

    @NotNull
    public ObjectFilter createFilter() throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        components.add(objectSelectorTranslator.createFilter());
        components.add(
                objectSelectorTranslator.prismContext.queryFor(objectSelectorTranslator.getNarrowedTargetType())
                        .item(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                        .ref(getTargetObjectRefValues())
                        .buildFilter());
        return objectSelectorTranslator.queryFactory.createAndOptimized(components);
    }

    private List<PrismReferenceValue> getTargetObjectRefValues() {
        if (selector.getRelation().isEmpty()) {
            return singletonList(targetObjectRef);
        } else {
            return selector.getRelation().stream()
                    .map(relation -> targetObjectRef.clone().relation(relation))
                    .collect(Collectors.toList());
        }
    }

    public Class<? extends ObjectType> getNarrowedTargetType() {
        return objectSelectorTranslator.getNarrowedTargetType();
    }
}
