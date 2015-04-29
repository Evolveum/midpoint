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

package com.evolveum.midpoint.cli.ninja.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.File;

/**
 * @author lazyman
 */
@Parameters(commandDescription = "Imports file or input stream with objects to MidPoint.")
public class Import extends Command {

    public static final String CMD_IMPORT = "import";

    public static final String P_RAW = "-r";
    public static final String P_RAW_LONG = "--raw";

    public static final String P_FILE = "-f";
    public static final String P_FILE_LONG = "--file";

    public static final String P_TYPE = "-t";
    public static final String P_TYPE_LONG = "--type";

    public static final String P_IGNORE = "-i";
    public static final String P_IGNORE_LONG = "--ignore";

    @Parameter(names = {P_RAW, P_RAW_LONG},
            description = "Use raw option during import")
    private boolean raw = false;

    @Parameter(names = {P_FILE, P_FILE_LONG}, required = true,
            description = "File with objects to import")
    private File file;

    @Parameter(names = {P_TYPE, P_TYPE_LONG},
            description = "Object type to import from file, other object types will be skipped")
    private String type;

    @Parameter(names = {P_IGNORE, P_IGNORE_LONG},
            description = "Ignore errors/warnings during import")
    private boolean ignore;

    public boolean isRaw() {
        return raw;
    }

    public File getFile() {
        return file;
    }

    public String getType() {
        return type;
    }

    public boolean isIgnore() {
        return ignore;
    }
}
