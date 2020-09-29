/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.constants;

import org.apache.commons.lang3.StringUtils;

/**
 * Enumeration of built-in channels.
 */
public enum Channel {

    LIVE_SYNC(SchemaConstants.CHANNEL_LIVE_SYNC_URI, SchemaConstants.CHANNEL_LIVE_SYNC_LEGACY_URI, "Channel.liveSync"),
    RECONCILIATION(SchemaConstants.CHANNEL_RECON_URI, SchemaConstants.CHANNEL_RECON_LEGACY_URI, "Channel.reconciliation"),
    RECOMPUTATION(SchemaConstants.CHANNEL_RECOMPUTE_URI, SchemaConstants.CHANNEL_RECOMPUTE_LEGACY_URI, "Channel.recompute"),
    DISCOVERY(SchemaConstants.CHANNEL_DISCOVERY_URI, SchemaConstants.CHANNEL_DISCOVERY_LEGACY_URI, "Channel.discovery"),
    @Deprecated WEB_SERVICE(SchemaConstants.CHANNEL_WEB_SERVICE_URI, SchemaConstants.CHANNEL_WEB_SERVICE_LEGACY_URI, "Channel.webService"),
    OBJECT_IMPORT(SchemaConstants.CHANNEL_OBJECT_IMPORT_URI, SchemaConstants.CHANNEL_OBJECT_IMPORT_LEGACY_URI, "Channel.objectImport"),
    REST(SchemaConstants.CHANNEL_REST_URI, SchemaConstants.CHANNEL_REST_LEGACY_URI, "Channel.rest"),
    INIT(SchemaConstants.CHANNEL_INIT_URI, SchemaConstants.CHANNEL_INIT_LEGACY_URI, "Channel.init"),
    USER(SchemaConstants.CHANNEL_USER_URI, SchemaConstants.CHANNEL_USER_LEGACY_URI, "Channel.user"),
    SELF_REGISTRATION(SchemaConstants.CHANNEL_SELF_REGISTRATION_URI, SchemaConstants.CHANNEL_SELF_REGISTRATION_LEGACY_URI, "Channel.selfRegistration"),
    SELF_SERVICE(SchemaConstants.CHANNEL_SELF_SERVICE_URI, null, "Channel.selfService"),
    RESET_PASSWORD(SchemaConstants.CHANNEL_RESET_PASSWORD_URI, SchemaConstants.CHANNEL_RESET_PASSWORD_LEGACY_URI, "Channel.resetPassword"),
    IMPORT(SchemaConstants.CHANNEL_IMPORT_URI, SchemaConstants.CHANNEL_IMPORT_LEGACY_URI, "Channel.import"),
    ASYNC_UPDATE(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI, SchemaConstants.CHANNEL_ASYNC_UPDATE_LEGACY_URI, "Channel.asyncUpdate"),
    CLEANUP(SchemaConstants.CHANNEL_CLEANUP_URI, SchemaConstants.CHANGE_CHANNEL_DEL_NOT_UPDATED_SHADOWS_LEGACY_URI, "Channel.cleanup"),
    REMEDIATION(SchemaConstants.CHANNEL_REMEDIATION_URI, SchemaConstants.CHANNEL_REMEDIATION_LEGACY_URI, "Channel.remediation"),
    ACTUATOR(SchemaConstants.CHANNEL_ACTUATOR_URI, null, "Channel.actuator");

    private final String uri;
    private final String localizationKey;
    private final String legacyUri;

    Channel(String uri, String legacyUri, String localizationKey) {
        this.uri = uri;
        this.localizationKey = localizationKey;
        this.legacyUri = legacyUri;
    }

    /**
     * Current channel URI.
     */
    public String getUri() {
        return uri;
    }

    /**
     * Localization key describing the channel.
     */
    public String getLocalizationKey() {
        return localizationKey;
    }

    /**
     * Channel URI that was used for midPoint before 4.2.
     */
    public String getLegacyUri() {
        return legacyUri;
    }

    /**
     * @return Channel for the URI (matching current, not compatibility URIs); or null if it does not exist.
     */
    public static Channel findChannel(String uri) {
        if (StringUtils.isEmpty(uri)) {
            return null;
        }
        for (Channel channel : values()) {
            if (uri.equals(channel.getUri())) {
                return channel;
            }
        }
        return null;
    }

    /**
     * @return Migration object.
     */
    public static Migration findMigration(String uri) {
        if (StringUtils.isEmpty(uri)) {
            return null;
        }
        for (Channel channel : values()) {
            if (uri.equals(channel.getUri())) {
                return new Migration(channel, false);
            }
            if (uri.equals(channel.getLegacyUri())) {
                return new Migration(channel, true);
            }
        }
        return new Migration(null, false);
    }

    /**
     * Returns "migrated" URI: if the provided URI matches compatibility URI of any channel,
     * the method returns the current URI for that channel. In all other cases it returns back the client-provided value.
     */
    public static String migrateUri(String uri) {
        if (StringUtils.isEmpty(uri)) {
            return uri;
        }
        for (Channel channel : values()) {
            if (uri.equals(channel.getUri())) {
                return uri;
            }
            if (uri.equals(channel.getLegacyUri())) {
                return channel.getUri();
            }
        }
        return uri;
    }

    /**
     * Describes migration from (potentially) old channel URI to a current channel URI.
     * (What is the most important, is the "change" flag.)
     */
    public static class Migration {

        /**
         * Channel matching the URI.
         */
        private final Channel channel;

        /**
         * Is the migration needed, i.e. does the original URI differ from the channel URI?
         */
        private final boolean needed;

        private Migration(Channel channel, boolean needed) {
            this.channel = channel;
            this.needed = needed;
        }

        public Channel getChannel() {
            return channel;
        }

        public String getNewUri() {
            return channel != null ? channel.getUri() : null;
        }

        public boolean isNeeded() {
            return needed;
        }
    }
}
