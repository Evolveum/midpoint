/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.impl.Command;

/**
 * Created by Viliam Repan (lazyman).
 */
public class HelpVersionTest implements NinjaTestMixin {

    @Test
    public void test100FullHelp() throws Exception {
        executeTest(null, null, "-h");

        //todo assert help somehow
    }

    @Test
    public void test200HelpForCommands() throws Exception {
        // todo assert
        for (Command command : Command.values()) {
            executeTest(null, null, "-h", command.getCommandName());
        }
    }

    @Test
    public void test300Version() throws Exception {
        StreamValidator outValidator = list -> {
            String version = FileUtils.readFileToString(new File("./target/classes/version"), StandardCharsets.UTF_8).trim();
            Assertions.assertThat(version).isNotEmpty();

            Assertions.assertThat(list).hasSize(1);
            Assertions.assertThat(list.get(0)).isEqualTo(version);
        };

        executeTest(outValidator, EMPTY_STREAM_VALIDATOR, "-V");
    }
}
