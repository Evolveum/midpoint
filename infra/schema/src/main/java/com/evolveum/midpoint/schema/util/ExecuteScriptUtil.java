/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ValueListType;

import java.util.Collection;

/** TODO move to {@link ExecuteScriptConfigItem}. */
public class ExecuteScriptUtil {

    /**
     * "Implants" specified input into ExecuteScriptType, not changing the original.
     */
    public static ExecuteScriptType implantInput(ExecuteScriptType originalBean, ValueListType input) {
        return originalBean.clone().input(input);
    }

    public static ValueListType createInput(Collection<? extends PrismValue> values) {
        ValueListType input = new ValueListType();
        values.forEach(o -> input.getValue().add(o));
        return input;
    }

    public static ValueListType createInputCloned(Collection<? extends PrismValue> values) {
        ValueListType input = new ValueListType();
        values.forEach(o -> input.getValue().add(o.clone()));
        return input;
    }
}
