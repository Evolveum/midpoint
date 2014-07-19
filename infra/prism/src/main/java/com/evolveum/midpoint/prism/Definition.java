/*
 * Copyright (c) 2010-2013 Evolveum
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
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.MiscUtil;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Abstract definition in the schema.
 * 
 * This is supposed to be a superclass for all definitions. It defines common
 * properties for all definitions.
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
public abstract class Definition implements Serializable, DebugDumpable, Revivable {

	private static final long serialVersionUID = -2643332934312107274L;
	protected QName typeName;
	protected boolean ignored = false;
    protected boolean isAbstract = false;
	protected String displayName;
	protected Integer displayOrder;
	protected String help;
    protected String documentation;
    protected boolean deprecated = false;
    protected boolean inherited = false;            // whether an item is inherited from a supertype (experimental feature)
	
	/**
     * This means that the property container is not defined by fixed (compile-time) schema.
     * This in fact means that we need to use getAny in a JAXB types. It does not influence the
     * processing of DOM that much, as that does not really depend on compile-time/run-time distinction.
     */
    protected boolean isRuntimeSchema;
    
	protected transient PrismContext prismContext;

	Definition(QName typeName, PrismContext prismContext) {
		if (typeName == null) {
			throw new IllegalArgumentException("Type name can't be null.");
		}
        if (prismContext == null) {
            throw new IllegalArgumentException("prismContext can't be null.");
        }
		this.typeName = typeName;
		this.prismContext = prismContext;
	}

	/**
	 * Returns the name of the definition type.
	 * 
	 * Returns a name of the type for this definition.
	 * 
	 * In XML representation that corresponds to the name of the XSD type.
	 * 
	 * @return the typeName
	 */
	public QName getTypeName() {
		return typeName;
	}
	
	public void setTypeName(QName typeName) {
		this.typeName = typeName;
	}

	public boolean isIgnored() {
		return ignored;
	}

	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}

    public boolean isInherited() {
        return inherited;
    }

    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    /**
	 * Returns display name.
	 * 
	 * Specifies the printable name of the object class or attribute. It must
	 * contain a printable string. It may also contain a key to catalog file.
	 * 
	 * Returns null if no display name is set.
	 * 
	 * Corresponds to "displayName" XSD annotation.
	 * 
	 * @return display name string or catalog key
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(String displayName) {		
		this.displayName = displayName;
	}
	
	/**
	 * Specifies an order in which the item should be displayed relative to other items
	 * at the same level. The items will be displayed by sorting them by the
	 * values of displayOrder annotation (ascending). Items that do not have
	 * any displayOrder annotation will be displayed last. The ordering of
	 * values with the same displayOrder is undefined and it may be arbitrary.
	 */
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	/**
	 * Returns help string.
	 * 
	 * Specifies the help text or a key to catalog file for a help text. The
	 * help text may be displayed in any suitable way by the GUI. It should
	 * explain the meaning of an attribute or object class.
	 * 
	 * Returns null if no help string is set.
	 * 
	 * Corresponds to "help" XSD annotation.
	 * 
	 * @return help string or catalog key
	 */
	public String getHelp() {
		return help;
	}
	
	public void setHelp(String help) {
		this.help = help;
	}

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    /**
     * Returns only a first sentence of documentation.
     */
    public String getDocumentationPreview() {
        if (documentation == null || documentation.isEmpty()) {
            return documentation;
        }
        String plainDoc = MiscUtil.stripHtmlMarkup(documentation);
        int i = plainDoc.indexOf('.');
        if (i<0) {
            return plainDoc;
        }
        return plainDoc.substring(0,i+1);
    }

    public boolean isRuntimeSchema() {
        return isRuntimeSchema;
    }

    public void setRuntimeSchema(boolean isRuntimeSchema) {
        this.isRuntimeSchema = isRuntimeSchema;
    }
	
	public PrismContext getPrismContext() {
		return prismContext;
	}
	
	protected SchemaRegistry getSchemaRegistry() {
		return prismContext.getSchemaRegistry();
	}

    public Class getTypeClassIfKnown() {
        return XsdTypeMapper.toJavaTypeIfKnown(getTypeName());
    }

	public Class getTypeClass() {
		return XsdTypeMapper.toJavaType(getTypeName());
	}
	
	public abstract void revive(PrismContext prismContext);
	
	public abstract Definition clone(); 
	
	protected void copyDefinitionData(Definition clone) {
		clone.ignored = this.ignored;
		clone.typeName = this.typeName;
		clone.displayName = this.displayName;
		clone.displayOrder = this.displayOrder;
		clone.help = this.help;
		clone.inherited = this.inherited;
        clone.documentation = this.documentation;
        clone.isAbstract = this.isAbstract;
        clone.deprecated = this.deprecated;
    }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (ignored ? 1231 : 1237);
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
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
		Definition other = (Definition) obj;
		if (ignored != other.ignored)
			return false;
		if (typeName == null) {
			if (other.typeName != null)
				return false;
		} else if (!typeName.equals(other.typeName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getDebugDumpClassName() + " ("+getTypeName()+")";
	}
	
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<indent; i++) {
			sb.append(DebugDumpable.INDENT_STRING);
		}
		sb.append(toString());
		return sb.toString();
	}
	
	/**
     * Return a human readable name of this class suitable for logs. (e.g. "PPD")
     */
    protected abstract String getDebugDumpClassName();

    /**
     * Returns human-readable name of this class suitable for documentation. (e.g. "property")
     */
    public abstract String getDocClassName();
}
