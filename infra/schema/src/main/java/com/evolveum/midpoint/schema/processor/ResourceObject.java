/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;

/**
 * Resource Object.
 * 
 * Resource Object understands resource-specific annotations, such as
 * identifiers, native object class, etc.
 * 
 * Object class can be determined by using the definition (inherited from
 * PropertyContainer)
 * 
 * @author Radovan Semancik
 * 
 */
public final class ResourceObject extends PropertyContainer {
	
	protected ActivationType activation;
	protected CredentialsType credentials;

//	/**
//	 * Default constructor.
//	 * The constructors should be used only occasionally (if used at all).
//	 * Use the factory methods in the ResourceObjectDefintion instead.
//	 */
//	public ResourceObject() {
//		activation = null;
//		credentials = null;
//	}
//
//	/**
// 	 * The constructors should be used only occasionally (if used at all).
//	 * Use the factory methods in the ResourceObjectDefintion instead.
//	 * @param name resource object name (element name)
//	 */
//	public ResourceObject(QName name) {
//		super(name);
//		activation = null;
//		credentials = null;
//	}
//
//	/**
//	 * The constructors should be used only occasionally (if used at all).
//	 * Use the factory methods in the ResourceObjectDefintion instead.
//	 * @param name resource object name (element name)
//	 * @param definition resource object definition (schema)
//	 */
//	public ResourceObject(QName name, ResourceObjectDefinition definition) {
//		super(name, definition);
//		activation = null;
//		credentials = null;
//	}

	/**
	 * The constructors should be used only occasionally (if used at all).
	 * Use the factory methods in the ResourceObjectDefintion instead.
	 */
	public ResourceObject(QName name, ResourceObjectDefinition definition, Object element, PropertyPath parentPath) {
		// Resource object has no parent
		super(name, definition, element, parentPath);
		activation = null;
		credentials = null;
	}

	@Override
	public ResourceObjectDefinition getDefinition() {
		return (ResourceObjectDefinition) super.getDefinition();
	}

	/**
	 * Returns set of resource object attributes.
	 * 
	 * The order of attributes is insignificant.
	 * 
	 * The returned set is imutable! Any change to it will be ignored.
	 * 
	 * @return set of resource object attributes.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Set<ResourceObjectAttribute> getAttributes() {
		// TODO: Iterate over the list to assert correct types
		return (Set) getProperties();
	}

	/**
	 * Returns a (single) identifier.
	 * 
	 * This method returns a property that acts as an (primary) identifier for
	 * the resource object. Primary identifiers are used to access the resource
	 * objects, retrieve them from resource, identify objects for modifications,
	 * etc.
	 * 
	 * Returns null if no identifier is defined.
	 * 
	 * Resource objects may have multiple (composite) identifiers, but this
	 * method assumes that there is only a single identifier. The method will
	 * throw exception if that assumption is not satisfied.
	 * 
	 * @return identifier property
	 * @throws IllegalStateException
	 *             if resource object has multiple identifiers
	 */
	public Property getIdentifier() {
		Set<ResourceObjectAttribute> attrDefs = getIdentifiers();
		if (attrDefs.size() > 1){
			throw new IllegalStateException("Resource object has more than one identifier.");
		}
		
		for (Property p : attrDefs){
			return p;
		}
		
		return null;
	}

	/**
	 * Returns identifiers.
	 * 
	 * This method returns properties that act as (primary) identifiers for the
	 * resource object. Primary identifiers are used to access the resource
	 * objects, retrieve them from resource, identify objects for modifications,
	 * etc.
	 * 
	 * Returns empty set if no identifier is defined. Must not return null.
	 * 
	 * Resource objects may have multiple (composite) identifiers, all of them
	 * are returned.
	 * 
	 * The returned set it immutable! Any modifications will be lost.
	 * 
	 * @return set of identifier properties
	 */
	public Set<ResourceObjectAttribute> getIdentifiers() {
		Set<ResourceObjectAttribute> identifiers = new HashSet<ResourceObjectAttribute>();
		Collection<ResourceObjectAttributeDefinition> attrDefs = getDefinition().getIdentifiers();
		for (ResourceObjectAttributeDefinition attrDef : attrDefs) {		
			for (ResourceObjectAttribute property : getAttributes()){
				if (attrDef.getName().equals(property.getName())){
					property.setDefinition(attrDef);
					identifiers.add(property);
				}
			}
		}
		return identifiers;
	}

	/**
	 * Returns a (single) secondary identifier.
	 * 
	 * This method returns a property that acts as an secondary identifier for
	 * the resource object. Secondary identifiers are used to confirm primary
	 * identification of resource object.
	 * 
	 * Returns null if no secondary identifier is defined.
	 * 
	 * Resource objects may have multiple (composite) identifiers, but this
	 * method assumes that there is only a single identifier. The method will
	 * throw exception if that assumption is not satisfied.
	 * 
	 * @return secondary identifier property
	 * @throws IllegalStateException
	 *             if resource object has multiple secondary identifiers
	 */
	public Property getSecondaryIdentifier() {
		throw new IllegalStateException("not implemented yet.");
		// TODO assert single value
	}

	/**
	 * Returns secondary identifiers.
	 * 
	 * This method returns properties that act as secondary identifiers for the
	 * resource object. Secondary identifiers are used to confirm primary
	 * identification of resource object.
	 * 
	 * Returns empty set if no identifier is defined. Must not return null.
	 * 
	 * Resource objects may have multiple (composite) identifiers, all of them
	 * are returned.
	 * 
	 * @return set of secondary identifier properties
	 */
	public Set<Property> getSecondaryIdentifiers() {
		throw new IllegalStateException("not implemented yet.");
	}

	/**
	 * Returns description attribute of a resource object.
	 * 
	 * Returns null if there is no description attribute or the attribute is not
	 * known.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 * 
	 * @return description attribute of a resource object.
	 * @throws IllegalStateException
	 *             if there is no definition for the referenced attributed
	 */
	public ResourceObjectAttribute getDescriptionAttribute() {
		if (getDefinition() == null) {
			return null;
		}
		return findAttribute(getDefinition().getDisplayNameAttribute());
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
	 * @return attribute that should be used as a "technical" name
	 * 				for the account.
	 */
	public ResourceObjectAttribute getNamingAttribute() {
		if (getDefinition() == null) {
			return null;
		}
		if (getDefinition().getNamingAttribute()==null) {
			return null;
		}
		return findAttribute(getDefinition().getNamingAttribute());
	}

	/**
	 * Returns display name attribute of a resource object.
	 * 
	 * Returns null if there is no display name attribute or the attribute is
	 * not known.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 * 
	 * @return display name attribute of a resource object.
	 * @throws IllegalStateException
	 *             if there is no definition for the referenced attributed
	 */
	public ResourceObjectAttribute getDisplayNameAttribute() {
		if (getDefinition() == null) {
			return null;
		}
		return findAttribute(getDefinition().getDisplayNameAttribute());
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
	 * Returns null if there is no native object class or the native object
	 * class is not known.
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
		return getDefinition() == null ? null : getDefinition().getNativeObjectClass();
	}

	/**
	 * Indicates whether definition is should be used as account type.
	 * 
	 * If true value is returned then the definition should be used as an
	 * account type definition. This is a way how a resource connector may
	 * suggest applicable object classes (resource object definitions) for
	 * accounts.
	 * 
	 * If no information about account type is present, false should be
	 * returned.
	 * 
	 * @return true if the definition should be used as account type.
	 */
	public boolean isAccountType() {
		ResourceObjectDefinition definition = getDefinition();
		return (definition != null ? definition.isAccountType() : null);
	}

	/**
	 * Indicates whether definition is should be used as default account type.
	 * 
	 * If true value is returned then the definition should be used as a default
	 * account type definition. This is a way how a resource connector may
	 * suggest applicable object classes (resource object definitions) for
	 * accounts.
	 * 
	 * If no information about account type is present, false should be
	 * returned. This method must return true only if isAccountType() returns
	 * true.
	 * 
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of at-most-one value should be done at the time of
	 * schema parsing. The exception may not even be thrown at all if the
	 * implementation is not able to determine duplicity.
	 * 
	 * @return true if the definition should be used as account type.
	 * @throws IllegalStateException
	 *             if more than one default account is suggested in the schema.
	 */
	public boolean isDefaultAccountType() {
		ResourceObjectDefinition definition = getDefinition();
		return (definition != null ? definition.isDefaultAccountType() : null);
	}

	/**
	 * Finds a specific attribute in the resource object by name.
	 * 
	 * Returns null if nothing is found.
	 * 
	 * @param attributeQName
	 *            attribute name to find.
	 * @return found attribute or null
	 */
	public ResourceObjectAttribute findAttribute(QName attributeQName) {
		return (ResourceObjectAttribute) super.findProperty(attributeQName);
	}

	/**
	 * Finds a specific attribute in the resource object by definition.
	 * 
	 * Returns null if nothing is found.
	 * 
	 * @param attributeDefinition
	 *            attribute definition to find.
	 * @return found attribute or null
	 */
	private ResourceObjectAttribute findAttribute(ResourceObjectAttributeDefinition attributeDefinition) {
		return (ResourceObjectAttribute) super.findProperty(attributeDefinition);
	}
	
	public ActivationType getActivation() {
		return activation;
	}
	
	public ActivationType createOrGetActivation() {
		if (activation == null) {
			activation = new ActivationType();
		}
		return activation;
	}

	public void setActivation(ActivationType activation) {
		this.activation = activation;
	}

	public CredentialsType getCredentials() {
		return credentials;
	}
	
	public CredentialsType createOrGetCredentials() {
		if (credentials == null) {
			credentials = new CredentialsType();
		}
		return credentials;
	}

	public void setCredentials(CredentialsType credentials) {
		this.credentials = credentials;
	}

	/**
	 * Return a human readable name of this class suitable for logs.
	 */
	protected String getDebugDumpClassName() {
		return "ReO";
	}

}
