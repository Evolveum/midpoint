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
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.OutputFormatType;

/**
 * @author lazyman
 */
public class Bulk extends Command {

    public static final String CMD_BULK = "bulk";

    public static final String P_ASYNC = "-a";
    public static final String P_ASYNC_LONG = "--async";

    public static final String P_OUTPUT = "-o";
    public static final String P_OUTPUT_LONG = "--output";

    public static final String P_LIMIT = "-l";
    public static final String P_LIMIT_LONG = "--limit";

    public static final String P_MSL_SCRIPT = "-m";
    public static final String P_MSL_SCRIPT_LONG = "--msl";

    public static final String P_XML_SCRIPT = "-x";
    public static final String P_XML_SCRIPT_LONG = "--xml";

    @Parameter(names = {P_ASYNC, P_ASYNC_LONG},
            description = "Execute scripts asynchronously")
    private boolean async;

    @Parameter(names = {P_OUTPUT, P_OUTPUT_LONG},
            description = "Output format")
    private OutputFormatType output;

    @Parameter(names = {P_LIMIT, P_LIMIT_LONG},
            description = "Object limit")
    private Integer limit;

    @Parameter(names = {P_MSL_SCRIPT, P_MSL_SCRIPT_LONG},
            description = "MSL script")
    private String mslScript;

    @Parameter(names = {P_XML_SCRIPT, P_XML_SCRIPT_LONG},
            description = "XML script")
    private String xmlScript;

    public boolean isAsync() {
        return async;
    }

    public Integer getLimit() {
        return limit;
    }

    public OutputFormatType getOutput() {
        return output;
    }

    public String getMslScript() {
        return mslScript;
    }

    public String getXmlScript() {
        return xmlScript;
    }
}
