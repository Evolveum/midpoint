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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.web.component.data.column.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.Selectable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class UserListItemDto extends Selectable implements InlineMenu {

    public static final String F_ICON = "icon";
    public static final String F_NAME = "name";
    public static final String F_GIVEN_NAME = "givenName";
    public static final String F_FAMILY_NAME = "familyName";
    public static final String F_FULL_NAME = "fullName";
    public static final String F_ACCOUNT_COUNT = "accountCount";
    public static final String F_EMAIL = "email";

    private String oid;
    private String name;
    private String givenName;
    private String familyName;
    private String fullName;
    private String email;
    private int accountCount;
    private String icon;
    private PrismContainer credentials;

    public UserListItemDto(String oid, String name, String givenName,
                           String familyName, String fullName, String email) {
        this.oid = oid;
        this.familyName = familyName;
        this.fullName = fullName;
        this.givenName = givenName;
        this.name = name;
        this.email = email;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public String getOid() {
        return oid;
    }

    public String getEmail() {
        return email;
    }

    public int getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(int accountCount) {
        this.accountCount = accountCount;
    }

    public PrismContainer getCredentials() {
        return credentials;
    }

    public void setCredentials(PrismContainer credentials) {
        this.credentials = credentials;
    }

    @Override
    public List<InlineMenuItem> getMenuItems() {
        return new ArrayList<InlineMenuItem>();
    }
}
