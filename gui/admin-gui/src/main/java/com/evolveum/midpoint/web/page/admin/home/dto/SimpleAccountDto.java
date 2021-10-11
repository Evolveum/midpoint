/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class SimpleAccountDto implements Serializable {

    private String accountName;
    private String resourceName;

    public SimpleAccountDto(String accountName, String resourceName) {
        this.accountName = accountName;
        this.resourceName = resourceName;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getResourceName() {
        return resourceName;
    }
}
