/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.action;

import com.beust.jcommander.JCommander;

import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.ninja.util.NinjaUtils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HelpAction extends Action<HelpOptions, ActionResult<Void>> {

    @Override
    public @NotNull NinjaApplicationContextLevel getApplicationContextLevel(List<Object> allOptions) {
        return NinjaApplicationContextLevel.NONE;
    }

    @Override
    public String getOperationName() {
        return null;
    }

    @Override
    public ActionResult<Void> execute() throws Exception {
        String command = options.getCommand();

        JCommander jc = NinjaUtils.setupCommandLineParser();

        if (StringUtils.isNotEmpty(command)) {
            JCommander specific = jc.getCommands().get(command);
            if (specific == null) {
                log.error(
                        "Unknown command {}, known commands: {}",
                        command,
                        StringUtils.join(jc.getCommands().keySet().stream().sorted().iterator(), ", "));
                return new ActionResult<>(null, 1);
            }
        }

        String help = NinjaUtils.createHelp(jc, command);
        context.out.println(help);

        return new ActionResult<>(null, 0);
    }
}
