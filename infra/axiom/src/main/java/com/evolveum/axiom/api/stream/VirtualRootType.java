package com.evolveum.axiom.api.stream;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;

public class VirtualRootType implements AxiomTypeDefinition{

    AxiomSchemaContext context;
    private Map<AxiomName, AxiomItemDefinition> roots = new HashMap<>();



    public VirtualRootType(AxiomSchemaContext context2) {
        for(AxiomItemDefinition root : context2.roots()) {
            roots.put(root.name(), root);
        }
    }

    @Override
    public AxiomName name() {
        return AxiomName.axiom("VirtualRoot");
    }

    @Override
    public String documentation() {
        return null;
    }

    @Override
    public Map<AxiomName, AxiomItem<?>> itemMap() {
        return null;
    }

    @Override
    public Map<AxiomName, AxiomItem<?>> infraItems() {
        return null;
    }

    @Override
    public Optional<AxiomItemDefinition> argument() {
        return Optional.empty();
    }

    @Override
    public Optional<AxiomTypeDefinition> superType() {
        return Optional.empty();
    }

    @Override
    public Map<AxiomName, AxiomItemDefinition> itemDefinitions() {
        return roots;
    }

    @Override
    public Collection<AxiomIdentifierDefinition> identifierDefinitions() {
        // TODO Auto-generated method stub
        return null;
    }

    public static AxiomTypeDefinition from(AxiomSchemaContext context2) {
        return new VirtualRootType(context2);
    }


}
