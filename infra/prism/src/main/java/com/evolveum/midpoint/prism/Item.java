/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;


/**
 * Item is a common abstraction of Property and PropertyContainer.
 * <p/>
 * This is supposed to be a superclass for all items. Items are things
 * that can appear in property containers, which generally means only a property
 * and property container itself. Therefore this is in fact superclass for those
 * two definitions.
 *
 * @author Radovan Semancik
 */
public abstract class Item implements Dumpable, DebugDumpable, Serializable {

	// The object should basically work without definition and prismContext. This is the
	// usual case when it is constructed "out of the blue", e.g. as a new JAXB object
	// It may not work perfectly, but basic things should work
    protected QName name;
    protected PrismValue parent;
    protected ItemDefinition definition;
    private List<PrismValue> values = new ArrayList<PrismValue>();
    
    transient protected PrismContext prismContext;

    /**
     * This is used for definition-less construction, e.g. in JAXB beans.
     * 
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the ResourceObjectDefintion instead.
     */
    Item(QName name) {
        super();
        this.name = name;
    }

    /**
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the ResourceObjectDefintion instead.
     */
    Item(QName name, ItemDefinition definition, PrismContext prismContext) {
        super();
        this.name = name;
        this.definition = definition;
        this.prismContext = prismContext;
    }
        
    /**
     * Returns applicable property definition.
     * <p/>
     * May return null if no definition is applicable or the definition is not
     * know.
     *
     * @return applicable property definition
     */
    public ItemDefinition getDefinition() {
        return definition;
    }

    /**
     * Returns the name of the property.
     * <p/>
     * The name is a QName. It uniquely defines a property.
     * <p/>
     * The name may be null, but such a property will not work.
     * <p/>
     * The name is the QName of XML element in the XML representation.
     *
     * @return property name
     */
    public QName getName() {
        return name;
    }

    /**
     * Sets the name of the property.
     * <p/>
     * The name is a QName. It uniquely defines a property.
     * <p/>
     * The name may be null, but such a property will not work.
     * <p/>
     * The name is the QName of XML element in the XML representation.
     *
     * @param name the name to set
     */
    public void setName(QName name) {
        this.name = name;
    }

    /**
     * Sets applicable property definition.
     *
     * @param definition the definition to set
     */
    public void setDefinition(PrismPropertyDefinition definition) {
        this.definition = definition;
    }

	/**
     * Returns a display name for the property type.
     * <p/>
     * Returns null if the display name cannot be determined.
     * <p/>
     * The display name is fetched from the definition. If no definition
     * (schema) is available, the display name will not be returned.
     *
     * @return display name for the property type
     */
    public String getDisplayName() {
        return getDefinition() == null ? null : getDefinition().getDisplayName();
    }

    /**
     * Returns help message defined for the property type.
     * <p/>
     * Returns null if the help message cannot be determined.
     * <p/>
     * The help message is fetched from the definition. If no definition
     * (schema) is available, the help message will not be returned.
     *
     * @return help message for the property type
     */
    public String getHelp() {
        return getDefinition() == null ? null : getDefinition().getHelp();
    }
    
    public PrismContext getPrismContext() {
    	return prismContext;
    }
    
    public PrismValue getParent() {
    	return parent;
    }
    
    public void setParent(PrismValue parentValue) {
    	this.parent = parentValue;
    }
    
    public List<? extends PrismValue> getValues() {
		return values;
	}
    
    public Element asDomElement() {
    	// TODO
    	throw new UnsupportedOperationException();
    }
    
	/**
     * Serializes property to DOM or JAXB element(s).
     * <p/>
     * The property name will be used as an element QName.
     * The values will be in the element content. Single-value
     * properties will produce one element (on none), multi-valued
     * properies may produce several elements. All of the elements will
     * have the same QName.
     * <p/>
     * The property must have a definition (getDefinition() must not
     * return null).
     *
     * @param parentNode DOM Document
     * @return property serialized to DOM Element or JAXBElement
     * @throws SchemaException No definition or inconsistent definition
     */
//    abstract public void serializeToDom(Node parentNode) throws SchemaException;
    
	void applyDefinition(ItemDefinition definition) {
		this.definition = definition;
	}
    
    public void revive(PrismContext prismContext) {
    	if (this.prismContext != null) {
    		return;
    	}
    	this.prismContext = prismContext;
    	if (definition != null) {
    		definition.revive(prismContext);
    	}
    }

    public abstract Item clone();

    protected void copyValues(Item clone) {
        clone.name = this.name;
        clone.definition = this.definition;
        clone.prismContext = this.prismContext;
    }
    
    public static <T extends Item> T createNewDefinitionlessItem(QName name, Class<T> type) {
    	T item = null;
		try {
			Constructor<T> constructor = type.getConstructor(QName.class);
			item = constructor.newInstance(name);
		} catch (Exception e) {
			throw new SystemException("Error creating new definitionless "+type.getSimpleName()+": "+e.getClass().getName()+" "+e.getMessage(),e);
		}
    	return item;
    }
    
    public boolean isEmpty() {
        return (getValues() == null || getValues().isEmpty());
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((definition == null) ? 0 : definition.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((values == null) ? 0 : values.hashCode());
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
		Item other = (Item) obj;
		if (definition == null) {
			if (other.definition != null)
				return false;
		} else if (!definition.equals(other.definition))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		// The != is there by purpose (to avoid loops)
		} else if (parent != other.parent)
			return false;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

	@Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getName() + ")";
    }

    @Override
    public String dump() {
        return toString();
    }

    /**
     * Provide terse and readable dump of the object suitable for log (at debug level).
     */
    public String debugDump() {
        return debugDump(0);
    }

    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(getDebugDumpClassName()).append(": ").append(DebugUtil.prettyPrint(getName()));
        return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    protected String getDebugDumpClassName() {
        return "Item";
    }


}
