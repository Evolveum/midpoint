/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces.visualizer;

import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.util.annotation.Experimental;

@Experimental
public interface Visualizer {

    void visualize(StringBuilder sb, OpNode node, int indent);

    default void visualizeAfter(StringBuilder sb, OpNode node, int indent) {
    }
}
