package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.apache.commons.lang.StringUtils;
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
 */
public class RawType implements Serializable, Cloneable, Equals, Revivable, ShortDumpable {
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

    public RawType(XNode xnode, @NotNull PrismContext prismContext) {
        this(prismContext);
        this.xnode = xnode;
        if (xnode != null) {
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
		return new RootXNode(itemName, xnode);
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
	public <IV extends PrismValue,ID extends ItemDefinition> IV getParsedValue(@Nullable ItemDefinition itemDefinition, @Nullable QName itemName) throws SchemaException {
        if (parsed != null) {
//        	Check too intense for normal operation?
//        	if (!itemDefinition.canBeDefinitionOf(parsed)) {
//				throw new SchemaException("Attempt to return parsed raw value "+parsed+" that does not match provided definition "+itemDefinition); 
//			}
			return (IV) parsed;
		} else if (xnode != null) {
            IV value;
			if (itemDefinition != null
					&& !(itemDefinition instanceof PrismPropertyDefinition && ((PrismPropertyDefinition) itemDefinition).isAnyType())) {
                if (itemName == null) {
                    itemName = itemDefinition.getName();
                }
                checkPrismContext();
				Item<IV,ID> subItem = prismContext.parserFor(getRootXNode(itemName)).name(itemName).definition(itemDefinition).parseItem();
				if (!subItem.isEmpty()){
					value = subItem.getValue(0);
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
				return (IV) PrismPropertyValue.createRaw(xnode);
			}
		} else {
		    return null;
        }
	}

	public <V,ID extends ItemDefinition> V getParsedRealValue(ID itemDefinition, ItemPath itemPath) throws SchemaException {
        if (parsed == null && xnode != null) {
			if (itemDefinition == null) {
				return prismContext.parserFor(xnode.toRootXNode()).parseRealValue();		// TODO what will be the result without definition?
        	} else {
        		QName itemName = ItemPath.getName(itemPath.lastNamed());
	        	getParsedValue(itemDefinition, itemName);
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

	public <T> T getParsedRealValue(@NotNull Class<T> clazz) throws SchemaException {
		if (parsed != null) {
			if (clazz.isAssignableFrom(parsed.getRealValue().getClass())) {
				return (T) parsed.getRealValue();
			} else {
				throw new IllegalArgumentException("Parsed value ("+parsed.getClass()+") is not assignable to "+clazz);
			}
		} else if (xnode != null) {
			return prismContext.parserFor(xnode.toRootXNode()).parseRealValue(clazz);
		} else {
			return null;
		}
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

//    // Returns either an item or a real value.
//    // VERY EXPERIMENTAL.
//	public Object getParsedItemOrRealValue() throws SchemaException {
//		if (parsed != null) {
//			return
//		} else if (xnode != null) {
//			return prismContext.parserFor(xnode.toRootXNode()).parseItemOrRealValue();
//		} else {
//			return null;
//		}
//	}


	public XNode serializeToXNode() throws SchemaException {
        if (xnode != null) {
//        	QName type = xnode.getTypeQName();
//        	if (xnode instanceof PrimitiveXNode && type != null){
//        		if (!((PrimitiveXNode)xnode).isParsed()){
//        			Object realValue = PrismUtil.getXnodeProcessor(prismContext).parseAnyValue(xnode, ParsingContext.createDefault());
//        			((PrimitiveXNode)xnode).setValue(realValue, type);
//        		}
//        	}
            return xnode;
        } else if (parsed != null) {
            checkPrismContext();
	        XNode rv = prismContext.xnodeSerializer().root(new QName("dummy")).serialize(parsed).getSubnode();
	        rv.setTypeQName(explicitTypeName);
	        rv.setExplicitTypeDeclaration(explicitTypeDeclaration);
	        return rv;
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
        clone.explicitTypeName = explicitTypeName;
        clone.explicitTypeDeclaration = explicitTypeDeclaration;
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
        PrimitiveXNode<String> xnode = new PrimitiveXNode<>(value);
		return new RawType(xnode, prismContext);
    }

    public static RawType create(XNode node, PrismContext prismContext) {
		return new RawType(node, prismContext);
    }

	@Override
	public String toString() {
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
	public void shortDump(StringBuilder sb) {
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
}
