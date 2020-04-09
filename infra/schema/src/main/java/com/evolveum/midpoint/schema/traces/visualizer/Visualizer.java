/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
