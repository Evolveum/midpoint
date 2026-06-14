/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import org.jetbrains.annotations.NotNull;

/**
 * Generic interface for policy rules that may come from different places,
 * be it activity, global (system configuration) or task assignment, etc.
 */
public interface PolicyRuleIdentifier {

    /**
     * Returns a string representation of the policy rule identifier.
     * The format is not defined and may be different for different implementations.
     */
    @NotNull String asString();
}
