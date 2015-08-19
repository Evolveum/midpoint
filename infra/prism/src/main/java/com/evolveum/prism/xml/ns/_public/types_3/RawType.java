package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Revivable;
import com.evolveum.midpoint.prism.parser.XNodeProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
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
     * Unparsed value. It is set on RawType instance construction.
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
	public <IV extends PrismValue,ID extends ItemDefinition> IV getParsedValue(ItemDefinition itemDefinition, QName itemName) throws SchemaException {
        if (parsed != null) {
			return (IV) parsed;
		} else if (xnode != null) {
            IV value;
			if (itemDefinition != null) {
                if (itemName == null) {
                    itemName = itemDefinition.getName();
                }
                checkPrismContext();
				Item<IV,ID> subItem = PrismUtil.getXnodeProcessor(prismContext).parseItem(xnode, itemName, itemDefinition);
				if (!subItem.isEmpty()){
					value = subItem.getValue(0);
				} else {
					value = null;
				}
			} else {
				PrismProperty subItem = XNodeProcessor.parsePrismPropertyRaw(xnode, itemName, prismContext);
				value = (IV) subItem.getValue();
			}
            xnode = null;
            parsed = value;
            return (IV) parsed;
		} else {
		    return null;
        }
	}
	
	public <V,ID extends ItemDefinition> V getParsedRealValue(ItemDefinition itemDefinition, ItemPath itemPath) throws SchemaException {
        if (parsed == null && xnode != null){
        	
			if (itemDefinition == null){
        			return PrismUtil.getXnodeProcessor(prismContext).parseAnyValue(xnode);
        	} else {
        		QName itemName = ItemPath.getName(itemPath.lastNamed());
	        	getParsedValue(itemDefinition, itemName);
        	}
        } 
        if (parsed != null){
        	if (parsed instanceof PrismPropertyValue){
        		return (V) ((PrismPropertyValue) parsed).getValue();
        	} else if (parsed instanceof PrismContainerValue){
        		return (V) ((PrismContainerValue) parsed).asContainerable();
        	} else if (parsed instanceof PrismReferenceValue){
        		return (V) ((PrismReferenceValue) parsed).asReferencable();
        	}
        }
        
        return null;
        
	}
	
	

    public <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> getParsedItem(ID itemDefinition) throws SchemaException {
        Validate.notNull(itemDefinition);
        return getParsedItem(itemDefinition, itemDefinition.getName());
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

    public XNode serializeToXNode() throws SchemaException {
        if (xnode != null) {
        	QName type = xnode.getTypeQName();
        	if (xnode instanceof PrimitiveXNode && type != null){
        		if (!((PrimitiveXNode)xnode).isParsed()){
        			Object realValue = PrismUtil.getXnodeProcessor(prismContext).parseAnyValue(xnode);
        			((PrimitiveXNode)xnode).setValue(realValue, type);
        		}
        	}
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

    public static RawType create(String value, PrismContext prismContext) {
        PrimitiveXNode<String> xnode = new PrimitiveXNode<>(value);
        RawType rv = new RawType(xnode, prismContext);
        return rv;
    }

}
