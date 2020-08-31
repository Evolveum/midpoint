/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.apache.commons.lang.StringUtils;

public enum Channel {

    // TODO: move this to schema component
    LIVE_SYNC(SchemaConstants.CHANNEL_LIVE_SYNC_URI, "Channel.liveSync", GuiStyleConstants.CLASS_RECONCILE),
    RECONCILIATION(SchemaConstants.CHANNEL_RECON_URI, "Channel.reconciliation", GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM),
    RECOMPUTATION(SchemaConstants.CHANNEL_RECOMPUTE_URI, "Channel.recompute", GuiStyleConstants.CLASS_RECONCILE),
    DISCOVERY(SchemaConstants.CHANNEL_DISCOVERY_URI, "Channel.discovery", "fa fa-ambulance"),
    WEB_SERVICE(SchemaConstants.CHANNEL_WEB_SERVICE_URI, "Channel.webService", GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON),  //TODO remove eventualy
    OBJECT_IMPORT(SchemaConstants.CHANNEL_OBJECT_IMPORT_URI, "Channel.objectImport", "fa  fa-file-code-o"),
    REST(SchemaConstants.CHANNEL_REST_URI, "Channel.rest", GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON),
    INIT(SchemaConstants.CHANNEL_INIT_URI, "Channel.init", "fa fa-plug"),
    USER(SchemaConstants.CHANNEL_USER_URI, "Channel.user", "fa fa-keyboard-o"),
    SELF_REGISTRATION(SchemaConstants.CHANNEL_SELF_REGISTRATION_URI, "Channel.selfRegistration", "fa fa-file-text-o"),
    RESET_PASSWORD(SchemaConstants.CHANNEL_RESET_PASSWORD_URI, "Channel.resetPassword", "fa fa-shield"),
    IMPORT(SchemaConstants.CHANNEL_IMPORT_URI, "Channel.import", "fa fa-upload"),
    ASYNC_UPDATE(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI, "Channel.asyncUpdate", "fa fa-gears");

    private String channel;
    private String resourceKey;
    private String iconCssClass;

    Channel(String channel, String resourceKey, String iconCssClass) {
        this.channel = channel;
        this.resourceKey = resourceKey;
        this.iconCssClass = iconCssClass;
    }

    public String getChannel() {
        return channel;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public String getIconCssClass() {
        return iconCssClass;
    }

    public static Channel findChannel(String channelUri) {
        if (StringUtils.isBlank(channelUri)) {
            return null;
        }

        for (Channel channel : values()) {
            if (channel.getChannel().equals(channelUri)) {
                return channel;
            }
        }

        return null;
    }
}
