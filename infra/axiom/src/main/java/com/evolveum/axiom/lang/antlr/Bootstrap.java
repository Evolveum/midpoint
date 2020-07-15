package com.evolveum.axiom.lang.antlr;

import java.util.List;
import java.util.Set;

import com.evolveum.axiom.lang.antlr.AxiomParser.ItemContext;
import com.google.common.collect.ImmutableSet;

public class Bootstrap {

    public static Set<String> builtInTypes() {
        try {
            ImmutableSet.Builder<String> typeNames = ImmutableSet.builder();
            AxiomModelStatementSource types = AxiomModelStatementSource.from("axiom-types.axiom", Bootstrap.class.getResourceAsStream("/axiom-types.axiom"));
            List<ItemContext> rootItems = types.root().itemValue().item();
            for(ItemContext rootItem : rootItems) {
                if(rootItem.itemName().dataName().prefixedName().localName().getText().equals("type")) {
                    typeNames.add(rootItem.itemValue().argument().prefixedName().localName().getText());
                }
            }

        return typeNames.build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load basic types",e);
        }
    }

}
