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
@Parameters(commandDescription = "Exports objects from MidPoint to file or input stream")
public class Export extends Command {

    public static final String CMD_EXPORT = "export";

    public static final String P_RAW = "-r";
    public static final String P_RAW_LONG = "--raw";

    public static final String P_FILE = "-f";
    public static final String P_FILE_LONG = "--file";

    public static final String P_TYPE = "-t";
    public static final String P_TYPE_LONG = "--type";

    public static final String P_OID = "-o";
    public static final String P_OID_LONG = "--oid";

    public static final String P_ZIP = "-z";
    public static final String P_ZIP_LONG = "--zip";

    public static final String P_IGNORE = "-i";
    public static final String P_IGNORE_LONG = "--ignore";

    public static final String P_QUERY = "-q";
    public static final String P_QUERY_LONG = "--query";

    @Parameter(names = {P_RAW, P_RAW_LONG},
            description = "Use raw option during export")
    private boolean raw = false;

    @Parameter(names = {P_FILE, P_FILE_LONG},
            description = "Export objects to file")
    private File file;

    @Parameter(names = {P_QUERY, P_QUERY_LONG},
            description = "File with query")
    private File query;

    @Parameter(names = {P_TYPE, P_TYPE_LONG},
            description = "Object type to export from MidPoint to file, other object types will be skipped")
    private String type;

    @Parameter(names = {P_OID, P_OID_LONG},
            description = "Export object with selected oid")
    private String oid;

    @Parameter(names = {P_ZIP, P_ZIP_LONG},
            description = "Zip exported objects. Use with " + P_FILE)
    private boolean zip;

    @Parameter(names = {P_IGNORE, P_IGNORE_LONG},
            description = "Ignore errors/warnings during export")
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

    public String getOid() {
        return oid;
    }

    public boolean isZip() {
        return zip;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public File getQuery() {
        return query;
    }
}
