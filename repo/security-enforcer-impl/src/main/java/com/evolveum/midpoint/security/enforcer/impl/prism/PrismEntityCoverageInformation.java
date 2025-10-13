/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.security.enforcer.impl.prism;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.DebugDumpable;

/**
 * Detailed information on how is given prism entity ({@link Item} or {@link PrismValue}) covered by
 * (e.g.) set of authorizations.
 */
public interface PrismEntityCoverageInformation extends DebugDumpable {

    @NotNull PrismEntityCoverage getCoverage();
    boolean isExceptMetadata();
}
