/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.impl.Command;

/**
 * Created by Viliam Repan (lazyman).
 */
public class HelpTest implements NinjaTestMixin {

    @Test
    public void runHelp() throws Exception {
        executeTest(null, null, "-h");

        //todo assert help somehow
    }

    @Test
    public void runHelpForEachCommand() throws Exception {
        for (Command command : Command.values()) {
            executeTest(null, null, "-h", command.getCommandName());
        }
    }
}
