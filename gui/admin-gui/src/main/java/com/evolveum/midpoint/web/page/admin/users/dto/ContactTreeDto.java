/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
