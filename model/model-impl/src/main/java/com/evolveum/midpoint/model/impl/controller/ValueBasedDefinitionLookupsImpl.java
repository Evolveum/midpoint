package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.model.impl.schema.transform.TransformableObjectDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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

    /**
     * Value Lookup helper for Shadow
     *
     *
     **/
    private ValueBasedDefinitionLookupHelper shadowLookupByKindAndIntent = new ValueBasedDefinitionLookupHelper() {

        @Override
        public @NotNull QName baseTypeName() {
            return ShadowType.COMPLEX_TYPE;
        }

        @Override
        public @NotNull Set<ItemPath> valuePaths() {
            return RESOURCE_KIND_INTENT_PATHS;
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
    }
}
