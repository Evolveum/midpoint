package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.schemaContext.SchemaContextImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schemaContext.SchemaContext;
import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;
import com.evolveum.midpoint.prism.schemaContext.resolver.Algorithm;
import com.evolveum.midpoint.prism.schemaContext.resolver.SchemaContextResolver;
import com.evolveum.midpoint.prism.schemaContext.resolver.SchemaContextResolverRegistry;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ImmutableSet;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
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

    @Autowired ResourceSchemaRegistry provisioning;

    public void setResourceSchemaRegistry(ResourceSchemaRegistry resourceSchemaRegistry) {
        this.provisioning = resourceSchemaRegistry;
    }

    @PostConstruct
    public void init() {
        init(PrismContext.get());
    }

    public void init(PrismContext prismContext) {
        prismContext.registerValueBasedDefinitionLookup(shadowLookupByKindAndIntent);
        SchemaContextResolverRegistry.register(Algorithm.RESOURCE_OBJECT_CONTEXT_RESOLVER, ResourceObjectContextResolver::new);
        SchemaContextResolverRegistry.register(Algorithm.SHADOW_CONSTRUCTION_CONTEXT_RESOLVER, ShadowConstructionContextResolver::new);

    }

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
                    ResourceObjectDefinition rocd = provisioning.getDefinitionForShadow(fakeShadow.asObjectable());
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

    class ResourceObjectContextResolver implements SchemaContextResolver {

        SchemaContextDefinition schemaContextDefinition;

        public ResourceObjectContextResolver(SchemaContextDefinition schemaContextDefinition) {
            this.schemaContextDefinition = schemaContextDefinition;
        }

        @Override
        public SchemaContext computeContext(PrismValue prismValue) {
            if (prismValue instanceof PrismContainerValue<?> container) {
                ResourceObjectTypeDefinitionType rotd = (ResourceObjectTypeDefinitionType) container.asContainerable();
                ResourceType resource = (ResourceType) container.getRootObjectable();
                if (resource == null) {
                    return null;
                }
                try {
                    ResourceObjectDefinition resourceObjectDefinition = provisioning.getDefinitionForKindIntent(resource.getOid(), rotd.getKind(), rotd.getIntent());
                    if (resourceObjectDefinition != null) {
                        return new SchemaContextImpl(resourceObjectDefinition.toPrismObjectDefinition());
                    }
                } catch (SchemaException e) {
                    // NOOP?
                }
            }
            return new SchemaContextImpl(prismValue.schemaLookup().findObjectDefinitionByCompileTimeClass(ShadowType.class));
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
                ResourceObjectDefinition resourceObjectDefinition = provisioning.getDefinitionForConstruction(construction);
                if (resourceObjectDefinition != null) {
                    return new SchemaContextImpl(resourceObjectDefinition.toPrismObjectDefinition());
                }
            }
            return new SchemaContextImpl(prismValue.schemaLookup().findObjectDefinitionByCompileTimeClass(ShadowType.class));
        }
    }
}
