/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

public enum Channel {

    // TODO: move this to schema component
    LIVE_SYNC(SchemaConstants.CHANNEL_LIVE_SYNC_URI, "Channel.liveSync", GuiStyleConstants.CLASS_RECONCILE, "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#liveSync"),
    RECONCILIATION(SchemaConstants.CHANNEL_RECON_URI, "Channel.reconciliation", GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM, "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#reconciliation"),
    RECOMPUTATION(SchemaConstants.CHANNEL_RECOMPUTE_URI, "Channel.recompute", GuiStyleConstants.CLASS_RECONCILE, "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#recompute"),
    DISCOVERY(SchemaConstants.CHANNEL_DISCOVERY_URI, "Channel.discovery", "fa fa-ambulance", "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#discovery"),
    WEB_SERVICE(SchemaConstants.CHANNEL_WEB_SERVICE_URI, "Channel.webService", GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON, "http://midpoint.evolveum.com/xml/ns/public/model/channels-3#webService"),  //TODO remove eventualy
    OBJECT_IMPORT(SchemaConstants.CHANNEL_OBJECT_IMPORT_URI, "Channel.objectImport", "fa  fa-file-code-o", "http://midpoint.evolveum.com/xml/ns/public/model/channels-3#objectImport"),
    REST(SchemaConstants.CHANNEL_REST_URI, "Channel.rest", GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON, "http://midpoint.evolveum.com/xml/ns/public/model/channels-3#rest"),
    INIT(SchemaConstants.CHANNEL_INIT_URI, "Channel.init", "fa fa-plug", "http://midpoint.evolveum.com/xml/ns/public/gui/channels-3#init"),
    USER(SchemaConstants.CHANNEL_USER_URI, "Channel.user", "fa fa-keyboard-o", "http://midpoint.evolveum.com/xml/ns/public/gui/channels-3#user"),
    SELF_REGISTRATION(SchemaConstants.CHANNEL_SELF_REGISTRATION_URI, "Channel.selfRegistration", "fa fa-file-text-o", "http://midpoint.evolveum.com/xml/ns/public/gui/channels-3#selfRegistration"),
    RESET_PASSWORD(SchemaConstants.CHANNEL_RESET_PASSWORD_URI, "Channel.resetPassword", "fa fa-shield", "http://midpoint.evolveum.com/xml/ns/public/gui/channels-3#resetPassword"),
    IMPORT(SchemaConstants.CHANNEL_IMPORT_URI, "Channel.import", "fa fa-upload", "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#import"),
    ASYNC_UPDATE(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI, "Channel.asyncUpdate", "fa fa-gears", "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#asyncUpdate");

    private final String channel;
    private final String resourceKey;
    private final String iconCssClass;
    private final String compatibilityOldChannel;

    Channel(String channel, String resourceKey, String iconCssClass, String compatibilityOldChannel) {
        this.channel = channel;
        this.resourceKey = resourceKey;
        this.iconCssClass = iconCssClass;
        this.compatibilityOldChannel = compatibilityOldChannel;
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

    public String getCompatibilityOldChannel() {
        return compatibilityOldChannel;
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
