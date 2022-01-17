package com.evolveum.midpoint.model.impl.schema.transform;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemDefinitionTransformer;
import com.evolveum.midpoint.prism.TypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;

public class DefinitionsToTransformable implements ItemDefinitionTransformer {

    @Override
    public <T extends TypeDefinition> T applyValue(ComplexTypeDefinition parentDef, ItemDefinition<?> itemDef,
            T valueDef) {
        if (valueDef instanceof ComplexTypeDefinition) {
            if (itemDef instanceof TransformableContainerDefinition) {
                TransformableComplexTypeDefinition maybe = ((TransformableContainerDefinition) itemDef).getComplexTypeDefinition();
                if (valueDef.getTypeName().equals(maybe.getTypeName())) {
                    return (T) maybe;
                }
            }
            return (T) TransformableComplexTypeDefinition.from((ComplexTypeDefinition) valueDef);
        }
        return valueDef;
    }


    @Override
    public <I extends ItemDefinition<?>> I transformItem(ComplexTypeDefinition parentDef, I currentDef) {
        if (currentDef == null || currentDef instanceof TransformableItemDefinition) {
            return currentDef;
        }
        // Parent is transformable
        if (parentDef instanceof TransformableComplexTypeDefinition) {
            if (currentDef instanceof ResourceAttributeDefinition) {
                return (I) TransformablePropertyDefinition.of((ResourceAttributeDefinition) currentDef);
            }
            if (currentDef instanceof ResourceAttributeContainerDefinition) {
                return (I) TransformableContainerDefinition.of((ResourceAttributeContainerDefinition) currentDef);
            }
            ItemDefinition<?> defFromParent = parentDef.findLocalItemDefinition(currentDef.getItemName());
            if (defFromParent != null) {
                return (I) defFromParent;
            }
        }
        return TransformableItemDefinition.publicFrom(currentDef);
    }
}
