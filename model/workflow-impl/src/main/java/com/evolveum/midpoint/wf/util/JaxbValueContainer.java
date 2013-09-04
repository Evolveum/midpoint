package com.evolveum.midpoint.wf.util;

import com.evolveum.midpoint.prism.PrismContext;

import java.io.Serializable;

/**
 * This is used in process variables that could be used either as XML or as java object (JAXB).
 *
 * Implementation is done using quite a hack where internal XML representation used in SerializationSafeContainer
 * is exposed to clients.
 *
 * @author mederly
 */
public class JaxbValueContainer<T extends Serializable> extends SerializationSafeContainer<T> {

    private static final long serialVersionUID = 1233214324324368L;

    public JaxbValueContainer(T value, PrismContext prismContext) {
        super(value, prismContext);
    }

    public String getXmlValue() {
        if (encodingScheme != EncodingScheme.JAXB && encodingScheme != EncodingScheme.PRISM_CONTAINER && encodingScheme != EncodingScheme.PRISM_OBJECT) {
            throw new UnsupportedOperationException("Couldn't obtain an XML representation of an object; encodingScheme = " + encodingScheme);
        }

        if (valueForStorageWhenEncoded == null) {
            throw new IllegalStateException("XML value of an element is null");
        }

        return valueForStorageWhenEncoded;
    }
}
