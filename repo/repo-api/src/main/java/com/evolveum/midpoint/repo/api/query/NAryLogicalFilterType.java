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
public enum NAryLogicalFilterType {

    AND("and"), OR("or");

    private String elementName;

    NAryLogicalFilterType(String elementName) {
        this.elementName = elementName;
    }

    public String getElementName() {
        return elementName;
    }
}
