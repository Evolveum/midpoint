/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.dataModel.dot;

import com.evolveum.midpoint.model.impl.dataModel.model.Relation;
import org.jetbrains.annotations.NotNull;

public class DotOtherRelation implements DotRelation {

    @NotNull private final Relation relation;

    public DotOtherRelation(@NotNull Relation relation) {
        this.relation = relation;
    }

    public String getEdgeLabel() {
        return "";
    }

    public String getNodeLabel(String defaultLabel) {
        return null;
    }

    public String getEdgeStyle() {
        return "";
    }

    public String getNodeStyleAttributes() {
        return "";
    }

    public String getEdgeTooltip() {
        return "";
    }

    public String getNodeTooltip() {
        return "";
    }

}
