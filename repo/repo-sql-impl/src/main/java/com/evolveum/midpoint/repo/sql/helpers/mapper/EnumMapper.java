/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Created by Viliam Repan (lazyman).
 */
public class EnumMapper implements Mapper<Enum, SchemaEnum> {

    @Override
    public SchemaEnum map(Enum input, MapperContext context) {
        String repoEnumClass = null;
        try {
            String className = input.getClass().getSimpleName();
            className = StringUtils.left(className, className.length() - 4);
            repoEnumClass = "com.evolveum.midpoint.repo.sql.data.common.enums.R" + className;
            Class clazz = Class.forName(repoEnumClass);

            if (!SchemaEnum.class.isAssignableFrom(clazz)) {
                throw new SystemException("Can't translate enum value " + input);
            }

            return RUtil.getRepoEnumValue(input, clazz);
        } catch (ClassNotFoundException ex) {
            throw new SystemException("Couldn't find class '" + repoEnumClass + "' for enum '" + input + "'", ex);
        }
    }
}
