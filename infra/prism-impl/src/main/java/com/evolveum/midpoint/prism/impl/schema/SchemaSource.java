package com.evolveum.midpoint.prism.impl.schema;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.evolveum.midpoint.prism.impl.schema.SchemaDescriptionImpl.InputStreamable;


public abstract class SchemaSource {

    private final Element element;

    public SchemaSource(Element element) {
        this.element = element;
    }

    static SchemaSource from(Element element) {
        return new Dynamic(element);
    }

    static SchemaSource from(Element element, SchemaDescriptionImpl.InputStreamable streamable) {
        if(streamable == null) {
            return from(element);
        }
        return new Streamable(element, streamable);
    }

    public abstract InputSource saxInputSource();

    public Element element() {
        return element;
    }

    private static class Dynamic extends SchemaSource {

        private Dynamic(Element element) {
            super(element);
        }

        @Override
        public InputSource saxInputSource() {
            InputSource inputSource = new InputSource(DomToSchemaProcessor.inputStreamFrom(this.element()));
            inputSource.setEncoding("UTF-8");
            return inputSource;
        }
    }

    private static class Streamable extends SchemaSource {

        private final InputStreamable streamable;

        private Streamable(Element element, InputStreamable streamable) {
            super(element);
            this.streamable = streamable;
        }

        @Override
        public InputSource saxInputSource() {
            return new InputSource(streamable.openInputStream());
        }
    }
}
