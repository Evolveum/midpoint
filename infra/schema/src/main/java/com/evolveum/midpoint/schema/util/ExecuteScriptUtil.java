/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
