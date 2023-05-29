/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.trace;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.List;

/**
 *
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "editTrace")
public class EditTraceOptions {

    private static final String P_INPUT_LONG = "--input";
    private static final String P_OUTPUT_LONG = "--output";
    private static final String P_PRINT_STAT_LONG = "--print-stat";
    private static final String P_PRINT_STAT_EXTRA_LONG = "--print-stat-extra";
    private static final String P_KILL_LONG = "--kill";
    private static final String P_KEEP_LONG = "--keep";

    @Parameter(names = { P_INPUT_LONG }, descriptionKey = "editTrace.input")
    private String input;

    @Parameter(names = { P_OUTPUT_LONG }, descriptionKey = "editTrace.output")
    private String output;

    @Parameter(names = { P_PRINT_STAT_LONG }, descriptionKey = "editTrace.printStat")
    private boolean printStat;

    @Parameter(names = { P_PRINT_STAT_EXTRA_LONG }, descriptionKey = "editTrace.printStatExtra")
    private boolean printStatExtra;

    @Parameter(names = { P_KILL_LONG }, descriptionKey = "editTrace.kill")
    private List<String> kill;

    @Parameter(names = { P_KEEP_LONG }, descriptionKey = "editTrace.keep")
    private List<String> keep;

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public boolean isPrintStat() {
        return printStat;
    }

    public boolean isPrintStatExtra() {
        return printStatExtra;
    }

    public List<String> getKill() {
        return kill;
    }

    public List<String> getKeep() {
        return keep;
    }
}
