/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
        validateHelp(null);
    }

    @Test
    public void test200HelpForCommands() throws Exception {
        for (Command command : Command.values()) {
            validateHelp(command.getCommandName());
        }
    }

    private void validateHelp(String command) throws Exception {

        String[] args;
        if (command == null) {
            args = new String[] { "-h" };
        } else {
            args = new String[] { "-h", command };
        }

        List<String> result1 = new ArrayList<>();
        executeTest(list -> result1.addAll(list),
                EMPTY_STREAM_VALIDATOR,
                args);

        if (command == null) {
            args = new String[] { "help" };
        } else {
            args = new String[] { "help", command };
        }

        List<String> result2 = new ArrayList<>();
        executeTest(list -> result2.addAll(list),
                EMPTY_STREAM_VALIDATOR,
                args);

        Assertions.assertThat(result1).isEqualTo(result2);
    }

    @Test
    public void test300Version() throws Exception {
        StreamValidator outValidator = list -> {
            String version = FileUtils.readFileToString(new File("./target/classes/version-long"), StandardCharsets.UTF_8).trim();
            Assertions.assertThat(version).isNotEmpty();

            Assertions.assertThat(list).hasSize(1);
            Assertions.assertThat(list.get(0)).isEqualTo(version);
        };

        executeTest(outValidator, EMPTY_STREAM_VALIDATOR, "-V");
    }
}
