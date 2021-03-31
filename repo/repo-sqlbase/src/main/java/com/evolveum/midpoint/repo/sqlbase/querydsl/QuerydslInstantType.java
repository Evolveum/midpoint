/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import com.querydsl.sql.types.JSR310InstantType;
import org.jetbrains.annotations.Nullable;

/**
 * Instant converter for Querydsl.
 * Just like the superclass, but does NOT use calendar parameter for get/set values.
 * <p>
 * It is important to use one version consistently, if mixing prepared statements using version
 * with and without calendar parameter, results can be off by default timezone offset.
 * This works for us using JDBC - the question is: What method is used by ORM/JPA?
 */
public class QuerydslInstantType extends JSR310InstantType {

    @Nullable
    @Override
    public Instant getValue(ResultSet rs, int startIndex) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(startIndex);
        return timestamp != null ? timestamp.toInstant() : null;
    }

    @Override
    public void setValue(PreparedStatement st, int startIndex, Instant value) throws SQLException {
        st.setTimestamp(startIndex, Timestamp.from(value));
    }
}
