/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.xnode;

import java.util.List;

/**
 *
 */
public interface ListXNode extends XNode {
    @Override
    boolean isEmpty();
    int size();
    XNode get(int i);

    // todo reconsider this
    List<? extends XNode> asList();

    @Override
    ListXNode copy();
}
