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
import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.FileReferenceConverter;
import com.evolveum.midpoint.ninja.util.ObjectTypesConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "count")
public class CountOptions {

    public static final String P_TYPE = "-t";
    public static final String P_TYPE_LONG = "--type";

    public static final String P_FILTER = "-f";
    public static final String P_FILTER_LONG = "--filter";

    @Parameter(names = {P_TYPE, P_TYPE_LONG}, descriptionKey = "count.type",
            validateWith = ObjectTypesConverter.class, converter = ObjectTypesConverter.class)
    private ObjectTypes type;

    @Parameter(names = {P_FILTER, P_FILTER_LONG}, descriptionKey = "count.filter",
            converter = FileReferenceConverter.class, validateWith = FileReferenceConverter.class)
    private FileReference filter;

    public ObjectTypes getType() {
        return type;
    }

    public FileReference getFilter() {
        return filter;
    }
}
