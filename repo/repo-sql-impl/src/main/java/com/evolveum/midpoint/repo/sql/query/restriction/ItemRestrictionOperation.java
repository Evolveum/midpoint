/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
