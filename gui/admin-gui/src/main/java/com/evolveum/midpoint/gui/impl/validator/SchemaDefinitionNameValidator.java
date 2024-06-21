/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.validator;

import com.evolveum.midpoint.util.DOMUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class SchemaDefinitionNameValidator implements IValidator<QName> {


    public SchemaDefinitionNameValidator() {
    }

    @Override
    public void validate(IValidatable<QName> validatable) {
        QName value = validatable.getValue();
        if (value == null) {
            return;
        }

        InputStream is = new ByteArrayInputStream(
                ("<xsd:schema xmlns:xsd='" + XMLConstants.W3C_XML_SCHEMA_NS_URI + "'><xsd:complexType name='"
                        + value.getLocalPart()
                        + "'/></xsd:schema>").getBytes() );
        try {
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(is));
        } catch (SAXException e) {

            String normalizedName = DOMUtil.escapeInvalidXmlCharsIfPresent(value.getLocalPart());
            normalizedName = WordUtils.capitalizeFully(normalizedName);
            normalizedName = StringUtils.uncapitalize(normalizedName);
            normalizedName = StringUtils.deleteWhitespace(normalizedName);

            ValidationError error = new ValidationError();
            error.addKey("SchemaDefinitionNameValidator.invalidName");
            error.setVariable("0", value.getLocalPart());
            error.setVariable("1", normalizedName);
            error.setMessage("Name '" + value.getLocalPart() + "' is not valid. Try '" + normalizedName + "'.");
            validatable.error(error);
        }
    }
}
