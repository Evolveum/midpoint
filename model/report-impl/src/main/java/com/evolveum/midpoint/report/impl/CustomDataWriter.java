/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import java.util.Collection;

import javax.xml.validation.Schema;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;

public class CustomDataWriter implements DataWriter<XMLStreamWriter>{

    private PrismContext prismContex;


    public CustomDataWriter(PrismContext prismContex) {
        this.prismContex = prismContex;
    }


    @Override
    public void write(Object obj, MessagePartInfo part, XMLStreamWriter output) {
        QName rootElement = part.getElementQName();
        Element serialized;
        try {
            serialized = prismContex.domSerializer().serializeAnyData(obj, rootElement);
            StaxUtils.copy(serialized, output);
//            output.writeCharacters(serialized);
        } catch (SchemaException | XMLStreamException e) {
            // TODO Auto-generated catch block
            throw new Fault(e);
        }


    }

    @Override
    public void setAttachments(Collection<Attachment> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setProperty(String arg0, Object arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSchema(Schema arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void write(Object arg0, XMLStreamWriter arg1) {
        // TODO Auto-generated method stub

    }

}
