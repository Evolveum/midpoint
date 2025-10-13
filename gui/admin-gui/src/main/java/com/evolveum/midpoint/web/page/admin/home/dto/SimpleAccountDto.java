/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
