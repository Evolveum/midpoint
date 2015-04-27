/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.cli.ninja.command;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import com.evolveum.midpoint.cli.ninja.util.ObjectType;
import org.apache.commons.lang.StringUtils;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class ObjectTypeConverter implements IStringConverter<QName>, IValueValidator {

    @Override
    public QName convert(String value) {
        return ObjectType.getType(value);
    }

    @Override
    public void validate(String name, Object value) throws ParameterException {
        if (value == null) {
            return;
        }

        if (!(value instanceof String) || ObjectType.getType((String) value) == null) {
            StringBuilder sb = createMessage(value);
            throw new ParameterException(sb.toString());
        }
    }

    private StringBuilder createMessage(Object value) {
        StringBuilder sb = new StringBuilder();
        sb.append("Unknown value '").append(value).append("', possible values are: ");

        List<String> values = new ArrayList<>();
        for (ObjectType type : ObjectType.values()) {
            values.add(type.getParameterName());
        }
        sb.append(StringUtils.join(values, ", "));
        sb.append('.');

        return sb;
    }
}
