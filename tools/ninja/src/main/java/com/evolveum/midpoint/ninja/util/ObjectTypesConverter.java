package com.evolveum.midpoint.ninja.util;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ObjectTypesConverter implements IStringConverter<Set<ObjectTypes>>, IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value == null) {
            return;
        }

        Set<ObjectTypes> types = getType(value);

        if (types.isEmpty()) {
            throw new ParameterException("Unknown value " + value + " for option " + name);
        }
    }

    @Override
    public Set<ObjectTypes> convert(String value) {
        if (value == null) {
            return new HashSet<>();
        }

        Set<ObjectTypes> types = getType(value);

        if (types.isEmpty()) {
            throw new IllegalArgumentException("Unknown object type " + value);
        }

        return types;
    }

    private Set<ObjectTypes> getType(String value) {
        Set<ObjectTypes> set = new HashSet<>();

        if (StringUtils.isEmpty(value)) {
            return set;
        }

        String[] items = value.split(",");
        for (String item : items) {
            if (StringUtils.isEmpty(item)) {
                continue;
            }

            boolean found = false;
            for (ObjectTypes o : ObjectTypes.values()) {
                if (o.name().equalsIgnoreCase(item) || o.getRestType().equalsIgnoreCase(value)) {
                    set.add(o);
                    found = true;
                }
            }

            if (!found) {
                throw new IllegalArgumentException("Unknown object type " + item);
            }
        }


        return set;
    }
}
