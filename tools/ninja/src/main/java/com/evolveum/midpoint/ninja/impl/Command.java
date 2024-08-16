/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.impl;

import com.evolveum.midpoint.ninja.action.*;
import com.evolveum.midpoint.ninja.action.audit.*;
import com.evolveum.midpoint.ninja.action.mining.ExportMiningOptions;
import com.evolveum.midpoint.ninja.action.mining.ExportMiningRepositoryAction;
import com.evolveum.midpoint.ninja.action.trace.EditTraceAction;
import com.evolveum.midpoint.ninja.action.trace.EditTraceOptions;
import com.evolveum.midpoint.ninja.action.upgrade.action.*;

/**
 * Enumeration of Ninja commands (or actions).
 */
public enum Command {

    IMPORT("import", ImportOptions.class, ImportRepositoryAction.class),

    EXPORT("export", ExportOptions.class, ExportRepositoryAction.class),

    DELETE("delete", DeleteOptions.class, DeleteRepositoryAction.class),

    COUNT("count", CountOptions.class, CountRepositoryAction.class),

    VERIFY("verify", VerifyOptions.class, VerifyAction.class),

    KEYS("keys", ListKeysOptions.class, ListKeysRepositoryAction.class),

    INFO("info", InfoOptions.class, InfoRepositoryAction.class),

    IMPORT_AUDIT("import-audit", ImportAuditOptions.class, ImportAuditRepositoryAction.class),

    EXPORT_AUDIT("export-audit", ExportAuditOptions.class, ExportAuditRepositoryAction.class),

    VERIFY_AUDIT("verify-audit", VerifyAuditOptions.class, VerifyAuditRepositoryAction.class),

    EXPORT_MINING("export-mining", ExportMiningOptions.class, ExportMiningRepositoryAction.class),

    TRACE("trace", EditTraceOptions.class, EditTraceAction.class),

    DOWNLOAD_DISTRIBUTION("download-distribution", DownloadDistributionOptions.class, DownloadDistributionAction.class),

    RUN_SQL("run-sql", RunSqlOptions.class, RunSqlAction.class),

    UPGRADE_INSTALLATION("upgrade-installation", UpgradeInstallationOptions.class, UpgradeInstallationAction.class),

    UPGRADE_DISTRIBUTION("upgrade-distribution", UpgradeDistributionOptions.class, UpgradeDistributionAction.class),

    UPGRADE_OBJECTS("upgrade-objects", UpgradeObjectsOptions.class, UpgradeObjectsAction.class),

    PRE_UPGRADE_CHECK("pre-upgrade-check", PreUpgradeCheckOptions.class, PreUpgradeCheckAction.class),

    HELP("help", HelpOptions.class, HelpAction.class),

    // todo disabled, because it's not finished yet
    // UPGRADE("upgrade", UpgradeOptions.class, UpgradeAction.class),

    INITIAL_OBJECTS("initial-objects", InitialObjectsOptions.class, InitialObjectsAction.class);

    private final String commandName;

    private final Class<?> options;

    private final Class<? extends Action<?, ?>> action;

    <T> Command(String commandName, Class<T> options, Class<? extends Action<T, ?>> action) {
        this.commandName = commandName;
        this.options = options;
        this.action = action;
    }

    public String getCommandName() {
        return commandName;
    }

    public Object createOptions() {
        try {
            return options.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static <T> Action<T, ?> createAction(String command) {
        Command cmd = findCommand(command);
        if (cmd == null) {
            return null;
        }

        try {
            if (cmd.action == null) {
                return null;
            }

            //noinspection unchecked
            return (Action<T, ?>) cmd.action.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Command findCommand(String command) {
        if (command == null) {
            return null;
        }

        for (Command cmd : values()) {
            if (command.equals(cmd.getCommandName())) {
                return cmd;
            }
        }

        return null;
    }
}
