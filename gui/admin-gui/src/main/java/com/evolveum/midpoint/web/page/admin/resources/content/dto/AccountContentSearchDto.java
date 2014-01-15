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
