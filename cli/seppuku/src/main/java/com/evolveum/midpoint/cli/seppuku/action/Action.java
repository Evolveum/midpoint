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

package com.evolveum.midpoint.cli.seppuku.action;

import com.beust.jcommander.JCommander;
import com.evolveum.midpoint.cli.common.ToolsUtils;
import com.evolveum.midpoint.cli.seppuku.command.Command;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Viliam Repan (lazyman)
 */
public abstract class Action<T extends Command> {

    protected Logger STD_OUT = LoggerFactory.getLogger(ToolsUtils.LOGGER_SYS_OUT);
    protected Logger STD_ERR = LoggerFactory.getLogger(ToolsUtils.LOGGER_SYS_ERR);

    private JCommander commander;
    private T params;

    public Action(T params, JCommander commander) {
        Validate.notNull(params, "Action parameters must not be null.");
        Validate.notNull(commander, "Commander must not be null.");

        this.params = params;
        this.commander = commander;
    }

    public T getParams() {
        return params;
    }

    public void execute() throws Exception {
        if (params.isHelp()) {
            StringBuilder sb = new StringBuilder();
            commander.usage(commander.getParsedCommand(), sb);

            STD_OUT.info(sb.toString());

            return;
        }

        executeAction();
    }

    protected abstract void executeAction() throws Exception;

    protected ApplicationContext createApplicationContext() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext();


        return ctx;
    }
}
