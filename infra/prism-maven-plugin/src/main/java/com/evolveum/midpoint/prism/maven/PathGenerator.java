/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.maven;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.schema.PrismSchema;

import javax.xml.namespace.QName;
import java.io.File;

/**
 * @author semancik
 */
public class PathGenerator {

    private final File outDir;

    public PathGenerator(File outDir) {
        this.outDir = outDir;
    }

    public File prepareSchemaOutputFile(PrismSchema schema) {
        File schemaOutDir = getSchemaDir(schema);
        if (!schemaOutDir.exists()) {
            schemaOutDir.mkdirs();
        }
        return new File(schemaOutDir, "index.html");
    }

    private File getSchemaDir(PrismSchema schema) {
        return new File(outDir, getSchemaDirName(schema));
    }

    public File prepareObjectDefinitionOutputFile(PrismSchema schema, PrismObjectDefinition definition) {
        return prepareDefinitionOutputFile(schema, definition, "object");
    }

    public File prepareTypeDefinitionOutputFile(PrismSchema schema, ComplexTypeDefinition definition) {
        return prepareDefinitionOutputFile(schema, definition, "type");
    }

    private File prepareDefinitionOutputFile(PrismSchema schema, Definition definition, String subDirName) {
        File schemaOutputDir = getSchemaDir(schema);
        File subDir = new File(schemaOutputDir, subDirName);
        if (!subDir.exists()) {
            subDir.mkdirs();
        }
        return new File(subDir, definition.getTypeName().getLocalPart() + ".html");
    }

    public String schemaUrl(PrismSchema schema) {
        return getSchemaDirName(schema)+"/index.html";
    }

    public String objectDefinitionUrl(PrismObjectDefinition objDef) {
        return "object/"+getTypeBaseName(objDef)+".html";
    }

    public String typeDefinitionUrl(ComplexTypeDefinition typeDef) {
        return "type/"+getTypeBaseName(typeDef)+".html";
    }

    public String typeDefinitionUrl(QName typeName, String prefixToBase, PrismContext prismContext) {
        String namespaceURI = typeName.getNamespaceURI();
        PrismSchema schema = prismContext.getSchemaRegistry().findSchemaByNamespace(namespaceURI);
        if (schema == null) {
            return null;
        }
        return prefixToBase+"/"+getSchemaDirName(schema)+"/type/"+typeName.getLocalPart()+".html";
    }

    private String getTypeBaseName(Definition def) {
        return def.getTypeName().getLocalPart();
    }

    private String getSchemaDirName(PrismSchema schema) {
        String namespace = schema.getNamespace();
        String filenamedNamespace = toFilename(namespace);
        return filenamedNamespace;
    }

    public File prepareSchemaIndexOutputFile() {
        return new File(outDir, "index.html");
    }

    private String toFilename(String namespace) {
        return namespace.replaceAll("[^a-zA-Z0-9_-]","-");
    }
}
