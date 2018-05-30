/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

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
public abstract class DefinitionImpl implements Definition {

	private static final long serialVersionUID = -2643332934312107274L;
	@NotNull protected QName typeName;
	protected ItemProcessing processing;
    protected boolean isAbstract = false;
	protected String displayName;
	protected Integer displayOrder;
	protected String help;
    protected String documentation;
    protected boolean deprecated = false;
    protected String deprecatedSince;
    protected boolean experimental = false;
    protected boolean elaborate = false;
    private Map<QName,Object> annotations;

    /**
     * whether an item is inherited from a supertype (experimental feature)
     */
    protected boolean inherited = false;

	/**
	 * This means that this particular definition (of an item or of a type) is part of the runtime schema, e.g.
	 * extension schema, resource schema or connector schema or something like that. I.e. it is not defined statically.
     */
    protected boolean isRuntimeSchema;

    /**
     * Set true for definitions that are more important than others and that should be emphasized
     * during presentation. E.g. the emphasized definitions will always be displayed in the user
     * interfaces (even if they are empty), they will always be included in the dumps, etc.
     */
    protected boolean emphasized = false;

	protected transient PrismContext prismContext;

	DefinitionImpl(@NotNull QName typeName, @NotNull PrismContext prismContext) {
		this.typeName = typeName;
		this.prismContext = prismContext;
	}


	@Override
	@NotNull
	public QName getTypeName() {
		return typeName;
	}

	public void setTypeName(@NotNull QName typeName) {
		this.typeName = typeName;
	}

	@Override
	public boolean isIgnored() {
		return processing == ItemProcessing.IGNORE;
	}

	@Override
	public ItemProcessing getProcessing() {
		return processing;
	}

	public void setProcessing(ItemProcessing processing) {
		this.processing = processing;
	}

    @Override
	public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    @Override
	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}
	
	@Override
	public String getDeprecatedSince() {
		return deprecatedSince;
	}
	
	public void setDeprecatedSince(String deprecatedSince) {
		this.deprecatedSince = deprecatedSince;
	}

	@Override
    public boolean isExperimental() {
		return experimental;
	}

	public void setExperimental(boolean experimental) {
		this.experimental = experimental;
	}

	@Override
	public boolean isElaborate() {
		return elaborate;
	}

	public void setElaborate(boolean elaborate) {
		this.elaborate = elaborate;
	}

	public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    @Override
	public boolean isEmphasized() {
		return emphasized;
	}

	public void setEmphasized(boolean emphasized) {
		this.emphasized = emphasized;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Override
	public String getHelp() {
		return help;
	}

	public void setHelp(String help) {
		this.help = help;
	}

    @Override
	public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    @Override
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

    @Override
	public boolean isRuntimeSchema() {
        return isRuntimeSchema;
    }

    public void setRuntimeSchema(boolean isRuntimeSchema) {
        this.isRuntimeSchema = isRuntimeSchema;
    }

	@Override
	public PrismContext getPrismContext() {
		return prismContext;
	}

    @Override
	public Class getTypeClassIfKnown() {
        return XsdTypeMapper.toJavaTypeIfKnown(getTypeName());
    }

	@Override
	public Class getTypeClass() {
		return XsdTypeMapper.toJavaType(getTypeName());
	}

	@Override
	public Object getAnnotation(QName qname) {
		if (annotations == null) {
			return null;
		} else {
			return annotations.get(qname);
		}
	}

	@Override
	public void setAnnotation(QName qname, Object value) {
		if (annotations == null) {
			// Lazy init. Most definitions will not have any annotations.
			// We do not want to fill memory with empty hashmaps.
			annotations = new HashMap<>();
		}
		annotations.put(qname, value);
	}

	public abstract void revive(PrismContext prismContext);

	protected void copyDefinitionData(DefinitionImpl clone) {
		clone.processing = this.processing;
		clone.typeName = this.typeName;
		clone.displayName = this.displayName;
		clone.displayOrder = this.displayOrder;
		clone.help = this.help;
		clone.inherited = this.inherited;
        clone.documentation = this.documentation;
        clone.isAbstract = this.isAbstract;
        clone.deprecated = this.deprecated;
		clone.isRuntimeSchema = this.isRuntimeSchema;
		clone.emphasized = this.emphasized;
		clone.experimental = this.experimental;
		clone.elaborate = this.elaborate;
    }

	@SuppressWarnings("ConstantConditions")
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((processing == null) ? 0 : processing.hashCode());
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
		return result;
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefinitionImpl other = (DefinitionImpl) obj;
		if (processing != other.processing)
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
		return getDebugDumpClassName() + " ("+PrettyPrinter.prettyPrint(getTypeName())+")";
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createIndentedStringBuilder(indent);
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

	@NotNull
	@Override
	public abstract Definition clone();

//	@Override
//	public void accept(Visitor visitor) {
//		visitor.visit(this);
//	}
}
