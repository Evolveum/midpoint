/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;

/**
 * @author Viliam Repan (lazyman).
 */
public interface Mapper<I, O> {

    O map(I input, MapperContext context);
}
