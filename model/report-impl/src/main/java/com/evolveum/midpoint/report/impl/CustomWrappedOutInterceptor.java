package com.evolveum.midpoint.report.impl;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.CachingXmlEventWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.wsdl.interceptors.BareOutInterceptor;

import com.evolveum.midpoint.prism.PrismContext;

public class CustomWrappedOutInterceptor extends BareOutInterceptor{

	private PrismContext prismContext;

	public CustomWrappedOutInterceptor(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	@Override
	public void handleMessage(Message message) {
		super.handleMessage(message);
		Interceptor<? extends Message> defaultInterceptor = null;
    	for (Iterator<Interceptor<? extends Message>> iterator = message.getInterceptorChain().getIterator(); iterator.hasNext();) {
    		Interceptor<? extends Message> interceptor = iterator.next();
    		if (interceptor instanceof BareOutInterceptor) {
    			defaultInterceptor = interceptor;
		    }
		}
    	message.getInterceptorChain().remove(defaultInterceptor);
	}

	@Override
	protected void writeParts(Message message, Exchange exchange,
            BindingOperationInfo operation, MessageContentsList objs,
            List<MessagePartInfo> parts) {
		// TODO Auto-generated method stub
		OutputStream out = message.getContent(OutputStream.class);
        XMLStreamWriter origXmlWriter = message.getContent(XMLStreamWriter.class);
        Service service = exchange.getService();
        XMLStreamWriter xmlWriter = origXmlWriter;
        CachingXmlEventWriter cache = null;

        Object en = message.getContextualProperty(OUT_BUFFERING);
        boolean allowBuffer = true;
        boolean buffer = false;
        if (en != null) {
            buffer = Boolean.TRUE.equals(en) || "true".equals(en);
            allowBuffer = !(Boolean.FALSE.equals(en) || "false".equals(en));
        }
        // need to cache the events in case validation fails or buffering is enabled
        if (buffer || (allowBuffer && shouldValidate(message) && !isRequestor(message))) {
            cache = new CachingXmlEventWriter();
            try {
                cache.setNamespaceContext(origXmlWriter.getNamespaceContext());
            } catch (XMLStreamException e) {
                //ignorable, will just get extra namespace decls
            }
            xmlWriter = cache;
            out = null;
        }

        if (out != null
            && writeToOutputStream(message, operation.getBinding(), service)
            && !MessageUtils.isTrue(message.getContextualProperty(DISABLE_OUTPUTSTREAM_OPTIMIZATION))) {
            if (xmlWriter != null) {
                try {
                    xmlWriter.writeCharacters("");
                    xmlWriter.flush();
                } catch (XMLStreamException e) {
                    throw new Fault(e);
                }
            }

            DataWriter<OutputStream> osWriter = getDataWriter(message, service, OutputStream.class);

            for (MessagePartInfo part : parts) {
                if (objs.hasValue(part)) {
                    Object o = objs.get(part);
                    osWriter.write(o, part, out);
                }
            }
        } else {
            DataWriter<XMLStreamWriter> dataWriter = new CustomDataWriter(prismContext);

            for (MessagePartInfo part : parts) {
                if (objs.hasValue(part)) {
                    Object o = objs.get(part);
                    dataWriter.write(o, part, xmlWriter);
                }
            }
        }
        if (cache != null) {
            try {
                for (XMLEvent event : cache.getEvents()) {
                    StaxUtils.writeEvent(event, origXmlWriter);
                }
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
        }
	}


}
