/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm;

/**
 * @author mederly
 */
public class GenericProjectionElement extends ProjectionElement {

    private String text;

    public GenericProjectionElement(String text) {
        this.text = text;
    }

    protected void dumpToHql(StringBuilder sb) {
        sb.append(text);
    }
}
