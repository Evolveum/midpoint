/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
