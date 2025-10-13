/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.other;

/**
 * This is just helper enumeration for different types of reference entities
 * used in many relationships.
 *
 * @author lazyman
 */
public enum RCReferenceType {

    CREATE_APPROVER,        // 0

    MODIFY_APPROVER,        // 1

    CASE_REVIEWER;          // 2
}
