package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Revivable;
import com.evolveum.midpoint.prism.parser.XNodeProcessor;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import org.apache.commons.lang.Validate;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

import javax.xml.namespace.QName;
import java.beans.Transient;
import java.io.Serializable;

/**
 * A class used to hold raw XNodes until the definition for such an object is known.
 */
public class RawType implements Serializable, Cloneable, Equals, Revivable {
	private static final long serialVersionUID = 4430291958902286779L;

    /**
     * This is obligatory.
     */
    private transient PrismContext prismContext;

    /*
     *  At most one of these two values (xnode, parsed) should be set.
     */

    /**
     * Unparsed value. It is set either on RawType instance construction
     * or gradually constructed when parsing via JAXB (see ContentList class).
     *
     * Note that its type QName is coupled with the "type" attribute.
     */
	private XNode xnode;

    /**
     * Parsed value. It is computed when calling getParsedValue/getParsedItem methods.
     *
     * Beware: At most one of these fields (xnode, parsed) may be non-null at any instant.
     */
	private PrismValue parsed;

    public RawType(PrismContext prismContext) {
        Validate.notNull(prismContext, "prismContext is not set - perhaps a forgotten call to adopt() somewhere?");
        this.prismContext = prismContext;
    }

    public RawType(XNode xnode, PrismContext prismContext) {
        this(prismContext);
        this.xnode = xnode;
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

    public PrismContext getPrismContext() {
        return prismContext;
    }

    //endregion

    //region Parsing and serialization
    // itemDefinition may be null; in that case we do the best what we can
	public <V extends PrismValue> V getParsedValue(ItemDefinition itemDefinition, QName itemName) throws SchemaException {
        if (parsed != null) {
			return (V) parsed;
		} else if (xnode != null) {
            V value;
			if (itemDefinition != null) {
                if (itemName == null) {
                    itemName = itemDefinition.getName();
                }
                checkPrismContext();
				Item<V> subItem = PrismUtil.getXnodeProcessor(prismContext).parseItem(xnode, itemName, itemDefinition);
				value = subItem.getValue(0);
			} else {
				PrismProperty<V> subItem = XNodeProcessor.parsePrismPropertyRaw(xnode, itemName, prismContext);
				value = (V) subItem.getValue();
			}
            xnode = null;
            parsed = value;
            return (V) parsed;
		} else {
		    return null;
        }
	}

    public <V extends PrismValue> Item<V> getParsedItem(ItemDefinition itemDefinition) throws SchemaException {
        Validate.notNull(itemDefinition);
        return getParsedItem(itemDefinition, itemDefinition.getName());
    }

    public <V extends PrismValue> Item<V> getParsedItem(ItemDefinition itemDefinition, QName itemName) throws SchemaException {
        Validate.notNull(itemDefinition);
        Validate.notNull(itemName);
        Item<V> item = itemDefinition.instantiate();
        V newValue = getParsedValue(itemDefinition, itemName);
        if (newValue != null) {
            item.add((V) newValue.clone());
        }
        return item;
    }

    public XNode serializeToXNode() throws SchemaException {
        if (xnode != null) {
            return xnode;
        } else if (parsed != null) {
            checkPrismContext();
            return PrismUtil.getXnodeProcessor(prismContext).serializeItemValue(parsed);
        } else {
            return null;            // or an exception here?
        }
    }
    //endregion

    //region Cloning, comparing, dumping (TODO)
    public RawType clone() {
    	RawType clone = new RawType(prismContext);
        if (xnode != null) {
    	    clone.xnode = xnode.clone();
        } else if (parsed != null) {
            clone.parsed = parsed.clone();
        }
    	return clone;
    }
    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((xnode == null) ? 0 : xnode.hashCode());
        result = prime * result + ((parsed == null) ? 0 : parsed.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
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
    }

    private boolean xnodeSerializationsAreEqual(RawType other) {
        try {
            return serializeToXNode().equals(other.serializeToXNode());
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

}
