package com.evolveum.midpoint.ninja.impl;

import com.evolveum.midpoint.ninja.action.*;
import com.evolveum.midpoint.ninja.opts.*;

/**
 * @author Viliam Repan (lazyman)
 */
public enum Command {

    IMPORT("import", ImportOptions.class, ImportRepositoryAction.class, null),

    EXPORT("export", ExportOptions.class, ExportRepositoryAction.class, null),

//    DELETE("delete", DeleteOptions.class, DeleteRepositoryAction.class, null),
//
//    PASSWORD_RESET("password", PasswordResetOptions.class, PasswordResetRepositoryAction.class, null),
//
//    UNLOCK("unlock", UnlockOptions.class, UnlockRepositoryAction.class, null),
//
//    TEST("test", TestResourceOptions.class, null, TestResourceRestAction.class),
//
    KEYS("keys", ListKeysOptions.class, ListKeysRepositoryAction.class, null);
//
//    TRANSFORM("transform", TransformOptions.class, TransformRepositoryAction.class, null),
//
//    SCHEMA("schema", SchemaOptions.class, SchemaRepositoryAction.class, null);

    // todo reencrypt, modify, bulk, etc

    private String commandName;

    private Class options;

    private Class<? extends RepositoryAction> repositoryAction;

    private Class<? extends RestAction> restAction;

    Command(String commandName, Class options, Class<? extends RepositoryAction> repositoryAction,
            Class<? extends RestAction> restAction) {
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
            return options.newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static RepositoryAction createRepositoryAction(String command) {
        Command cmd = findCommand(command);
        if (cmd == null) {
            return null;
        }

        try {
            if (cmd.repositoryAction == null) {
                return null;
            }

            return cmd.repositoryAction.newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static RestAction createRestAction(String command) {
        Command cmd = findCommand(command);
        if (cmd == null) {
            return null;
        }

        try {
            if (cmd.restAction == null) {
                return null;
            }

            return cmd.restAction.newInstance();
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
