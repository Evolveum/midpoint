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
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.*;

/**
 * @author semancik
 */
public class RefinedObjectClassDefinition extends ObjectClassComplexTypeDefinition implements Dumpable, DebugDumpable {

    private String intent;
    private String displayName;
    private String description;
    private boolean isDefault;
    private ObjectClassComplexTypeDefinition objectClassDefinition;
    private ResourceType resourceType;
    private Collection<? extends RefinedAttributeDefinition> identifiers;
	private Collection<? extends RefinedAttributeDefinition> secondaryIdentifiers;
	private Collection<ResourceObjectPattern> protectedObjectPatterns;
	private List<RefinedAttributeDefinition> attributeDefinitions;
	private Collection<ResourceEntitlementAssociationType> entitlementAssociations = new ArrayList<ResourceEntitlementAssociationType>();
	
    /**
     * Refined object definition. The "any" parts are replaced with appropriate schema (e.g. resource schema)
     */
    PrismObjectDefinition<ShadowType> objectDefinition = null;
	private ShadowKindType kind = null;
    
    /**
     * This is needed by the LayerRefinedObjectClassDefinition class
     */
    protected RefinedObjectClassDefinition(QName typeName, PrismContext prismContext) {
    	super(SchemaConstants.C_ATTRIBUTES, typeName, prismContext);
    }

    private RefinedObjectClassDefinition(PrismContext prismContext, ResourceType resourceType,
    		ObjectClassComplexTypeDefinition objectClassDefinition) {
        super(SchemaConstants.C_ATTRIBUTES, objectClassDefinition.getTypeName(), prismContext);
        Validate.notNull(objectClassDefinition, "ObjectClass definition must not be null");
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
    public QName getTypeName() {
        return objectClassDefinition.getTypeName();
    }

	@Override
    public String getNativeObjectClass() {
        return objectClassDefinition.getNativeObjectClass();
    }

    @Override
    public boolean isDefaultInAKind() {
        return isDefault;
    }

    @Override
    public void setDefaultInAKind(boolean defaultAccountType) {
        this.isDefault = defaultAccountType;
    }

    @Override
    public String getIntent() {
        return intent;
    }

    @Override
    public void setIntent(String intent) {
        this.intent = intent;
    }
    
    @Override
	public ShadowKindType getKind() {
    	if (kind != null) {
    		return kind;
    	}
		return getObjectClassDefinition().getKind();
	}

	@Override
	public void setKind(ShadowKindType kind) {
		this.kind = kind;
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
	
	public Collection<ResourceEntitlementAssociationType> getEntitlementAssociations() {
		return entitlementAssociations;
	}
	
	public ResourceEntitlementAssociationType findEntitlementAssociation(QName name) {
		for (ResourceEntitlementAssociationType assocType: entitlementAssociations) {
			if (assocType.getName().equals(name)) {
				return assocType;
			}
		}
		return null;
	}
	
	public Collection<ResourceObjectPattern> getProtectedObjectPatterns() {
		if (protectedObjectPatterns == null) {
			protectedObjectPatterns = new ArrayList<ResourceObjectPattern>();
		}
		return protectedObjectPatterns;
	}
	
	public PrismContext getPrismContext() {
		return resourceType.asPrismObject().getPrismContext();
	}

    @Override
    public RefinedObjectClassDefinition clone() {
        RefinedObjectClassDefinition clone = new RefinedObjectClassDefinition(getPrismContext(), resourceType, objectClassDefinition);
        copyDefinitionData(clone);
        return clone;
    }

    protected void copyDefinitionData(RefinedObjectClassDefinition clone) {
        super.copyDefinitionData(clone);
        clone.intent = this.intent;
        clone.attributeDefinitions = this.attributeDefinitions;
        clone.description = this.description;
        clone.displayName = this.displayName;
        clone.isDefault = this.isDefault;
        clone.objectClassDefinition = this.objectClassDefinition;
        clone.objectDefinition = this.objectDefinition;
        clone.resourceType = this.resourceType;
    }

    @Override
    public RefinedAttributeDefinition findAttributeDefinition(QName elementQName) {
        return findItemDefinition(elementQName, RefinedAttributeDefinition.class);
    }

    @Override
    public RefinedAttributeDefinition findAttributeDefinition(String elementLocalname) {
        QName elementQName = new QName(getResourceNamespace(), elementLocalname);
        return findAttributeDefinition(elementQName);
    }

	private String getResourceNamespace() {
		return ResourceTypeUtil.getResourceNamespace(resourceType);
	}

	@Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public Collection<? extends RefinedAttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }
    
	@Override
    public List<? extends ItemDefinition> getDefinitions() {
        return attributeDefinitions;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public PrismObjectDefinition<ShadowType> getObjectDefinition() {
        if (objectDefinition == null) {
            constructObjectDefinition();
        }
        return objectDefinition;
    }

    private void constructObjectDefinition() {
        // Almost-shallow clone of object definition and complex type
        PrismObjectDefinition<ShadowType> originalObjectDefinition = 
        	getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        PrismObjectDefinition<ShadowType> refinedObjectDef = 
        	originalObjectDefinition.cloneWithReplacedDefinition(ShadowType.F_ATTRIBUTES, 
        			this.toResourceAttributeContainerDefinition());
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

    static RefinedObjectClassDefinition parse(ResourceAccountTypeDefinitionType accountTypeDefType,
            ResourceType resourceType, RefinedResourceSchema rSchema,
            PrismContext prismContext, String contextDescription) throws
            SchemaException {

    	RefinedObjectClassDefinition rObjectClassDef = parseRefinedObjectClass(accountTypeDefType, resourceType, rSchema, 
    			prismContext, ShadowKindType.ACCOUNT, "account", contextDescription);
        return rObjectClassDef;
    }

	private static void parseProtected(RefinedObjectClassDefinition rAccountDef, ResourceObjectClassTypeDefinitionType accountTypeDefType) throws SchemaException {
		for (ResourceObjectPatternType protectedType: accountTypeDefType.getProtected()) {
			ResourceObjectPattern protectedPattern = convertToPattern(protectedType, rAccountDef);
			rAccountDef.getProtectedObjectPatterns().add(protectedPattern);
		}
	}
	
	private static ResourceObjectPattern convertToPattern(ResourceObjectPatternType protectedType, RefinedObjectClassDefinition rAccountDef) throws SchemaException {
		ResourceObjectPattern resourceObjectPattern = new ResourceObjectPattern();
		Collection<? extends Item<?>> items = rAccountDef.getPrismContext().getPrismDomProcessor().parseContainerItems(
				rAccountDef.toResourceAttributeContainerDefinition(), protectedType.getAny());
		for(Item<?> item: items) {
			if (item instanceof ResourceAttribute<?>) {
				resourceObjectPattern.addIdentifier((ResourceAttribute<?>)item);
			} else {
				throw new SchemaException("Unexpected item in pattern for "+rAccountDef+": "+item);
			}
		}
		return resourceObjectPattern;
	}

	static RefinedObjectClassDefinition parse(ObjectClassComplexTypeDefinition objectClassDef, ResourceType resourceType,
            RefinedResourceSchema rSchema,
            PrismContext prismContext, String contextDescription) throws SchemaException {

        RefinedObjectClassDefinition rAccountDef = new RefinedObjectClassDefinition(prismContext, resourceType, objectClassDef);

        String accountTypeName = null;
        if (objectClassDef.getIntent() != null) {
            accountTypeName = objectClassDef.getIntent();
            if (accountTypeName == null) {
            	accountTypeName = SchemaConstants.INTENT_DEFAULT;
            }
            rAccountDef.setIntent(accountTypeName);
        } else {
            if (objectClassDef.isDefaultInAKind()) {
                rAccountDef.setIntent(MidPointConstants.DEFAULT_ACCOUNT_TYPE_NAME);
            } else {
                throw new SchemaException("Account type definition does not have a name, in " + contextDescription);
            }
        }


        if (objectClassDef.getDisplayName() != null) {
            rAccountDef.setDisplayName(objectClassDef.getDisplayName());
        }

        rAccountDef.setDefault(objectClassDef.isDefaultInAKind());

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
	
	static RefinedObjectClassDefinition parse(ResourceEntitlementTypeDefinitionType entTypeDefType,
			ResourceType resourceType, RefinedResourceSchema rSchema, PrismContext prismContext,
			String contextDescription) throws SchemaException {
	
		RefinedObjectClassDefinition rObjectClassDef = parseRefinedObjectClass(entTypeDefType, 
				resourceType, rSchema, prismContext, ShadowKindType.ENTITLEMENT, "entitlement", contextDescription);
		return rObjectClassDef;
				
	}
	
	private static RefinedObjectClassDefinition parseRefinedObjectClass(ResourceObjectClassTypeDefinitionType rOcDefType,
			ResourceType resourceType, RefinedResourceSchema rSchema, PrismContext prismContext,
			ShadowKindType kind, String typeDesc, String contextDescription) throws SchemaException {
		
		ObjectClassComplexTypeDefinition objectClassDef = null;
        if (rOcDefType.getObjectClass() != null) {
            QName objectClass = rOcDefType.getObjectClass();
            objectClassDef = rSchema.getOriginalResourceSchema().findObjectClassDefinition(objectClass);
            if (objectClassDef == null) {
                throw new SchemaException("Object class " + objectClass + " as specified in "+typeDesc+" type " + rOcDefType.getName() + " was not found in the resource schema of " + contextDescription);
            }
        } else {
            throw new SchemaException("Definition of "+typeDesc+" type " + rOcDefType.getName() + " does not have objectclass, in " + contextDescription);
        }
        
        RefinedObjectClassDefinition rOcDef = new RefinedObjectClassDefinition(prismContext, resourceType, objectClassDef);
        rOcDef.setKind(kind);

        if (rOcDefType.getName() != null) {
            rOcDef.setIntent(rOcDefType.getName());
        } else {
            throw new SchemaException(StringUtils.capitalize(typeDesc)+" type definition does not have a name, in " + contextDescription);
        }
        
        if (rOcDefType.getDisplayName() != null) {
            rOcDef.setDisplayName(rOcDefType.getDisplayName());
        } else {
            if (objectClassDef.getDisplayName() != null) {
                rOcDef.setDisplayName(objectClassDef.getDisplayName());
            }
        }

        if (rOcDefType.getDescription() != null) {
            rOcDef.setDescription(rOcDefType.getDescription());
        }

        if (rOcDefType.isDefault() != null) {
            rOcDef.setDefault(rOcDefType.isDefault());
        } else {
            rOcDef.setDefault(objectClassDef.isDefaultInAKind());
        }

        for (ResourceAttributeDefinition road : objectClassDef.getAttributeDefinitions()) {
            String attrContextDescription = road.getName() + ", in " + contextDescription;
            ResourceAttributeDefinitionType attrDefType = findAttributeDefinitionType(road.getName(), rOcDefType,
            		typeDesc, attrContextDescription);
            // We MUST NOT skip ignored attribute definitions here. We must include them in the schema as
            // the shadows will still have that attributes and we will need their type definition to work
            // well with them. They may also be mandatory. We cannot pretend that they do not exist.

            RefinedAttributeDefinition rAttrDef = RefinedAttributeDefinition.parse(road, attrDefType, objectClassDef, 
            		prismContext, "in "+typeDesc+" type " + rOcDefType.getName() + ", in " + contextDescription);
            rOcDef.processIdentifiers(rAttrDef, objectClassDef);

            if (rOcDef.containsAttributeDefinition(rAttrDef.getName())) {
                throw new SchemaException("Duplicate definition of attribute " + rAttrDef.getName() + " in "+typeDesc+" type " +
                		rOcDefType.getName() + ", in " + contextDescription);
            }
            rOcDef.add(rAttrDef);

        }

        // Check for extra attribute definitions in the account type
        for (ResourceAttributeDefinitionType attrDefType : rOcDefType.getAttribute()) {
            if (!rOcDef.containsAttributeDefinition(attrDefType.getRef()) && !RefinedAttributeDefinition.isIgnored(attrDefType)) {
                throw new SchemaException("Definition of attribute " + attrDefType.getRef() + " not found in object class " + objectClassDef.getTypeName() + " as defined in " + contextDescription);
            }
        }
        
        // Entitlement associations
        if (rOcDefType.getEntitlementAssociation() != null) {
        	rOcDef.getEntitlementAssociations().addAll(rOcDefType.getEntitlementAssociation());
        }
        
        parseProtected(rOcDef, rOcDefType);
   
        return rOcDef;
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
			ResourceObjectClassTypeDefinitionType rOcDefType, String typeDesc, String contextDescription) throws SchemaException {
        ResourceAttributeDefinitionType foundAttrDefType = null;
        for (ResourceAttributeDefinitionType attrDefType : rOcDefType.getAttribute()) {
            if (attrDefType.getRef() != null) {
                if (attrDefType.getRef().equals(attrName)) {
                    if (foundAttrDefType == null) {
                        foundAttrDefType = attrDefType;
                    } else {
                        throw new SchemaException("Duplicate definition of attribute " + attrDefType.getRef() + " in "+typeDesc+" type "
                                + rOcDefType.getName() + ", in " + contextDescription);
                    }
                }
            } else {
                throw new SchemaException("Missing reference to the attribute schema definition in definition " + SchemaDebugUtil.prettyPrint(attrDefType) + " during processing of " + contextDescription);
            }
        }
        return foundAttrDefType;
    }

    

    public PrismObject<ShadowType> createBlankShadow() {
    	PrismObject<ShadowType> accountShadow;
		try {
			accountShadow = prismContext.getSchemaRegistry().instantiate(ShadowType.class);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal error instantiating account shadow: "+e.getMessage(), e);
		}
		ShadowType accountShadowType = accountShadow.asObjectable();
        
    	accountShadowType.setIntent(getIntent());
        accountShadowType.setObjectClass(objectClassDefinition.getTypeName());
        accountShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(resourceType));
        
        // Setup definition
        PrismObjectDefinition<ShadowType> newDefinition = accountShadow.getDefinition().cloneWithReplacedDefinition(
        		ShadowType.F_ATTRIBUTES, toResourceAttributeContainerDefinition());
        accountShadow.setDefinition(newDefinition);
        
        return accountShadow;
    }

    public ResourceShadowDiscriminator getShadowDiscriminator() {
        return new ResourceShadowDiscriminator(resourceType.getOid(), getIntent());
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
            if (intent == null && account.isDefault()) {
                definition = account;
                break;
            }

            if (intent.equals(account.getName())) {
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
    
	public AttributeFetchStrategyType getPasswordFetchStrategy() {
		ResourcePasswordDefinitionType password = getPasswordDefinition();
		if (password == null) {
			return AttributeFetchStrategyType.IMPLICIT;
		}
		if (password.getFetchStrategy() == null) {
			return AttributeFetchStrategyType.IMPLICIT;
		}
		return password.getFetchStrategy();
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
    
    public AttributeFetchStrategyType getActivationEnableFetchStrategy() {
    	ResourceActivationEnableDefinitionType enableDef = getActivationEnableDefinition();
		if (enableDef == null) {
			return AttributeFetchStrategyType.IMPLICIT;
		}
		if (enableDef.getFetchStrategy() == null) {
			return AttributeFetchStrategyType.IMPLICIT;
		}
		return enableDef.getFetchStrategy();
	}

    @Override
    public String dump() {
        return debugDump(0);
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
        sb.append(SchemaDebugUtil.prettyPrint(getTypeName()));
        if (isDefault()) {
            sb.append(",default");
        }
        if (getKind() != null) {
        	sb.append(" ").append(getKind().value());
        }
        sb.append(",");
        if (getIntent() != null) {
        	sb.append("intent=").append(getIntent());
        }
        sb.append(")\n");
        Iterator<? extends ItemDefinition> i = getDefinitions().iterator();
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
        return "rOCD";
    }
    
}
