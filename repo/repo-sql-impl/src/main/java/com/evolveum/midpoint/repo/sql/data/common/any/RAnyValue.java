/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.repo.sql.util.EntityState;

import java.io.Serializable;

/**
 * @author lazyman
 */
public interface RAnyValue<T> extends Serializable, EntityState {

    String F_VALUE = "value";

    String F_ITEM_ID = "itemId";

    Integer getItemId();

    void setItemId(Integer id);

    T getValue();

    Serializable createId();
}
