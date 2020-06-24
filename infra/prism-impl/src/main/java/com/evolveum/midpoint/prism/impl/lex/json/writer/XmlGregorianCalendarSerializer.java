/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.lex.json.writer;

import java.io.IOException;

import javax.xml.datatype.XMLGregorianCalendar;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ext.CoreXMLSerializers.XMLGregorianCalendarSerializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public class XmlGregorianCalendarSerializer extends XMLGregorianCalendarSerializer {

    @Override
    public void serialize(XMLGregorianCalendar value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(value.toXMLFormat());
    }

    @Override
    public void serializeWithType(XMLGregorianCalendar value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException {
        serialize(value, jgen, provider);
    }

}
