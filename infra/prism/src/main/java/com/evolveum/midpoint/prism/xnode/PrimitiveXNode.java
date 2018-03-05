/*
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.prism.xnode;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrimitiveXNode<T> extends XNode implements Serializable {

	private static final Trace LOGGER = TraceManager.getTrace(PrimitiveXNode.class);

    /*
     * Invariants:
     *   - At most one of value-valueParser may be null.
     *   - If value is non-null, super.typeName must be non-null.
     */
	private T value;
	private ValueParser<T> valueParser;

	/**
	 * If set to true then this primitive value either came from an attribute
	 * or we prefer this to be represented as an attribute (if the target format
	 * is capable of representing attributes)
	 */
	private boolean isAttribute = false;

	public PrimitiveXNode() {
		super();
	}

	public PrimitiveXNode(T value) {
		super();
		this.value = value;
	}

	private void parseValue(@NotNull QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException {
        Validate.notNull(typeName, "Cannot parse primitive XNode without knowing its type");
		if (valueParser != null) {
			value = valueParser.parse(typeName, mode);
			// Necessary. It marks that the value is parsed. It also frees some memory.
			valueParser = null;
		}
	}

	public T getValue() {
		return value;
	}

	@Deprecated
	public T getParsedValue(@NotNull QName typeName) throws SchemaException {
		return getParsedValue(typeName, null, XNodeProcessorEvaluationMode.STRICT);
	}

	public T getParsedValue(@NotNull QName typeName, @Nullable Class<T> expectedClass) throws SchemaException {
		return getParsedValue(typeName, expectedClass, XNodeProcessorEvaluationMode.STRICT);
	}

	public T getParsedValue(@NotNull QName typeName, @Nullable Class<T> expectedClass, XNodeProcessorEvaluationMode mode) throws SchemaException {
		if (!isParsed()) {
			parseValue(typeName, mode);
		}
		if (JavaTypeConverter.isTypeCompliant(value, expectedClass)) {
			return value;
		} else {
			throw new SchemaException("Expected " + expectedClass + " but got " + value.getClass() + " instead. Value is " + value);
		}
	}

	public ValueParser<T> getValueParser() {
		return valueParser;
	}

	public void setValueParser(ValueParser<T> valueParser) {
        Validate.notNull(valueParser, "Value parser cannot be null");
		this.valueParser = valueParser;
        this.value = null;
	}

	public void setValue(T value, QName typeQName) {
        if (value != null) {
            if (typeQName == null) {
                // last desperate attempt to determine type name from the value type
                typeQName = XsdTypeMapper.getJavaToXsdMapping(value.getClass());
                if (typeQName == null) {
                    throw new IllegalStateException("Cannot determine type QName for a value of '" + value + "'");            // todo show only class? (security/size reasons)
                }
            }
			this.setTypeQName(typeQName);
        }
		this.value = value;
        this.valueParser = null;
	}

	public boolean isParsed() {
		return valueParser == null;
	}

	public boolean isAttribute() {
		return isAttribute;
	}

	public void setAttribute(boolean isAttribute) {
		this.isAttribute = isAttribute;
	}

	@Override
    public boolean isEmpty() {
		if (!isParsed()) {
			return valueParser.isEmpty();
		}
		if (value == null) {
			return true;
		}
		if (value instanceof String) {
			return StringUtils.isBlank((String)value);
		}
		return false;
	}

    /**
     * Returns parsed value without actually changing node state from UNPARSED to PARSED
     * (if node is originally unparsed).
     *
     * Useful when we are not sure about the type name and do not want to record parsed
     * value based on wrong type name.
     */
    public T getParsedValueWithoutRecording(QName typeName) throws SchemaException {
        Validate.notNull(typeName, "typeName");
        if (isParsed()) {
            return value;
        } else {
            return valueParser.parse(typeName, XNodeProcessorEvaluationMode.STRICT);
        }
    }

    /**
	 * Returns a value that is correctly string-formatted according
	 * to its type definition. Works properly only if definition is set.
	 */
	public String getFormattedValue() {
//		if (getTypeQName() == null) {
//			throw new IllegalStateException("Cannot fetch formatted value if type definition is not set");
//		}
		if (!isParsed()) {
			throw new IllegalStateException("Cannot fetch formatted value if the xnode is not parsed");
		}
        return formatValue(value);
	}

    /**
     * Returns formatted parsed value without actually changing node state from UNPARSED to PARSED
     * (if node is originally unparsed).
     *
     * Useful e.g. to serialize nodes that have a type declaration but were not parsed yet.
     *
     * Experimental. Should be thought through yet.
     *
     * @return properly formatted value
     */
    public String getGuessedFormattedValue() throws SchemaException {
        if (isParsed()) {
            return getFormattedValue();
        }
        if (getTypeQName() == null) {
            throw new IllegalStateException("Cannot fetch formatted value if type definition is not set");
        }
        // brutal hack - fix this! MID-3616
		try {
			T value = valueParser.parse(getTypeQName(), XNodeProcessorEvaluationMode.STRICT);
			return formatValue(value);
		} catch (SchemaException e) {
        	return (String) valueParser.parse(DOMUtil.XSD_STRING, XNodeProcessorEvaluationMode.COMPAT);
		}
    }

    private String formatValue(T value) {
        if (value instanceof PolyString) {
            return ((PolyString) value).getOrig();
        }
        if (value instanceof QName) {
            return QNameUtil.qNameToUri((QName) value);
        }
        if (value instanceof DisplayableValue) {
        	return ((DisplayableValue) value).getValue().toString();
        }

        if (value != null && value.getClass().isEnum()){
        	return value.toString();
        }

        return XmlTypeConverter.toXmlTextContent(value, null);
    }

    @Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		valueToString(sb);
		String dumpSuffix = dumpSuffix();
		if (dumpSuffix != null) {
			sb.append(dumpSuffix);
		}
		return sb.toString();
	}

	@Override
	public String getDesc() {
		return "primitive";
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("XNode(primitive:");
		valueToString(sb);
		if (isAttribute) {
			sb.append(",attr");
		}
		sb.append(")");
		return sb.toString();
	}

	private void valueToString(StringBuilder sb) {
		if (value == null) {
			sb.append("parser ").append(valueParser);
		} else {
			sb.append(PrettyPrinter.prettyPrint(value));
			sb.append(" (").append(value.getClass()).append(")");
		}
	}

	/**
	 * Returns the value represented as string - in the best format that we can.
	 * There is no guarantee that the returned value will be precise.
	 * This method is used as a "last instance" if everything else fails.
	 * Invocation of this method will not change the state of this xnode, e.g.
	 * it will NOT cause it to be parsed.
	 */
	public String getStringValue() {
		if (isParsed()) {
			if (getTypeQName() != null) {
				return getFormattedValue();
			} else {
				if (value == null) {
					return null;
				} else {
					return value.toString();
				}
			}
		} else {
			return valueParser.getStringValue();
		}
	}

    /**
     * This method is used with conjunction with getStringValue, typically when serializing unparsed values.
     * Because the string value can represent QName or ItemPath, we have to provide relevant namespace declarations.
     *
     * Because we cannot know for sure, we are allowed to return namespace declarations that are not actually used.
     * We should minimize number of such declarations, however.
     *
     * Current implementation simply grabs all potential namespace declarations and searches
     * the xnode's string value for any 'prefix:' substrings. I'm afraid it is all we can do for now.
     *
     * THIS METHOD SHOULD BE CALLED ONLY ON EITHER UNPARSED OR EMPTY NODES.
     *
     * @return
     */
    public Map<String, String> getRelevantNamespaceDeclarations() {
        Map<String,String> retval = new HashMap<>();
        if (isEmpty()) {
            return retval;
        }
        if (valueParser == null) {
            throw new IllegalStateException("getRelevantNamespaceDeclarations called on parsed primitive XNode: " + this);
        }
        Map<String,String> candidateNamespaces = valueParser.getPotentiallyRelevantNamespaces();
        if (candidateNamespaces == null) {
            return retval;
        }
        String stringValue = getStringValue();
        if (stringValue == null) {
            return retval;
        }
        for (Map.Entry<String,String> candidateNamespace : candidateNamespaces.entrySet()) {
            String prefix = candidateNamespace.getKey();
            if (stringValue.contains(prefix+":")) {
                retval.put(candidateNamespace.getKey(), candidateNamespace.getValue());
            }
        }
        return retval;
    }

    @Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PrimitiveXNode)) {
			return false;
		}

		PrimitiveXNode other = (PrimitiveXNode) obj;
		if (other.isParsed() && isParsed()){
			return value.equals(other.value);
		} else if (!other.isParsed() && !isParsed()){
            // TODO consider problem with namespaces (if string value is QName/ItemPath its meaning can depend on namespace declarations that are placed outside the element)
			String thisStringVal = this.getStringValue();
			String otherStringVal = other.getStringValue();
			return thisStringVal.equals(otherStringVal);
		} else if (other.isParsed() && !isParsed()){
            String thisStringValue = this.getStringValue();
            String otherStringValue = String.valueOf(other.value);
			return otherStringValue.equals(thisStringValue);
		} else if (!other.isParsed() && isParsed()){
            String thisStringValue = String.valueOf(value);
            String otherStringValue = other.getStringValue();
			return thisStringValue.equals(otherStringValue);
		}

		return false;
	}

    /**
     * The basic idea of equals() is:
     *  - if parsed, compare the value;
     *  - if unparsed, compare getStringValue()
     * Therefore the hashcode is generated based on value (if parsed) or getStringValue() (if unparsed).
     */
	@Override
	public int hashCode() {
        Object objectToHash;
        if (isParsed()) {
            objectToHash = value;
        } else {
            // TODO consider problem with namespaces (if string value is QName/ItemPath its meaning can depend on namespace declarations that are placed outside the element)
            objectToHash = getStringValue();
        }
        return objectToHash != null ? objectToHash.hashCode() : 0;
	}

	PrimitiveXNode cloneInternal() {

		PrimitiveXNode clone;
		if (value != null) {
			// if we are parsed, things are much simpler
			clone = new PrimitiveXNode(CloneUtil.clone(getValue()));
		} else {
			clone = new PrimitiveXNode();
			clone.valueParser = valueParser;			// for the time being we simply don't clone the valueParser
		}

		clone.isAttribute = this.isAttribute;
		clone.copyCommonAttributesFrom(this);
		return clone;
	}
}
