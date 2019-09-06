/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;

import java.io.Serializable;

/**
 * @author lazyman
 */
public interface ObjectReference extends Serializable {

    String F_TARGET_OID = "targetOid";

    String F_RELATION = "relation";

    String F_TARGET_TYPE = "type";

    String getTargetOid();

    String getRelation();

    RObjectType getType();

    void setRelation(String relation);

    void setTargetOid(String targetOid);

    void setType(RObjectType type);
}
