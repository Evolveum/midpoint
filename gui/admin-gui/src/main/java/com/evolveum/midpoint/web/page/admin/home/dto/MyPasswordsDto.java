/*
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class MyPasswordsDto implements Serializable {

    public static final String F_ACCOUNTS = "accounts";

    private boolean changeIdmPassword;
    private List<PasswordAccountDto> accounts;
    private String password;

    public List<PasswordAccountDto> getAccounts() {
        if (accounts == null) {
            accounts = new ArrayList<PasswordAccountDto>();
        }
        return accounts;
    }

    public void setChangeIdmPassword(boolean changeIdmPassword) {
        this.changeIdmPassword = changeIdmPassword;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
