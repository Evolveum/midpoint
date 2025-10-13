/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
