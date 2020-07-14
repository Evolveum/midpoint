/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

/**
 * @author lazyman
 */
public enum ItemRestrictionOperation {

    // TODO: NULL and NOT_NULL are never assigned and probably useless here.
    // Instead, RootHibernateQuery.createIs(Not)Null is used directly.

    EQ("="), GT(">"), GE(">="), LT("<"), LE("<="), NULL, NOT_NULL, SUBSTRING, STARTS_WITH, ENDS_WITH;

    private String symbol;

    ItemRestrictionOperation() {
    }

    ItemRestrictionOperation(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
