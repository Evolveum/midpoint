/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;

/**
 * @author semancik
 *
 */
public class ObjectClassComplexTypeDefinition extends ComplexTypeDefinition {
	
	private Set<ResourceAttributeDefinition> idenitifiers;
	private Set<ResourceAttributeDefinition> secondaryIdenitifiers;
	private ResourceAttributeDefinition descriptionAttribute;
	private ResourceAttributeDefinition displayNameAttribute;
	private ResourceAttributeDefinition namingAttribute;
	private boolean defaultAccountType = false;
	private boolean accountType = false;
	private String accountTypeName;
	private String nativeObjectClass;

	ObjectClassComplexTypeDefinition(QName defaultName, QName typeName, PrismContext prismContext) {
		super(defaultName, typeName, prismContext);
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
	public Collection<ResourceAttributeDefinition> getIdentifiers() {
		if (idenitifiers == null) {
			idenitifiers = new HashSet<ResourceAttributeDefinition>();
		}
		return idenitifiers;
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
	public Set<ResourceAttributeDefinition> getSecondaryIdentifiers() {
		if (secondaryIdenitifiers == null) {
			secondaryIdenitifiers = new HashSet<ResourceAttributeDefinition>();
		}
		return secondaryIdenitifiers;
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
	public ResourceAttributeDefinition getDescriptionAttribute() {
		return descriptionAttribute;
	}

	public void setDescriptionAttribute(ResourceAttributeDefinition descriptionAttribute) {
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
	public ResourceAttributeDefinition getNamingAttribute() {
		return namingAttribute;
	}

	public void setNamingAttribute(ResourceAttributeDefinition namingAttribute) {
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
		return accountType;
	}

	public void setAccountType(boolean accountType) {
		this.accountType = accountType;
		if (!accountType) {
			defaultAccountType = false;
		}
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
		return defaultAccountType;
	}
	
	public void setDefaultAccountType(boolean defaultAccountType) {
		this.defaultAccountType = defaultAccountType;
		if (defaultAccountType && !accountType) {
			throw new IllegalStateException(
					"Can't be default account type, flat account type (boolean) not set.");
		}
	}
	
	public String getAccountTypeName() {
		return accountTypeName;
	}
	
	public void setAccountTypeName(String accountTypeName) {
		this.accountTypeName = accountTypeName;
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
	public ResourceAttributeDefinition getDisplayNameAttribute() {
		return displayNameAttribute;
	}

	public void setDisplayNameAttribute(ResourceAttributeDefinition displayName) {
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
    public ResourceAttributeDefinition findAttributeDefinition(QName name) {
        return findItemDefinition(name, ResourceAttributeDefinition.class);
    }
	
	public ObjectClassComplexTypeDefinition clone() {
		ObjectClassComplexTypeDefinition clone = new ObjectClassComplexTypeDefinition(getDefaultName(), 
				getTypeName(), prismContext);
		copyDefinitionData(clone);
		return clone;
	}
	
	protected void copyDefinitionData(ObjectClassComplexTypeDefinition clone) {
		super.copyDefinitionData(clone);
		clone.accountType = this.accountType;
		clone.accountTypeName = this.accountTypeName;
		clone.defaultAccountType = this.defaultAccountType;
		clone.descriptionAttribute = this.descriptionAttribute;
		clone.displayNameAttribute = this.displayNameAttribute;
		clone.idenitifiers = this.idenitifiers;
		clone.namingAttribute = this.namingAttribute;
		clone.nativeObjectClass = this.nativeObjectClass;
		clone.secondaryIdenitifiers = this.secondaryIdenitifiers;
	}

	@Override
	protected String getDebugDumpClassName() {
		return "OCD";
	}

}
