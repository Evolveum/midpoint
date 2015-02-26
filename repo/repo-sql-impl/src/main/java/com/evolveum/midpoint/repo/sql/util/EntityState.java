package com.evolveum.midpoint.repo.sql.util;

/**
 * @author lazyman
 */
public interface EntityState {

    Boolean isTransient();

    void setTransient(Boolean trans);
}
