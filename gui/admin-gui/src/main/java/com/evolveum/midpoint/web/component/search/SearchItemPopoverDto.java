package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.util.DisplayableValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchItemPopoverDto implements Serializable {

    public static final String F_VALUES = "values";

    private List<DisplayableValue> values;

    public List<DisplayableValue> getValues() {
        if (values == null) {
            values = new ArrayList<>();
        }
        return values;
    }
}
