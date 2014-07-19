/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.prism;

import java.io.Serializable;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.lang.StringUtils;

/**
 * Abstract item definition in the schema.
 * 
 * This is supposed to be a superclass for all item definitions. Items are things
 * that can appear in property containers, which generally means only a property
 * and property container itself. Therefore this is in fact superclass for those
 * two definitions.
 * 
 * The definitions represent data structures of the schema. Therefore instances
 * of Java objects from this class represent specific <em>definitions</em> from
 * the schema, not specific properties or objects. E.g the definitions does not
 * have any value.
 * 
 * To transform definition to a real property or object use the explicit
 * instantiate() methods provided in the definition classes. E.g. the
 * instantiate() method will create instance of Property using appropriate
 * PropertyDefinition.
 * 
 * The convenience methods in Schema are using this abstract class to find
 * appropriate definitions easily.
 * 
 * @author Radovan Semancik
 * 
 */
public abstract class ItemDefinition extends Definition implements Serializable {

	private static final long serialVersionUID = -2643332934312107274L;
	protected QName name;
	private int minOccurs = 1;
    private int maxOccurs = 1;
    private boolean operational = false;
    private boolean dynamic;
    private boolean canAdd = true;
    private boolean canRead = true;
    private boolean canModify = true;

	// TODO: annotations

	/**
	 * The constructors should be used only occasionally (if used at all).
	 * Use the factory methods in the ResourceObjectDefintion instead.
	 * 
	 * @param name definition name (element Name)
	 * @param defaultName default element name
	 * @param typeName type name (XSD complex or simple type)
	 */
	ItemDefinition(QName elementName, QName typeName, PrismContext prismContext) {
		super(typeName, prismContext);
		this.name = elementName;
	}

	/**
	 * Returns name of the defined entity.
	 * 
	 * The name is a name of the entity instance if it is fixed by the schema.
	 * E.g. it may be a name of the property in the container that cannot be
	 * changed.
	 * 
	 * The name corresponds to the XML element name in the XML representation of
	 * the schema. It does NOT correspond to a XSD type name.
	 * 
	 * Name is optional. If name is not set the null value is returned. If name is
	 * not set the type is "abstract", does not correspond to the element.
	 * 
	 * @return the name name of the entity or null.
	 */
	public QName getName() {
		return name;
	}

	public void setName(QName name) {
		this.name = name;
	}
	
    public String getNamespace() {
    	return getName().getNamespaceURI();
    }
	
    /**
     * Return the number of minimal value occurrences.
     *
     * @return the minOccurs
     */
    public int getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }
    
    /**
     * Return the number of maximal value occurrences.
     * <p/>
     * Any negative number means "unbounded".
     *
     * @return the maxOccurs
     */
    public int getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }
    
    /**
     * Returns true if property is single-valued.
     *
     * @return true if property is single-valued.
     */
    public boolean isSingleValue() {
    	int maxOccurs = getMaxOccurs();
        return maxOccurs >= 0 && maxOccurs <= 1;
    }

    /**
     * Returns true if property is multi-valued.
     *
     * @return true if property is multi-valued.
     */
    public boolean isMultiValue() {
    	int maxOccurs = getMaxOccurs();
        return maxOccurs < 0 || maxOccurs > 1;
    }

    /**
     * Returns true if property is mandatory.
     *
     * @return true if property is mandatory.
     */
    public boolean isMandatory() {
        return getMinOccurs() > 0;
    }

    /**
     * Returns true if property is optional.
     *
     * @return true if property is optional.
     */
    public boolean isOptional() {
        return getMinOccurs() == 0;
    }

	public boolean isOperational() {
		return operational;
	}

	public void setOperational(boolean operational) {
		this.operational = operational;
	}

	/**
	 * Returns true if definition was created during the runtime based on a dynamic information
	 * such as xsi:type attributes in XML. This means that the definition needs to be stored
	 * alongside the data to have a successful serialization "roundtrip". The definition is not
	 * part of any schema and therefore cannot be determined. It may even be different for every
	 * instance of the associated item (element name).
	 */
	public boolean isDynamic() {
		return dynamic;
	}
	
	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}
	
    /**
     * Returns true if the property can be read. I.e. if it is returned in objects
     * retrieved from "get", "search" and similar operations.
     */
    public boolean canRead() {
        return canRead;
    }

    /**
     * Returns true if the item can be modified. I.e. if it can be changed
     * during a modification of existing object.
     */
    public boolean canModify() {
        return canModify;
    }

    /**
     *
     */
    public void setReadOnly() {
        canAdd = false;
        canRead = true;
        canModify = false;
    }

	public void setCanRead(boolean read) {
        this.canRead = read;
    }

    public void setCanModify(boolean modify) {
        this.canModify = modify;
    }

    public void setCanAdd(boolean add) {
        this.canAdd = add;
    }

    /**
     * Returns true if the item can be added. I.e. if it can be present
     * in the object when a new object is created.
     */
    public boolean canAdd() {
        return canAdd;
    }
	
	public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
		if (!clazz.isAssignableFrom(this.getClass())) {
    		return false;
    	}
        if (QNameUtil.match(elementQName, getName())) {
        	return true;
        }
        return false;
	}
	
	public void adoptElementDefinitionFrom(ItemDefinition otherDef) {
		if (otherDef == null) {
			return;
		}
		setName(otherDef.getName());
		setMinOccurs(otherDef.getMinOccurs());
		setMaxOccurs(otherDef.getMaxOccurs());
	}    

	/**
	 * Create an item instance. Definition name or default name will
	 * used as an element name for the instance. The instance will otherwise be empty.
	 * @return created item instance
	 */
	abstract public Item instantiate();

	/**
	 * Create an item instance. Definition name will use provided name.
	 * for the instance. The instance will otherwise be empty.
	 * @return created item instance
	 */
	abstract public Item instantiate(QName name);

    // add namespace from the definition if it's safe to do so
    protected QName addNamespaceIfApplicable(QName name) {
        if (StringUtils.isEmpty(name.getNamespaceURI())) {
            if (QNameUtil.match(name, this.name)) {
                return this.name;
            }
        }
        return name;
    }

    <T extends ItemDefinition> T findItemDefinition(ItemPath path, Class<T> clazz) {
        if (path.isEmpty()) {
        	if (clazz.isAssignableFrom(this.getClass())) {
        		return (T) this;
        	} else {
        		throw new IllegalArgumentException("Looking for definition of class " + clazz + " but found " + this);
        	}
        } else {
            throw new IllegalArgumentException("No definition for path " + path + " in " + this);
        }
    }
    
    abstract public ItemDelta createEmptyDelta(ItemPath path);
    
    abstract public ItemDefinition clone();
    	
	protected void copyDefinitionData(ItemDefinition clone) {
		super.copyDefinitionData(clone);
		clone.name = this.name;
		clone.minOccurs = this.minOccurs;
		clone.maxOccurs = this.maxOccurs;
		clone.dynamic = this.dynamic;
		clone.canAdd = this.canAdd;
		clone.canRead = this.canRead;
		clone.canModify = this.canModify;
		clone.operational = this.operational;
	}

	@Override
	public void revive(PrismContext prismContext) {
		if (this.prismContext != null) {
			return;
		}
		this.prismContext = prismContext;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + maxOccurs;
        result = prime * result + minOccurs;
        result = prime * result + (canAdd ? 1231 : 1237);
        result = prime * result + (canRead ? 1231 : 1237);
        result = prime * result + (canModify ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemDefinition other = (ItemDefinition) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (maxOccurs != other.maxOccurs)
            return false;
        if (minOccurs != other.minOccurs)
            return false;
        if (canAdd != other.canAdd)
            return false;
        if (canRead != other.canRead)
            return false;
        if (canModify != other.canModify)
            return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getDebugDumpClassName());
		sb.append(":");
		sb.append(PrettyPrinter.prettyPrint(getName()));
		sb.append(" ");
		debugDumpShortToString(sb);
		return sb.toString();
	}
	
	/**
	 * Used in debugDumping items. Does not need to have name in it as item already has it. Does not need
	 * to have class as that is just too much info that is almost anytime pretty obvious anyway.
	 */
	void debugDumpShortToString(StringBuilder sb) {
		sb.append(PrettyPrinter.prettyPrint(getTypeName()));
        debugMultiplicity(sb);
        debugFlags(sb);
	}

    private void debugMultiplicity(StringBuilder sb) {
        sb.append("[");
        sb.append(getMinOccurs());
        sb.append(",");
        sb.append(getMaxOccurs());
        sb.append("]");
    }

    public String debugMultiplicity() {
        StringBuilder sb = new StringBuilder();
        debugMultiplicity(sb);
        return sb.toString();
    }

    private void debugFlags(StringBuilder sb) {
        if (isIgnored()) {
            sb.append(",ignored");
        }
        if (isDynamic()) {
            sb.append(",dyn");
        }
        extendToString(sb);
    }

    public String debugFlags() {
        StringBuilder sb = new StringBuilder();
        debugFlags(sb);
        // This starts with a collon, we do not want it here
        if (sb.length() > 0) {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }
	
	protected void extendToString(StringBuilder sb) {
		sb.append(",");
		if (canRead()) {
			sb.append("R");
		} else {
			sb.append("-");
		}
		if (canAdd()) {
			sb.append("A");
		} else {
			sb.append("-");
		}
		if (canModify()) {
			sb.append("M");
		} else {
			sb.append("-");
		}

	}
	
}
