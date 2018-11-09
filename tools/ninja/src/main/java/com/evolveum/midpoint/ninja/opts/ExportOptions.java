/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.ninja.opts;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.File;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "export")
public class ExportOptions extends BaseImportExportOptions {

    public static final String P_OUTPUT = "-O";
    public static final String P_OUTPUT_LONG = "--output";

    public static final String P_SPLIT = "-n";
    public static final String P_SPLIT_LONG = "-split";

    @Parameter(names = {P_OUTPUT, P_OUTPUT_LONG}, descriptionKey = "export.output")
    private File output;

//    @Parameter(names = {P_SPLIT, P_SPLIT_LONG}, descriptionKey = "export.split")
//    private boolean split;

    public File getOutput() {
        return output;
    }

//    public boolean isSplit() {
//        return split;
//    }
}
