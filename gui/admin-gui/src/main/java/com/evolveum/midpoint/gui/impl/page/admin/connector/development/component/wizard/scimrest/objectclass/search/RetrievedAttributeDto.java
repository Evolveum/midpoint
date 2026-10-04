/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search;

import java.io.Serial;
import java.io.Serializable;

/**
 * One attribute of the object returned by the GET operation.
 */
public class RetrievedAttributeDto implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    public static final String F_NAME = "name";
    public static final String F_VALUE = "value";

    private final String name;
    private final String value;

    public RetrievedAttributeDto(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
