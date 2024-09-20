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

public class SchemaEnumerationConstantNameValidator implements IValidator<String> {


    public SchemaEnumerationConstantNameValidator() {
    }

    @Override
    public void validate(IValidatable<String> validatable) {
        String value = validatable.getValue();
        if (value == null) {
            return;
        }

        InputStream is = new ByteArrayInputStream(
                ("<xsd:schema xmlns:xsd='" + XMLConstants.W3C_XML_SCHEMA_NS_URI + "' xmlns:jaxb='https://jakarta.ee/xml/ns/jaxb'>"
                        + "    <xsd:simpleType name='Test'>"
                        + "        <xsd:restriction base='xsd:string'>"
                        + "            <xsd:enumeration value='testValue'>"
                        + "                <xsd:annotation>"
                        + "                    <xsd:appinfo>"
                        + "                        <jaxb:typesafeEnumMember name='" + value + "'/>"
                        + "                    </xsd:appinfo>"
                        + "                </xsd:annotation>"
                        + "            </xsd:enumeration>        "
                        + "        </xsd:restriction>"
                        + "    </xsd:simpleType>"
                        + "</xsd:schema>").getBytes() );

        if (!StringUtils.isAllUpperCase(value) || StringUtils.containsWhitespace(value)) {
            throw new IllegalArgumentException();
        }

        try {
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(is));
        } catch (SAXException | IllegalArgumentException e) {

            String normalizedName = DOMUtil.escapeInvalidXmlCharsIfPresent(value);
            normalizedName = StringUtils.upperCase(normalizedName);
            normalizedName = StringUtils.trim(normalizedName);
            normalizedName = normalizedName.replaceAll("\\s", "_");

            ValidationError error = new ValidationError();
            error.addKey("SchemaEnumerationConstantNameValidator.invalidConstantName");
            error.setVariable("0", value);
            error.setVariable("1", normalizedName);
            error.setMessage("Constant name '" + value + "' is not valid. Try '" + normalizedName + "'.");
            validatable.error(error);
        }
    }
}
