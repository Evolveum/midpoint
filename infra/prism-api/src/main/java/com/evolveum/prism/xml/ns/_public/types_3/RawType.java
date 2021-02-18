/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import com.evolveum.midpoint.util.exception.TunnelException;
import com.google.common.base.Preconditions;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A class used to hold raw XNodes until the definition for such an object is known.
 *
 * This class should be thread-safe because it is used in shared objects, like cached resources or roles. (See MID-6506.)
 * But by default it is not, as it contains internal state (xnode/parsed) that can lead to race conditions when parsing.
 *
 * In midPoint 4.2 it was made roughly thread-safe by including explicit synchronization at appropriate places.
 * See MID-6542.
 * In midPoint 4.3 following changes were made to address following issues:
 *  (1) we need to support freezing the content (embedded xnode/prism value)
 *  XNodes are freezable, and RawType requires frozen XNode
 *  (2) we should consider avoiding explicit synchronization for performance reasons
 *  Internal structure is State class
 *    - State Parsed (noop for subsequent parsing calls)
 *    - State Raw (parsing results in transition to state Parsed)
 *
 * Implementation has stable Equals, but hashcode is unstable since it would require
 * significant effort to unify XNode and parsed items hashcode computation.
 *
 */
public class RawType implements Serializable, Cloneable, Equals, Revivable, ShortDumpable, JaxbVisitable, PrismContextSensitive {
    private static final long serialVersionUID = 4430291958902286779L;

    /**
     * State wrapper class captures if we have xnode or parsed value
     *
     */
    private State state;

    public RawType(PrismContext prismContext) {
        this(new Parsed<>(prismContext, prismContext.itemFactory().createValue(null), null));
        Validate.notNull(prismContext, "prismContext is not set - perhaps a forgotten call to adopt() somewhere?");
    }

    private RawType(State state) {
        Validate.notNull(state, "State is not set - perhaps a forgotten call to adopt() somewhere?");
        this.state = state;
    }


    public RawType(XNode node, @NotNull PrismContext prismContext) {
        this(new Raw(prismContext, node));
    }

    public RawType(PrismValue parsed, QName explicitTypeName, @NotNull PrismContext prismContext) {
        this(new Parsed<>(prismContext, parsed, explicitTypeName));
    }

    public static RawType fromPropertyRealValue(Object realValue, QName explicitTypeName, @NotNull PrismContext prismContext) {
        return new RawType(prismContext.itemFactory().createPropertyValue(realValue), explicitTypeName, prismContext);
    }

    /**
     * Extracts a "real value" from a potential RawType object without expecting any specific type beforehand.
     * (Useful e.g. for determining value of xsd:anyType XML property.)
     */
    public static Object getValue(Object value) throws SchemaException {
        if (value instanceof RawType) {
            return ((RawType) value).getValue();
        } else {
            return value;
        }
    }

    public Object getValue() throws SchemaException {
        return getValue(false);
    }

    /**
     * Extracts a "real value" from RawType object without expecting any specific type beforehand.
     * If no explicit type is present, assumes xsd:string (and fails if the content is structured).
     */
    public synchronized Object getValue(boolean store) throws SchemaException {
        Parsed<?> parsed = current().parse();
        if(store) {
            transition(parsed);
        }
        return parsed.realValue();
    }


    /**
     * TEMPORARY. EXPERIMENTAL. DO NOT USE.
     */
    @Experimental
    public synchronized String extractString() {
        return current().extractString(() -> toString());
    }

    public synchronized String extractString(String defaultValue) {
        return current().extractString(() -> defaultValue);
    }


    @Override
    public void revive(PrismContext prismContext) throws SchemaException {
        Validate.notNull(prismContext);
        current().revive(prismContext);
    }

    //region General getters/setters

    public XNode getXnode() {
        return current().xNodeNullable();
    }

    @NotNull
    public RootXNode getRootXNode(@NotNull QName itemName) {
        return getPrismContext().xnodeFactory().root(itemName, getXnode());

    }

    @Override
    public PrismContext getPrismContext() {
        return current().prismContext;
    }

    // experimental
    public QName getExplicitTypeName() {
        return current().explicitTypeName();
    }
    //endregion

    //region Parsing and serialization
    // itemDefinition may be null; in that case we do the best what we can
    public <IV extends PrismValue> IV getParsedValue(@Nullable ItemDefinition<?> itemDefinition, @Nullable QName itemName) throws SchemaException {
        return this.<IV>parse(itemDefinition, itemName).value();
    }

    private <V extends PrismValue> Parsed<V> parse(@Nullable ItemDefinition<?> itemDefinition, @Nullable QName itemName) throws SchemaException {
        return transition(current().<V>parse(itemDefinition, itemName));
    }

    private <V extends PrismValue> Parsed<V> transition(Parsed<V> newState) {
        if(!newState.isTransient()) {
            this.state = newState;
        }
        return newState;
    }

    State current() {
        return state;
    }

    @SuppressWarnings("unchecked")
    public <V> V getParsedRealValue(ItemDefinition<?> itemDefinition, ItemPath itemPath) throws SchemaException {
        var current = current();
        var parsed = current.asParsed();
        if(current instanceof Parsed) {
            return (V) ((Parsed<?>) parsed).realValue();
        }
        var raw = (Raw) current;
        if(itemDefinition == null) {
            // TODO what will be the result without definition?
            return raw.checkPrismContext().parserFor(raw.xNodeNullable().toRootXNode()).parseRealValue();
        }
        return parse(itemDefinition, itemPath.lastName()).realValue();
    }

    public PrismValue getAlreadyParsedValue() {
        return current().parsedValueNullable();
    }

    public <T> T getParsedRealValue(@NotNull Class<T> clazz) throws SchemaException {
        return current().realValue(clazz);
    }

    public <IV extends PrismValue,ID extends ItemDefinition<?>> Item<IV,ID> getParsedItem(ID itemDefinition) throws SchemaException {
        Validate.notNull(itemDefinition);
        return getParsedItem(itemDefinition, itemDefinition.getItemName());
    }

    public <IV extends PrismValue,ID extends ItemDefinition<?>> Item<IV,ID> getParsedItem(ID itemDefinition, QName itemName) throws SchemaException {
        Validate.notNull(itemDefinition);
        Validate.notNull(itemName);
        @SuppressWarnings("unchecked")
        Item<IV,ID> item = itemDefinition.instantiate();
        IV newValue = getParsedValue(itemDefinition, itemName);
        if (newValue != null) {
            // TODO: Is clone neccessary?
            item.add((IV) newValue.clone());
        }
        return item;
    }

    public synchronized XNode serializeToXNode() throws SchemaException {
        return current().toXNode();
    }
    //endregion

    //region Cloning, comparing, dumping
    @Override
    public synchronized RawType clone() {
        // FIXME:  MID-6833 RawType is semi-immutable, we can reuse raw state class
        // Currently we can not reuse parsed, since lot of code assumes
        // clone contract returns mutable (unfrozen) copy.
        return state.performClone();
    }

    @Override
    public synchronized int hashCode() {
        return current().hashCode();
    }

    @Override
    public synchronized boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RawType other = (RawType) obj;
        return current().equals(other.current());
    }

    @Override
    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object that,
            EqualsStrategy equalsStrategy) {
        return equals(that);
    }


    public static RawType create(String value, PrismContext prismContext) {
        var xnode = prismContext.xnodeFactory().primitive(value).frozen();
        return new RawType(xnode, prismContext);
    }

    public static RawType create(XNode node, PrismContext prismContext) {
        return new RawType(node, prismContext);
    }

    @Override
    public synchronized String toString() {
        return new StringBuilder("RawType: ").append(current().toString()).append(")").toString();
    }



    @Override
    public synchronized void shortDump(StringBuilder sb) {
        current().shortDump(sb);
    }

    public boolean isParsed() {
        return current().isParsed();
    }

    // avoid if possible
    public synchronized String guessFormattedValue() throws SchemaException {
        var current = current();
        if (current instanceof Parsed) {
            return ((Parsed<?>) current).realValue().toString();
        }
        if (current instanceof Raw) {
            var node = current.toXNode();
            if(node instanceof PrimitiveXNode) {
                return ((PrimitiveXNode<?>) node).getGuessedFormattedValue();
            }
        }
        return null;
    }

    @Override
    public void accept(JaxbVisitor visitor) {
        Object value;

        if (isParsed() || getExplicitTypeName() != null) {
            // (Potentially) parsing the value before visiting it.
            try {
                value = getValue(true);
            } catch (SchemaException e) {
                throw new TunnelException(e);
            }
        } else {
            value = null;
        }
        visitor.visit(this);

        if (value instanceof JaxbVisitable) {
            ((JaxbVisitable) value).accept(visitor);
        }
    }

    private static abstract class State implements Serializable {

        protected transient PrismContext prismContext;

        private static final long serialVersionUID = 1L;

        public State(PrismContext prismContext) {
            this.prismContext = prismContext;
        }

        protected abstract RawType performClone();

        protected PrismContext checkPrismContext() {
            if (prismContext == null) {
                throw new IllegalStateException("prismContext is not set - perhaps a forgotten call to adopt() somewhere?");
            }
            return prismContext;
        }

        boolean isTransient() {
            return false;
        }

        protected abstract <T> T realValue(@NotNull Class<T> clazz) throws SchemaException;

        protected abstract boolean isParsed();

        protected abstract void shortDump(StringBuilder sb);

        protected abstract QName explicitTypeName();

        protected abstract PrismValue parsedValueNullable();

        protected abstract Parsed<?> asParsed();

        protected void revive(PrismContext prismContext) throws SchemaException {
            this.prismContext = prismContext;
        }

        abstract <IV extends PrismValue> Parsed<IV> parse(@Nullable ItemDefinition<?> itemDefinition, @Nullable QName itemName) throws SchemaException;

        XNode xNodeNullable() {
            return null;
        }

        abstract XNode toXNode() throws SchemaException;

        protected abstract Parsed<PrismValue> parse() throws SchemaException;

        protected abstract String extractString(Supplier<String> defaultValue);

        protected void toStringExplicitType(StringBuilder sb) {
            if (explicitTypeDeclaration()) {
                sb.append(":");
                if (explicitTypeName() == null) {
                    sb.append("null");
                } else {
                    sb.append(explicitTypeName().getLocalPart());
                }
            }
        }

        protected abstract boolean explicitTypeDeclaration();

        @Override
        public abstract String toString();

        @Override
        public abstract boolean equals(Object other);

        boolean equalsXNode(State other) {
            try {
                return Objects.equals(toXNode(), other.toXNode());
            } catch (SchemaException e) {
                // or should we silently return false?
                throw new SystemException("Couldn't serialize RawType to XNode when comparing them", e);
            }
        }
    }

    private static class Parsed<V extends PrismValue> extends State {

        private static final long serialVersionUID = 1L;
        private final V value;
        private final QName explicitTypeName;

        public Parsed(PrismContext context, V parsed, QName explicitTypeName) {
            super(context);
            this.value = parsed;
            if (explicitTypeName != null) {
                this.explicitTypeName = explicitTypeName;
            } else if (parsed != null && parsed.getTypeName() != null) {
                this.explicitTypeName = parsed.getTypeName();
            } else {
                this.explicitTypeName = null;
            }
        }

        public V value() {
            return value;
        }

        @Override
        protected boolean isParsed() {
            return true;
        }

        @Override
        protected String extractString(Supplier<String> defaultValue) {
            return String.valueOf(value.getRealValue());

        }

        @SuppressWarnings("unchecked")
        @Override
        protected Parsed<PrismValue> parse() {
            return (Parsed<PrismValue>) this;
        }

        @SuppressWarnings("unchecked")
        public <T> T realValue() {
            return (T) value.getRealValue();
        }

        @Override
        protected <T> T realValue(@NotNull Class<T> clazz) throws SchemaException {
            Object realValue = realValue();
            if (realValue == null) {
                return null;
            }
            Preconditions.checkArgument(clazz.isInstance(realValue), "Parsed value (%s) is not assignable to %s", realValue.getClass(), clazz);
            return clazz.cast(realValue);
        }

        @Override
        XNode toXNode() throws SchemaException {
            checkPrismContext();
            XNode rv = prismContext.xnodeSerializer().root(new QName("dummy")).serialize(value).getSubnode();
            prismContext.xnodeMutator().setXNodeType(rv, explicitTypeName, explicitTypeDeclaration());
            return rv;
        }

        @Override
        protected Parsed<?> asParsed() {
            return this;
        }

        @Override
        protected PrismValue parsedValueNullable() {
            return value;
        }

        @Override
        protected void shortDump(StringBuilder sb) {
            if (value instanceof ShortDumpable) {
                ((ShortDumpable)value).shortDump(sb);
            } else {
                Object realValue = value.getRealValue();
                if (realValue == null) {
                    sb.append("null");
                } else if (realValue instanceof ShortDumpable) {
                    ((ShortDumpable)realValue).shortDump(sb);
                } else {
                    sb.append(realValue.toString());
                }
            }
        }

        @Override
        protected boolean explicitTypeDeclaration() {
            return explicitTypeName() != null;
        }

        @Override
        protected QName explicitTypeName() {
            return explicitTypeName;
        }

        @Override
        protected void revive(PrismContext prismContext) throws SchemaException {
            value.revive(prismContext);
        }

        @SuppressWarnings("unchecked")
        @Override
        <IV extends PrismValue> Parsed<IV> parse(@Nullable ItemDefinition<?> itemDefinition, @Nullable QName itemName)
                throws SchemaException {
            // No need to reparse
            return (Parsed<IV>) this;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (other instanceof Parsed) {
                return Objects.equals(value, ((Parsed<?>) other).value());
            }
            if (other instanceof State) {
                return equalsXNode((State) other);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            var sb = new StringBuilder("(parsed");
            toStringExplicitType(sb);
            return sb.append("): ").append(value).toString();
        }

        @Override
        protected RawType performClone() {
            // FIXME: MID-6833 We clone value since Midpoint assumes clone to mutable contract
            return new RawType(new Parsed<PrismValue>(prismContext, value.clone(), explicitTypeName));
        }
    }

    private static class Transient<V extends PrismValue> extends Parsed<V> {

        private static final long serialVersionUID = 1L;

        public Transient(V parsed) {
            super(null, parsed, null);
        }

        @Override
        boolean isTransient() {
            return true;
        }
    }

    private static class Raw extends State {

        private static final long serialVersionUID = 1L;

        private XNode node;

        public Raw(PrismContext context, XNode node) {
            super(context);
            Preconditions.checkArgument(node.isImmutable(), "Supplied XNode must be immutable");
            this.node = node;
        }

        @Override
        protected boolean isParsed() {
            return false;
        }

        @Override
        protected String extractString(Supplier<String> defaultValue) {
            if (node instanceof PrimitiveXNode) {
                return ((PrimitiveXNode<?>) node).getStringValue();
            }
            return defaultValue.get();
        }

        @Override
        protected PrismValue parsedValueNullable() {
            return null;
        }

        @Override
        protected Parsed<PrismValue> parse() throws SchemaException {
            if (node.getTypeQName() != null) {
                TypeDefinition typeDefinition = checkPrismContext().getSchemaRegistry().findTypeDefinitionByType(node.getTypeQName());
                Class<?> javaClass = null;
                if (typeDefinition != null && typeDefinition.getCompileTimeClass() != null) {
                    javaClass = typeDefinition.getCompileTimeClass();
                }
                if(javaClass != null) {
                    javaClass = XsdTypeMapper.getXsdToJavaMapping(node.getTypeQName());
                }
                if (javaClass != null) {
                    return new Parsed<>(prismContext, valueFor(realValue(javaClass)), node.getTypeQName());
                }
                PrismValue asValue = prismContext.parserFor(node.toRootXNode()).parseItemValue();
                return new Parsed<PrismValue>(prismContext, asValue, node.getTypeQName());
            }

            // unknown or null type -- try parsing as string
            if (!(node instanceof PrimitiveXNode<?>)) {
                throw new SchemaException("Trying to parse non-primitive XNode as type '" + node.getTypeQName() + "'");
            }
            String stringValue = ((PrimitiveXNode<?>) node).getStringValue();

            // We return transient, so state is not updated.
            return new Transient<PrismValue>(valueFor(stringValue));
        }

        @Override
        protected <T> T realValue(Class<T> javaClass) throws SchemaException {
            return prismContext.parserFor(node.toRootXNode()).parseRealValue(javaClass);
        }

        @Override
        <IV extends PrismValue> Parsed<IV> parse(@Nullable ItemDefinition<?> itemDefinition, @Nullable QName itemName) throws SchemaException {
            IV value;
            if (itemDefinition != null  && !(itemDefinition instanceof PrismPropertyDefinition && ((PrismPropertyDefinition<?>) itemDefinition).isAnyType())) {
                if (itemName == null) {
                    itemName = itemDefinition.getItemName();
                }
                checkPrismContext();
                var rootNode = prismContext.xnodeFactory().root(itemName, node);
                Item<IV, ItemDefinition<?>> subItem = prismContext.parserFor(rootNode).name(itemName).definition(itemDefinition).<IV,ItemDefinition<?>>parseItem();
                if (!subItem.isEmpty()) {
                    value = subItem.getAnyValue();
                } else {
                    value = null;
                }
                if (value != null && !itemDefinition.canBeDefinitionOf(value)) {
                    throw new SchemaException("Attempt to parse raw value into "+value+" that does not match provided definition "+itemDefinition);
                }
                return new Parsed<>(prismContext, value, itemDefinition.getTypeName());
            }
            // we don't really want to set 'parsed', as we didn't performed real parsing
            @SuppressWarnings("unchecked")
            Parsed<IV> ret = (Parsed<IV>) new Transient<>(checkPrismContext().itemFactory().createPropertyValue(node));
            return ret;
        }

        @Override
        XNode toXNode() {
            return node;
        }

        @Override
        XNode xNodeNullable() {
            return node;
        }

        @Override
        protected Parsed<?> asParsed() {
            return null;
        }

        @Override
        protected void shortDump(StringBuilder sb) {
            sb.append("(raw").append("):").append(node);
        }

        @Override
        protected QName explicitTypeName() {
            return node.getTypeQName();
        }

        @Override
        protected void revive(PrismContext prismContext) throws SchemaException {
            super.revive(prismContext);
        }

        @Override
        public String toString() {
            var sb = new StringBuilder("(raw");
            toStringExplicitType(sb);
            return sb.append("): ").append(node).toString();
        }

        @Override
        protected boolean explicitTypeDeclaration() {
            return node.isExplicitTypeDeclaration();
        }

        @Override
        public boolean equals(Object other) {
            if(other == this) {
                return true;
            }
            if(other instanceof State) {
                return equalsXNode((State) other);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return node.hashCode();
        }


        private PrismValue valueFor(Object parsedValue) {
            if (parsedValue instanceof Containerable) {
                return ((Containerable) parsedValue).asPrismContainerValue();
            }
            if (parsedValue instanceof Referencable) {
                return ((Referencable) parsedValue).asReferenceValue();
            }
            if (parsedValue instanceof PolyStringType) {
                return prismContext.itemFactory().createPropertyValue(PolyString.toPolyString((PolyStringType) parsedValue));   // hack
            }
            return prismContext.itemFactory().createPropertyValue(parsedValue);
        }

        @Override
        protected RawType performClone() {
            // Raw XNode form is effectively immutable all the time, so we can reuse our state.
            return new RawType(this);
        }
    }

}
