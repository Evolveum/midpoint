package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.model.impl.schema.transform.TransformableContainerDefinition;
import com.evolveum.midpoint.model.impl.schema.transform.TransformableObjectDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.schemaContext.SchemaContextImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schemaContext.SchemaContext;
import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;
import com.evolveum.midpoint.prism.schemaContext.resolver.Algorithm;
import com.evolveum.midpoint.prism.schemaContext.resolver.ContextResolverFactory;
import com.evolveum.midpoint.prism.schemaContext.resolver.SchemaContextResolver;
import com.evolveum.midpoint.prism.schemaContext.resolver.SchemaContextResolverRegistry;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ImmutableSet;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class ValueBasedDefinitionLookupsImpl {


    private static final Set<ItemPath> RESOURCE_KIND_INTENT_PATHS = ImmutableSet.of(ShadowType.F_RESOURCE_REF, ShadowType.F_KIND, ShadowType.F_INTENT);
    private static final Set<ItemPath> RESOURCE_OBJECTCLASS_PATHS = ImmutableSet.of(ShadowType.F_RESOURCE_REF, ShadowType.F_OBJECT_CLASS);

    /**
     * Value Lookup helper for Shadow
     *
     *
     **/
    private ValueBasedDefinitionLookupHelper shadowLookupByKindAndIntent = new ShadowLookup(RESOURCE_KIND_INTENT_PATHS);
    private ValueBasedDefinitionLookupHelper shadowLookupByObjectClass = new ShadowLookup(RESOURCE_OBJECTCLASS_PATHS);

    private class ShadowLookup implements ValueBasedDefinitionLookupHelper {

        private final Set<ItemPath> paths;

        public ShadowLookup(Set<ItemPath> paths) {
            this.paths = paths;
        }

        @Override
        public @NotNull QName baseTypeName() {
            return ShadowType.COMPLEX_TYPE;
        }

        @Override
        public @NotNull Set<ItemPath> valuePaths() {
            return paths;
        }

        @Nullable
        @Override
        public ComplexTypeDefinition findComplexTypeDefinition(QName typeName, Map<ItemPath, PrismValue> hintValues) {
            var resourceValue = hintValues.get(ShadowType.F_RESOURCE_REF);
            var result = lookupTask.getResult().createSubresult("ValueBasedDefinitionLookupsImpl.findComplexTypeDefinition");
            if (resourceValue instanceof PrismReferenceValue resourceRef) {
                var oid = resourceRef.getOid();
                try {
                    var fakeShadow = PrismContext.get().createObject(ShadowType.class);
                    for (var hint : hintValues.entrySet()) {
                        var value = hint.getValue();
                        if (value instanceof PrismPropertyValue propValue) {
                            fakeShadow.findOrCreateProperty(hint.getKey()).add(propValue.clone());
                        } else if (value instanceof PrismReferenceValue refValue) {
                            fakeShadow.findOrCreateReference(hint.getKey()).add(refValue.clone());
                        }
                    }

                    var resource = provisioning.getObject(ResourceType.class, oid, null, lookupTask, result);
                    ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchema(resource);
                    ResourceObjectDefinition rocd = resourceSchema.findDefinitionForShadow(fakeShadow.asObjectable());
                    if (rocd != null) {
                        return rocd.getPrismObjectDefinition().getComplexTypeDefinition();
                    }
                } catch (Exception e) {
                    // Suppress and return null?
                    return null;
                }
            }
            // We did not successfully found schema, do not override
            return null;


        }
    };


    @Autowired ProvisioningService provisioning;

    @Autowired TaskManager taskManager;

    // FIXME: This is antipattern, but prism does not have tasks associated and parsing APIs do not have OperationResults
    private Task lookupTask;

    @PostConstruct
    public void init() {
        PrismContext.get().registerValueBasedDefinitionLookup(shadowLookupByKindAndIntent);
        this.lookupTask = taskManager.createTaskInstance("system-resource-lookup-for-queries");
        SchemaContextResolverRegistry.register(Algorithm.RESOURCE_OBJECT_CONTEXT_RESOLVER, ResourceObjectContextResolver::new);
        SchemaContextResolverRegistry.register(Algorithm.SHADOW_CONSTRUCTION_CONTEXT_RESOLVER, ShadowConstructionContextResolver::new);
    }

    class ResourceObjectContextResolver implements SchemaContextResolver {

        SchemaContextDefinition schemaContextDefinition;

        public ResourceObjectContextResolver(SchemaContextDefinition schemaContextDefinition) {
            this.schemaContextDefinition = schemaContextDefinition;
        }

        @Override
        public SchemaContext computeContext(PrismValue prismValue) {
            if (prismValue instanceof PrismContainerValue<?> container) {
                ResourceObjectTypeDefinitionType resourceObjectDefinitionType = (ResourceObjectTypeDefinitionType) container.asContainerable();
                ResourceType resource = (ResourceType) container.getRootObjectable();

                ShadowType shadowType = new ShadowType();
                shadowType.resourceRef(resource.getOid(), ResourceType.COMPLEX_TYPE);
                shadowType.setKind(resourceObjectDefinitionType.getKind());
                shadowType.setIntent(resourceObjectDefinitionType.getIntent());

                try {
                    var result = lookupTask.getResult().createSubresult("ValueBasedDefinitionLookupsImpl.findComplexTypeDefinition");
                    var resourceObj = provisioning.getObject(ResourceType.class, resource.getOid(), null, lookupTask, result);
                    ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchema(resourceObj);
                    ResourceObjectDefinition resourceObjectDefinition = resourceSchema.findDefinitionForShadow(shadowType);
                    if (resourceObjectDefinition != null) {
                        return new SchemaContextImpl(resourceObjectDefinition.toPrismObjectDefinition());
                    }
                } catch (Exception e) {
                    return null;
                }
            }

            return null;
        }
    }

    class ShadowConstructionContextResolver implements SchemaContextResolver {

        SchemaContextDefinition schemaContextDefinition;

        public ShadowConstructionContextResolver(SchemaContextDefinition schemaContextDefinition) {
            this.schemaContextDefinition = schemaContextDefinition;
        }

        @Override
        public SchemaContext computeContext(PrismValue prismValue) {
            if (prismValue instanceof PrismContainerValue<?> container) {
                ConstructionType construction = (ConstructionType) container.asContainerable();

                ShadowType shadowType = new ShadowType();
                shadowType.resourceRef(construction.getResourceRef().getOid(), ConstructionType.COMPLEX_TYPE);
                shadowType.setKind(construction.getKind());

                if (construction.getIntent() != null) {
                    shadowType.setIntent(construction.getIntent());
                } else {
                    shadowType.setIntent("default");
                }

                try {
                    var result = lookupTask.getResult().createSubresult("ValueBasedDefinitionLookupsImpl.findComplexTypeDefinition");
                    var resourceObj = provisioning.getObject(ResourceType.class, construction.getResourceRef().getOid(), null, lookupTask, result);
                    ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchema(resourceObj);
                    ResourceObjectDefinition resourceObjectDefinition = resourceSchema.findDefinitionForShadow(shadowType);
                    if (resourceObjectDefinition != null) {
                        return new SchemaContextImpl(resourceObjectDefinition.toPrismObjectDefinition());
                    }
                } catch (Exception e) {
                    return null;
                }
            }

            return null;
        }
    }
}


