/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.exception.ConfigurationException;

/**
 * Creates {@link TopDownSpecification} instances for the following scenarios:
 *
 * - authorization + object: to determine access (at the object or sub-object level)
 * - authorization + type + position: to determine filter
 *
 * TODO move somewhere else
 */
public class ObjectSpecParser {

    /**
     * Creates specifications relevant to given `value`, derived from the currently-processed authorization.
     */
    public static Collection<TopDownSpecification> forAutzAndValue(
            @NotNull PrismObjectValue<?> value, @NotNull AuthorizationEvaluation evaluation)
            throws ConfigurationException {
        Authorization autz = evaluation.getAuthorization();
        List<TopDownSpecification> specifications = new ArrayList<>();
        var autzSelectors = autz.getParsedObjectSelectors();
        // Autz selectors can be empty e.g. for #all autz or for weird ones like role-prop-read-some-modify-some.xml.
        List<ValueSelector> objectSelectors = !autzSelectors.isEmpty() ? autzSelectors : List.of(ValueSelector.empty());
        for (ValueSelector objectSelector : objectSelectors) {
            if (objectSelector.isSubObject() && evaluation.shouldSkipSubObjectSelectors()) {
                continue;
            }
            Specification base = Specification.of(objectSelector, autz.getItems(), autz.getExceptItems(), evaluation.getDesc());
            TopDownSpecification topDown = base.asTopDown(value.asObjectable().getClass());
            if (topDown != null) {
                specifications.add(topDown);
            }
        }
        return specifications;
    }
}
