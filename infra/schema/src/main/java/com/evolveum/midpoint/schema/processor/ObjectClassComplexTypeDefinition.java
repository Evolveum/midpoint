/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.schema.processor;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ObjectClassComplexTypeDefinition extends ComplexTypeDefinition {
	
	private Collection<ResourceAttributeDefinition> identifiers;
	private Collection<ResourceAttributeDefinition> secondaryIdentifiers;
	private ResourceAttributeDefinition descriptionAttribute;
	private ResourceAttributeDefinition displayNameAttribute;
	private ResourceAttributeDefinition namingAttribute;
	private boolean defaultInAKind = false;
	private ShadowKindType kind;
	private String intent;
	private String nativeObjectClass;
	private boolean auxiliary;

	public ObjectClassComplexTypeDefinition(QName typeName, PrismContext prismContext) {
		super(typeName, prismContext);
	}
	
	public Collection<? extends ResourceAttributeDefinition> getAttributeDefinitions() {
		Collection<ResourceAttributeDefinition> attrs = new ArrayList<ResourceAttributeDefinition>(getDefinitions().size());
		for (ItemDefinition def: getDefinitions()) {
			if (def instanceof ResourceAttributeDefinition) {
				attrs.add((ResourceAttributeDefinition)def);
			}
		}
		return attrs;
	}
	
	/**
	 * Returns the definition of identifier attributes of a resource object.
	 * 
	 * May return empty set if there are no identifier attributes. Must not
	 * return null.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 * 
	 * @return definition of identifier attributes
	 * @throws IllegalStateException
	 *             if there is no definition for the referenced attributed
	 */
	public Collection<? extends ResourceAttributeDefinition> getIdentifiers() {
		if (identifiers == null) {
			identifiers = new ArrayList<ResourceAttributeDefinition>(1);
		}
		return identifiers;
	}
	
	public boolean isIdentifier(QName attrName) {
		for (ResourceAttributeDefinition idDef: getIdentifiers()) {
			if (idDef.getName().equals(attrName)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the definition of secondary identifier attributes of a resource
	 * object.
	 * 
	 * May return empty set if there are no secondary identifier attributes.
	 * Must not return null.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 * 
	 * @return definition of secondary identifier attributes
	 * @throws IllegalStateException
	 *             if there is no definition for the referenced attributed
	 */
	public Collection<? extends ResourceAttributeDefinition> getSecondaryIdentifiers() {
		if (secondaryIdentifiers == null) {
			secondaryIdentifiers = new ArrayList<ResourceAttributeDefinition>(1);
		}
		return secondaryIdentifiers;
	}
	
	public boolean isSecondaryIdentifier(QName attrName) {
		for (ResourceAttributeDefinition idDef: getSecondaryIdentifiers()) {
			if (idDef.getName().equals(attrName)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the definition of description attribute of a resource object.
	 * 
	 * Returns null if there is no description attribute.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 * 
	 * @return definition of secondary identifier attributes
	 * @throws IllegalStateException
	 *             if there is more than one description attribute. But this
	 *             should never happen.
	 * @throws IllegalStateException
	 *             if there is no definition for the referenced attributed
	 */
	public ResourceAttributeDefinition<?> getDescriptionAttribute() {
		return descriptionAttribute;
	}

	public void setDescriptionAttribute(ResourceAttributeDefinition<?> descriptionAttribute) {
		this.descriptionAttribute = descriptionAttribute;
	}
	
	/**
	 * Specifies which resource attribute should be used as a "technical" name
	 * for the account. This name will appear in log files and other troubleshooting
	 * tools. The name should be a form of unique identifier that can be used to
	 * locate the resource object for diagnostics. It should not contain white chars and
	 * special chars if that can be avoided and it should be reasonable short.
                
	 * It is different from a display name attribute. Display name is intended for a 
	 * common user or non-technical administrator (such as role administrator). The
	 * naming attribute is intended for technical IDM administrators and developers.
	 * 
	 * @return resource attribute definition that should be used as a "technical" name
	 * 					for the account.
	 */
	public ResourceAttributeDefinition<?> getNamingAttribute() {
		return namingAttribute;
	}

	public void setNamingAttribute(ResourceAttributeDefinition<?> namingAttribute) {
		this.namingAttribute = namingAttribute;
	}
	
	public void setNamingAttribute(QName namingAttribute) {
		setNamingAttribute(findAttributeDefinition(namingAttribute));
	}
	
	/**
	 * Returns the native object class string for the resource object.
	 * 
	 * Native object class is the name of the Resource Object Definition (Object
	 * Class) as it is seen by the resource itself. The name of the Resource
	 * Object Definition may be constrained by XSD or other syntax and therefore
	 * may be "mangled" to conform to such syntax. The <i>native object
	 * class</i> value will contain unmangled name (if available).
	 * 
	 * Returns null if there is no native object class.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 * 
	 * @return native object class
	 * @throws IllegalStateException
	 *             if there is more than one description attribute.
	 */
	public String getNativeObjectClass() {
		return nativeObjectClass;
	}

	public void setNativeObjectClass(String nativeObjectClass) {
		this.nativeObjectClass = nativeObjectClass;
	}
	
	public boolean isAuxiliary() {
		return auxiliary;
	}

	public void setAuxiliary(boolean auxiliary) {
		this.auxiliary = auxiliary;
	}

	public ShadowKindType getKind() {
		return kind;
	}

	public void setKind(ShadowKindType kind) {
		this.kind = kind;
	}

	/**
	 * Indicates whether definition is should be used as default definition in ist kind.
	 * E.g. if used in an "account" kind it indicates default account definition.
	 * 
	 * If true value is returned then the definition should be used as a default
	 * definition for the kind. This is a way how a resource connector may
	 * suggest applicable object classes (resource object definitions) for
	 * individual shadow kinds (e.g. accounts).
	 * 
	 * @return true if the definition should be used as account type.
	 * @throws IllegalStateException
	 *             if more than one default account is suggested in the schema.
	 */
	public boolean isDefaultInAKind() {
		return defaultInAKind;
	}
	
	public void setDefaultInAKind(boolean defaultAccountType) {
		this.defaultInAKind = defaultAccountType;
	}
	
	public String getIntent() {
		return intent;
	}
	
	public void setIntent(String intent) {
		this.intent = intent;
	}
	
	/**
	 * Returns the definition of display name attribute.
	 * 
	 * Display name attribute specifies which resource attribute should be used
	 * as title when displaying objects of a specific resource object class. It
	 * must point to an attribute of String type. If not present, primary
	 * identifier should be used instead (but this method does not handle this
	 * default behavior).
	 * 
	 * Returns null if there is no display name attribute.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 * 
	 * @return native object class
	 * @throws IllegalStateException
	 *             if there is more than one display name attribute or the
	 *             definition of the referenced attribute does not exist.
	 */
	public ResourceAttributeDefinition<?> getDisplayNameAttribute() {
		return displayNameAttribute;
	}

	public void setDisplayNameAttribute(ResourceAttributeDefinition<?> displayName) {
		this.displayNameAttribute = displayName;
	}
	
	/**
	 * TODO
	 * 
	 * Convenience method. It will internally look up the correct definition.
	 * 
	 * @param displayName
	 */
	public void setDisplayNameAttribute(QName displayName) {
		setDisplayNameAttribute(findAttributeDefinition(displayName));
	}
	
	/**
     * Finds a attribute definition by looking at the property name.
     * <p/>
     * Returns null if nothing is found.
     *
     * @param name property definition name
     * @return found property definition or null
     */
    public <X> ResourceAttributeDefinition<X> findAttributeDefinition(QName name) {
        return findItemDefinition(name, ResourceAttributeDefinition.class);
    }
    
    public <X> ResourceAttributeDefinition<X> findAttributeDefinition(QName name, boolean caseInsensitive) {
        return findItemDefinition(name, ResourceAttributeDefinition.class, caseInsensitive);
    }
    
    public <X> ResourceAttributeDefinition<X> findAttributeDefinition(String name) {
    	QName qname = new QName(getTypeName().getNamespaceURI(), name);
        return findAttributeDefinition(qname);
    }
    
	public <X> ResourceAttributeDefinition<X> createAttributeDefinition(QName name, QName typeName) {
		ResourceAttributeDefinition propDef = new ResourceAttributeDefinition(name, typeName, prismContext);
		addDefinition(propDef);
		return propDef;
	}
	
	public <X> ResourceAttributeDefinition<X> createAttributeDefinition(String localName, QName typeName) {
		QName name = new QName(getSchemaNamespace(),localName);
		return createAttributeDefinition(name,typeName);
	}

	
	public <X> ResourceAttributeDefinition<X> createAttributeDefinition(String localName, String localTypeName) {
		QName name = new QName(getSchemaNamespace(),localName);
		QName typeName = new QName(getSchemaNamespace(),localTypeName);
		return createAttributeDefinition(name,typeName);
	}
	
	public ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition() {
		return toResourceAttributeContainerDefinition(ShadowType.F_ATTRIBUTES);
	}
	
	public ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition(QName elementName) {
		return new ResourceAttributeContainerDefinition(elementName, this, getPrismContext());
	}
	
	/**
	 * This may not be really "clean" as it actually does two steps instead of one. But it is useful.
	 */
	public ResourceAttributeContainer instantiate(QName elementName) {
		ResourceAttributeContainerDefinition racDef = toResourceAttributeContainerDefinition(elementName);
		ResourceAttributeContainer rac = new ResourceAttributeContainer(elementName, racDef, getPrismContext());
		return rac;
	}
	
	public ObjectClassComplexTypeDefinition clone() {
		ObjectClassComplexTypeDefinition clone = new ObjectClassComplexTypeDefinition( 
				getTypeName(), prismContext);
		copyDefinitionData(clone);
		return clone;
	}
	
	protected void copyDefinitionData(ObjectClassComplexTypeDefinition clone) {
		super.copyDefinitionData(clone);
		clone.kind = this.kind;
		clone.intent = this.intent;
		clone.defaultInAKind = this.defaultInAKind;
		clone.descriptionAttribute = this.descriptionAttribute;
		clone.displayNameAttribute = this.displayNameAttribute;
		clone.identifiers = this.identifiers;
		clone.namingAttribute = this.namingAttribute;
		clone.nativeObjectClass = this.nativeObjectClass;
		clone.secondaryIdentifiers = this.secondaryIdentifiers;
		clone.auxiliary = this.auxiliary;
	}

	@Override
	protected String getDebugDumpClassName() {
		return "OCD";
	}

	@Override
	protected void extendDumpHeader(StringBuilder sb) {
		super.extendDumpHeader(sb);
		if (defaultInAKind) {
			sb.append(" def");
		}
		if (auxiliary) {
			sb.append(" aux");
		}
		if (kind != null) {
			sb.append(" ").append(kind.value());
		}
		if (intent != null) {
			sb.append(" intent=").append(intent);
		}
	}

	@Override
	protected void extendDumpDefinition(StringBuilder sb, ItemDefinition def) {
		super.extendDumpDefinition(sb, def);
		if (getIdentifiers() != null && getIdentifiers().contains(def)) {
			sb.append(",primID");
		}
		if (getSecondaryIdentifiers() != null && getSecondaryIdentifiers().contains(def)) {
			sb.append(",secID");
		}
	}
	

}
