/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.xnode;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Freezable;
import com.evolveum.midpoint.prism.impl.marshaller.PrismBeanInspector;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.impl.xml.XmlTypeConverterInternal;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.*;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrimitiveXNodeImpl<T> extends XNodeImpl implements Serializable, PrimitiveXNode<T> {

    //private static final Trace LOGGER = TraceManager.getTrace(PrimitiveXNodeImpl.class);

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

    public PrimitiveXNodeImpl() {
        super();
    }

    public PrimitiveXNodeImpl(T value) {
        super();
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Deprecated
    public T getParsedValue(@NotNull QName typeName) throws SchemaException {
        return getParsedValue(typeName, null, XNodeProcessorEvaluationMode.STRICT);
    }

    // @post: return value is type-compliant with expectedClass (if both are non-null)
    public <X> X getParsedValue(@NotNull QName typeName, @Nullable Class<X> expectedClass) throws SchemaException {
        return getParsedValue(typeName, expectedClass, XNodeProcessorEvaluationMode.STRICT);
    }

    // @post: return value is type-compliant with expectedClass (if both are non-null)
    public <X> X getParsedValue(@NotNull QName typeName, @Nullable Class<X> expectedClass, XNodeProcessorEvaluationMode mode) throws SchemaException {
        X parsedValue;
        if (isParsed()) {
            //noinspection unchecked
            parsedValue = (X) value;
        } else {
            //noinspection unchecked
            parsedValue = (X) valueParser.parse(typeName, mode);
            if (!immutable) {
                //noinspection unchecked
                value = (T) parsedValue;
                valueParser = null;     // Necessary. It marks that the value is parsed. It also frees some memory.
            }
        }
        if (JavaTypeConverter.isTypeCompliant(value, expectedClass)) {
            return parsedValue;
        } else {
            throw new SchemaException("Expected " + expectedClass + " but got " + value.getClass() + " instead. Value is " + value);
        }
    }

    public ValueParser<T> getValueParser() {
        return valueParser;
    }

    public void setValueParser(ValueParser<T> valueParser) {
        Validate.notNull(valueParser, "Value parser cannot be null");
        checkMutable();
        this.valueParser = valueParser;
        this.value = null;
    }

    public void setValue(T value, QName typeQName) {
        checkMutable();
        if (value != null) {
            if (typeQName == null) {
                // last desperate attempt to determine type name from the value type
                typeQName = XsdTypeMapper.getJavaToXsdMapping(value.getClass());
                if (typeQName == null) {
                    typeQName = PrismBeanInspector.determineTypeForClassUncached(value.getClass());     // little hack
                }
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
        checkMutable();
        this.isAttribute = isAttribute;
    }

    public boolean isEmpty() {
        if (isParsed()) {
            return value == null || value instanceof String && StringUtils.isBlank((String) value);
        } else {
            return valueParser.isEmpty();
        }
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
//        if (getTypeQName() == null) {
//            throw new IllegalStateException("Cannot fetch formatted value if type definition is not set");
//        }
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
    @Override
    public String getGuessedFormattedValue() throws SchemaException {
        if (isParsed()) {
            return getFormattedValue();
        }
        QName typeName = getTypeQName();
        if (typeName == null) {
            throw new IllegalStateException("Cannot fetch formatted value if type definition is not set");
        }
        if (valueParser.canParseAs(typeName)) {
            T value = valueParser.parse(typeName, XNodeProcessorEvaluationMode.STRICT);
            return formatValue(value);
        } else {
            return valueParser.getStringValue();
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

        return XmlTypeConverterInternal.toXmlTextContent(value, null);
    }

    @Override
    public void accept(Visitor<XNode> visitor) {
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

    @Override
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
        if (!(obj instanceof PrimitiveXNodeImpl)) {
            return false;
        }

        PrimitiveXNodeImpl other = (PrimitiveXNodeImpl) obj;
        if (other.isParsed() && isParsed()){
            return value.equals(other.value);
        } else if (!other.isParsed() && !isParsed()){
            // TODO consider problem with namespaces (if string value is QName/ItemPath its meaning can depend on namespace declarations that are placed outside the element)
            String thisStringVal = this.getStringValue();
            String otherStringVal = other.getStringValue();
            return Objects.equals(thisStringVal, otherStringVal);
        } else if (other.isParsed() && !isParsed()){
            String thisStringValue = this.getStringValue();
            String otherStringValue = String.valueOf(other.value);
            return Objects.equals(otherStringValue, thisStringValue);
        } else if (!other.isParsed() && isParsed()){
            String thisStringValue = String.valueOf(value);
            String otherStringValue = other.getStringValue();
            return Objects.equals(thisStringValue, otherStringValue);
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

    PrimitiveXNodeImpl<T> cloneInternal() {

        PrimitiveXNodeImpl<T> clone;
        if (value != null) {
            // if we are parsed, things are much simpler
            clone = new PrimitiveXNodeImpl<>(CloneUtil.clone(getValue()));
        } else {
            clone = new PrimitiveXNodeImpl<>();
            clone.valueParser = valueParser;            // for the time being we simply don't clone the valueParser
        }

        clone.isAttribute = this.isAttribute;
        clone.copyCommonAttributesFrom(this);
        return clone;
    }

    @Override
    public void performFreeze() {
        if (value instanceof Freezable) {
            ((Freezable) value).freeze();
        }
        if (valueParser != null) {
            valueParser = valueParser.freeze();
        }
        super.performFreeze();
    }
}
