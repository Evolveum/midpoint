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

package com.evolveum.midpoint.web.page.admin.resources.content.dto;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class AccountContentSearchDto implements Serializable {

    private String searchText;
    private boolean accountName = true;
    private boolean ownerName;

    public boolean isAccountName() {
        return accountName;
    }

    public boolean isOwnerName() {
        return ownerName;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setAccountName(boolean accountName) {
        this.accountName = accountName;
    }

    public void setOwnerName(boolean ownerName) {
        this.ownerName = ownerName;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
}
