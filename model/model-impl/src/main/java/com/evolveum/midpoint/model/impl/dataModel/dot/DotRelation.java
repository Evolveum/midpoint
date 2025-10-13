/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.dataModel.dot;

public interface DotRelation {

    String getEdgeLabel();

    String getNodeLabel(String defaultLabel);

    String getEdgeStyle();

    String getNodeStyleAttributes();

    String getEdgeTooltip();

    String getNodeTooltip();
}
