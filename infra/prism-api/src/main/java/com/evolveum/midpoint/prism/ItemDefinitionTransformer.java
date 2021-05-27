package com.evolveum.midpoint.prism;


public interface ItemDefinitionTransformer {

    <I extends ItemDefinition<?>> I transformItem(ComplexTypeDefinition parentDef, I currentDef);

    <T extends TypeDefinition> T applyValue(ComplexTypeDefinition parentDef, ItemDefinition<?> itemDef, T valueDef);


    public interface TransformableItem {

        void transformDefinition(ComplexTypeDefinition parentDef, ItemDefinitionTransformer transformer);

    }

    public interface TransformableValue {

        void transformDefinition(ComplexTypeDefinition parentDef, ItemDefinition<?> itemDef, ItemDefinitionTransformer transformation);
    }

}
