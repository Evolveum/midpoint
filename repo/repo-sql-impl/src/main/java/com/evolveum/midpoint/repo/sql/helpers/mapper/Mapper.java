/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;

/**
 * @author Viliam Repan (lazyman).
 */
public interface Mapper<I, O> {

    O map(I input, MapperContext context);
}
