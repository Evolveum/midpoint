/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
