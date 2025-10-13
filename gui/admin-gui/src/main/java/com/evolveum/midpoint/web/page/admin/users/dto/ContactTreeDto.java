/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ContactTreeDto implements Serializable {

    private String name;

    public ContactTreeDto(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
