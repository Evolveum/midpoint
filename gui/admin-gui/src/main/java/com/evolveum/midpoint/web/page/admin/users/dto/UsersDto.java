/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author lazyman
 */
public class UsersDto implements Serializable {

    public static enum SearchType {

        NAME("SearchType.NAME"),
        GIVEN_NAME("SearchType.GIVEN_NAME"),
        FAMILY_NAME("SearchType.FAMILY_NAME"),
        FULL_NAME("SearchType.FULL_NAME");

        private String key;

        private SearchType(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public static final String F_TEXT = "text";
    public static final String F_TYPE = "type";

    private String text;
    private Collection<SearchType> type;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Collection<SearchType> getType() {
        if (type == null) {
            type = new ArrayList<>();
            type.add(SearchType.NAME);
        }
        return type;
    }

    public void setType(Collection type) {
        this.type = type;
    }

    public boolean hasType(SearchType type) {
        if (getType().contains(type)) {
            return true;
        }
        return false;
    }
}
