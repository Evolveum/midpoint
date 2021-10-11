/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.util.DisplayableValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchItemPopoverDto<T extends Serializable> implements Serializable {

    public static final String F_VALUES = "values";

    private List<DisplayableValue<T>> values;

    public List<DisplayableValue<T>> getValues() {
        if (values == null) {
            values = new ArrayList<>();
        }
        return values;
    }
}
