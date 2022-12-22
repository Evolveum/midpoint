/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface CandidateIdentifierFormatter {

    /** Suggests child activity identifier */
    @NotNull String formatCandidateIdentifier(int iteration);
}
