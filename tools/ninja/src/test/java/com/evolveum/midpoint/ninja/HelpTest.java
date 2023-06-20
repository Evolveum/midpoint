/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

import com.evolveum.midpoint.ninja.impl.Command;

import org.testng.annotations.Test;

/**
 * Created by Viliam Repan (lazyman).
 */
public class HelpTest extends BaseTest {

    @Test
    public void runHelp() throws Exception {
        executeTest(new String[] { "-h" }, null, null, null);

        //todo assert help somehow
    }

    @Test
    public void runHelpForEachCommand() throws Exception {
        for (Command command : Command.values()) {
            executeTest(new String[] { "-h", command.getCommandName() }, null, null, null);
        }
    }
}
