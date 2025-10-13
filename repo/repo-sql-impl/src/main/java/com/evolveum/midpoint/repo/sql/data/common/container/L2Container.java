/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.container;

import com.evolveum.midpoint.repo.sql.util.EntityState;

import java.io.Serializable;

/**
 * "Level 2 container" = container in container in object
 */
public interface L2Container<T extends Container> extends EntityState, Serializable {

    T getOwner();

    String getOwnerOwnerOid();

    Integer getOwnerId();

    Integer getId();

    void setOwner(T owner);

    void setOwnerOwnerOid(String ownerOwnerOid);

    void setOwnerId(Integer id);

    void setId(Integer id);
}
