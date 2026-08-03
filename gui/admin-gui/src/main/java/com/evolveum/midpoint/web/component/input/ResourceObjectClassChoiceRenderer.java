/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

import org.apache.wicket.markup.html.form.IChoiceRenderer;

import javax.xml.namespace.QName;

//TODO very simplified, what about same local part and different namespace?
public class ResourceObjectClassChoiceRenderer implements IChoiceRenderer<QName> {
    @Override
    public Object getDisplayValue(QName resourceObjectTypeDefinitionType) {
        if (resourceObjectTypeDefinitionType == null) {
            return null;
        }
        return resourceObjectTypeDefinitionType.getLocalPart();
    }

}
