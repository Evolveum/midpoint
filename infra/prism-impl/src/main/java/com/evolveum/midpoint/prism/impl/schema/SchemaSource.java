/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.schema;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.evolveum.midpoint.prism.impl.schema.SchemaDescriptionImpl.InputStreamable;


abstract class SchemaSource {

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

    Element element() {
        return element;
    }

    private static class Dynamic extends SchemaSource {

        private Dynamic(Element element) {
            super(element);
        }

        @Override
        public synchronized InputSource saxInputSource() {
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
