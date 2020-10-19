/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl;

import com.evolveum.midpoint.schema.constants.Channel;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

import org.jetbrains.annotations.NotNull;

public enum GuiChannel {

    LIVE_SYNC(Channel.LIVE_SYNC, GuiStyleConstants.CLASS_RECONCILE),
    RECONCILIATION(Channel.RECONCILIATION, GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM),
    RECOMPUTATION(Channel.RECOMPUTATION, GuiStyleConstants.CLASS_RECONCILE),
    DISCOVERY(Channel.DISCOVERY, "fa fa-ambulance"),
    @Deprecated WEB_SERVICE(Channel.WEB_SERVICE, GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON),
    OBJECT_IMPORT(Channel.OBJECT_IMPORT, "fa  fa-file-code-o"),
    REST(Channel.REST, GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON),
    INIT(Channel.INIT, "fa fa-plug"),
    USER(Channel.USER, "fa fa-keyboard-o"),
    SELF_REGISTRATION(Channel.SELF_REGISTRATION, "fa fa-file-text-o"),
    RESET_PASSWORD(Channel.RESET_PASSWORD, "fa fa-shield"),
    IMPORT(Channel.IMPORT, "fa fa-upload"),
    ASYNC_UPDATE(Channel.ASYNC_UPDATE, "fa fa-gears");

    @NotNull private final Channel channel;
    private final String iconCssClass;

    GuiChannel(@NotNull Channel channel, String iconCssClass) {
        this.channel = channel;
        this.iconCssClass = iconCssClass;
    }

    public String getUri() {
        return channel.getUri();
    }

    public String getLocalizationKey() {
        return channel.getLocalizationKey();
    }

    public String getIconCssClass() {
        return iconCssClass;
    }

    public String getLegacyUri() {
        return channel.getLegacyUri();
    }

    public static GuiChannel findChannel(String uri) {
        if (StringUtils.isBlank(uri)) {
            return null;
        }

        for (GuiChannel channel : values()) {
            if (channel.getUri().equals(uri)) {
                return channel;
            }
        }

        return null;
    }
}
