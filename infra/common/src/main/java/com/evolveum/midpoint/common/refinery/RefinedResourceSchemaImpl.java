/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaImpl;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * TODO: this whole class would benefit from more refactoring.
 * TODO: especially the parsing part.
 *
 * @author semancik
 * @author mederly
 */
public class RefinedResourceSchemaImpl implements RefinedResourceSchema {
    private static final Trace LOGGER = TraceManager.getTrace(RefinedResourceSchemaImpl.class);
    private static final String USER_DATA_KEY_PARSED_RESOURCE_SCHEMA = RefinedResourceSchema.class.getName() + ".parsedResourceSchema";
    private static final String USER_DATA_KEY_REFINED_SCHEMA = RefinedResourceSchema.class.getName() + ".refinedSchema";

    // Original resource schema is there to make parsing easier.
    // But it is also useful in some cases, e.g. we do not need to pass both refined schema and
    // original schema as a method parameter.
    private ResourceSchema originalResourceSchema;

    // This object contains the real data of the refined schema
    private ResourceSchema resourceSchema;

    private RefinedResourceSchemaImpl(@NotNull ResourceSchema originalResourceSchema) {
        this.originalResourceSchema = originalResourceSchema;
        this.resourceSchema = new ResourceSchemaImpl(originalResourceSchema.getNamespace(), originalResourceSchema.getPrismContext());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Collection<ObjectClassComplexTypeDefinition> getObjectClassDefinitions() {
        return (Collection) getRefinedDefinitions();
    }

    @Override
    public List<? extends RefinedObjectClassDefinition> getRefinedDefinitions() {
        return resourceSchema.getDefinitions(RefinedObjectClassDefinition.class);
    }

    @Override
    public List<? extends RefinedObjectClassDefinition> getRefinedDefinitions(ShadowKindType kind) {
        List<RefinedObjectClassDefinition> rv = new ArrayList<>();
        for (RefinedObjectClassDefinition def : getRefinedDefinitions()) {
            if (MiscSchemaUtil.matchesKind(kind, def.getKind())) {
                rv.add(def);
            }
        }
        return rv;
    }

    @Override
    public ResourceSchema getOriginalResourceSchema() {
        return originalResourceSchema;
    }

    @Override
    public RefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, String intent) {
        for (RefinedObjectClassDefinition acctDef : getRefinedDefinitions(kind)) {
            if (intent == null && acctDef.isDefault()) {
                return acctDef;
            }
            if (acctDef.getIntent() != null && acctDef.getIntent().equals(intent)) {
                return acctDef;
            }
            if (acctDef.getIntent() == null && intent == null) {
                return acctDef;
            }
        }
        return null;
    }

    @Override
    public CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(ResourceShadowDiscriminator discriminator) {
        if (discriminator.getKind() == null && discriminator.getObjectClass() == null) {
            return null;
        }
        RefinedObjectClassDefinition structuralObjectClassDefinition;
        if (discriminator.getKind() == null && discriminator.getObjectClass() != null) {
            structuralObjectClassDefinition = getRefinedDefinition(discriminator.getObjectClass());
        } else {
            structuralObjectClassDefinition = getRefinedDefinition(discriminator.getKind(), discriminator.getIntent());
        }
        if (structuralObjectClassDefinition == null) {
            return null;
        }
        Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions = structuralObjectClassDefinition.getAuxiliaryObjectClassDefinitions();
        return new CompositeRefinedObjectClassDefinitionImpl(structuralObjectClassDefinition, auxiliaryObjectClassDefinitions);
    }

    @Override
    public CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(PrismObject<ShadowType> shadow) throws SchemaException {
        return determineCompositeObjectClassDefinition(shadow, null);
    }

    @Override
    public CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(PrismObject<ShadowType> shadow,
                                                                                         Collection<QName> additionalAuxiliaryObjectClassQNames) throws SchemaException {
        ShadowType shadowType = shadow.asObjectable();

        RefinedObjectClassDefinition structuralObjectClassDefinition = null;
        ShadowKindType kind = shadowType.getKind();
        String intent = shadowType.getIntent();
        QName structuralObjectClassQName = shadowType.getObjectClass();

        if (kind != null) {
            structuralObjectClassDefinition = getRefinedDefinition(kind, intent);
        }

        if (structuralObjectClassDefinition == null) {
            // Fallback to objectclass only
            if (structuralObjectClassQName == null) {
                return null;
            }
            structuralObjectClassDefinition = getRefinedDefinition(structuralObjectClassQName);
        }

        if (structuralObjectClassDefinition == null) {
            return null;
        }
        List<QName> auxiliaryObjectClassQNames = shadowType.getAuxiliaryObjectClass();
        if (additionalAuxiliaryObjectClassQNames != null) {
            auxiliaryObjectClassQNames.addAll(additionalAuxiliaryObjectClassQNames);
        }
        Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>(auxiliaryObjectClassQNames.size());
        for (QName auxiliaryObjectClassQName : auxiliaryObjectClassQNames) {
            RefinedObjectClassDefinition auxiliaryObjectClassDef = getRefinedDefinition(auxiliaryObjectClassQName);
            if (auxiliaryObjectClassDef == null) {
                throw new SchemaException("Auxiliary object class " + auxiliaryObjectClassQName + " specified in " + shadow + " does not exist");
            }
            auxiliaryObjectClassDefinitions.add(auxiliaryObjectClassDef);
        }

        return new CompositeRefinedObjectClassDefinitionImpl(structuralObjectClassDefinition, auxiliaryObjectClassDefinitions);
    }

    @Override
    public CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(QName structuralObjectClassQName,
                                                                                         ShadowKindType kind, String intent) {
        RefinedObjectClassDefinition structuralObjectClassDefinition = null;
        Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions;
        if (kind != null) {
            structuralObjectClassDefinition = getRefinedDefinition(kind, intent);
        }
        if (structuralObjectClassDefinition == null) {
            // Fallback to objectclass only
            if (structuralObjectClassQName == null) {
                throw new IllegalArgumentException("No kind nor objectclass defined");
            }
            structuralObjectClassDefinition = getRefinedDefinition(structuralObjectClassQName);
        }

        if (structuralObjectClassDefinition == null) {
            return null;
        }

        auxiliaryObjectClassDefinitions = structuralObjectClassDefinition.getAuxiliaryObjectClassDefinitions();

        return new CompositeRefinedObjectClassDefinitionImpl(structuralObjectClassDefinition, auxiliaryObjectClassDefinitions);
    }

    @Override
    public RefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, Collection<String> intents) throws SchemaException {
        RefinedObjectClassDefinition found = null;
        for (RefinedObjectClassDefinition acctDef : getRefinedDefinitions(kind)) {
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

    @Override
    public RefinedObjectClassDefinition getRefinedDefinition(QName objectClassName) {
        for (RefinedObjectClassDefinition def : getRefinedDefinitions()) {
            if (def.isDefault() && (QNameUtil.match(def.getTypeName(), objectClassName))) {
                return def;
            }
        }
        // No default for this object class, so just use the first one.
        // This is not strictly correct .. but it is a "compatible bug" :-)
        // TODO: remove this in next major revision
        for (RefinedObjectClassDefinition def : getRefinedDefinitions()) {
            if ((QNameUtil.match(def.getTypeName(), objectClassName))) {
                return def;
            }
        }
        return null;
    }

    @Override
    public RefinedObjectClassDefinition findRefinedDefinitionByObjectClassQName(ShadowKindType kind, QName objectClass) {
        if (objectClass == null) {
            return getDefaultRefinedDefinition(kind);
        }
        for (RefinedObjectClassDefinition acctDef : getRefinedDefinitions(kind)) {
            if (acctDef.isDefault() && QNameUtil.match(acctDef.getObjectClassDefinition().getTypeName(), objectClass)) {
                return acctDef;
            }
        }
        // No default for this object class, so just use the first one.
        // This is not strictly correct .. but it is a "compatible bug" :-)
        // TODO: remove this in next major revision
        for (RefinedObjectClassDefinition acctDef : getRefinedDefinitions(kind)) {
            if (QNameUtil.match(acctDef.getObjectClassDefinition().getTypeName(), objectClass)) {
                return acctDef;
            }
        }
        return null;
    }


    @Override
    public LayerRefinedResourceSchema forLayer(LayerType layer) {
        return new LayerRefinedResourceSchemaImpl(this, layer);
    }

    @Override
    public String toString() {
        return "rSchema(ns=" + getNamespace() + ")";
    }

    //region Static methods
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
        if (resource == null) {
            throw new SchemaException("Could not get refined schema, resource does not exist.");
        }

        Object userDataEntry = resource.getUserData(USER_DATA_KEY_REFINED_SCHEMA);
        if (userDataEntry != null) {
            if (userDataEntry instanceof RefinedResourceSchema) {
                return (RefinedResourceSchema) userDataEntry;
            } else {
                throw new IllegalStateException("Expected RefinedResourceSchema under user data key " + USER_DATA_KEY_REFINED_SCHEMA +
                    "in " + resource + ", but got " + userDataEntry.getClass());
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
                return (ResourceSchema) userDataEntry;
            } else {
                throw new IllegalStateException("Expected ResourceSchema under user data key " +
                    USER_DATA_KEY_PARSED_RESOURCE_SCHEMA + "in " + resource + ", but got " + userDataEntry.getClass());
            }
        } else {
            InternalMonitor.recordCount(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
            ResourceSchemaImpl parsedSchema = ResourceSchemaImpl.parse(resourceXsdSchema, "resource schema of " + resource, prismContext);
            if (parsedSchema == null) {
                throw new IllegalStateException("Parsed schema is null: most likely an internall error");
            }
            resource.setUserData(USER_DATA_KEY_PARSED_RESOURCE_SCHEMA, parsedSchema);
            parsedSchema.setNamespace(ResourceTypeUtil.getResourceNamespace(resource));
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

        String contextDescription = "definition of " + resourceType;

        RefinedResourceSchemaImpl rSchema = new RefinedResourceSchemaImpl(originalResourceSchema);

        SchemaHandlingType schemaHandling = resourceType.getSchemaHandling();
        if (schemaHandling != null) {
            parseObjectTypeDefsFromSchemaHandling(rSchema, resourceType, schemaHandling,
                schemaHandling.getObjectType(), null, prismContext, contextDescription);
        }

        parseObjectTypesFromSchema(rSchema, resourceType, prismContext, contextDescription);

        // We need to parse associations and auxiliary object classes in a second pass. We need to have all object classes parsed before correctly setting association
        // targets
        for (RefinedObjectClassDefinition rOcDef : rSchema.getRefinedDefinitions()) {
            ((RefinedObjectClassDefinitionImpl) rOcDef).parseAssociations(rSchema);
            ((RefinedObjectClassDefinitionImpl) rOcDef).parseAuxiliaryObjectClasses(rSchema);
        }

        // We can parse attributes only after we have all the object class info parsed (including auxiliary object classes)
        for (RefinedObjectClassDefinition rOcDef : rSchema.getRefinedDefinitions()) {
            ((RefinedObjectClassDefinitionImpl) rOcDef).parseAttributes(rSchema, contextDescription);
        }

        return rSchema;
    }

//	private static boolean hasAnyObjectTypeDef(SchemaHandlingType schemaHandling) {
//		if (schemaHandling == null) {
//			return false;
//		}
//		if (!schemaHandling.getObjectType().isEmpty()) {
//			return true;
//		}
//		return false;
//	}

    // TODO: The whole parsing is a big mess. TODO: refactor parsing
    private static void parseObjectTypeDefsFromSchemaHandling(RefinedResourceSchemaImpl rSchema, ResourceType resourceType,
                                                              SchemaHandlingType schemaHandling, Collection<ResourceObjectTypeDefinitionType> resourceObjectTypeDefs,
                                                              ShadowKindType impliedKind, PrismContext prismContext, String contextDescription) throws SchemaException {

        if (resourceObjectTypeDefs == null) {
            return;
        }

        Map<ShadowKindType, RefinedObjectClassDefinition> defaults = new HashMap<>();

        for (ResourceObjectTypeDefinitionType accountTypeDefType : resourceObjectTypeDefs) {
            RefinedObjectClassDefinition rOcDef = RefinedObjectClassDefinitionImpl.parse(accountTypeDefType, resourceType, rSchema, impliedKind,
                prismContext, contextDescription);

            if (rOcDef.isDefault()) {
                if (defaults.containsKey(rOcDef.getKind())) {
                    LOGGER.warn("More than one default " + rOcDef.getKind() + " definitions ("
                        + defaults.get(rOcDef.getKind()) + ", " + rOcDef + ") in "+ contextDescription
                        + ".  " + rOcDef + " is automatically assumed to not be the default");
                    ((RefinedObjectClassDefinitionImpl) rOcDef).setDefault(false);
                } else {
                    defaults.put(rOcDef.getKind(), rOcDef);
                }
            }

            rSchema.add(rOcDef);
        }
    }

    public static List<String> getIntentsForKind(RefinedResourceSchema rSchema, ShadowKindType kind) {
        List<String> intents = new ArrayList<>();
        for (ObjectClassComplexTypeDefinition objClassDef : rSchema.getObjectClassDefinitions()) {
            if (objClassDef.getKind() == kind) {
                intents.add(objClassDef.getIntent());
            }
        }

        return intents;

    }


    private static void parseObjectTypesFromSchema(RefinedResourceSchemaImpl rSchema, ResourceType resourceType,
                                                   PrismContext prismContext, String contextDescription) throws SchemaException {

        RefinedObjectClassDefinition rAccountDefDefault = null;
        for (ObjectClassComplexTypeDefinition objectClassDef : rSchema.getOriginalResourceSchema().getObjectClassDefinitions()) {
            QName objectClassname = objectClassDef.getTypeName();
            if (rSchema.getRefinedDefinition(objectClassname) != null) {
                continue;
            }
            RefinedObjectClassDefinition rOcDef = RefinedObjectClassDefinitionImpl.parseFromSchema(objectClassDef, resourceType, rSchema, prismContext,
                "object class " + objectClassname + ", in " + contextDescription);

            if (objectClassDef.getKind() == ShadowKindType.ACCOUNT && rOcDef.isDefault()) {
                if (rAccountDefDefault == null) {
                    rAccountDefDefault = rOcDef;
                } else {
                    throw new SchemaException("More than one default account definitions (" + rAccountDefDefault + ", " + rOcDef + ") in " + contextDescription);
                }
            }

            rSchema.add(rOcDef);
        }
    }
    //endregion

    private void add(RefinedObjectClassDefinition rOcDef) {
        ((ResourceSchemaImpl) resourceSchema).add(rOcDef);
    }

    //region Delegations
    @Override
    public ObjectClassComplexTypeDefinition findObjectClassDefinition(QName objectClassQName) {
        return resourceSchema.findObjectClassDefinition(objectClassQName);
    }

    @Override
    public ObjectClassComplexTypeDefinition findObjectClassDefinition(ShadowKindType kind, String intent) {
        return resourceSchema.findObjectClassDefinition(kind, intent);
    }

    @Override
    public ObjectClassComplexTypeDefinition findDefaultObjectClassDefinition(ShadowKindType kind) {
        return resourceSchema.findDefaultObjectClassDefinition(kind);
    }

    @Override
    public String getNamespace() {
        return resourceSchema.getNamespace();
    }

    @Override
    @NotNull
    public Collection<Definition> getDefinitions() {
        return resourceSchema.getDefinitions();
    }

    @Override
    @NotNull
    public <T extends Definition> List<T> getDefinitions(@NotNull Class<T> type) {
        return resourceSchema.getDefinitions(type);
    }

    @Override
    public PrismContext getPrismContext() {
        return resourceSchema.getPrismContext();
    }

    @Override
    @NotNull
    public Document serializeToXsd() throws SchemaException {
        return resourceSchema.serializeToXsd();
    }

    @Override
    public boolean isEmpty() {
        return resourceSchema.isEmpty();
    }

    @NotNull
    @Override
    public <ID extends ItemDefinition> List<ID> findItemDefinitionsByCompileTimeClass(
        @NotNull Class<?> compileTimeClass, @NotNull Class<ID> definitionClass) {
        return resourceSchema.findItemDefinitionsByCompileTimeClass(compileTimeClass, definitionClass);
    }

    @Nullable
    @Override
    public <TD extends TypeDefinition> TD findTypeDefinitionByCompileTimeClass(@NotNull Class<?> compileTimeClass, @NotNull Class<TD> definitionClass) {
        return resourceSchema.findTypeDefinitionByCompileTimeClass(compileTimeClass, definitionClass);
    }

    @Override
    @Nullable
    public <TD extends TypeDefinition> TD findTypeDefinitionByType(@NotNull QName typeName, @NotNull Class<TD> definitionClass) {
        return resourceSchema.findTypeDefinitionByType(typeName, definitionClass);
    }

    @Override
    public String debugDump() {
        return resourceSchema.debugDump();
    }

    @Override
    public String debugDump(int indent) {
        return resourceSchema.debugDump(indent);
    }

    @Nullable
    @Override
    public <ID extends ItemDefinition> ID findItemDefinitionByType(
        @NotNull QName typeName, @NotNull Class<ID> definitionType) {
        return resourceSchema.findItemDefinitionByType(typeName, definitionType);
    }

    @Override
    @NotNull
    public <ID extends ItemDefinition> List<ID> findItemDefinitionsByElementName(@NotNull QName elementName,
                                                                                 @NotNull Class<ID> definitionClass) {
        return resourceSchema.findItemDefinitionsByElementName(elementName, definitionClass);
    }

    @NotNull
    @Override
    public <TD extends TypeDefinition> Collection<? extends TD> findTypeDefinitionsByType(@NotNull QName typeName,
                                                                                          @NotNull Class<TD> definitionClass) {
        return resourceSchema.findTypeDefinitionsByType(typeName, definitionClass);
    }

    //endregion

    public static void validateRefinedSchema(RefinedResourceSchema refinedSchema, PrismObject<ResourceType> resource) throws SchemaException {

        Set<RefinedObjectClassDefinitionKey> discrs = new HashSet<>();

        for (RefinedObjectClassDefinition rObjectClassDefinition : refinedSchema.getRefinedDefinitions()) {
            QName typeName = rObjectClassDefinition.getTypeName();
            RefinedObjectClassDefinitionKey key = new RefinedObjectClassDefinitionKey(rObjectClassDefinition);
            if (discrs.contains(key)) {
                throw new SchemaException("Duplicate definition of object class " + key + " in resource schema of " + resource);
            }
            discrs.add(key);

            ResourceTypeUtil.validateObjectClassDefinition(rObjectClassDefinition, resource);
        }
    }


}
