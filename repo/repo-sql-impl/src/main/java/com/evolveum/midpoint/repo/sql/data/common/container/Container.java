/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.container;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.util.EntityState;

import java.io.Serializable;

/**
 * @author lazyman
 */
public interface Container<T extends RObject> extends EntityState, Serializable {

    T getOwner();

    String getOwnerOid();

    Integer getId();

    void setOwner(T owner);

    void setOwnerOid(String ownerOid);

    void setId(Integer id);
}
