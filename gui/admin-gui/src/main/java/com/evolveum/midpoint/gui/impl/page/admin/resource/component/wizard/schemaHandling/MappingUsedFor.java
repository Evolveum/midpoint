/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling;

import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingUseType;

public enum MappingUsedFor {
    CORRELATION(InboundMappingUseType.CORRELATION,
            "text-warning fa fa-code-branch",
            "UsedFor.CORRELATION"),
    SYNCHRONIZATION(InboundMappingUseType.SYNCHRONIZATION,
            "text-warning fa fa-rotate",
            "UsedFor.SYNCHRONIZATION"),
    ALL(InboundMappingUseType.ALL,
            "text-info fa fa-retweet",
            "UsedFor.ALL");

    private final InboundMappingUseType type;
    private final String icon;

    private final String tooltip;

    MappingUsedFor(InboundMappingUseType type, String icon, String tooltip) {
        this.type = type;
        this.icon = icon;
        this.tooltip = tooltip;
    }

    public InboundMappingUseType getType() {
        return type;
    }

    public String getIcon() {
        return icon;
    }

    public String getTooltip() {
        return tooltip;
    }
}
