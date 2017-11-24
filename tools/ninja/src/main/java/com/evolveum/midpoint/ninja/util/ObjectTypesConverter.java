package com.evolveum.midpoint.ninja.util;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ObjectTypesConverter implements IStringConverter<ObjectTypes>, IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value == null) {
            return;
        }

        ObjectTypes type = getType(value);

        if (type == null) {
            throw new ParameterException("Unknown value " + value + " for option " + name);
        }
    }

    @Override
    public ObjectTypes convert(String value) {
        if (value == null) {
            return null;
        }

        ObjectTypes type = getType(value);

        if (type == null) {
            throw new IllegalArgumentException("Unknown object type " + value);
        }

        return type;
    }

    private ObjectTypes getType(String value) {
        for (ObjectTypes o : ObjectTypes.values()) {
            if (o.name().toLowerCase().equals(value.toLowerCase())
                    || o.getRestType().equals(value)) {
                return o;
            }
        }

        return null;
    }
}
