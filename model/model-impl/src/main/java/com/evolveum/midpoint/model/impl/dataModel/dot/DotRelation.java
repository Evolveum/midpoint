/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.dataModel.dot;

/**
 * @author mederly
 */
public interface DotRelation {

    String getEdgeLabel();

    String getNodeLabel(String defaultLabel);

    String getEdgeStyle();

    String getNodeStyleAttributes();

    String getEdgeTooltip();

    String getNodeTooltip();
}
