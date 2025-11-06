/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.matching;

import com.evolveum.midpoint.common.matching.matcher.VariableBindingDefMatchingRule;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.match.MatchingRuleRegistryImpl;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registers custom matching rules into the Prism MatchingRuleRegistry.
 */
@Configuration
public class MatchingRuleRegister {

    private static final Trace LOGGER = TraceManager.getTrace(MatchingRuleRegister.class);

    /**
     * Static list of all custom matching rules to register into Prism.
     * Add your rules here and they will automatically be registered on startup.
     */
    private static final List<MatchingRule<?>> CUSTOM_RULES = List.of(
            new VariableBindingDefMatchingRule()
    );

    @PostConstruct
    public void registerMatchingRules() {
        MatchingRuleRegistry registry = PrismContext.get().getMatchingRuleRegistry();

        if (registry instanceof MatchingRuleRegistryImpl impl) {
            for (MatchingRule<?> rule : CUSTOM_RULES) {
                impl.registerMatchingRule(rule);
                LOGGER.trace("Registered matching rule: {}", rule.getClass().getSimpleName());
            }
        }
    }
}
