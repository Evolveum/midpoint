package com.evolveum.midpoint.prism.impl.schema;

import java.io.InputStream;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.evolveum.concepts.func.FailableFunction;
import com.evolveum.midpoint.util.exception.SchemaException;

public class SchemaSource {

    private final Element element;
    private final FailableFunction<Element, InputStream, SchemaException> elementToStream;


    public SchemaSource(Element element, FailableFunction<Element, InputStream, SchemaException> streamProvider) {
        this.element = element;
        this.elementToStream = streamProvider;
    }

    public InputSource xsomInputSource() throws SchemaException {
        InputSource inSource = new InputSource(elementToStream.apply(element));
        inSource.setEncoding("utf-8");
        return null;
    }

    public Element element() {
        return element;
    }
}
