/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import org.apache.commons.lang.StringUtils;

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
