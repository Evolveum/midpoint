/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.subscription;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * The system features related to the subscription handling. Used e.g. to determine if we run in the production mode.
 * Or if we use a generic production database.
 */
public class SystemFeatures implements Serializable, DebugDumpable {

    private final boolean publicHttpUrlPatternPresent;

    private final boolean customLoggingPresent;

    private final boolean mailNotificationsPresent;

    private final boolean clusterPresent;

    private final boolean genericProductionDatabasePresent;

    SystemFeatures(
            boolean publicHttpUrlPatternPresent,
            boolean customLoggingPresent,
            boolean mailNotificationsPresent,
            boolean clusterPresent,
            boolean genericProductionDatabasePresent) {
        this.publicHttpUrlPatternPresent = publicHttpUrlPatternPresent;
        this.customLoggingPresent = customLoggingPresent;
        this.mailNotificationsPresent = mailNotificationsPresent;
        this.clusterPresent = clusterPresent;
        this.genericProductionDatabasePresent = genericProductionDatabasePresent;
    }

    public boolean isPublicHttpUrlPatternPresent() {
        return publicHttpUrlPatternPresent;
    }

    public boolean isCustomLoggingPresent() {
        return customLoggingPresent;
    }

    public boolean isMailNotificationsPresent() {
        return mailNotificationsPresent;
    }

    public boolean isClusterPresent() {
        return clusterPresent;
    }

    public boolean isGenericProductionDatabasePresent() {
        return genericProductionDatabasePresent;
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "publicHttpUrlPatternPresent", publicHttpUrlPatternPresent, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "customLoggingPresent", customLoggingPresent, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "mailNotificationsPresent", mailNotificationsPresent, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "clusterPresent", clusterPresent, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "genericProductionDatabasePresent", genericProductionDatabasePresent, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "publicHttpUrlPatternPresent=" + publicHttpUrlPatternPresent +
                ", customLoggingPresent=" + customLoggingPresent +
                ", mailNotificationsPresent=" + mailNotificationsPresent +
                ", clusterPresent=" + clusterPresent +
                ", genericProductionDatabasePresent=" + genericProductionDatabasePresent +
                '}';
    }
}
