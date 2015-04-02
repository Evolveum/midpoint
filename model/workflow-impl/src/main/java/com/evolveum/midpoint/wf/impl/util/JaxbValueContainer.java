package com.evolveum.midpoint.wf.impl.util;

import com.evolveum.midpoint.prism.PrismContext;

/**
 * This is used in process variables that could be used either as XML or as java object (JAXB).
 *
 * Implementation is done using quite a hack where internal XML representation used in SerializationSafeContainer
 * is exposed to clients.
 *
 * TODO TODO TODO decide how to deal with this one (w.r.t. SerializationSafeContainer vs. its implementations)
 *
 * @author mederly
 */
public class JaxbValueContainer<T> extends SingleItemSerializationSafeContainerImpl<T> {

    private static final long serialVersionUID = 1233214324324368L;

    public static final int TEXT_CHUNK_SIZE = 3500;             // experimental

    public JaxbValueContainer(T value, PrismContext prismContext) {
        super(value, prismContext);
    }

    public String getXmlValue() {
        if (encodingScheme != EncodingScheme.PRISM) {
            throw new UnsupportedOperationException("Couldn't obtain an XML representation of an object; encodingScheme = " + encodingScheme);
        }

        if (valueForStorageWhenEncoded == null) {
            throw new IllegalStateException("XML value of an element is null");
        }

        return valueForStorageWhenEncoded;
    }

    // experimental
    public String getXmlValue(int chunkNumber) {
        return getChunk(getXmlValue(), chunkNumber);
    }

    public static String getChunk(String wholeData, int chunkNumber) {
        int startPosition = chunkNumber * TEXT_CHUNK_SIZE;
        if (wholeData.length() < startPosition) {
            return "";
        } else {
            int endPosition = startPosition + TEXT_CHUNK_SIZE;
            if (endPosition > wholeData.length()) {
                endPosition = wholeData.length();
            }
            return wholeData.substring(startPosition, endPosition);
        }
    }

    // prerequisite: the object already contained a value, so the encoding scheme is known
    public void setXmlValue(String newValue) {
        if (encodingScheme != EncodingScheme.PRISM) {
            throw new UnsupportedOperationException("Couldn't set new XML value for an object with encodingScheme = " + encodingScheme);
        }
        valueForStorageWhenEncoded = newValue;
        clearActualValue();
    }
}
