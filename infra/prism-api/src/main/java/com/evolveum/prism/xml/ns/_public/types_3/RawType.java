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
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import com.evolveum.midpoint.util.exception.TunnelException;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Objects;

/**
 * A class used to hold raw XNodes until the definition for such an object is known.
 *
 * This class should be thread-safe because it is used in shared objects, like cached resources or roles. (See MID-6506.)
 * But by default it is not, as it contains internal state (xnode/parsed) that can lead to race conditions when parsing.
 *
 * In midPoint 4.2 it was made roughly thread-safe by including explicit synchronization at appropriate places.
 * But there is much more to be done:
 *  (1) we need to support freezing the content (embedded xnode/prism value),
 *  (2) we should consider avoiding explicit synchronization for performance reasons
 *
 * See MID-6542.
 */
public class RawType implements Serializable, Cloneable, Equals, Revivable, ShortDumpable, JaxbVisitable, PrismContextSensitive {
    private static final long serialVersionUID = 4430291958902286779L;

    /**
     * This is obligatory.
     */
    private transient PrismContext prismContext;

    /*
     *  At most one of these two values (xnode, parsed) should be set.
     */

    /**
     * Unparsed value. It is set on RawType instance construction.
     */
    private XNode xnode;

    /**
     * Parsed value. It is computed when calling getParsedValue/getParsedItem methods.
     *
     * Beware: At most one of these fields (xnode, parsed) may be non-null at any instant.
     */
    private PrismValue parsed;

    private QName explicitTypeName;
    private boolean explicitTypeDeclaration;

    public RawType(PrismContext prismContext) {
        Validate.notNull(prismContext, "prismContext is not set - perhaps a forgotten call to adopt() somewhere?");
        this.prismContext = prismContext;
    }

    public RawType(XNode node, @NotNull PrismContext prismContext) {
        this(prismContext);
        this.xnode = node;
        if (this.xnode != null) {
            this.explicitTypeName = xnode.getTypeQName();
            this.explicitTypeDeclaration = xnode.isExplicitTypeDeclaration();
        }
    }

    public RawType(PrismValue parsed, QName explicitTypeName, @NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
        this.parsed = parsed;
        if (explicitTypeName != null) {
            this.explicitTypeName = explicitTypeName;
            this.explicitTypeDeclaration = true;        // todo
        } else if (parsed != null && parsed.getTypeName() != null) {
            this.explicitTypeName = parsed.getTypeName();
            this.explicitTypeDeclaration = true;        // todo
        }
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
        if (parsed != null) {
            return parsed.getRealValue();
        }
        if (xnode == null) {
            return null;
        }
        if (xnode.getTypeQName() != null) {
            TypeDefinition typeDefinition = prismContext.getSchemaRegistry().findTypeDefinitionByType(xnode.getTypeQName());
            if (typeDefinition != null && typeDefinition.getCompileTimeClass() != null) {
                return storeIfRequested(getParsedRealValue(typeDefinition.getCompileTimeClass()), store);
            }
            Class<?> javaClass = XsdTypeMapper.getXsdToJavaMapping(xnode.getTypeQName());
            if (javaClass != null) {
                return storeIfRequested(getParsedRealValue(javaClass), store);
            }
        }
        // unknown or null type -- try parsing as string
        if (!(xnode instanceof PrimitiveXNode<?>)) {
            throw new SchemaException("Trying to parse non-primitive XNode as type '" + xnode.getTypeQName() + "'");
        } else {
            return ((PrimitiveXNode) xnode).getStringValue();
        }
    }

    private Object storeIfRequested(Object parsedValue, boolean store) {
        if (parsed == null && store) {
            if (parsedValue instanceof Containerable) {
                parsed = ((Containerable) parsedValue).asPrismContainerValue();
                xnode = null;
            } else if (parsedValue instanceof Referencable) {
                parsed = ((Referencable) parsedValue).asReferenceValue();
                xnode = null;
            } else if (parsedValue instanceof PolyStringType) {
                parsed = prismContext.itemFactory().createPropertyValue(PolyString.toPolyString((PolyStringType) parsedValue));   // hack
                xnode = null;
            } else if (parsedValue != null) {
                parsed = prismContext.itemFactory().createPropertyValue(parsedValue);
                xnode = null;
            }
        }
        return parsedValue;
    }

    /**
     * TEMPORARY. EXPERIMENTAL. DO NOT USE.
     */
    @Experimental
    public synchronized String extractString() {
        if (xnode instanceof PrimitiveXNode) {
            return ((PrimitiveXNode<?>) xnode).getStringValue();
        } else if (parsed != null) {
            return String.valueOf((Object) parsed.getRealValue());
        } else {
            return toString();
        }
    }

    public synchronized String extractString(String defaultValue) {
        if (xnode instanceof PrimitiveXNode) {
            return ((PrimitiveXNode<?>) xnode).getStringValue();
        } else if (parsed != null) {
            return String.valueOf((Object) parsed.getRealValue());
        } else {
            return defaultValue;
        }
    }


    @Override
    public void revive(PrismContext prismContext) throws SchemaException {
        Validate.notNull(prismContext);
        this.prismContext = prismContext;
        if (parsed != null) {
            parsed.revive(prismContext);
        }
    }

    //region General getters/setters

    public XNode getXnode() {
        return xnode;
    }

    @NotNull
    public RootXNode getRootXNode(@NotNull QName itemName) {
        return prismContext.xnodeFactory().root(itemName, xnode);
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    // experimental
    public QName getExplicitTypeName() {
        return explicitTypeName;
    }
    //endregion

    //region Parsing and serialization
    // itemDefinition may be null; in that case we do the best what we can
    public synchronized <IV extends PrismValue,ID extends ItemDefinition> IV getParsedValue(@Nullable ItemDefinition itemDefinition, @Nullable QName itemName) throws SchemaException {
        if (parsed != null) {
//            Check too intense for normal operation?
//            if (!itemDefinition.canBeDefinitionOf(parsed)) {
//                throw new SchemaException("Attempt to return parsed raw value "+parsed+" that does not match provided definition "+itemDefinition);
//            }
            return (IV) parsed;
        } else if (xnode != null) {
            IV value;
            if (itemDefinition != null
                    && !(itemDefinition instanceof PrismPropertyDefinition && ((PrismPropertyDefinition) itemDefinition).isAnyType())) {
                if (itemName == null) {
                    itemName = itemDefinition.getItemName();
                }
                checkPrismContext();
                Item<IV,ID> subItem = prismContext.parserFor(getRootXNode(itemName)).name(itemName).definition(itemDefinition).parseItem();
                if (!subItem.isEmpty()) {
                    value = subItem.getAnyValue();
                } else {
                    value = null;
                }
                if (value != null && !itemDefinition.canBeDefinitionOf(value)) {
                    throw new SchemaException("Attempt to parse raw value into "+value+" that does not match provided definition "+itemDefinition);
                }
                xnode = null;
                parsed = value;
                explicitTypeName = itemDefinition.getTypeName();
                explicitTypeDeclaration = true; // todo
                return (IV) parsed;
            } else {
                // we don't really want to set 'parsed', as we didn't performed real parsing
                //noinspection unchecked
                return (IV) prismContext.itemFactory().createPropertyValue(xnode);
            }
        } else {
            return null;
        }
    }

    public synchronized <V,ID extends ItemDefinition> V getParsedRealValue(ID itemDefinition, ItemPath itemPath) throws SchemaException {
        if (parsed == null && xnode != null) {
            if (itemDefinition == null) {
                return prismContext.parserFor(xnode.toRootXNode()).parseRealValue();        // TODO what will be the result without definition?
            } else {
                getParsedValue(itemDefinition, itemPath.lastName());
            }
        }
        if (parsed != null) {
            return parsed.getRealValue();
        }
        return null;
    }

    public PrismValue getAlreadyParsedValue() {
        return parsed;
    }

    public synchronized <T> T getParsedRealValue(@NotNull Class<T> clazz) throws SchemaException {
        if (parsed != null) {
            Object realValue = parsed.getRealValue();
            if (realValue != null) {
                if (clazz.isAssignableFrom(realValue.getClass())) {
                    //noinspection unchecked
                    return (T) realValue;
                } else {
                    throw new IllegalArgumentException("Parsed value ("+realValue.getClass()+") is not assignable to "+clazz);
                }
            } else {
                return null; // strange but possible
            }
        } else if (xnode != null) {
            return prismContext.parserFor(xnode.toRootXNode()).parseRealValue(clazz);
        } else {
            return null;
        }
    }

    public <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> getParsedItem(ID itemDefinition) throws SchemaException {
        Validate.notNull(itemDefinition);
        return getParsedItem(itemDefinition, itemDefinition.getItemName());
    }

    public <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> getParsedItem(ID itemDefinition, QName itemName) throws SchemaException {
        Validate.notNull(itemDefinition);
        Validate.notNull(itemName);
        Item<IV,ID> item = itemDefinition.instantiate();
        IV newValue = getParsedValue(itemDefinition, itemName);
        if (newValue != null) {
            item.add((IV) newValue.clone());
        }
        return item;
    }

    public synchronized XNode serializeToXNode() throws SchemaException {
        if (xnode != null) {
            return xnode;
        } else if (parsed != null) {
            checkPrismContext();
            XNode rv = prismContext.xnodeSerializer().root(new QName("dummy")).serialize(parsed).getSubnode();
            prismContext.xnodeMutator().setXNodeType(rv, explicitTypeName, explicitTypeDeclaration);
            return rv;
        } else {
            return null;            // or an exception here?
        }
    }
    //endregion

    //region Cloning, comparing, dumping (TODO)
    public synchronized RawType clone() {
        RawType clone = new RawType(prismContext);
        if (xnode != null) {
            clone.xnode = xnode.clone();
        } else if (parsed != null) {
            clone.parsed = parsed.clone();
        }
        clone.explicitTypeName = explicitTypeName;
        clone.explicitTypeDeclaration = explicitTypeDeclaration;
        return clone;
    }

    @Override
    public synchronized int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((xnode == null) ? 0 : xnode.hashCode());
        result = prime * result + ((parsed == null) ? 0 : parsed.hashCode());
        return result;
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
        if (xnode != null && other.xnode != null) {
            return xnode.equals(other.xnode);
        } else if (parsed != null && other.parsed != null) {
            return parsed.equals(other.parsed);
        } else {
            return xnodeSerializationsAreEqual(other);
        }
        // TODO explicit type declaration? (probably should be ignored as it is now)
    }

    private boolean xnodeSerializationsAreEqual(RawType other) {
        try {
            return Objects.equals(serializeToXNode(), other.serializeToXNode());
        } catch (SchemaException e) {
            // or should we silently return false?
            throw new SystemException("Couldn't serialize RawType to XNode when comparing them", e);
        }
    }

    @Override
    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object that,
            EqualsStrategy equalsStrategy) {
        return equals(that);
    }
    //endregion

    private void checkPrismContext() {
        if (prismContext == null) {
            throw new IllegalStateException("prismContext is not set - perhaps a forgotten call to adopt() somewhere?");
        }
    }

    public static RawType create(String value, PrismContext prismContext) {
        PrimitiveXNode<String> xnode = prismContext.xnodeFactory().primitive(value);
        return new RawType(xnode, prismContext);
    }

    public static RawType create(XNode node, PrismContext prismContext) {
        return new RawType(node, prismContext);
    }

    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RawType: ");
        if (xnode != null) {
            sb.append("(raw");
            toStringExplicitType(sb);
            sb.append("): ").append(xnode);
        } else if (parsed != null) {
            sb.append("(parsed");
            toStringExplicitType(sb);
            sb.append("): ").append(parsed);
        } else {
            sb.append("(empty");
            toStringExplicitType(sb);
            sb.append(")");
        }

        return sb.toString();
    }

    private void toStringExplicitType(StringBuilder sb) {
        if (explicitTypeDeclaration) {
            sb.append(":");
            if (explicitTypeName == null) {
                sb.append("null");
            } else {
                sb.append(explicitTypeName.getLocalPart());
            }
        }
    }

    @Override
    public synchronized void shortDump(StringBuilder sb) {
        if (xnode != null) {
            sb.append("(raw");
            sb.append("):").append(xnode);
        } else if (parsed != null) {
            if (parsed instanceof ShortDumpable) {
                ((ShortDumpable)parsed).shortDump(sb);
            } else {
                Object realValue = parsed.getRealValue();
                if (realValue == null) {
                    sb.append("null");
                } else if (realValue instanceof ShortDumpable) {
                    ((ShortDumpable)realValue).shortDump(sb);
                } else {
                    sb.append(realValue.toString());
                }
            }
        }
    }

    public boolean isParsed() {
        return parsed != null;
    }

    // avoid if possible
    public synchronized String guessFormattedValue() throws SchemaException {
        if (parsed != null) {
            return parsed.getRealValue().toString();    // todo reconsider this
        } else if (xnode instanceof PrimitiveXNode) {
            return ((PrimitiveXNode) xnode).getGuessedFormattedValue();
        } else {
            return null;
        }
    }

    @Override
    public void accept(JaxbVisitor visitor) {
        Object value;
        if (isParsed() || explicitTypeName != null) {
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
}
