/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PolyStringMapper implements Mapper<PolyString, RPolyString> {

    @Override
    public RPolyString map(PolyString input, MapperContext context) {
        return new RPolyString(input.getOrig(), input.getNorm());
    }
}
