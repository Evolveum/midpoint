/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
