/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.autocomplete;

import java.util.Locale;

import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.converter.AbstractConverter;

import com.evolveum.midpoint.prism.ItemDefinition;

public class ItemDefinitionConverter extends AbstractConverter<ItemDefinition<?>>{

    private static final long serialVersionUID = 1L;

    @Override
    public ItemDefinition<?> convertToObject(String value, Locale locale) throws ConversionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Class<ItemDefinition<?>> getTargetType() {
        // TODO Auto-generated method stub
        return null;
    }

}
