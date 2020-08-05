/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.xnode;

import com.evolveum.midpoint.prism.util.CloneUtil;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Some XNodes can hold value metadata.
 */
public interface MetadataAware {

    @NotNull
    List<MapXNode> getMetadataNodes();

    void setMetadataNodes(@NotNull List<MapXNode> metadataNodes);

    default void addMetadataNode(MapXNode metadataNode) {
        getMetadataNodes().add(metadataNode);
    }

    static void cloneMetadata(MetadataAware target, MetadataAware source) {
        target.setMetadataNodes(CloneUtil.cloneCollectionMembers(source.getMetadataNodes()));
    }
}
