/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "help")
public class HelpOptions {

    @Parameter(descriptionKey = "help.command")
    private String command;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
