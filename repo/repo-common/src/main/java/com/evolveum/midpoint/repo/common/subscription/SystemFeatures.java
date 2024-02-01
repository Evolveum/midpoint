/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

    private final boolean clusteringEnabled;

    private final boolean genericNonH2DatabaseUsed;

    SystemFeatures(
            boolean publicHttpsUrlPatternDefined,
            boolean remoteHostAddressHeaderDefined,
            boolean customLoggingDefined,
            boolean realNotificationsEnabled,
            boolean clusteringEnabled,
            boolean genericNonH2DatabaseUsed) {
        this.publicHttpsUrlPatternDefined = publicHttpsUrlPatternDefined;
        this.remoteHostAddressHeaderDefined = remoteHostAddressHeaderDefined;
        this.customLoggingDefined = customLoggingDefined;
        this.realNotificationsEnabled = realNotificationsEnabled;
        this.clusteringEnabled = clusteringEnabled;
        this.genericNonH2DatabaseUsed = genericNonH2DatabaseUsed;
    }

    /** Fallback values to be used in the case of an error. */
    public static SystemFeatures error() {
        return new SystemFeatures(
                true, true, true, true, false, false);
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

    /** Is the clustering enabled in the task manager configuration? */
    public boolean isClusteringEnabled() {
        return clusteringEnabled;
    }

    /** Are we using the generic repo (other than H2)? */
    public boolean isGenericNonH2DatabaseUsed() {
        return genericNonH2DatabaseUsed;
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "publicHttpsUrlPatternDefined", publicHttpsUrlPatternDefined, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "remoteHostAddressHeaderDefined", remoteHostAddressHeaderDefined, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "customLoggingDefined", customLoggingDefined, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "realNotificationsEnabled", realNotificationsEnabled, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "clusteringEnabled", clusteringEnabled, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "genericNonH2DatabaseUsed", genericNonH2DatabaseUsed, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "publicHttpsUrlPatternDefined=" + publicHttpsUrlPatternDefined +
                ", remoteHostAddressHeaderDefined=" + remoteHostAddressHeaderDefined +
                ", customLoggingDefined=" + customLoggingDefined +
                ", realNotificationsEnabled=" + realNotificationsEnabled +
                ", clusteringEnabled=" + clusteringEnabled +
                ", genericNonH2DatabaseUsed=" + genericNonH2DatabaseUsed +
                '}';
    }
}
