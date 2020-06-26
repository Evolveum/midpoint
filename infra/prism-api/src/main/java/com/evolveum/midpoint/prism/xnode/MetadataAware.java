/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.xnode;

/**
 * Some XNodes can hold value metadata.
 */
public interface MetadataAware {

    MapXNode getMetadataNode();

    void setMetadataNode(MapXNode metadata);
}
