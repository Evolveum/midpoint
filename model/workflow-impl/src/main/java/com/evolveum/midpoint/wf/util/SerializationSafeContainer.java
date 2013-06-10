/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
public class SerializationSafeContainer<T> implements Serializable {

    private static final long serialVersionUID = 7269803380754945968L;

    private static final Trace LOGGER = TraceManager.getTrace(SerializationSafeContainer.class);
    public static final int MAX_WIDTH = 500;

    // this is the actual (directly usable) value of the item
    private transient T actualValue;

    // if there's no need to encode the value, it is stored in the first attribute
    // if there is (e.g. for PrismObjects) the encoded value is stored in the second attribute
    private T valueForStorageWhenNotEncoded;
    private String valueForStorageWhenEncoded;

    private EncodingScheme encodingScheme;

    private transient PrismContext prismContext;

    public SerializationSafeContainer(T value, PrismContext prismContext) {

        Validate.notNull(prismContext, "prismContext must not be null");

        this.actualValue = value;
        this.prismContext = prismContext;

        if (value instanceof PrismObject) {         // todo what with Itemable?
            this.valueForStorageWhenEncoded = MiscDataUtil.serializeObjectToXml((PrismObject) value, prismContext);
            this.valueForStorageWhenNotEncoded = null;
            encodingScheme = EncodingScheme.PRISM;
        } else if (value != null && prismContext.getPrismJaxbProcessor().canConvert(value.getClass())) {
            try {
                //this.valueForStorageWhenEncoded = prismContext.getPrismJaxbProcessor().marshalObjectToString(value);
                this.valueForStorageWhenEncoded = prismContext.getPrismJaxbProcessor().marshalElementToString(new JAXBElement<Object>(new QName("value"), Object.class, value));
            } catch (JAXBException e) {
                throw new SystemException("Couldn't serialize JAXB object of type " + value.getClass(), e);
            }
            this.valueForStorageWhenNotEncoded = null;
            encodingScheme = EncodingScheme.JAXB;
        } else {
            this.valueForStorageWhenNotEncoded = value;
            this.valueForStorageWhenEncoded = null;
            encodingScheme = EncodingScheme.NONE;

            if (value instanceof Itemable) {
                LOGGER.warn("Itemable value is used as not-encoded serializable item; value = " + value);
            }
        }
    }

    public T getValue() {

        if (actualValue != null) {
            return actualValue;
        }

        if (valueForStorageWhenNotEncoded != null) {
            actualValue = valueForStorageWhenNotEncoded;
            return actualValue;
        }

        if (valueForStorageWhenEncoded != null) {
            if (prismContext == null) {
                throw new IllegalStateException("PrismContext not set for SerializationSafeContainer holding " + StringUtils.abbreviate(valueForStorageWhenEncoded, MAX_WIDTH));
            }

            if (encodingScheme == EncodingScheme.PRISM) {
                actualValue = (T) MiscDataUtil.deserializeObjectFromXml(valueForStorageWhenEncoded, prismContext);
                return actualValue;
            } else if (encodingScheme == EncodingScheme.JAXB) {
                try {
                    LOGGER.trace("Trying to decode JAXB value {}", valueForStorageWhenEncoded);
                    actualValue = (T) prismContext.getPrismJaxbProcessor().unmarshalElement(valueForStorageWhenEncoded, Object.class).getValue();
                } catch (JAXBException e) {
                    LOGGER.trace("Problem with JAXB value {}", valueForStorageWhenEncoded);
                    throw new SystemException("Couldn't deserialize value from JAXB: " + StringUtils.abbreviate(valueForStorageWhenEncoded, MAX_WIDTH), e);
                } catch (SchemaException e) {
                    LOGGER.trace("Problem with JAXB value {}", valueForStorageWhenEncoded);
                    throw new SystemException("Couldn't deserialize value from JAXB: " + StringUtils.abbreviate(valueForStorageWhenEncoded, MAX_WIDTH), e);
                } catch (RuntimeException e) {
                    LOGGER.trace("Problem with JAXB value {}", valueForStorageWhenEncoded);
                    throw new SystemException("Couldn't deserialize value from JAXB: " + StringUtils.abbreviate(valueForStorageWhenEncoded, MAX_WIDTH), e);
                }
                return actualValue;
            } else {
                throw new IllegalStateException("Unexpected encoding scheme " + encodingScheme);
            }
        }

        return null;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public enum EncodingScheme { PRISM, JAXB, NONE };

    @Override
    public String toString() {
        return "SerializationSafeContainer{" +
                "actualValue " + (actualValue != null ? "SET" : "NOT SET") +
                ", valueForStorageWhenNotEncoded=" + valueForStorageWhenNotEncoded +
                ", valueForStorageWhenEncoded='" + valueForStorageWhenEncoded + '\'' +
                ", encodingScheme=" + encodingScheme +
                ", prismContext " + (prismContext != null ? "SET" : "NOT SET") +
                '}';
    }
}
