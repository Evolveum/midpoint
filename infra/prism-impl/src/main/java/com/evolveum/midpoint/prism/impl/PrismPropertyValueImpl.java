/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.marshaller.BeanMarshaller;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.impl.util.PrismUtilInternal;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * @author lazyman
 */
public class PrismPropertyValueImpl<T> extends PrismValueImpl implements DebugDumpable, Serializable, PrismPropertyValue<T> {

    final static Trace LOGGER = TraceManager.getTrace(PrismPropertyValueImpl.class);

    private T value;

    // The rawElement is set during a schema-less parsing, e.g. during parsing without a definition.
    // We can't do anything smarter, as we don't have definition nor prism context. So we store the raw
    // elements here and process them later (e.g. during applyDefinition or getting a value with explicit type).
    private XNodeImpl rawElement;

    @Nullable private ExpressionWrapper expression;

    public PrismPropertyValueImpl(T value) {
        this(value, null, null, null, null);
    }

    public PrismPropertyValueImpl(T value, PrismContext prismContext) {
        this(value, prismContext, null, null, null);
    }

    public PrismPropertyValueImpl(T value, OriginType type, Objectable source) {
        this(value, null, type, source, null);
    }

    public PrismPropertyValueImpl(T value, PrismContext prismContext, OriginType type, Objectable source, ExpressionWrapper expression) {
        super(prismContext, type, source, null);
        if (value instanceof PrismPropertyValue) {
            throw new IllegalArgumentException("Probably problem somewhere, encapsulating property " +
                    "value object to another property value.");
        }
        this.value = value;
        this.expression = expression;
        checkValue();
    }

    /**
     * Private constructor just for cloning.
     */
    private PrismPropertyValueImpl(OriginType type, Objectable source) {
        super(type,source);
    }

    PrismPropertyValueImpl() {
    }

    public void setValue(T value) {
        checkMutable();
        this.value = value;
        this.rawElement = null;
        checkValue();
    }

    public T getValue() {
        if (rawElement != null) {
            ItemDefinition def = null;
            Itemable parent = getParent();
            if (parent != null && parent.getDefinition() != null) {
                def = getParent().getDefinition();
            }
//            if (def == null) {
//                // We are weak now. If there is no better definition for this we assume a default definition and process
//                // the attribute now. But we should rather do this: TODO:
//                // throw new IllegalStateException("Attempt to get value withot a type from raw value of property "+getParent());
//                if (parent != null && getPrismContext() != null) {
//                    def = SchemaRegistryImpl.createDefaultItemDefinition(parent.getElementName(), getPrismContext());
//                } else if (PrismContextImpl.isAllowSchemalessSerialization()) {
//                    if (rawElement instanceof PrimitiveXNode) {
//                        try {
//                            QName type = rawElement.getTypeQName() != null ? rawElement.getTypeQName() : DOMUtil.XSD_STRING;
//                            value = (T) ((PrimitiveXNode) rawElement).getParsedValueWithoutRecording(type);
//                        } catch (SchemaException ex){
//                            throw new IllegalStateException("Cannot fetch value from raw element. " + ex.getMessage(), ex);
//                        }
//                    } else {
//                        throw new IllegalStateException("No parent or prism context in property value "+this+", cannot create default definition." +
//                            "The element is also not a DOM element but it is "+rawElement.getClass()+". Epic fail.");
//                    }
//                } else {
//                    throw new IllegalStateException("No parent or prism context in property value "+this+" (schemaless serialization is disabled)");
//                }
//            }
            if (def != null) {
                try {
                    applyDefinition(def);
                } catch (SchemaException e) {
                    throw new IllegalStateException(e.getMessage(),e);
                }
            }
            if (rawElement != null) {
                return (T) RawType.create(rawElement, getPrismContext());
            }
        }
        return value;
    }

    public XNodeImpl getRawElement() {
        return rawElement;
    }

    public void setRawElement(XNode rawElement) {
        this.rawElement = (XNodeImpl) rawElement;
    }

    @Override
    public boolean isRaw() {
        return rawElement != null;
    }

    @Nullable
    public ExpressionWrapper getExpression() {
        return expression;
    }

    public void setExpression(@Nullable ExpressionWrapper expression) {
        this.expression = expression;
    }

    @Override
    public void applyDefinition(ItemDefinition definition) throws SchemaException {
        PrismPropertyDefinition propertyDefinition = (PrismPropertyDefinition) definition;
        if (propertyDefinition != null && !propertyDefinition.isAnyType() && rawElement != null) {
            value = (T) parseRawElementToNewRealValue(this, propertyDefinition);
            if (value ==  null) {
                // Be careful here. Expression element can be legal sub-element of complex properties.
                // Therefore parse expression only if there is no legal value.
                expression = PrismUtilInternal.parseExpression(rawElement, prismContext);
            }
            rawElement = null;
        }
    }

    @Override
    public void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException {
        applyDefinition(definition);
    }

    @Override
    public void revive(PrismContext prismContext) throws SchemaException {
        super.revive(prismContext);
        if (value != null) {
            if (value instanceof Revivable) {
                ((Revivable)value).revive(prismContext);
            } else {
                BeanMarshaller marshaller = ((PrismContextImpl) prismContext).getBeanMarshaller();
                if (marshaller.canProcess(value.getClass())) {
                    marshaller.revive(value, prismContext);
                }
            }
        }
    }

    @Override
    public void recompute(PrismContext prismContext) {
        if (isRaw()) {
            return;
        }
        T realValue = getValue();
        if (realValue == null) {
            return;
        }
        checkMutable();            // TODO reconsider this
        PrismUtil.recomputeRealValue(realValue, prismContext);
    }

    @Override
    public Object find(ItemPath path) {
        if (path == null || path.isEmpty()) {
            return this;
        }
        T value = getValue();
        if (value instanceof Structured) {
            return ((Structured)value).resolve(path);
        } else {
            throw new IllegalArgumentException("Attempt to resolve sub-path '"+path+"' on non-structured property value "+value);
        }
    }

    @Override
    public <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path) {
        throw new UnsupportedOperationException("Attempt to invoke findPartialItem on a property value");
    }

    void checkValue() {
        if (isRaw()) {
            // Cannot really check raw values
            return;
        }
        if (expression != null) {
            return;
        }
        if (value == null) {
            // can be used not because of prism forms in gui (will be fixed later [lazyman]
            // throw new IllegalArgumentException("Null value in "+this);
            return;
        }
        if (value instanceof PolyStringType) {
            // This is illegal. PolyString should be there instead.
            throw new IllegalArgumentException("PolyStringType found where PolyString should be in "+this);
        }
        if (value instanceof Serializable) {
            // This is OK (covers also primitives, SchemaDefinitionType, RawType)
            return;
        }
        throw new IllegalArgumentException("Unsupported value "+value+" ("+value.getClass()+") in "+this);
    }

    @Override
    public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope) {
        if (!scope.isThorough()) {
            return;
        }

        ItemPath myPath = getPath();
        if (prohibitRaw && rawElement != null) {
            throw new IllegalStateException("Raw element in property value "+this+" ("+myPath+" in "+rootItem+")");
        }
        if (value == null && rawElement == null && expression == null) {
            throw new IllegalStateException("Neither value, expression nor raw element specified in property value "+this+" ("+myPath+" in "+rootItem+")");
        }
        if (value != null && rawElement != null) {
            throw new IllegalStateException("Both value and raw element specified in property value "+this+" ("+myPath+" in "+rootItem+")");
        }
        if (value != null) {
            if (value instanceof Recomputable) {
                try {
                    ((Recomputable)value).checkConsistence();
                } catch (IllegalStateException e) {
                    throw new IllegalStateException(e.getMessage()+" in property value "+this+" ("+myPath+" in "+rootItem+")", e);
                }
            }
            if (value instanceof PolyStringType) {
                throw new IllegalStateException("PolyStringType found in property value "+this+" ("+myPath+" in "+rootItem+")");
            }
            if (value instanceof ProtectedStringType) {
                if (((ProtectedStringType)value).isEmpty()) {
                    throw new IllegalStateException("Empty ProtectedStringType found in property value "+this+" ("+myPath+" in "+rootItem+")");
                }
            }
            PrismContext prismContext = getPrismContext();
            if (value instanceof PolyString && prismContext != null) {
                PolyString poly = (PolyString)value;
                String orig = poly.getOrig();
                String norm = poly.getNorm();
                PolyStringNormalizer polyStringNormalizer = prismContext.getDefaultPolyStringNormalizer();
                String expectedNorm = polyStringNormalizer.normalize(orig);
                if (!Objects.equals(norm, expectedNorm)) {
                    throw new IllegalStateException("PolyString has inconsistent orig ("+orig+") and norm ("+norm+") in property value "+this+" ("+myPath+" in "+rootItem+")");
                }
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public PrismPropertyValue<T> clone() {
        return cloneComplex(CloneStrategy.LITERAL);
    }

    @Override
    public PrismPropertyValue<T> cloneComplex(CloneStrategy strategy) {
        PrismPropertyValueImpl<T> clone = new PrismPropertyValueImpl<>(getOriginType(), getOriginObject());
        copyValues(strategy, clone);
        return clone;
    }

    protected void copyValues(CloneStrategy strategy, PrismPropertyValueImpl<T> clone) {
        super.copyValues(strategy, clone);
        clone.value = CloneUtil.clone(this.value);
        if (this.expression != null) {
            clone.expression = this.expression.clone();
        }
        clone.rawElement = this.rawElement;
    }

    /**
     * Takes the definition from the definitionSource parameter and uses it to parse raw elements in origValue.
     * It returns a new parsed value without touching the original value.
     */
    private PrismPropertyValue<T> parseRawElementToNewValue(
            PrismPropertyValue<T> origValue, PrismPropertyValue<T> definitionSource) throws SchemaException {
        if (definitionSource.getParent() != null && definitionSource.getParent().getDefinition() != null) {
            T parsedRealValue = (T) parseRawElementToNewRealValue(origValue,
                    (PrismPropertyDefinition) definitionSource.getParent().getDefinition());
            PrismPropertyValue<T> newPVal = new PrismPropertyValueImpl<>(parsedRealValue);
            return newPVal;
        } else if (definitionSource.getRealClass() != null && definitionSource.getPrismContext() != null) {
            T parsedRealValue = parseRawElementToNewRealValue(origValue, (Class<T>) definitionSource.getRealClass(), definitionSource.getPrismContext());
            PrismPropertyValue<T> newPVal = new PrismPropertyValueImpl<>(parsedRealValue);
            return newPVal;
        } else {
            throw new IllegalArgumentException("Attempt to use property " + origValue.getParent() +
                    " values in a raw parsing state (raw elements) with parsed value that has no definition nor class with prism context");
        }
    }

    private T parseRawElementToNewRealValue(PrismPropertyValue<T> prismPropertyValue, PrismPropertyDefinition<T> definition)
                throws SchemaException {
        PrismContext prismCtx = definition.getPrismContext() != null ? definition.getPrismContext() : prismContext;
        //noinspection UnnecessaryLocalVariable
        if (prismCtx == null) {
            throw new SchemaException("Unexpected null prism context.");
        }
        T value = prismCtx.parserFor(prismPropertyValue.getRawElement().toRootXNode()).definition(definition).parseRealValue();
        return value;
    }

    private T parseRawElementToNewRealValue(PrismPropertyValue<T> prismPropertyValue, Class<T> clazz, PrismContext prismContext)
                throws SchemaException {
        return prismContext.parserFor(prismPropertyValue.getRawElement().toRootXNode()).parseRealValue(clazz);
    }

    @Override
    public boolean equals(PrismValue other, @NotNull ParameterizedEquivalenceStrategy strategy) {
        return other instanceof PrismPropertyValue && equals((PrismPropertyValue<?>) other, strategy, null);
    }

    @Override
    public boolean equals(PrismPropertyValue<?> other, @NotNull ParameterizedEquivalenceStrategy strategy, @Nullable MatchingRule<T> matchingRule) {
        if (!super.equals(other, strategy)) {
            return false;
        }

        if (this.rawElement != null && other.getRawElement() != null) {
            return equalsRawElements((PrismPropertyValue<T>)other);
        }

        PrismPropertyValue<T> otherProcessed = (PrismPropertyValue<T>) other;
        PrismPropertyValue<T> thisProcessed = this;
        if (this.rawElement != null || other.getRawElement() != null) {
            try {
                if (this.rawElement == null) {
                    otherProcessed = parseRawElementToNewValue((PrismPropertyValue<T>) other, this);
                } else if (other.getRawElement() == null) {
                    thisProcessed = parseRawElementToNewValue(this, (PrismPropertyValue<T>) other);
                }
            } catch (SchemaException e) {
                // TODO: Maybe just return false?
                throw new IllegalArgumentException("Error parsing the value of property "+getParent()+" using the 'other' definition "+
                        "during a compare: "+e.getMessage(),e);
            }
        }

        T otherRealValue = otherProcessed.getValue();
        T thisRealValue = thisProcessed.getValue();
        if (otherRealValue == null && thisRealValue == null) {
            return true;
        }
        if (otherRealValue == null || thisRealValue == null) {
            return false;
        }

        if (matchingRule != null) {
            try {
                return matchingRule.match(thisRealValue, otherRealValue);
            } catch (SchemaException e) {
                // At least one of the values is invalid. But we do not want to throw exception from
                // a comparison operation. That will make the system very fragile. Let's fall back to
                // ordinary equality mechanism instead.
                return thisRealValue.equals(otherRealValue);
            }
        } else {

            if (thisRealValue instanceof Element && otherRealValue instanceof Element) {
                return DOMUtil.compareElement((Element)thisRealValue, (Element)otherRealValue, strategy.isLiteralDomComparison());
            }

            if (thisRealValue instanceof SchemaDefinitionType && otherRealValue instanceof SchemaDefinitionType) {
                SchemaDefinitionType thisSchema = (SchemaDefinitionType) thisRealValue;
                return thisSchema.equals(otherRealValue, strategy.isLiteralDomComparison());
            }

            if (thisRealValue instanceof ProtectedStringType && otherRealValue instanceof ProtectedStringType) {
                PrismContext prismContext = getPrismContext();
                if (prismContext == null || prismContext.getDefaultProtector() == null) {
                    // Slightly dangerous, may get wrong results. See javadoc of ProtectedDataType.equals()
                    // But what else can we do?
                    return thisRealValue.equals(otherRealValue);
                } else {
                    try {
                        return prismContext.getDefaultProtector().areEquivalent((ProtectedStringType)thisRealValue, (ProtectedStringType)otherRealValue);
                    } catch (SchemaException | EncryptionException e) {
                        // Not absolutely correct. But adding those throws clauses to all equals(...) signature will wreak havoc.
                        throw new SystemException("Error comparing protected string values: "+e.getMessage(), e);
                    }
                }
            }

            if (thisRealValue instanceof byte[] && otherRealValue instanceof byte[]) {
                return Arrays.equals((byte[]) thisRealValue, (byte[]) otherRealValue);
            }

            if (strategy.isLiteralDomComparison()) {
                if (thisRealValue instanceof QName && otherRealValue instanceof QName) {
                    // we compare prefixes as well
                    return thisRealValue.equals(otherRealValue) &&
                            StringUtils.equals(((QName) thisRealValue).getPrefix(), ((QName) otherRealValue).getPrefix());
                } else if (thisRealValue instanceof Equals && otherRealValue instanceof Equals) {
                    return ((Equals) thisRealValue).equals(null, null, otherRealValue, LiteralEqualsStrategy.INSTANCE);
                }
            }
            return thisRealValue.equals(otherRealValue);
        }
    }

    private boolean equalsRawElements(PrismPropertyValue<T> other) {
        return this.rawElement.equals(other.getRawElement());
    }



    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        PrismPropertyValue other = (PrismPropertyValue) obj;
        return equals(other, getEqualsHashCodeStrategy());
    }

    // Just to make checkstyle happy
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public int hashCode(@NotNull ParameterizedEquivalenceStrategy strategy) {
        final int prime = 31;
        int result = super.hashCode(strategy);
        if (value != null && value instanceof Element) {
            // We need special handling here. We haven't found out the proper way now.
            // so we just do not include this in the hashcode now.
        } else {
            result = prime * result + ((value == null) ? 0 : value.hashCode());
        }
        return result;
    }

    @Override
    public String debugDump() {
        return toString();
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, false);
    }

    public String debugDump(int indent, boolean detailedDump) {
        detailedDump = detailedDump || DebugUtil.isDetailedDebugDump();
        StringBuilder sb = new StringBuilder();
        boolean wasIndent = false;
        if (detailedDump) {
            DebugUtil.indentDebugDump(sb, indent);
            wasIndent = true;
            sb.append("PPV(");
            dumpSuffix(sb);
            sb.append("):");
        }
        if (value != null) {
            if (detailedDump) {
                sb.append(" ").append(value.getClass().getSimpleName()).append(":");
            }
            if (value instanceof DebugDumpable) {
                if (wasIndent) {
                    sb.append("\n");
                }
                sb.append(((DebugDumpable)value).debugDump(indent + 1));
            } else {
                if (!wasIndent) {
                    DebugUtil.indentDebugDump(sb, indent);
                }
                PrismPrettyPrinter.debugDumpValue(sb, indent, value, prismContext, null, null);
            }
        } else if (expression != null) {
            if (!wasIndent) {
                DebugUtil.indentDebugDump(sb, indent);
            }
            sb.append("expression: ");
            // TODO: nicer output
            sb.append(expression);
        } else {
            if (!wasIndent) {
                DebugUtil.indentDebugDump(sb, indent);
            }
            sb.append("null");
        }

        ValueMetadata valueMetadata = getValueMetadata();
        if (!valueMetadata.isEmpty()) {
            sb.append(", meta: ").append(valueMetadata.shortDump());
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PPV(");
        // getValue() must not be here. getValue() contains exception that in turn causes a call to toString()
        if (value != null) {
            builder.append(value.getClass().getSimpleName()).append(":");
            builder.append(PrettyPrinter.prettyPrint(value));
        } else if (isRaw()) {
            builder.append("[raw]");
        } else if (expression != null) {
            builder.append("[expression]");
        } else {
            builder.append("null");
        }
        dumpSuffix(builder);
        builder.append(")");

        return builder.toString();
    }

    private void dumpSuffix(StringBuilder builder) {
        appendOriginDump(builder);
        ValueMetadata valueMetadata = getValueMetadata();
        if (!valueMetadata.isEmpty()) {
            builder.append(", meta: ").append(valueMetadata.shortDump());
        }
        if (getRawElement() != null) {
            builder.append(", raw element: ");
            builder.append(PrettyPrinter.prettyPrint(getRawElement()));
        }
        if (getExpression() != null) {
            builder.append(", expression: ");
            builder.append(getExpression());
        }
    }

    @Override
    public String toHumanReadableString() {
        ValueMetadata valueMetadata = getValueMetadata();
        if (valueMetadata.isEmpty()) {
            return toHumanReadableStringInternal();
        } else {
            return toHumanReadableStringInternal() + " [meta: " + valueMetadata.shortDump() + "]";
        }
    }

    private String toHumanReadableStringInternal() {
        if (value == null && expression != null) {
            return ("expression("+expression+")");
        } else if (value instanceof PolyString) {
            // We intentionally do not put this code into PrettyPrinter, to avoid unwanted side effects
            // (displaying the aux information in user-visible context). But for e.g. deltas we need this information.
            PolyString ps = (PolyString) this.value;
            StringBuilder sb = new StringBuilder();
            if (MapUtils.isNotEmpty(ps.getLang()) || ps.getTranslation() != null && StringUtils.isNotEmpty(ps.getTranslation().getKey())){
                sb.append("orig=").append(ps.getOrig());
            } else {
                sb.append(ps.getOrig());
            }
            if (ps.getTranslation() != null) {
                sb.append(", translation.key=").append(ps.getTranslation().getKey());
            }
            if (MapUtils.isNotEmpty(ps.getLang())) {
                sb.append("; lang:");
                ps.getLang().keySet().forEach(langKey ->
                        sb.append(" ").append(langKey).append("=").append(ps.getLang().get(langKey)).append(","));
            }
            return sb.toString();
        } else {
            return PrettyPrinter.prettyPrint(value);
        }
    }

    /**
     * Returns JAXBElement corresponding to the this value.
     * Name of the element is the name of parent property; its value is the real value of the property.
     *
     * @return Created JAXBElement.
     */
    public JAXBElement<T> toJaxbElement() {
        Itemable parent = getParent();
        if (parent == null) {
            throw new IllegalStateException("Couldn't represent parent-less property value as a JAXBElement");
        }
        Object realValue = getValue();
        return new JAXBElement<T>(parent.getElementName(), (Class) realValue.getClass(), (T) realValue);
    }

    @Override
    public Class<?> getRealClass() {
        return value != null ? value.getClass() : null;
    }

    @Nullable
    @Override
    public T getRealValue() {
        return getValue();
    }

    @Override
    public void performFreeze() {
        if (value instanceof Freezable) {
            ((Freezable) value).freeze();
        } else if (value instanceof JaxbVisitable) {
            ((JaxbVisitable) value).accept(v -> {
                if (v instanceof Freezable) {
                    ((Freezable) v).freeze();
                }
            });
        }
        if (rawElement != null) {
            rawElement.freeze();
        }
        if (expression != null) {
            expression.freeze();
        }
        super.performFreeze();
    }

    @Override
    public @Nullable Object getRealValueOrRawType(PrismContext prismContext) {
        if (value != null) {
            return value;
        } else if (rawElement != null) {
            return new RawType(this, getTypeName(), prismContext);
        } else {
            return null;
        }
    }
}
