/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.resources.content.dto;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class AccountContentSearchDto implements Serializable {

    public static final String F_SEARCH_TEXT = "searchText";
    public static final String F_NAME = "name";
    public static final String F_IDENTIFIERS = "identifiers";

    private String searchText;
    private boolean name = true;
    private boolean identifiers;

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public boolean isIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(boolean identifiers) {
        this.identifiers = identifiers;
    }

    public boolean isName() {
        return name;
    }

    public void setName(boolean name) {
        this.name = name;
    }
}
