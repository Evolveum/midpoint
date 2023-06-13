/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.FileReferenceConverter;
import com.evolveum.midpoint.ninja.util.ObjectTypesConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "count")
public class CountOptions {

    public static final String P_TYPE = "-t";
    public static final String P_TYPE_LONG = "--type";

    public static final String P_FILTER = "-f";
    public static final String P_FILTER_LONG = "--filter";

    @Parameter(names = {P_TYPE, P_TYPE_LONG}, descriptionKey = "base.type",
            validateWith = ObjectTypesConverter.class, converter = ObjectTypesConverter.class)
    private Set<ObjectTypes> type = new HashSet<>();

    @Parameter(names = {P_FILTER, P_FILTER_LONG}, descriptionKey = "base.filter",
            converter = FileReferenceConverter.class, validateWith = FileReferenceConverter.class)
    private FileReference filter;

    public Set<ObjectTypes> getType() {
        return type;
    }

    public FileReference getFilter() {
        return filter;
    }
}
