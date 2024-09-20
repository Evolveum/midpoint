package com.evolveum.midpoint.model.impl.schema.transform;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemDefinitionTransformer;
import com.evolveum.midpoint.prism.TypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainerDefinition;

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
            if (currentDef instanceof ShadowSimpleAttributeDefinition) {
                return (I) TransformablePropertyDefinition.of((ShadowSimpleAttributeDefinition) currentDef);
            }
            if (currentDef instanceof ShadowAttributesContainerDefinition) {
                return (I) TransformableContainerDefinition.of((ShadowAttributesContainerDefinition) currentDef);
            }
            ItemDefinition<?> defFromParent = parentDef.findLocalItemDefinition(currentDef.getItemName());
            if (defFromParent != null) {
                return (I) defFromParent;
            }
        }
        return TransformableItemDefinition.publicFrom(currentDef);
    }
}
