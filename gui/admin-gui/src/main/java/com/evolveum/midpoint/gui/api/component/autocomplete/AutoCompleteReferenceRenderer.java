/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.autocomplete;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteRenderer;
import org.apache.wicket.request.Response;

/**
 * @author honchar
 */
public class AutoCompleteReferenceRenderer extends AbstractAutoCompleteRenderer<ObjectReferenceType> {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getTextValue(ObjectReferenceType object) {
        return WebComponentUtil.getName(object);
    }

    @Override
    protected void renderChoice(ObjectReferenceType object, Response response, String criteria) {
        String textValue = getTextValue(object);
        response.write(textValue);

    }
}
