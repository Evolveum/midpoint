package com.evolveum.midpoint.schema.xjc.schema;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.ComplexTypeDefinitionImpl;
import com.evolveum.midpoint.prism.impl.schema.axiom.AxiomEnabledSchemaRegistry;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.google.common.collect.Iterables;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;

public class PrismCodeGenerator {

    private final PrismContext prism;
    private final File targetDirectory;
    private final JCodeModel model;

    private final SchemaProcessor processor;

    public PrismCodeGenerator(PrismContext prism, File targetDirectory) {
        super();
        this.prism = prism;
        this.targetDirectory = targetDirectory;
        this.model = new JCodeModel();
        processor = new SchemaProcessor(model);
    }

    public void generate() throws JClassAlreadyExistsException {
        AxiomEnabledSchemaRegistry registry = (AxiomEnabledSchemaRegistry) prism.getSchemaRegistry();
        Iterables.filter(registry.getAxiomProvided(), ComplexTypeDefinitionImpl.class).forEach(
                t -> processor.process(t, model));
    }

    public void write() throws IOException {
        model.build(targetDirectory);
    }





}
