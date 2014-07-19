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

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class PrimitiveXNode<T> extends XNode implements Serializable {
	
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

	public void parseValue(QName typeName) throws SchemaException {
		if (valueParser != null) {
			value = valueParser.parse(typeName);
			// Necessary. It marks that the value is parsed. It also frees some memory.
			valueParser = null;
		}
	}
	
	public T getValue() {
		return value;
	}

	public T getParsedValue(QName typeName) throws SchemaException {
		if (!isParsed()) {
			parseValue(typeName);
		}
		return value;
	}
	
	public ValueParser<T> getValueParser() {
		return valueParser;
	}

	public void setValueParser(ValueParser<T> valueParser) {
		this.valueParser = valueParser;
	}

	public void setValue(T value) {
		this.value = value;
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
        if (isParsed()) {
            return value;
        } else {
            return valueParser.parse(typeName);
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
        T value = valueParser.parse(getTypeQName());
        return formatValue(value);
    }

    private String formatValue(T value) {
        if (value instanceof PolyString) {
            return ((PolyString) value).getOrig();
        }
        if (value instanceof QName) {
            return QNameUtil.qNameToUri((QName) value);
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
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PrimitiveXNode)) {
			return false;
		}
		
		PrimitiveXNode other = (PrimitiveXNode) obj;
		if (other.isParsed() && isParsed()){
			return value.equals(other.value);
		} else if (!other.isParsed() && !isParsed()){
			String thisStringVal = this.getStringValue();
			String otherStringVal = other.getStringValue();
			return thisStringVal.equals(otherStringVal);
		} else if (other.isParsed() && !isParsed()){
			return other.value.equals(this.getStringValue());
		} else if (!other.isParsed() && isParsed()){
			return value.equals(other.getStringValue());
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
            objectToHash = getStringValue();
        }
        return objectToHash != null ? objectToHash.hashCode() : 0;
	}
}
