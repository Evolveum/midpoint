/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api.query;

/**
 * @author lazyman
 */
public enum SimpleFilterType {

    EQ("equal"), GT("gt"), LT("lt"), GTEQ("gtEqual"), LTEQ("ltEqual"), NEQ("notEqual"),
    NULL("null"), NOT_NULL("notNull"), LIKE("like");

    private String elementName;

    private SimpleFilterType(String elementName) {
        this.elementName = elementName;
    }

    public String getElementName() {
        return elementName;
    }
}
