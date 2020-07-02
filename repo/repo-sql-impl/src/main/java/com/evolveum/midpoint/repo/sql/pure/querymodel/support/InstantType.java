package com.evolveum.midpoint.repo.sql.pure.querymodel.support;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import javax.annotation.Nullable;

import com.querydsl.sql.types.JSR310InstantType;

/**
 * Instant converter for Querydsl.
 * Just like the superclass, but does NOT use calendar parameter for get/set values.
 * TODO: this is rather an experiment:
 * It is important to use one version consistently, if mixing prepared statements using version
 * with and without calendar parameter, results can be off by default timezone offset.
 * This works for us using JDBC - the question is: What method is used by ORM/JPA?
 * The question is, what is used y
 */
public class InstantType extends JSR310InstantType {

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
