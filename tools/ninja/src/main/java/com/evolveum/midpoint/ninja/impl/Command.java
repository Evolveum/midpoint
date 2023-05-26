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
import com.evolveum.midpoint.ninja.opts.*;

/**
 * Enumeration of Ninja commands (or actions).
 */
public enum Command {

    IMPORT("import", ImportOptions.class, ImportRepositoryAction.class, null),

    EXPORT("export", ExportOptions.class, ExportRepositoryAction.class, null),

    DELETE("delete", DeleteOptions.class, DeleteRepositoryAction.class, null),

    COUNT("count", CountOptions.class, CountRepositoryAction.class, null),

    VERIFY("verify", VerifyOptions.class, VerifyRepositoryAction.class, null),

    KEYS("keys", ListKeysOptions.class, ListKeysRepositoryAction.class, null),

    INFO("info", Object.class, InfoRepositoryAction.class, null),

    IMPORT_AUDIT("importAudit", ImportAuditOptions.class, ImportAuditRepositoryAction.class, null),

    EXPORT_AUDIT("exportAudit", ExportAuditOptions.class, ExportAuditRepositoryAction.class, null),

    EXPORT_MINING("exportMining", ExportMiningOptions.class, ExportMiningRepositoryAction.class, null),

    TRACE("trace", EditTraceOptions.class, EditTraceAction.class, null);

    // todo reencrypt, modify, bulk, etc

    private final String commandName;

    private final Class<?> options;

    private final Class<? extends RepositoryAction<?>> repositoryAction;

    private final Class<? extends RestAction<?>> restAction;

    <T> Command(String commandName, Class<T> options, Class<? extends RepositoryAction<T>> repositoryAction,
            Class<? extends RestAction<T>> restAction) {
        this.commandName = commandName;
        this.options = options;
        this.repositoryAction = repositoryAction;
        this.restAction = restAction;
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

    public static <T> RepositoryAction<T> createRepositoryAction(String command) {
        Command cmd = findCommand(command);
        if (cmd == null) {
            return null;
        }

        try {
            if (cmd.repositoryAction == null) {
                return null;
            }

            //noinspection unchecked
            return (RepositoryAction<T>) cmd.repositoryAction.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static <T> RestAction<T> createRestAction(String command) {
        Command cmd = findCommand(command);
        if (cmd == null) {
            return null;
        }

        try {
            if (cmd.restAction == null) {
                return null;
            }

            //noinspection unchecked
            return (RestAction<T>) cmd.restAction.getDeclaredConstructor().newInstance();
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
