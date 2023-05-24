/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.impl;

import com.evolveum.midpoint.ninja.action.*;
import com.evolveum.midpoint.ninja.action.audit.ExportAuditOptions;
import com.evolveum.midpoint.ninja.action.audit.ExportAuditRepositoryAction;
import com.evolveum.midpoint.ninja.action.audit.ImportAuditOptions;
import com.evolveum.midpoint.ninja.action.audit.ImportAuditRepositoryAction;
import com.evolveum.midpoint.ninja.action.mining.ExportMiningOptions;
import com.evolveum.midpoint.ninja.action.mining.ExportMiningRepositoryAction;
import com.evolveum.midpoint.ninja.action.trace.EditTraceAction;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeAction;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeOptions;
import com.evolveum.midpoint.ninja.opts.*;

/**
 * Enumeration of Ninja commands (or actions).
 */
public enum Command {

    IMPORT("import", ImportOptions.class, ImportRepositoryAction.class),

    EXPORT("export", ExportOptions.class, ExportRepositoryAction.class),

    DELETE("delete", DeleteOptions.class, DeleteRepositoryAction.class),

    COUNT("count", CountOptions.class, CountRepositoryAction.class),

    VERIFY("verify", VerifyOptions.class, VerifyRepositoryAction.class),

    KEYS("keys", ListKeysOptions.class, ListKeysRepositoryAction.class),

    INFO("info", InfoOptions.class, InfoRepositoryAction.class),

    IMPORT_AUDIT("importAudit", ImportAuditOptions.class, ImportAuditRepositoryAction.class),

    EXPORT_AUDIT("exportAudit", ExportAuditOptions.class, ExportAuditRepositoryAction.class),

    EXPORT_MINING("exportMining", ExportMiningOptions.class, ExportMiningRepositoryAction.class),

    TRACE("trace", EditTraceOptions.class, EditTraceAction.class),

    UPGRADE("upgrade", UpgradeOptions.class, UpgradeAction.class);

    // todo reencrypt, modify, bulk, etc

    private final String commandName;

    private final Class<?> options;

    private final Class<? extends Action<?>> action;

    <T> Command(String commandName, Class<T> options, Class<? extends Action<T>> action) {
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

    public static <T> Action<T> createAction(String command) {
        Command cmd = findCommand(command);
        if (cmd == null) {
            return null;
        }

        try {
            if (cmd.action == null) {
                return null;
            }

            //noinspection unchecked
            return (Action<T>) cmd.action.getDeclaredConstructor().newInstance();
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
