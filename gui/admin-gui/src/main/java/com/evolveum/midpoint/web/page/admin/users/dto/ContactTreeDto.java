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
