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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class RefinedResourceSchema extends ResourceSchema implements DebugDumpable {
	
	private static final String USER_DATA_KEY_PARSED_RESOURCE_SCHEMA = RefinedResourceSchema.class.getName()+".parsedResourceSchema";
	private static final String USER_DATA_KEY_REFINED_SCHEMA = RefinedResourceSchema.class.getName()+".refinedSchema";
	
	private ResourceSchema originalResourceSchema;
	
	protected RefinedResourceSchema(PrismContext prismContext) {
		super(prismContext);
	}

	private RefinedResourceSchema(ResourceType resourceType, ResourceSchema originalResourceSchema, PrismContext prismContext) {
		super(ResourceTypeUtil.getResourceNamespace(resourceType), prismContext);
		Validate.notNull(originalResourceSchema);
		this.originalResourceSchema = originalResourceSchema;
	}
	
	public Collection<? extends RefinedObjectClassDefinition> getRefinedDefinitions() {
		Collection<RefinedObjectClassDefinition> ocDefs = new ArrayList<RefinedObjectClassDefinition>();
		for (Definition def: definitions) {
			if (def instanceof RefinedObjectClassDefinition) {
				RefinedObjectClassDefinition rOcDef = (RefinedObjectClassDefinition)def;
				ocDefs.add(rOcDef);
			}
		}
		return ocDefs;
	}
	
	public Collection<? extends RefinedObjectClassDefinition> getRefinedDefinitions(ShadowKindType kind) {
		Collection<RefinedObjectClassDefinition> ocDefs = new ArrayList<RefinedObjectClassDefinition>();
		for (Definition def: definitions) {
			if ((def instanceof RefinedObjectClassDefinition) 
					&& MiscSchemaUtil.matchesKind(kind, ((RefinedObjectClassDefinition) def).getKind())) {
				RefinedObjectClassDefinition rOcDef = (RefinedObjectClassDefinition)def;
				ocDefs.add(rOcDef);
			}
		}
		return ocDefs;
	}
	
	public ResourceSchema getOriginalResourceSchema() {
		return originalResourceSchema;
	}

	
	public RefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, ShadowType shadow) {
		return getRefinedDefinition(kind, ShadowUtil.getIntent(shadow));
	}
	
	/**
	 * if null accountType is provided, default account definition is returned.
	 */
	public RefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, String intent) {
		for (RefinedObjectClassDefinition acctDef: getRefinedDefinitions(kind)) {
			if (intent == null && acctDef.isDefault()) {
				return acctDef;
			}
			if (acctDef.getIntent().equals(intent)) {
				return acctDef;
			}
		}
		return null;
	}

    /**
     * If no intents are provided, default account definition is returned.
     * We check whether there is only one relevant rOCD.
     */
    public RefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, Collection<String> intents) throws SchemaException {
        RefinedObjectClassDefinition found = null;
        for (RefinedObjectClassDefinition acctDef: getRefinedDefinitions(kind)) {
            RefinedObjectClassDefinition foundCurrent = null;
            if (intents == null || intents.isEmpty()) {
                if (acctDef.isDefault()) {
                    foundCurrent = acctDef;
                }
            } else {
                if (intents.contains(acctDef.getIntent())) {
                    foundCurrent = acctDef;
                }
            }
            if (foundCurrent != null) {
                if (found != null) {
                    if (!QNameUtil.match(found.getTypeName(), foundCurrent.getTypeName())) {
                        throw new SchemaException("More than one ObjectClass found for kind " + kind + ", intents: " + intents + ": " + found.getTypeName() + ", " + foundCurrent.getTypeName());
                    }
                } else {
                    found = foundCurrent;
                }
            }
        }
        return found;
    }

    public RefinedObjectClassDefinition getRefinedDefinition(QName objectClassName) {
		for (Definition def: definitions) {
			if ((def instanceof RefinedObjectClassDefinition) 
					&& (QNameUtil.match(def.getTypeName(), objectClassName))) {
                    //&& (def.getTypeName().equals(objectClassName))) {
				return (RefinedObjectClassDefinition)def;
			}
		}
		return null;
	}
	
	public RefinedObjectClassDefinition getDefaultRefinedDefinition(ShadowKindType kind) {
		return getRefinedDefinition(kind, (String)null);
	}
	
	public PrismObjectDefinition<ShadowType> getObjectDefinition(ShadowKindType kind, String intent) {
		return getRefinedDefinition(kind, intent).getObjectDefinition();
	}
	
	public PrismObjectDefinition<ShadowType> getObjectDefinition(ShadowKindType kind, ShadowType shadow) {
		return getObjectDefinition(kind, ShadowUtil.getIntent(shadow));
	}
		
	private void add(RefinedObjectClassDefinition rOcDef) {
		definitions.add(rOcDef);
	}
	
	public RefinedObjectClassDefinition findRefinedDefinitionByObjectClassQName(ShadowKindType kind, QName objectClass) {
		if (objectClass == null) {
			return getDefaultRefinedDefinition(kind);
		}
		for (RefinedObjectClassDefinition acctDef: getRefinedDefinitions(kind)) {
			if (acctDef.getObjectClassDefinition().getTypeName().equals(objectClass)) {
				return acctDef;
			}
		}
		return null;
	}
	
	public ObjectClassComplexTypeDefinition findObjectClassDefinition(QName objectClassQName) {
		return originalResourceSchema.findObjectClassDefinition(objectClassQName);
	}
	
	public static RefinedResourceSchema getRefinedSchema(ResourceType resourceType) throws SchemaException {
		return getRefinedSchema(resourceType, resourceType.asPrismObject().getPrismContext());
	}
	
	public static LayerRefinedResourceSchema getRefinedSchema(ResourceType resourceType, LayerType layer) throws SchemaException {
		return getRefinedSchema(resourceType, layer, resourceType.asPrismObject().getPrismContext());
	}

	public static RefinedResourceSchema getRefinedSchema(ResourceType resourceType, PrismContext prismContext) throws SchemaException {
		PrismObject<ResourceType> resource = resourceType.asPrismObject();
		return getRefinedSchema(resource, prismContext);
	}
	
	public static LayerRefinedResourceSchema getRefinedSchema(ResourceType resourceType, LayerType layer, PrismContext prismContext) throws SchemaException {
		PrismObject<ResourceType> resource = resourceType.asPrismObject();
		return getRefinedSchema(resource, layer, prismContext);
	}
	
	public static RefinedResourceSchema getRefinedSchema(PrismObject<ResourceType> resource) throws SchemaException {
		return getRefinedSchema(resource, resource.getPrismContext());
	}
	
	public static RefinedResourceSchema getRefinedSchema(PrismObject<ResourceType> resource, PrismContext prismContext) throws SchemaException {
		if (resource == null){
			throw new SchemaException("Could not get refined schema, resource does not exist.");
		}
		
		Object userDataEntry = resource.getUserData(USER_DATA_KEY_REFINED_SCHEMA);
		if (userDataEntry != null) {
			if (userDataEntry instanceof RefinedResourceSchema) {
				return (RefinedResourceSchema)userDataEntry;
			} else {
				throw new IllegalStateException("Expected RefinedResourceSchema under user data key "+USER_DATA_KEY_REFINED_SCHEMA+
						"in "+resource+", but got "+userDataEntry.getClass());
			}
		} else {
			RefinedResourceSchema refinedSchema = parse(resource, prismContext);
			resource.setUserData(USER_DATA_KEY_REFINED_SCHEMA, refinedSchema);
			return refinedSchema;
		}
	}
	
	public static LayerRefinedResourceSchema getRefinedSchema(PrismObject<ResourceType> resource, LayerType layer, PrismContext prismContext) throws SchemaException {
		RefinedResourceSchema refinedSchema = getRefinedSchema(resource, prismContext);
		if (refinedSchema == null) {
			return null;
		}
		return refinedSchema.forLayer(layer);
	}
	
	public static boolean hasRefinedSchema(ResourceType resourceType) {
		PrismObject<ResourceType> resource = resourceType.asPrismObject();
		return resource.getUserData(USER_DATA_KEY_REFINED_SCHEMA) != null;
	}
	
	public static ResourceSchema getResourceSchema(ResourceType resourceType, PrismContext prismContext) throws SchemaException {
		PrismObject<ResourceType> resource = resourceType.asPrismObject();
		return getResourceSchema(resource, prismContext);
	}
	
	public static ResourceSchema getResourceSchema(PrismObject<ResourceType> resource, PrismContext prismContext) throws SchemaException {
		Element resourceXsdSchema = ResourceTypeUtil.getResourceXsdSchema(resource);
		if (resourceXsdSchema == null) {
			return null;
		}
		Object userDataEntry = resource.getUserData(USER_DATA_KEY_PARSED_RESOURCE_SCHEMA);
		if (userDataEntry != null) {
			if (userDataEntry instanceof ResourceSchema) {
				return (ResourceSchema)userDataEntry;
			} else {
				throw new IllegalStateException("Expected ResourceSchema under user data key "+
						USER_DATA_KEY_PARSED_RESOURCE_SCHEMA+ "in "+resource+", but got "+userDataEntry.getClass());
			}
		} else {
			InternalMonitor.recordResourceSchemaParse();
			ResourceSchema parsedSchema = ResourceSchema.parse(resourceXsdSchema, "resource schema of "+resource, prismContext);
			if (parsedSchema == null) {
				throw new IllegalStateException("Parsed schema is null: most likely an internall error");
			}
			resource.setUserData(USER_DATA_KEY_PARSED_RESOURCE_SCHEMA, parsedSchema);
			return parsedSchema;
		}
	}
	
	public static void setParsedResourceSchemaConditional(ResourceType resourceType, ResourceSchema parsedSchema) {
		if (hasParsedSchema(resourceType)) {
			return;
		}
		PrismObject<ResourceType> resource = resourceType.asPrismObject();
		resource.setUserData(USER_DATA_KEY_PARSED_RESOURCE_SCHEMA, parsedSchema);
	}

	public static boolean hasParsedSchema(ResourceType resourceType) {
		PrismObject<ResourceType> resource = resourceType.asPrismObject();
		return resource.getUserData(USER_DATA_KEY_PARSED_RESOURCE_SCHEMA) != null;
	}

	public static RefinedResourceSchema parse(PrismObject<ResourceType> resource, PrismContext prismContext) throws SchemaException {
		return parse(resource.asObjectable(), prismContext);
	}
	
	public static RefinedResourceSchema parse(ResourceType resourceType, PrismContext prismContext) throws SchemaException {
		
		ResourceSchema originalResourceSchema = getResourceSchema(resourceType, prismContext);
		if (originalResourceSchema == null) {
			return null;
		}
		
		SchemaHandlingType schemaHandling = resourceType.getSchemaHandling();
		
		RefinedResourceSchema rSchema = new RefinedResourceSchema(resourceType, originalResourceSchema, prismContext);
		
		if (hasAnyObjectTypeDef(schemaHandling)) {
			
			parseObjectTypeDefsFromSchemaHandling(rSchema, resourceType, schemaHandling, 
					schemaHandling.getObjectType(), null, prismContext, "definition of "+resourceType);
					
		} else {
			parseObjectTypesFromSchema(rSchema, resourceType, prismContext, 
					"definition of "+resourceType);
		}
		
		return rSchema;
	}

	private static boolean hasAnyObjectTypeDef(SchemaHandlingType schemaHandling) {
		if (schemaHandling == null) {
			return false;
		}
		if (!schemaHandling.getObjectType().isEmpty()) {
			return true;
		}
		return false;
	}

	private static void parseObjectTypeDefsFromSchemaHandling(RefinedResourceSchema rSchema, ResourceType resourceType,
			SchemaHandlingType schemaHandling, Collection<ResourceObjectTypeDefinitionType> resourceObjectTypeDefs, 
			ShadowKindType impliedKind, PrismContext prismContext, String contextDescription) throws SchemaException {
		
		if (resourceObjectTypeDefs == null) {
			return;
		}
		
		Map<ShadowKindType, RefinedObjectClassDefinition> defaults = new HashMap<ShadowKindType, RefinedObjectClassDefinition>();
		
		for (ResourceObjectTypeDefinitionType accountTypeDefType: resourceObjectTypeDefs) {
			RefinedObjectClassDefinition rOcDef = RefinedObjectClassDefinition.parse(accountTypeDefType, resourceType, rSchema, impliedKind, 
					prismContext, contextDescription);
			
			if (rOcDef.isDefault()) {
				if (defaults.containsKey(rOcDef.getKind())) {
					throw new SchemaException("More than one default "+rOcDef.getKind()+" definitions ("+defaults.get(rOcDef.getKind())+", "+rOcDef+") in " + contextDescription);
				} else {
					defaults.put(rOcDef.getKind(), rOcDef);
				}
			}
				
			rSchema.add(rOcDef);
		}
		
		// We need to parse associations in a second pass. We need to have all object classes parsed before correctly setting association
		// targets
		for (RefinedObjectClassDefinition rOcDef: rSchema.getRefinedDefinitions()) {
			rOcDef.parseAssociations(rSchema);
		}
	}

	private static void parseObjectTypesFromSchema(RefinedResourceSchema rSchema, ResourceType resourceType,
			PrismContext prismContext, String contextDescription) throws SchemaException {

		RefinedObjectClassDefinition rAccountDefDefault = null;
		for(ObjectClassComplexTypeDefinition objectClassDef: rSchema.getOriginalResourceSchema().getObjectClassDefinitions()) {
			if (objectClassDef.getKind() == ShadowKindType.ACCOUNT) {
				QName objectClassname = objectClassDef.getTypeName();
				RefinedObjectClassDefinition rAccountDef = RefinedObjectClassDefinition.parse(objectClassDef, resourceType, rSchema, prismContext, 
						"object class "+objectClassname+" (interpreted as account type definition), in "+contextDescription);
				
				if (rAccountDef.isDefault()) {
					if (rAccountDefDefault == null) {
						rAccountDefDefault = rAccountDef;
					} else {
						throw new SchemaException("More than one default account definitions ("+rAccountDefDefault+", "+rAccountDef+") in " + contextDescription);
					}
				}
					
				rSchema.add(rAccountDef);
			}
		}
		
	}
		
	public LayerRefinedResourceSchema forLayer(LayerType layer) {
		return LayerRefinedResourceSchema.wrap(this, layer);
	}
	
	@Override
	public String toString() {
		return "rSchema(ns=" + namespace + ")";
	}

}
