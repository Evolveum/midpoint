/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
}
