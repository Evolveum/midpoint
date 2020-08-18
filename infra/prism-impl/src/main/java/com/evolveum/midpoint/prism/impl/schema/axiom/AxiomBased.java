package com.evolveum.midpoint.prism.impl.schema.axiom;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomNamedDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.midpoint.prism.MutableComplexTypeDefinition;
import com.evolveum.midpoint.prism.MutableDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.ComplexTypeDefinitionImpl;

public class AxiomBased {

    public static @NotNull QName qName(AxiomName name) {
        return new QName(name.namespace(), name.localName());
    }

    MutableComplexTypeDefinition complexTypeDefinition(PrismContext context, AxiomTypeDefinition source) {
        QName typeName = qName(source.name());
        ComplexTypeDefinitionImpl target = new ComplexTypeDefinitionImpl(typeName, context);
        fillFromAxiom(target, source);
        return target;
    }

    private void fillFromAxiom(ComplexTypeDefinitionImpl target, AxiomTypeDefinition source) {


        fillDefinitinon(target, source);


    }

    private void fillDefinitinon(MutableDefinition target, AxiomNamedDefinition source) {
        // TODO Auto-generated method stub
        // target.setTypeName(typeName);
        // target.setAnnotation(qname, value);
        // target.setDeprecated(deprecated);
        // target.setDisplayName(displayName);
        // target.setDisplayOrder(displayOrder);
        // target.setDocumentation(value);
        // target.setEmphasized(emphasized);
        // target.setEmphasized(emphasized);
        // target.setHelp(help);
        // target.setProcessing(processing);
        // target.setRuntimeSchema(value);
    }

}
