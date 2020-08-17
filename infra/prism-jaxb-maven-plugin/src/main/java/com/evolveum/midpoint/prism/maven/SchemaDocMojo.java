/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.maven;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.impl.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.prism.impl.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import java.io.*;

@Mojo(name="prismgen", requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(goal="prism", phase = LifecyclePhase.PACKAGE)
public class SchemaDocMojo extends AbstractMojo {

    private static final String VELOCITY_CONTEXT_VAR_PRISM_CONTEXT = "prismContext";
    private static final String VELOCITY_CONTEXT_VAR_SCHEMA = "schema";
    private static final String VELOCITY_CONTEXT_VAR_SCHEMA_REGISTRY = "schemaRegistry";
    private static final String VELOCITY_CONTEXT_VAR_PATH = "path";
    private static final String VELOCITY_CONTEXT_VAR_DEFINITION = "definition";
    private static final String VELOCITY_CONTEXT_VAR_PREFIX_TO_BASE = "prefixToBase";

    private static final String TEMPLATE_SCHEMA_INDEX_NAME = "schema-index.vm";
    private static final String TEMPLATE_SCHEMA_NAME = "schema.vm";
    private static final String TEMPLATE_OBJECT_DEFINITION_NAME = "object-definition.vm";
    private static final String TEMPLATE_COMPLEX_TYPE_DEFINITION_NAME = "complex-type-definition.vm";

    @Parameter
    private File[] schemaFiles;

    @Parameter
    private File[] catalogFiles;

    @Parameter(defaultValue="${project.build.directory}", required=true)
    private File buildDir;

    @Parameter(defaultValue="${project.build.directory}/schemadoc", required=true)
    private File destDir;

    @Parameter(defaultValue="src/main/schemadoc/templates", required=true)
    private File templateDir;

    @Parameter(defaultValue="src/main/schemadoc/resources")
    private File resourcesDir;

    @Parameter(defaultValue="${project}")
    private org.apache.maven.project.MavenProject project;

    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    @Parameter(defaultValue="${project.build.finalName}")
    private String finalName;

    @Component
    private MavenProjectHelper projectHelper;

    @Component(role=Archiver.class, hint="zip")
    private ZipArchiver zipArchiver;

    private String getTemplateDirName() {
        return templateDir.getAbsolutePath();
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug( "SchemaDoc plugin started" );

        PrismContext prismContext = createInitializedPrismContext();

        File outDir = initializeOutDir();
        PathGenerator pathGenerator = new PathGenerator(outDir);

        VelocityEngine velocityEngine = createVelocityEngine();

        SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
        try {
            renderSchemaIndex(schemaRegistry, prismContext, velocityEngine, pathGenerator);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(),e);
        }
        for (PrismSchema schema: schemaRegistry.getSchemas()) {
            try {
                renderSchema(schema, prismContext, velocityEngine, pathGenerator);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(),e);
            }
        }

        try {
            copyResources(outDir);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(),e);
        }

        File archiveFile = null;
        try {
            archiveFile = generateArchive(outDir, finalName + "-schemadoc.zip");
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(),e);
        } catch (ArchiverException e) {
            throw new MojoExecutionException(e.getMessage(),e);
        }
        projectHelper.attachArtifact(project, "zip", "schemadoc", archiveFile);

        getLog().debug( "SchemaDoc plugin finished" );
    }

    private void renderSchemaIndex(SchemaRegistry schemaRegistry, PrismContext prismContext, VelocityEngine velocityEngine, PathGenerator pathGenerator) throws IOException {
        getLog().debug("Rendering schema index");
        VelocityContext velocityContext = new VelocityContext();
        populateVelocityContextBase(velocityContext, prismContext, pathGenerator, null, ".");
        velocityContext.put(VELOCITY_CONTEXT_VAR_SCHEMA_REGISTRY, schemaRegistry);

        Template template = velocityEngine.getTemplate(TEMPLATE_SCHEMA_INDEX_NAME);

        Writer writer = new FileWriter(pathGenerator.prepareSchemaIndexOutputFile());
        template.merge(velocityContext, writer);
        writer.close();
    }

    private void renderSchema(PrismSchema schema, PrismContext prismContext, VelocityEngine velocityEngine, PathGenerator pathGenerator) throws IOException {
        getLog().debug("Processing schema: "+schema);
        VelocityContext velocityContext = new VelocityContext();
        populateVelocityContextBase(velocityContext, prismContext, pathGenerator, schema, "..");

        Template template = velocityEngine.getTemplate(TEMPLATE_SCHEMA_NAME);

        Writer writer = new FileWriter(pathGenerator.prepareSchemaOutputFile(schema));
        template.merge(velocityContext, writer);
        writer.close();

        // Object Definitions
        for (PrismObjectDefinition objectDefinition: schema.getObjectDefinitions()) {
            renderObjectDefinition(objectDefinition, schema, prismContext, velocityEngine, pathGenerator);
        }

        // Types
        for (ComplexTypeDefinition typeDefinition : schema.getComplexTypeDefinitions()) {
            renderComplexTypeDefinition(typeDefinition, schema, prismContext, velocityEngine, pathGenerator);
        }

    }

    private void renderObjectDefinition(PrismObjectDefinition objectDefinition, PrismSchema schema, PrismContext prismContext, VelocityEngine velocityEngine, PathGenerator pathGenerator) throws IOException {
        getLog().debug("  Processing object definition: "+objectDefinition);

        VelocityContext velocityContext = new VelocityContext();
        populateVelocityContextBase(velocityContext, prismContext, pathGenerator, schema, "../..");
        velocityContext.put(VELOCITY_CONTEXT_VAR_DEFINITION, objectDefinition);

        Template template = velocityEngine.getTemplate(TEMPLATE_OBJECT_DEFINITION_NAME);

        Writer writer = new FileWriter(pathGenerator.prepareObjectDefinitionOutputFile(schema, objectDefinition));
        template.merge(velocityContext, writer);
        writer.close();
    }

    private void renderComplexTypeDefinition(ComplexTypeDefinition typeDefinition, PrismSchema schema, PrismContext prismContext, VelocityEngine velocityEngine, PathGenerator pathGenerator) throws IOException {
        getLog().debug("  Processing complex type definition: "+typeDefinition);

        VelocityContext velocityContext = new VelocityContext();
        populateVelocityContextBase(velocityContext, prismContext, pathGenerator, schema, "../..");
        velocityContext.put(VELOCITY_CONTEXT_VAR_DEFINITION, typeDefinition);

        Template template = velocityEngine.getTemplate(TEMPLATE_COMPLEX_TYPE_DEFINITION_NAME);

        Writer writer = new FileWriter(pathGenerator.prepareTypeDefinitionOutputFile(schema, typeDefinition));
        template.merge(velocityContext, writer);
        writer.close();
    }

    private void populateVelocityContextBase(VelocityContext velocityContext, PrismContext prismContext, PathGenerator pathGenerator,
                                             PrismSchema schema, String prefixToBase) {
        if (schema != null) {
            velocityContext.put(VELOCITY_CONTEXT_VAR_SCHEMA, schema);
        }
        velocityContext.put(VELOCITY_CONTEXT_VAR_PRISM_CONTEXT, prismContext);
        velocityContext.put(VELOCITY_CONTEXT_VAR_PATH, pathGenerator);
        velocityContext.put(VELOCITY_CONTEXT_VAR_PREFIX_TO_BASE, prefixToBase);
    }

    private File initializeOutDir() throws MojoFailureException {
        getLog().debug("Output dir: "+destDir);
        if ( destDir.exists() && !destDir.isDirectory() ) {
            throw new MojoFailureException("Destination directory is not a directory: "+destDir);
        }
        if (destDir.exists() && !destDir.canWrite()) {
            throw new MojoFailureException("Destination directory is not writable: "+destDir);
        }
        destDir.mkdirs();
        return destDir;
    }

    private PrismContext createInitializedPrismContext() throws MojoFailureException {
        try {
            SchemaRegistryImpl schemaRegistry = createSchemaRegistry();

            for (File schemaFile: schemaFiles) {
                getLog().debug("SchemaDoc: registering schema file: "+schemaFile);
                if (!schemaFile.exists()) {
                    throw new MojoFailureException("Schema file "+schemaFile+" does not exist");
                }
                schemaRegistry.registerPrismSchemaFile(schemaFile);
            }

            if (catalogFiles != null && catalogFiles.length > 0) {
                for (File catalogFile : catalogFiles) {
                    getLog().debug("SchemaDoc: using catalog file: " + catalogFile);
                    if (!catalogFile.exists()) {
                        throw new IOException("Catalog file '" + catalogFile + "' does not exist.");
                    }
                }
                schemaRegistry.setCatalogFiles(catalogFiles);
            }

            PrismContextImpl context = PrismContextImpl.create(schemaRegistry);
            context.setDefinitionFactory(new SchemaDefinitionFactory());
            context.initialize();

            return context;

        } catch (SchemaException e) {
            handleFailure(e);
            // never reached
            return null;
        } catch (FileNotFoundException e) {
            handleFailure(e);
            // never reached
            return null;
        } catch (SAXException e) {
            handleFailure(e);
            // never reached
            return null;
        } catch (IOException e) {
            handleFailure(e);
            // never reached
            return null;
        }
    }

    private void handleFailure(Exception e) throws MojoFailureException {
        e.printStackTrace();
        throw new MojoFailureException(e.getMessage());
    }

    @NotNull
    private SchemaRegistryImpl createSchemaRegistry() throws SchemaException {
        SchemaRegistryImpl schemaRegistry = new SchemaRegistryImpl();
        schemaRegistry.setNamespacePrefixMapper(new GlobalDynamicNamespacePrefixMapper());
        return schemaRegistry;
    }

    private VelocityEngine createVelocityEngine() {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty("resource.loader","file");
        ve.setProperty("file.resource.loader.class","org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        ve.setProperty("file.resource.loader.path", getTemplateDirName());
        ve.setProperty("file.resource.loader.cache","true");
        ve.setProperty("directive.set.null.allowed","true");

        ve.init();
        return ve;
    }

    private void copyResources(File outDir) throws IOException {
        if (resourcesDir.exists()) {
            MiscUtil.copyDirectory(resourcesDir, outDir);
        }
    }


    private File generateArchive(File outDir, String archiveFilename) throws IOException, ArchiverException {
        File zipFile = new File(buildDir, archiveFilename);
        if (zipFile.exists()) {
            zipFile.delete();
        }

        zipArchiver.addDirectory(outDir);
        zipArchiver.setDestFile(zipFile);
        zipArchiver.createArchive();

        return zipFile;
    }

}
