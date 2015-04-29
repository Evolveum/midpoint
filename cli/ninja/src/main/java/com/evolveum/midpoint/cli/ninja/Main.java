/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.cli.ninja;

import com.beust.jcommander.JCommander;
import com.evolveum.midpoint.cli.common.DefaultCommand;
import com.evolveum.midpoint.cli.common.ToolsUtils;
import com.evolveum.midpoint.cli.ninja.action.*;
import com.evolveum.midpoint.cli.ninja.command.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final Map<String, ActionValue> ACTIONS = new HashMap<>();

    {
        ACTIONS.put(Import.CMD_IMPORT, new ActionValue(new Import(), ImportAction.class));
        ACTIONS.put(Export.CMD_EXPORT, new ActionValue(new Export(), ExportAction.class));
        ACTIONS.put(Delete.CMD_DELETE, new ActionValue(new Delete(), DeleteAction.class));
        ACTIONS.put(Bulk.CMD_BULK, new ActionValue(new Bulk(), BulkAction.class));
        ACTIONS.put(Modify.CMD_MODIFY, new ActionValue(new Modify(), ModifyAction.class));
    }

    private static class ActionValue {

        Command command;
        Class<? extends Action> action;

        ActionValue(Command command, Class<? extends Action> action) {
            this.command = command;
            this.action = action;
        }
    }

    public static void main(String[] args) {
        new Main().start(args);
    }

    private static final Logger STD_OUT = LoggerFactory.getLogger(ToolsUtils.LOGGER_SYS_OUT);
    private static final Logger STD_ERR = LoggerFactory.getLogger(ToolsUtils.LOGGER_SYS_ERR);

    private void start(String[] args) {
        LOG.debug("Arguments: {}", Arrays.toString(args));

        DefaultCommand def = new DefaultCommand();
        JCommander commander = new JCommander(def);

        for (Map.Entry<String, ActionValue> entry : ACTIONS.entrySet()) {
            ActionValue value = entry.getValue();
            commander.addCommand(entry.getKey(), value.command);
        }

        commander.parse(args);

        String cmd = commander.getParsedCommand();
        LOG.debug("Parsed command: '{}'", cmd);
        if (StringUtils.isEmpty(cmd)) {
            if (def.isVersion()) {
                printVersion();
            }
            if (def.isHelp()) {
                printHelp(commander);
            }
            return;
        }

        ActionValue actionValue = null;
        for (Map.Entry<String, ActionValue> entry : ACTIONS.entrySet()) {
            if (cmd != null && cmd.equals(entry.getKey())) {
                actionValue = entry.getValue();
                break;
            }
        }

        if (actionValue == null) {
            printHelp(commander);
            return;
        }

        try {
            LOG.debug("Executing action {} with params {}", actionValue.action.getSimpleName(), actionValue.command);

            Action action = createAction(actionValue.action, actionValue.command, commander);
            action.execute();
        } catch (Exception ex) {
            handleError(ex);
        }
    }

    private void printVersion() {
        try {
            String version = ToolsUtils.loadVersion();
            STD_OUT.info(version);
        } catch (IOException ex) {
            handleError(ex);
        }
    }

    private void handleError(Exception ex) {
        //todo error handling
        throw new RuntimeException(ex);
    }

    private void printHelp(JCommander commander) {
        StringBuilder sb = new StringBuilder();
        commander.usage(sb);

        STD_OUT.info(sb.toString());
    }

    private <T extends Command, A extends Action<T>> Action createAction(Class<A> clazz, T command, JCommander commander)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        Constructor<? extends Action> constructor = clazz.getConstructor(command.getClass(), JCommander.class);
        return constructor.newInstance(command, commander);
    }
}
