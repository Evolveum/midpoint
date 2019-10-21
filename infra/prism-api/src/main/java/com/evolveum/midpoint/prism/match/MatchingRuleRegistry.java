/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.match;

import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 *
 */
public interface MatchingRuleRegistry {
    // if typeQName is null, we skip the rule-type correspondence test
    @NotNull
    <T> MatchingRule<T> getMatchingRule(QName ruleName, QName typeQName) throws SchemaException;
}
