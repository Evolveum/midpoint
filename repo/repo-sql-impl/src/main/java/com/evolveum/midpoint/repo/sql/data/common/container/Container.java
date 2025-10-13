/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
