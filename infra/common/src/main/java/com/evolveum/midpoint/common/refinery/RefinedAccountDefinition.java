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
package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author semancik
 */
public class RefinedAccountDefinition extends ResourceAttributeContainerDefinition implements Dumpable, DebugDumpable {

    private String accountType;
    private String displayName;
    private String description;
    private boolean isDefault;
    private ObjectClassComplexTypeDefinition objectClassDefinition;
    private ResourceType resourceType;
    private Collection<? extends RefinedAttributeDefinition> identifiers;
	private Collection<? extends RefinedAttributeDefinition> secondaryIdentifiers;
	private Collection<ResourceObjectPattern> protectedAccounts;
	
    /**
     * Refined object definition. The "any" parts are replaced with appropriate schema (e.g. resource schema)
     */
    PrismObjectDefinition<AccountShadowType> objectDefinition = null;

    private List<RefinedAttributeDefinition> attributeDefinitions;
    
    protected RefinedAccountDefinition(PrismContext prismContext) {
    	super(SchemaConstants.I_ATTRIBUTES, null, prismContext);
    }

    private RefinedAccountDefinition(PrismContext prismContext, ResourceType resourceType) {
        super(SchemaConstants.I_ATTRIBUTES, null, prismContext);
        attributeDefinitions = new ArrayList<RefinedAttributeDefinition>();
        this.resourceType = resourceType;
    }

    private RefinedAccountDefinition(PrismContext prismContext, ResourceType resourceType,
    		ObjectClassComplexTypeDefinition objectClassDefinition) {
        super(SchemaConstants.I_ATTRIBUTES, null, prismContext);
        attributeDefinitions = new ArrayList<RefinedAttributeDefinition>();
        this.resourceType = resourceType;
        this.objectClassDefinition = objectClassDefinition;
    }

    @Override
    public ResourceAttributeDefinition getDescriptionAttribute() {
        return objectClassDefinition.getDescriptionAttribute();
    }

    @Override
    public void setDescriptionAttribute(ResourceAttributeDefinition descriptionAttribute) {
        throw new UnsupportedOperationException("Parts of refined account are immutable");
    }

    @Override
    public RefinedAttributeDefinition getNamingAttribute() {
        return substituteRefinedAttributeDefinition(objectClassDefinition.getNamingAttribute());
    }

	@Override
    public String getNativeObjectClass() {
        return objectClassDefinition.getNativeObjectClass();
    }

    @Override
    public void setAccountType(boolean accountType) {
        throw new UnsupportedOperationException("Parts of refined account are immutable");
    }

    @Override
    public boolean isDefaultAccountType() {
        return isDefault;
    }

    @Override
    public void setDefaultAccountType(boolean defaultAccountType) {
        this.isDefault = defaultAccountType;
    }

    @Override
    public String getAccountTypeName() {
        return accountType;
    }

    @Override
    public void setAccountTypeName(String accountTypeName) {
        this.accountType = accountTypeName;
    }

    @Override
    public RefinedAttributeDefinition getDisplayNameAttribute() {
        return substituteRefinedAttributeDefinition(objectClassDefinition.getDisplayNameAttribute());
    }

    @Override
    public void setDisplayNameAttribute(QName displayName) {
        throw new UnsupportedOperationException("Parts of refined account are immutable");
    }
    
    @Override
	public Collection<? extends RefinedAttributeDefinition> getIdentifiers() {
		if (identifiers == null) {
			identifiers = createIdentifiersCollection();
		}
		return identifiers;
	}

	@Override
	public Collection<? extends RefinedAttributeDefinition> getSecondaryIdentifiers() {
		if (secondaryIdentifiers == null) {
			secondaryIdentifiers = createIdentifiersCollection();
		}
		return secondaryIdentifiers;
	}
	
	private Collection<? extends RefinedAttributeDefinition> createIdentifiersCollection() {
		return new ArrayList<RefinedAttributeDefinition>();
	}
	
	public Collection<ResourceObjectPattern> getProtectedAccounts() {
		if (protectedAccounts == null) {
			protectedAccounts = new ArrayList<ResourceObjectPattern>();
		}
		return protectedAccounts;
	}
	
	public PrismContext getPrismContext() {
		return resourceType.asPrismObject().getPrismContext();
	}

	@Override
	public ObjectClassComplexTypeDefinition getComplexTypeDefinition() {
		return objectClassDefinition;
	}

	@Override
    public ResourceAttributeContainer instantiate() {
        return new ResourceAttributeContainer(getNameOrDefaultName(), this, getPrismContext());
    }

    @Override
    public ResourceAttributeContainer instantiate(QName name) {
        return new ResourceAttributeContainer(name, this, getPrismContext());
    }

    @Override
    public RefinedAccountDefinition clone() {
        RefinedAccountDefinition clone = new RefinedAccountDefinition(getPrismContext(), resourceType, objectClassDefinition);
        copyDefinitionData(clone);
        return clone;
    }

    protected void copyDefinitionData(RefinedAccountDefinition clone) {
        super.copyDefinitionData(clone);
        clone.accountType = this.accountType;
        clone.attributeDefinitions = this.attributeDefinitions;
        clone.description = this.description;
        clone.displayName = this.displayName;
        clone.isDefault = this.isDefault;
        clone.objectClassDefinition = this.objectClassDefinition;
        clone.objectDefinition = this.objectDefinition;
        clone.resourceType = this.resourceType;
    }

//    @Override
//    public Set<ResourceObjectAttribute> parseAttributes(List<Object> elements, PropertyPath parentPath) throws SchemaException {
//        return objectClassDefinition.parseAttributes(elements, parentPath);
//    }
//
//    @Override
//    public Collection<? extends ResourceObjectAttribute> parseIdentifiers(List<Object> elements, PropertyPath parentPath)
//            throws SchemaException {
//        return objectClassDefinition.parseIdentifiers(elements, parentPath);
//    }

    @Override
    public RefinedAttributeDefinition findAttributeDefinition(QName elementQName) {
        return findItemDefinition(elementQName, RefinedAttributeDefinition.class);
    }

    @Override
    public RefinedAttributeDefinition findAttributeDefinition(String elementLocalname) {
        QName elementQName = new QName(getNamespace(), elementLocalname);
        return findAttributeDefinition(elementQName);
    }

//    @Override
//    public ResourceAttributeDefinition createAttributeDefinition(QName name, QName typeName) {
//        throw new UnsupportedOperationException("Parts of refined account are immutable");
//    }

//    @Override
//    public ResourceAttributeDefinition createAttributeDefinition(String localName, QName typeName) {
//        throw new UnsupportedOperationException("Parts of refined account are immutable");
//    }
//
//    @Override
//    public ResourceAttributeDefinition createAttributeDefinition(String localName, String localTypeName) {
//        throw new UnsupportedOperationException("Parts of refined account are immutable");
//    }


    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean isRuntimeSchema() {
        return true;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
	public String getNamespace() {
    	return ResourceTypeUtil.getResourceNamespace(resourceType);
	}

	public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
        return objectClassDefinition;
    }

    public void setObjectClassDefinition(ObjectClassComplexTypeDefinition objectClassDefinition) {
        this.objectClassDefinition = objectClassDefinition;
    }
    
    @Override
    public boolean isAccountType() {
        return true;
    }

    @Override
    public Collection<? extends RefinedAttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }
    
    @Override
	public List<PrismPropertyDefinition> getPropertyDefinitions() {
    	return (List)attributeDefinitions;
	}

	@Override
    public List<ItemDefinition> getDefinitions() {
        return (List) attributeDefinitions;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public PrismObjectDefinition<AccountShadowType> getObjectDefinition() {
        if (objectDefinition == null) {
            constructObjectDefinition();
        }
        return objectDefinition;
    }

    private void constructObjectDefinition() {
        // Almost-shallow clone of object definition and complex type
        PrismObjectDefinition<AccountShadowType> originalObjectDefinition = 
        	getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AccountShadowType.class);
        PrismObjectDefinition<AccountShadowType> refinedObjectDef = 
        	originalObjectDefinition.cloneWithReplacedDefinition(AccountShadowType.F_ATTRIBUTES, this); 
        	
        this.objectDefinition = refinedObjectDef;
    }

    public RefinedAttributeDefinition getAttributeDefinition(QName attributeName) {
        for (RefinedAttributeDefinition attrDef : attributeDefinitions) {
            if (attrDef.getName().equals(attributeName)) {
                return attrDef;
            }
        }
        return null;
    }


    public void add(RefinedAttributeDefinition refinedAttributeDefinition) {
        attributeDefinitions.add(refinedAttributeDefinition);
    }

    public boolean containsAttributeDefinition(QName attributeName) {
        for (RefinedAttributeDefinition rAttributeDef : attributeDefinitions) {
            if (rAttributeDef.getName().equals(attributeName)) {
                return true;
            }
        }
        return false;
    }

    static RefinedAccountDefinition parse(ResourceAccountTypeDefinitionType accountTypeDefType,
            ResourceType resourceType, RefinedResourceSchema rSchema,
            PrismContext prismContext, String contextDescription) throws
            SchemaException {

        RefinedAccountDefinition rAccountDef = new RefinedAccountDefinition(prismContext, resourceType);

        if (accountTypeDefType.getName() != null) {
            rAccountDef.setAccountTypeName(accountTypeDefType.getName());
        } else {
            throw new SchemaException("Account type definition does not have a name, in " + contextDescription);
        }

        ObjectClassComplexTypeDefinition objectClassDef = null;
        if (accountTypeDefType.getObjectClass() != null) {
            QName objectClass = accountTypeDefType.getObjectClass();
            objectClassDef = rSchema.getOriginalResourceSchema().findObjectClassDefinition(objectClass);
            if (objectClassDef == null) {
                throw new SchemaException("Object class " + objectClass + " as specified in account type " + accountTypeDefType.getName() + " was not found in the resource schema of " + contextDescription);
            }
            rAccountDef.setObjectClassDefinition(objectClassDef);
        } else {
            throw new SchemaException("Definition of account type " + accountTypeDefType.getName() + " does not have objectclass, in " + contextDescription);
        }

        if (accountTypeDefType.getDisplayName() != null) {
            rAccountDef.setDisplayName(accountTypeDefType.getDisplayName());
        } else {
            if (objectClassDef.getDisplayName() != null) {
                rAccountDef.setDisplayName(objectClassDef.getDisplayName());
            }
        }

        if (accountTypeDefType.getDescription() != null) {
            rAccountDef.setDescription(accountTypeDefType.getDescription());
        }

        if (accountTypeDefType.isDefault() != null) {
            rAccountDef.setDefault(accountTypeDefType.isDefault());
        } else {
            rAccountDef.setDefault(objectClassDef.isDefaultAccountType());
        }

        for (ResourceAttributeDefinition road : objectClassDef.getAttributeDefinitions()) {
            String attrContextDescription = road.getName() + ", in " + contextDescription;
            ResourceAttributeDefinitionType attrDefType = findAttributeDefinitionType(road.getName(), accountTypeDefType,
            		attrContextDescription);
            // We MUST NOT skip ignored attribute definitions here. We must include them in the schema as
            // the shadows will still have that attributes and we will need their type definition to work
            // well with them. They may also be mandatory. We cannot pretend that they do not exist.

            RefinedAttributeDefinition rAttrDef = RefinedAttributeDefinition.parse(road, attrDefType, objectClassDef, 
            		prismContext, "in account type " + accountTypeDefType.getName() + ", in " + contextDescription);
            rAccountDef.processIdentifiers(rAttrDef, objectClassDef);

            if (rAccountDef.containsAttributeDefinition(rAttrDef.getName())) {
                throw new SchemaException("Duplicate definition of attribute " + rAttrDef.getName() + " in account type " +
                		accountTypeDefType.getName() + ", in " + contextDescription);
            }
            rAccountDef.add(rAttrDef);

        }

        // Check for extra attribute definitions in the account type
        for (ResourceAttributeDefinitionType attrDefType : accountTypeDefType.getAttribute()) {
            if (!rAccountDef.containsAttributeDefinition(attrDefType.getRef()) && !RefinedAttributeDefinition.isIgnored(attrDefType)) {
                throw new SchemaException("Definition of attribute " + attrDefType.getRef() + " not found in object class " + objectClassDef.getTypeName() + " as defined in " + contextDescription);
            }
        }
        
        parseProtectedAccounts(rAccountDef, accountTypeDefType);
   
        return rAccountDef;
    }

	private static void parseProtectedAccounts(RefinedAccountDefinition rAccountDef, ResourceAccountTypeDefinitionType accountTypeDefType) throws SchemaException {
		for (ResourceObjectPatternType protectedType: accountTypeDefType.getProtected()) {
			ResourceObjectPattern protectedPattern = convertToPatten(protectedType, rAccountDef);
			rAccountDef.getProtectedAccounts().add(protectedPattern);
		}
	}
	
	private static ResourceObjectPattern convertToPatten(ResourceObjectPatternType protectedType, RefinedAccountDefinition rAccountDef) throws SchemaException {
		ResourceObjectPattern resourceObjectPattern = new ResourceObjectPattern();
		Collection<? extends Item<?>> items = rAccountDef.getPrismContext().getPrismDomProcessor().parseContainerItems(rAccountDef, protectedType.getAny());
		for(Item<?> item: items) {
			if (item instanceof ResourceAttribute<?>) {
				resourceObjectPattern.addIdentifier((ResourceAttribute<?>)item);
			} else {
				throw new SchemaException("Unexpected item in pattern for "+rAccountDef+": "+item);
			}
		}
		return resourceObjectPattern;
	}

	static RefinedAccountDefinition parse(ObjectClassComplexTypeDefinition objectClassDef, ResourceType resourceType,
            RefinedResourceSchema rSchema,
            PrismContext prismContext, String contextDescription) throws SchemaException {

        RefinedAccountDefinition rAccountDef = new RefinedAccountDefinition(prismContext, resourceType, objectClassDef);

        String accountTypeName = null;
        if (objectClassDef.getIntent() != null) {
            accountTypeName = objectClassDef.getIntent();
            if (accountTypeName == null) {
            	accountTypeName = SchemaConstants.INTENT_DEFAULT;
            }
            rAccountDef.setAccountTypeName(accountTypeName);
        } else {
            if (objectClassDef.isDefaultAccountType()) {
                rAccountDef.setAccountTypeName(MidPointConstants.DEFAULT_ACCOUNT_TYPE_NAME);
            } else {
                throw new SchemaException("Account type definition does not have a name, in " + contextDescription);
            }
        }


        if (objectClassDef.getDisplayName() != null) {
            rAccountDef.setDisplayName(objectClassDef.getDisplayName());
        }

        rAccountDef.setDefault(objectClassDef.isDefaultAccountType());

        for (ResourceAttributeDefinition attrDef : objectClassDef.getAttributeDefinitions()) {
            String attrContextDescription = accountTypeName + ", in " + contextDescription;

            RefinedAttributeDefinition rAttrDef = RefinedAttributeDefinition.parse(attrDef, null, objectClassDef, prismContext, 
            		attrContextDescription);
            rAccountDef.processIdentifiers(rAttrDef, objectClassDef);

            if (rAccountDef.containsAttributeDefinition(rAttrDef.getName())) {
                throw new SchemaException("Duplicate definition of attribute " + rAttrDef.getName() + " in " + attrContextDescription);
            }
            rAccountDef.add(rAttrDef);

        }

        return rAccountDef;


    }

	private void processIdentifiers(RefinedAttributeDefinition rAttrDef, ObjectClassComplexTypeDefinition objectClassDef) {
		QName attrName = rAttrDef.getName();
		if (objectClassDef.isIdentifier(attrName)) {
			((Collection)getIdentifiers()).add(rAttrDef);
		}
		if (objectClassDef.isSecondaryIdentifier(attrName)) {
			((Collection)getSecondaryIdentifiers()).add(rAttrDef);
		}		
	}
	
	private RefinedAttributeDefinition substituteRefinedAttributeDefinition(ResourceAttributeDefinition attributeDef) {
		RefinedAttributeDefinition rAttrDef = findAttributeDefinition(attributeDef.getName());
		return rAttrDef;
	}

	private static ResourceAttributeDefinitionType findAttributeDefinitionType(QName attrName,
            ResourceAccountTypeDefinitionType accountTypeDefType, String contextDescription) throws SchemaException {
        ResourceAttributeDefinitionType foundAttrDefType = null;
        for (ResourceAttributeDefinitionType attrDefType : accountTypeDefType.getAttribute()) {
            if (attrDefType.getRef() != null) {
                if (attrDefType.getRef().equals(attrName)) {
                    if (foundAttrDefType == null) {
                        foundAttrDefType = attrDefType;
                    } else {
                        throw new SchemaException("Duplicate definition of attribute " + attrDefType.getRef() + " in account type "
                                + accountTypeDefType.getName() + ", in " + contextDescription);
                    }
                }
            } else {
                throw new SchemaException("Missing reference to the attribute schema definition in definition " + SchemaDebugUtil.prettyPrint(attrDefType) + " during processing of " + contextDescription);
            }
        }
        return foundAttrDefType;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(getDebugDumpClassName()).append("(");
        sb.append(SchemaDebugUtil.prettyPrint(getName()));
        if (isDefault()) {
            sb.append(",default");
        }
        sb.append(",");
        sb.append(SchemaDebugUtil.prettyPrint(getObjectClassDefinition().getTypeName()));
        sb.append(")\n");
        Iterator<ItemDefinition> i = getDefinitions().iterator();
        while (i.hasNext()) {
            ItemDefinition def = i.next();
            sb.append(def.debugDump(indent + 1));
            if (i.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
    
    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "RAccDef";
    }

    @Override
    public String dump() {
        return debugDump(0);
    }

    public PrismObject<AccountShadowType> createBlankShadow() {
    	PrismObject<AccountShadowType> accountShadow;
		try {
			accountShadow = prismContext.getSchemaRegistry().instantiate(AccountShadowType.class);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal error instantiating account shadow: "+e.getMessage(), e);
		}
    	AccountShadowType accountShadowType = accountShadow.asObjectable();
        
    	accountShadowType.setAccountType(getAccountTypeName());
        accountShadowType.setObjectClass(objectClassDefinition.getTypeName());
        accountShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(resourceType));
        
        // Setup definition
        PrismObjectDefinition<AccountShadowType> newDefinition = accountShadow.getDefinition().cloneWithReplacedDefinition(AccountShadowType.F_ATTRIBUTES, this);
        accountShadow.setDefinition(newDefinition);
        
        return accountShadow;
    }

    public ResourceShadowDiscriminator getResourceAccountType() {
        return new ResourceShadowDiscriminator(resourceType.getOid(), getAccountTypeName());
    }

    public Collection<? extends QName> getNamesOfAttributesWithOutboundExpressions() {
        Collection<QName> attrNames = new HashSet<QName>();
        for (RefinedAttributeDefinition attrDef : getAttributeDefinitions()) {
            if (attrDef.getOutboundMappingType() != null) {
                attrNames.add(attrDef.getName());
            }
        }
        return attrNames;
    }

    public Collection<? extends QName> getNamesOfAttributesWithInboundExpressions() {
        Collection<QName> attrNames = new HashSet<QName>();
        for (RefinedAttributeDefinition attrDef : getAttributeDefinitions()) {
            List<MappingType> inbounds = attrDef.getInboundMappingTypes();
            if (inbounds != null && !inbounds.isEmpty()) {
                attrNames.add(attrDef.getName());
            }
        }

        return attrNames;
    }

    private ResourceAccountTypeDefinitionType getAccountSchemaHandlingDefinition() {
        if (resourceType.getSchemaHandling() == null) {
            return null;
        }
        List<ResourceAccountTypeDefinitionType> types = resourceType.getSchemaHandling().getAccountType();
        ResourceAccountTypeDefinitionType definition = null;
        for (ResourceAccountTypeDefinitionType account : types) {
            if (accountType == null && account.isDefault()) {
                definition = account;
                break;
            }

            if (accountType.equals(account.getName())) {
                definition = account;
                break;
            }
        }

        return definition;
    }

    public MappingType getCredentialsInbound() {
        
    	ResourcePasswordDefinitionType password = getPasswordDefinition();
    	
        if (password == null || password.getInbound() == null) {
            return null;
        }

        return password.getInbound();
    }
    
	public MappingType getCredentialsOutbound() {

		ResourcePasswordDefinitionType password = getPasswordDefinition();

		if (password == null || password.getOutbound() == null) {
			return null;
		}

		return password.getOutbound();
	}
    
	
	public ObjectReferenceType getPasswordPolicy(){
		ResourcePasswordDefinitionType password = getPasswordDefinition();
		
		if (password == null || password.getPasswordPolicyRef() == null){
			return null;
		}
		
		return password.getPasswordPolicyRef();
	}
	
    private ResourcePasswordDefinitionType getPasswordDefinition(){
    	ResourceAccountTypeDefinitionType definition = getAccountSchemaHandlingDefinition();
        if (definition == null) {
            return null;
        }
        ResourceCredentialsDefinitionType credentials = definition.getCredentials();
        if (credentials == null) {
            return null;
        }
        
        return credentials.getPassword();
    }

    public MappingType getActivationInbound() {
        
        ResourceActivationEnableDefinitionType enabled = getActivationEnableDefinition();
        if (enabled == null || enabled.getInbound() == null) {
            return null;
        }

        return (enabled.getInbound());
    }
 
	public MappingType getActivationOutbound() {

		ResourceActivationEnableDefinitionType enabled = getActivationEnableDefinition();
		if (enabled == null || enabled.getOutbound() == null) {
			return null;
		}

		return (enabled.getOutbound());
	}
 
    
    private ResourceActivationEnableDefinitionType getActivationEnableDefinition(){
    	ResourceAccountTypeDefinitionType definition = getAccountSchemaHandlingDefinition();
        if (definition == null) {
            return null;
        }

        ResourceActivationDefinitionType activation = definition.getActivation();
        if (activation == null) {
            return null;
        }
        
        return activation.getEnabled();
    }
}
