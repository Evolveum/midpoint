/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.autocomplete;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteRenderer;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.string.Strings;

import com.evolveum.midpoint.prism.ItemDefinition;

public class AutoCompleteItemDefinitionRenderer extends AbstractAutoCompleteRenderer<ItemDefinition<?>>{

    private static final long serialVersionUID = 1L;

    @Override
    protected String getTextValue(ItemDefinition<?> object) {
        return object.getItemName().getLocalPart();
    }

    @Override
    protected void renderChoice(ItemDefinition<?> object, Response response, String criteria) {
        String textValue = getTextValue(object);
        textValue = Strings.escapeMarkup(textValue).toString();
        response.write(textValue);

    }


}
