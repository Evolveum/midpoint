/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
