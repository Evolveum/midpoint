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
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.*;

/**
 * @author semancik
 */
public class RefinedObjectClassDefinition extends ObjectClassComplexTypeDefinition implements DebugDumpable {

    private String intent;
    private String displayName;
    private String description;
    private boolean isDefault;
    private ObjectClassComplexTypeDefinition objectClassDefinition;
    private ResourceObjectTypeDefinitionType schemaHandlingObjectTypeDefinitionType;
    private ResourceType resourceType;
    private Collection<? extends RefinedAttributeDefinition> identifiers;
	private Collection<? extends RefinedAttributeDefinition> secondaryIdentifiers;
	private Collection<ResourceObjectPattern> protectedObjectPatterns;
	private List<RefinedAttributeDefinition> attributeDefinitions;
	private Collection<RefinedAssociationDefinition> associations = new ArrayList<RefinedAssociationDefinition>();
	
    /**
     * Refined object definition. The "any" parts are replaced with appropriate schema (e.g. resource schema)
     */
    PrismObjectDefinition<ShadowType> objectDefinition = null;
	private ShadowKindType kind = null;
    
    /**
     * This is needed by the LayerRefinedObjectClassDefinition class
     */
    protected RefinedObjectClassDefinition(QName typeName, PrismContext prismContext) {
    	super(typeName, prismContext);
    }

    private RefinedObjectClassDefinition(PrismContext prismContext, ResourceType resourceType,
    		ObjectClassComplexTypeDefinition objectClassDefinition) {
        super(objectClassDefinition.getTypeName(), prismContext);
        Validate.notNull(objectClassDefinition, "ObjectClass definition must not be null");
        attributeDefinitions = new ArrayList<RefinedAttributeDefinition>();
        this.resourceType = resourceType;
        this.objectClassDefinition = objectClassDefinition;
    }

    /**
     * Creates a derived version of this ROCD for a given layer.
     * TODO clone if necessary/if specified (currently there is no cloning)
     *
     * @param layerType
     * @return
     */
    public LayerRefinedObjectClassDefinition forLayer(LayerType layerType) {
        Validate.notNull(layerType);
        return LayerRefinedObjectClassDefinition.wrap(this, layerType);
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
	
	public Collection<RefinedAssociationDefinition> getAssociations() {
		return associations;
	}
	
	public Collection<RefinedAssociationDefinition> getAssociations(ShadowKindType kind) {
		Collection<RefinedAssociationDefinition> retAssoc = new ArrayList<RefinedAssociationDefinition>();
		for (RefinedAssociationDefinition association: associations) {
			if (kind == association.getKind()) {
				retAssoc.add(association);
			}
		}
		return retAssoc;
	}

	public void setAssociations(Collection<RefinedAssociationDefinition> associations) {
		this.associations = associations;
	}
	
	public RefinedAssociationDefinition findAssociation(QName name) {
		for (RefinedAssociationDefinition assocType: getAssociations()) {
			if (QNameUtil.match(assocType.getName(), name)) {
				return assocType;
			}
		}
		return null;
	}

	public Collection<RefinedAssociationDefinition> getEntitlementAssociations() {
		return getAssociations(ShadowKindType.ENTITLEMENT);
	}
	
	public RefinedAssociationDefinition findEntitlementAssociation(QName name) {
		for (RefinedAssociationDefinition assocType: getEntitlementAssociations()) {
			if (QNameUtil.match(assocType.getName(), name)) {
				return assocType;
			}
		}
		return null;
	}
	
    public Collection<QName> getNamesOfAssociations() {
        Collection<QName> names = new HashSet<QName>();
        for (RefinedAssociationDefinition assocDef : getAssociations()) {
            names.add(assocDef.getName());
        }
        return names;
    }

    public Collection<? extends QName> getNamesOfAssociationsWithOutboundExpressions() {
        Collection<QName> names = new HashSet<QName>();
        for (RefinedAssociationDefinition assocDef : getAssociations()) {
            if (assocDef.getOutboundMappingType() != null) {
                names.add(assocDef.getName());
            }
        }
        return names;
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

    private void copyDefinitionData(RefinedObjectClassDefinition clone) {
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

	protected String getResourceNamespace() {
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
    	Validate.notNull(objectClassDefinition, "ObjectClass definition must not be null");
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
            if (QNameUtil.match(attrDef.getName(), attributeName)) {
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
            if (QNameUtil.match(rAttributeDef.getName(), attributeName)) {
                return true;
            }
        }
        return false;
    }
    
    static RefinedObjectClassDefinition parse(ResourceObjectTypeDefinitionType entTypeDefType,
			ResourceType resourceType, RefinedResourceSchema rSchema, ShadowKindType impliedKind, PrismContext prismContext,
			String contextDescription) throws SchemaException {
	
    	ShadowKindType kind = entTypeDefType.getKind();
    	if (kind == null) {
    		kind = impliedKind;
    	}
    	if (kind == null) {
    		kind = ShadowKindType.ACCOUNT;
    	}
    	String intent = entTypeDefType.getIntent();
		RefinedObjectClassDefinition rObjectClassDef = parseRefinedObjectClass(entTypeDefType, 
				resourceType, rSchema, prismContext, kind, intent, kind.value(), kind.value() + " type definition '"+intent+"' in " + contextDescription);
		return rObjectClassDef;
				
	}

	private static void parseProtected(RefinedObjectClassDefinition rAccountDef, ResourceObjectTypeDefinitionType accountTypeDefType) throws SchemaException {
		for (ResourceObjectPatternType protectedType: accountTypeDefType.getProtected()) {
			ResourceObjectPattern protectedPattern = convertToPattern(protectedType, rAccountDef);
			rAccountDef.getProtectedObjectPatterns().add(protectedPattern);
		}
	}
	
	private static ResourceObjectPattern convertToPattern(ResourceObjectPatternType patternType, RefinedObjectClassDefinition rAccountDef) throws SchemaException {
		ResourceObjectPattern resourceObjectPattern = new ResourceObjectPattern(rAccountDef);
		SearchFilterType filterType = patternType.getFilter();
		if (filterType != null) {
			ObjectFilter filter = QueryConvertor.parseFilter(filterType, rAccountDef.getObjectDefinition());
			resourceObjectPattern.addFilter(filter);
			return resourceObjectPattern;
		}
		
		// Deprecated
		if (patternType.getName() != null) {
			RefinedAttributeDefinition attributeDefinition = rAccountDef.findAttributeDefinition(new QName(SchemaConstants.NS_ICF_SCHEMA,"name"));
			ResourceAttribute<String> attr = attributeDefinition.instantiate();
			attr.setRealValue(patternType.getName());
			resourceObjectPattern.addIdentifier(attr);
		} else if (patternType.getUid() != null) {
			RefinedAttributeDefinition attributeDefinition = rAccountDef.findAttributeDefinition(new QName(SchemaConstants.NS_ICF_SCHEMA,"uid"));
			ResourceAttribute<String> attr = attributeDefinition.instantiate();
			attr.setRealValue(patternType.getName());
			resourceObjectPattern.addIdentifier(attr);			
		} else {
			throw new SchemaException("No filter and no deprecated name/uid in resource object pattern");
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
	
	private static RefinedObjectClassDefinition parseRefinedObjectClass(ResourceObjectTypeDefinitionType schemaHandlingObjDefType,
			ResourceType resourceType, RefinedResourceSchema rSchema, PrismContext prismContext,
			ShadowKindType kind, String intent, String typeDesc, String contextDescription) throws SchemaException {
		
		ObjectClassComplexTypeDefinition objectClassDef = null;
        if (schemaHandlingObjDefType.getObjectClass() != null) {
            QName objectClass = schemaHandlingObjDefType.getObjectClass();
            objectClassDef = rSchema.getOriginalResourceSchema().findObjectClassDefinition(objectClass);
            if (objectClassDef == null) {
                throw new SchemaException("Object class " + objectClass + " as specified in "+typeDesc+" type " + schemaHandlingObjDefType.getIntent() + " was not found in the resource schema of " + contextDescription);
            }
        } else {
            throw new SchemaException("Definition of "+typeDesc+" type " + schemaHandlingObjDefType.getIntent() + " does not have objectclass, in " + contextDescription);
        }
        
        RefinedObjectClassDefinition rOcDef = new RefinedObjectClassDefinition(prismContext, resourceType, objectClassDef);
        rOcDef.setKind(kind);
        rOcDef.schemaHandlingObjectTypeDefinitionType = schemaHandlingObjDefType;

        if (intent == null && kind == ShadowKindType.ACCOUNT) {
        	intent = SchemaConstants.INTENT_DEFAULT;
        }
        
        if (intent != null) {
            rOcDef.setIntent(intent);
        } else {
            throw new SchemaException(StringUtils.capitalize(typeDesc)+" type definition does not have intent, in " + contextDescription);
        }
        
        if (schemaHandlingObjDefType.getDisplayName() != null) {
            rOcDef.setDisplayName(schemaHandlingObjDefType.getDisplayName());
        } else {
            if (objectClassDef.getDisplayName() != null) {
                rOcDef.setDisplayName(objectClassDef.getDisplayName());
            }
        }

        if (schemaHandlingObjDefType.getDescription() != null) {
            rOcDef.setDescription(schemaHandlingObjDefType.getDescription());
        }

        if (schemaHandlingObjDefType.isDefault() != null) {
            rOcDef.setDefault(schemaHandlingObjDefType.isDefault());
        } else {
            rOcDef.setDefault(objectClassDef.isDefaultInAKind());
        }

        for (ResourceAttributeDefinition road : objectClassDef.getAttributeDefinitions()) {
            String attrContextDescription = road.getName() + ", in " + contextDescription;
            ResourceAttributeDefinitionType attrDefType = findAttributeDefinitionType(road.getName(), schemaHandlingObjDefType,
            		typeDesc, attrContextDescription);
            // We MUST NOT skip ignored attribute definitions here. We must include them in the schema as
            // the shadows will still have that attributes and we will need their type definition to work
            // well with them. They may also be mandatory. We cannot pretend that they do not exist.

            RefinedAttributeDefinition rAttrDef = RefinedAttributeDefinition.parse(road, attrDefType, objectClassDef, 
            		prismContext, "in "+typeDesc+" type " + intent + ", in " + contextDescription);
            rOcDef.processIdentifiers(rAttrDef, objectClassDef);

            if (rOcDef.containsAttributeDefinition(rAttrDef.getName())) {
                throw new SchemaException("Duplicate definition of attribute " + rAttrDef.getName() + " in "+typeDesc+" type " +
                		intent + ", in " + contextDescription);
            }
            rOcDef.add(rAttrDef);

        }

        // Check for extra attribute definitions in the account type
        for (ResourceAttributeDefinitionType attrDefType : schemaHandlingObjDefType.getAttribute()) {
            if (!rOcDef.containsAttributeDefinition(attrDefType.getRef()) && !RefinedAttributeDefinition.isIgnored(attrDefType)) {
                throw new SchemaException("Definition of attribute " + attrDefType.getRef() + " not found in object class " + objectClassDef.getTypeName() + " as defined in " + contextDescription);
            }
        }
        
        parseProtected(rOcDef, schemaHandlingObjDefType);
   
        return rOcDef;
	}

	public void parseAssociations(RefinedResourceSchema rSchema) throws SchemaException {
		for (ResourceObjectAssociationType resourceObjectAssociationType: schemaHandlingObjectTypeDefinitionType.getAssociation()) {
			RefinedAssociationDefinition rAssocDef = new RefinedAssociationDefinition(resourceObjectAssociationType);
			ShadowKindType assocKind = rAssocDef.getKind();
			RefinedObjectClassDefinition assocTarget = rSchema.getRefinedDefinition(assocKind, rAssocDef.getIntents());
			rAssocDef.setAssociationTarget(assocTarget);
			associations.add(rAssocDef);
		}
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
			ResourceObjectTypeDefinitionType rOcDefType, String typeDesc, String contextDescription) throws SchemaException {
        ResourceAttributeDefinitionType foundAttrDefType = null;
        for (ResourceAttributeDefinitionType attrDefType : rOcDefType.getAttribute()) {
            if (attrDefType.getRef() != null) {
                if (QNameUtil.match(attrDefType.getRef(), attrName)) {
                    if (foundAttrDefType == null) {
                        foundAttrDefType = attrDefType;
                    } else {
                        throw new SchemaException("Duplicate definition of attribute " + attrDefType.getRef() + " in "+typeDesc+" type "
                                + rOcDefType.getIntent() + ", in " + contextDescription);
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
        accountShadowType.setKind(getKind());
        accountShadowType.setObjectClass(objectClassDefinition.getTypeName());
        accountShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(resourceType));
        
        // Setup definition
        PrismObjectDefinition<ShadowType> newDefinition = accountShadow.getDefinition().cloneWithReplacedDefinition(
        		ShadowType.F_ATTRIBUTES, toResourceAttributeContainerDefinition());
        accountShadow.setDefinition(newDefinition);
        
        return accountShadow;
    }

    public ResourceShadowDiscriminator getShadowDiscriminator() {
        return new ResourceShadowDiscriminator(resourceType.getOid(), getKind(), getIntent());
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
    
    public List<MappingType> getCredentialsInbound() {
        
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
        if (schemaHandlingObjectTypeDefinitionType == null) {
            return null;
        }
        ResourceCredentialsDefinitionType credentials = schemaHandlingObjectTypeDefinitionType.getCredentials();
        if (credentials == null) {
            return null;
        }
        
        return credentials.getPassword();
    }
    
    public ResourceActivationDefinitionType getActivationSchemaHandling(){
        if (schemaHandlingObjectTypeDefinitionType == null) {
            return null;
        }

        return schemaHandlingObjectTypeDefinitionType.getActivation();
    }
    
    public ResourceBidirectionalMappingType getActivationBidirectionalMappingType(QName propertyName) {
    	ResourceActivationDefinitionType activationSchemaHandling = getActivationSchemaHandling();
    	if (activationSchemaHandling == null) {
    		return null;
    	}
    	
    	if (QNameUtil.match(ActivationType.F_ADMINISTRATIVE_STATUS, propertyName)) {
    		return activationSchemaHandling.getAdministrativeStatus();
    	} else if (QNameUtil.match(ActivationType.F_VALID_FROM, propertyName)) {
    		return activationSchemaHandling.getValidFrom();
    	} else if (QNameUtil.match(ActivationType.F_VALID_TO, propertyName)) {
    		return activationSchemaHandling.getValidTo();
    	} else if (QNameUtil.match(ActivationType.F_LOCKOUT_STATUS, propertyName)) {
            return null;            // todo implement this
        } else if (QNameUtil.match(ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP, propertyName)) {
            return null;            // todo implement this
        } else {
    		throw new IllegalArgumentException("Unknown activation property "+propertyName);
    	}
    }
    
    public AttributeFetchStrategyType getActivationFetchStrategy(QName propertyName) {
    	ResourceBidirectionalMappingType biType = getActivationBidirectionalMappingType(propertyName);
		if (biType == null) {
			return AttributeFetchStrategyType.IMPLICIT;
		}
		if (biType.getFetchStrategy() == null) {
			return AttributeFetchStrategyType.IMPLICIT;
		}
		return biType.getFetchStrategy();
	}
    
	public boolean matches(ShadowType shadowType) {
		if (shadowType == null) {
			return false;
		}
		if (!QNameUtil.match(objectClassDefinition.getTypeName(), shadowType.getObjectClass())) {
			return false;
		}
		if (shadowType.getKind() == null) {
			if (kind != ShadowKindType.ACCOUNT) {
				return false;
			}
		} else {
			if (!MiscUtil.equals(kind, shadowType.getKind())) {
				return false;
			}
		}
		if (shadowType.getIntent() == null) {
			if (isDefault) {
				return true;
			} else {
				return false;
			}
		} else {
			return MiscUtil.equals(intent, shadowType.getIntent());
		}
	}

    @Override
    public String debugDump() {
        return debugDump(0);
    }
    
    @Override
    public String debugDump(int indent) {
    	return debugDump(indent, null);
    }

    protected String debugDump(int indent, LayerType layer) {
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
        if (layer != null) {
        	sb.append(",layer=").append(layer);
        }
        sb.append(")");
        for (RefinedAttributeDefinition rAttrDef: getAttributeDefinitions()) {
            sb.append("\n");
            sb.append(rAttrDef.debugDump(indent + 1, layer));
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

	public String getHumanReadableName() {
		if (getDisplayName() != null) {
			return getDisplayName();
		} else {
			return getKind()+":"+getIntent();
		}
	}

}
