/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

public class NinjaUsageFormatter extends DefaultUsageFormatter {

    private JCommander commander;

    public NinjaUsageFormatter(JCommander commander) {
        super(commander);

        this.commander = commander;
    }

    @Override
    public void appendCommands(StringBuilder out, int indentCount, int descriptionIndent, String indent) {
        out.append(indent + "\n  Commands:\n\n");

        List<String> commandNames = commander.getRawCommands().keySet().stream()
                .map(pn -> pn.getDisplayName()).sorted().collect(Collectors.toList());

        int maxCommandNameLength = commandNames.stream()
                .map(name -> name.length())
                .max(Integer::compareTo).orElse(0);

        // The magic value 3 is the number of spaces between the name of the option and its description
        for ( String commandName : commandNames) {
            Map.Entry<JCommander.ProgramName, JCommander> commands = commander.getRawCommands().entrySet().stream()
                            .filter(entry -> entry.getKey().getDisplayName().equals(commandName)).findFirst().orElse(null);

            Object arg = commands.getValue().getObjects().get(0);
            Parameters p = arg.getClass().getAnnotation(Parameters.class);

            if (p == null || !p.hidden()) {
                JCommander.ProgramName progName = commands.getKey();
                String dispName = progName.getDisplayName() + s(maxCommandNameLength - progName.getDisplayName().length() + 3);

                String description = indent + s(4) + dispName + getCommandDescription(progName.getName());

                wrapDescription(out, indentCount + descriptionIndent, description);
                out.append("\n");
            }
        }
    }
}
