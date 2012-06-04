/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class UsersDto implements Serializable {

    private String searchText;
    private boolean name = true;
    private boolean fullName;
    private boolean givenName;
    private boolean familyName;

    public boolean isFullName() {
        return fullName;
    }

    public void setFullName(boolean fullName) {
        this.fullName = fullName;
    }

    public boolean isGivenName() {
        return givenName;
    }

    public void setGivenName(boolean givenName) {
        this.givenName = givenName;
    }

    public boolean isFamilyName() {
        return familyName;
    }

    public void setFamilyName(boolean familyName) {
        this.familyName = familyName;
    }

    public boolean isName() {
        return name;
    }

    public void setName(boolean name) {
        this.name = name;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
}
