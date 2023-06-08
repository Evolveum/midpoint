/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
interface PrismEntityCoverageInformation extends DebugDumpable {

    @NotNull PrismEntityCoverage getCoverage();

}
