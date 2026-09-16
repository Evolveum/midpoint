/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.subscription;

import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import java.io.Serializable;

/**
 * The system features related to the subscription handling. Used e.g. to determine if we run in the production mode.
 * Or if we use a generic production database.
 */
public class SystemFeatures implements Serializable, DebugDumpable {

    private final boolean publicHttpsUrlPatternDefined;

    private final boolean remoteHostAddressHeaderDefined;

    private final boolean customLoggingDefined;

    private final boolean realNotificationsEnabled;

    private final boolean customDeploymentColorsDefined;

    private final boolean customLogoDefined;

    private final boolean clusteringEnabled;

    private final boolean genericDatabaseUsed;

    private SystemFeatures(Builder builder) {
        this.publicHttpsUrlPatternDefined = builder.publicHttpsUrlPatternDefined;
        this.remoteHostAddressHeaderDefined = builder.remoteHostAddressHeaderDefined;
        this.customLoggingDefined = builder.customLoggingDefined;
        this.realNotificationsEnabled = builder.realNotificationsEnabled;
        this.customDeploymentColorsDefined = builder.customDeploymentColorsDefined;
        this.customLogoDefined = builder.customLogoDefined;
        this.clusteringEnabled = builder.clusteringEnabled;
        this.genericDatabaseUsed = builder.genericDatabaseUsed;
    }

    static Builder builder() {
        return new Builder();
    }

    /** Fallback values to be used in the case of an error. All the "production" indications are turned on. */
    public static SystemFeatures error() {
        return builder()
                .publicHttpsUrlPatternDefined(true)
                .remoteHostAddressHeaderDefined(true)
                .customLoggingDefined(true)
                .realNotificationsEnabled(true)
                .customDeploymentColorsDefined(true)
                .customLogoDefined(true)
                .build();
    }

    /** Does the public HTTP URL pattern use secure (https) protocol? */
    public boolean isPublicHttpsUrlPatternDefined() {
        return publicHttpsUrlPatternDefined;
    }

    /** Is a header that defines the remote host (like `X-Forwarded-For`) defined? It indicates the use of a proxy. */
    public boolean isRemoteHostAddressHeaderDefined() {
        return remoteHostAddressHeaderDefined;
    }

    /** Custom logging currently means "using an appender other than the file-based one". */
    public boolean isCustomLoggingDefined() {
        return customLoggingDefined;
    }

    /** Are there SMTP or SMS notifications configured, without being redirected to a file? */
    public boolean areRealNotificationsEnabled() {
        return realNotificationsEnabled;
    }

    /** Are the colors of this deployment customized, i.e. is the header color or the skin set? */
    public boolean isCustomDeploymentColorsDefined() {
        return customDeploymentColorsDefined;
    }

    /** Is a custom logo (either an image or a CSS class) set for this deployment? */
    public boolean isCustomLogoDefined() {
        return customLogoDefined;
    }

    /** Is the clustering enabled in the task manager configuration? */
    public boolean isClusteringEnabled() {
        return clusteringEnabled;
    }

    /** Are we using the generic repo? */
    public boolean isGenericDatabaseUsed() {
        return genericDatabaseUsed;
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "publicHttpsUrlPatternDefined", publicHttpsUrlPatternDefined, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "remoteHostAddressHeaderDefined", remoteHostAddressHeaderDefined, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "customLoggingDefined", customLoggingDefined, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "realNotificationsEnabled", realNotificationsEnabled, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "customDeploymentColorsDefined", customDeploymentColorsDefined, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "customLogoDefined", customLogoDefined, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "clusteringEnabled", clusteringEnabled, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "genericDatabaseUsed", genericDatabaseUsed, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "publicHttpsUrlPatternDefined=" + publicHttpsUrlPatternDefined +
                ", remoteHostAddressHeaderDefined=" + remoteHostAddressHeaderDefined +
                ", customLoggingDefined=" + customLoggingDefined +
                ", realNotificationsEnabled=" + realNotificationsEnabled +
                ", customDeploymentColorsDefined=" + customDeploymentColorsDefined +
                ", customLogoDefined=" + customLogoDefined +
                ", clusteringEnabled=" + clusteringEnabled +
                ", genericDatabaseUsed=" + genericDatabaseUsed +
                '}';
    }

    /** All features default to `false`, i.e. to "this is not a production deployment". */
    static final class Builder {

        private boolean publicHttpsUrlPatternDefined;
        private boolean remoteHostAddressHeaderDefined;
        private boolean customLoggingDefined;
        private boolean realNotificationsEnabled;
        private boolean customDeploymentColorsDefined;
        private boolean customLogoDefined;
        private boolean clusteringEnabled;
        private boolean genericDatabaseUsed;

        Builder publicHttpsUrlPatternDefined(boolean val) {
            publicHttpsUrlPatternDefined = val;
            return this;
        }

        Builder remoteHostAddressHeaderDefined(boolean val) {
            remoteHostAddressHeaderDefined = val;
            return this;
        }

        Builder customLoggingDefined(boolean val) {
            customLoggingDefined = val;
            return this;
        }

        Builder realNotificationsEnabled(boolean val) {
            realNotificationsEnabled = val;
            return this;
        }

        Builder customDeploymentColorsDefined(boolean val) {
            customDeploymentColorsDefined = val;
            return this;
        }

        Builder customLogoDefined(boolean val) {
            customLogoDefined = val;
            return this;
        }

        Builder clusteringEnabled(boolean val) {
            clusteringEnabled = val;
            return this;
        }

        Builder genericDatabaseUsed(boolean val) {
            genericDatabaseUsed = val;
            return this;
        }

        SystemFeatures build() {
            return new SystemFeatures(this);
        }
    }
}
