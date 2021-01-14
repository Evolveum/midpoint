/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class MoreDialogDto implements Serializable {

    public static final String F_NAME_FILTER = "nameFilter";
    public static final String F_PROPERTIES = "properties";

    private String nameFilter;

    private List<SearchItemDefinition> properties;

    public String getNameFilter() {
        return nameFilter;
    }

    public void setNameFilter(String nameFilter) {
        this.nameFilter = nameFilter;
    }

    public List<SearchItemDefinition> getProperties() {
        if (properties == null) {
            properties = new ArrayList<>();
        }
        return properties;
    }

    public void setProperties(List<SearchItemDefinition> properties) {
        this.properties = properties;
    }
}
