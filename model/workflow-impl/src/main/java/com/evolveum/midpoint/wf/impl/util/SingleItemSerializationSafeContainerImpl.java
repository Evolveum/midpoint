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

package com.evolveum.midpoint.wf.impl.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * Helper class that allows putting (almost) arbitrary objects into Activiti processes.
 *
 * Generally, prism objects and containers and jaxb objects are stored in their XML form,
 * allowing for safe deserialization in potentially newer version of midpoint.
 *
 * Other serializable items are stored as such.
 *
 * There's a child class (JaxbValueContainer) that allows directly retrieving XML representation of the object
 * (if there's one).
 *
 * @author mederly
 */
public class SingleItemSerializationSafeContainerImpl<T> implements SerializationSafeContainer<T> {

    private static final long serialVersionUID = 7269803380754945968L;

    private static final Trace LOGGER = TraceManager.getTrace(SingleItemSerializationSafeContainerImpl.class);
    public static final int MAX_WIDTH = 500;

    // this is the actual (directly usable) value of the item
    private transient T actualValue;

    // if there's no need to encode the value, it is stored in the first attribute
    // if there is (e.g. for PrismObjects) the encoded value is stored in the second attribute
    private T valueForStorageWhenNotEncoded;
    protected String valueForStorageWhenEncoded;

    // beware, for JAXB, PRISM_OBJECT and PRISM_CONTAINER encoding schemes the value must be XML, as it might be
    // exposed through JaxbValueContainer
    protected EncodingScheme encodingScheme;

    private transient PrismContext prismContext;

    public SingleItemSerializationSafeContainerImpl(T value, PrismContext prismContext) {
        Validate.notNull(prismContext, "prismContext must not be null");
        this.prismContext = prismContext;
        setValue(value);
    }

    @Override
    public void setValue(T value) {
        this.actualValue = value;

        checkPrismContext();
        if (value != null && prismContext.canSerialize(value)) {
            try {
                this.valueForStorageWhenEncoded = prismContext.xmlSerializer().serializeAnyData(value, new QName("value"));
            } catch (SchemaException e) {
                throw new SystemException("Couldn't serialize value of type " + value.getClass() + ": " + e.getMessage(), e);
            }
            this.valueForStorageWhenNotEncoded = null;
            encodingScheme = EncodingScheme.PRISM;
        } else if (value == null || value instanceof Serializable) {
            this.valueForStorageWhenNotEncoded = value;
            this.valueForStorageWhenEncoded = null;
            encodingScheme = EncodingScheme.NONE;
            if (value instanceof Itemable) {
                throw new IllegalStateException("Itemable value is used as not-encoded serializable item; value = " + value);
            }
        } else {
            throw new IllegalStateException("Attempt to put non-serializable item " + value.getClass() + " into " + this.getClass().getSimpleName());
        }
    }

    private void checkPrismContext() {
        Validate.notNull(prismContext, "In SerializationSafeContainer the prismContext is not set up");
    }

    @Override
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
                try {
                    PrismValue prismValue = prismContext.parserFor(valueForStorageWhenEncoded).xml().parseItemValue();
                    actualValue = prismValue != null ? prismValue.getRealValue() : null;
                } catch (SchemaException e) {
                    throw new SystemException("Couldn't deserialize value from JAXB: " + StringUtils.abbreviate(valueForStorageWhenEncoded, MAX_WIDTH), e);
                }
                return actualValue;
            } else {
                throw new IllegalStateException("Unexpected encoding scheme " + encodingScheme);
            }
        }

        return null;
    }

    @Override
    public PrismContext getPrismContext() {
        return prismContext;
    }

    @Override
    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    // for testing purposes
    @Override
    public void clearActualValue() {
        actualValue = null;
    }

    public enum EncodingScheme { PRISM, NONE };

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

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        if (actualValue != null) {
			debugDumpValue(indent, sb, actualValue);
		} else if (valueForStorageWhenNotEncoded != null) {
			debugDumpValue(indent, sb, valueForStorageWhenNotEncoded);
		} else if (valueForStorageWhenEncoded != null) {
			DebugUtil.debugDumpWithLabel(sb, "encoded value", valueForStorageWhenEncoded, indent);
		} else {
			DebugUtil.debugDumpWithLabel(sb, "value", "null", indent);
		}
        return sb.toString();
    }

	private void debugDumpValue(int indent, StringBuilder sb, T value) {
		if (value instanceof DebugDumpable) {
			DebugUtil.debugDumpWithLabel(sb, "value", (DebugDumpable) value, indent);
			return;
		}
		String stringValue = null;
		if (value instanceof ExpressionType) {
			// brutal hack...
			String xml;
			try {
				xml = prismContext.xmlSerializer().serializeRealValue(value, SchemaConstantsGenerated.C_EXPRESSION);
				stringValue = DebugUtil.fixIndentInMultiline(indent, DebugDumpable.INDENT_STRING, xml);
			} catch (SchemaException e) {
				LOGGER.warn("Couldn't serialize an expression: {}", value, e);
			}
		}
		if (stringValue == null) {
			stringValue = String.valueOf(value);
		}
		DebugUtil.debugDumpWithLabel(sb, "value", stringValue, indent);
	}
}
