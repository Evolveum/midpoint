package com.evolveum.midpoint.schema.xjc.schema;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;

public class PrismCodeGenerator {

    private final PrismContext prism;
    private final File targetDirectory;
    private final JCodeModel model;

    private final SchemaProcessor processor = new SchemaProcessor();

    public PrismCodeGenerator(PrismContext prism, File targetDirectory) {
        super();
        this.prism = prism;
        this.targetDirectory = targetDirectory;
        this.model = new JCodeModel();
    }

    public void generate() throws JClassAlreadyExistsException {

        for(PrismSchema schema : prism.getSchemaRegistry().getSchemas()) {
            processor.process(schema, model);
        }
    }

    public void write() throws IOException {
        model.build(targetDirectory);
    }





}
