/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.RUtil;

import javax.xml.namespace.QName;

/**
 * Created by Viliam Repan (lazyman).
 */
public class QNameMapper implements Mapper<QName, String> {

    @Override
    public String map(QName input, MapperContext context) {
        return RUtil.qnameToString(input);
    }
}
