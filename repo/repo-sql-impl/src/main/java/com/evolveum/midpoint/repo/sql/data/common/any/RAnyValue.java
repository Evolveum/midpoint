/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
