/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
