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

package com.evolveum.midpoint.cli.common;

import com.beust.jcommander.Parameter;

/**
 * @author lazyman
 */
public class DefaultCommand {

    public static final String P_HELP = "-h";
    public static final String P_HELP_LONG = "--help";

    public static final String P_VERSION = "-v";
    public static final String P_VERSION_LONG = "--version";

    @Parameter(names = {P_HELP, P_HELP_LONG}, help = true,
            description = "Print this help")
    private boolean help;

    @Parameter(names = {P_VERSION, P_VERSION_LONG},
            description = "Print version")
    private boolean version;

    public boolean isHelp() {
        return help;
    }

    public boolean isVersion() {
        return version;
    }
}
