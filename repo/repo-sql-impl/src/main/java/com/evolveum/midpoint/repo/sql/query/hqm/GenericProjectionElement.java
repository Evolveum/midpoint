/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.hqm;

public class GenericProjectionElement extends ProjectionElement {

    private final String text;

    public GenericProjectionElement(String text) {
        this.text = text;
    }

    protected void dumpToHql(StringBuilder sb) {
        sb.append(text);
    }
}
