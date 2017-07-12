/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
//			output.writeCharacters(serialized);
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
