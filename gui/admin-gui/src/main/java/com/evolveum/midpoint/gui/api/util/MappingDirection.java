/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

/**
 * Attribute mapping related attributes
 */
public enum MappingDirection {
    INBOUND(ResourceAttributeDefinitionType.F_INBOUND),
    OUTBOUND(ResourceAttributeDefinitionType.F_OUTBOUND),
    OVERRIDE(null);

    private ItemName containerName;

    MappingDirection(ItemName containerName) {
        this.containerName = containerName;
    }

    public ItemName getContainerName() {
        return containerName;
    }
}
