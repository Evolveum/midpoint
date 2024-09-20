/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.model.api.context.Mapping;

public interface MappingInspector {

    MappingInspector EMPTY = evaluatedMapping -> { };

    static MappingInspector empty() {
        return EMPTY;
    }

    /**
     * May be used to gather profiling data, etc.
     */
    void afterMappingEvaluation(Mapping<?, ?> evaluatedMapping);
}
