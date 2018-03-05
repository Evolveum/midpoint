/*
 * Copyright (c) 2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.maven;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismContextImpl;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import java.io.*;

/**
 * @goal schemadoc
 * @requiresDependencyResolution compile
 * @phase package
 */
//@Mojo(name="schemadoc")
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

    /**
	 * @parameter
	 */
	private File[] schemaFiles;

	/**
	 * @parameter
     */
    private File[] catalogFiles;

    /**
     * @parameter default-value="${project.build.directory}" required=true
     */
    private File buildDir;

	/**
	 * @parameter default-value="${project.build.directory}/schemadoc" required=true
	 */
    private File destDir;

    /**
     * @parameter default-value="src/main/schemadoc/templates" required=true
     */
    private File templateDir;

    /**
     * @parameter default-value="src/main/schemadoc/resources"
     */
    private File resourcesDir;

    /** @parameter default-value="${project}" */
    private org.apache.maven.project.MavenProject project;

    /** @parameter */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /** @parameter default-value="${project.build.finalName}" */
    private String finalName;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /** @component role="org.codehaus.plexus.archiver.Archiver" roleHint="zip" */
    private ZipArchiver zipArchiver;

    private String getTemplateDirName() {
        return templateDir.getAbsolutePath();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info( "SchemaDoc plugin started" );

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

        getLog().info( "SchemaDoc plugin finished" );
    }

    private void renderSchemaIndex(SchemaRegistry schemaRegistry, PrismContext prismContext, VelocityEngine velocityEngine, PathGenerator pathGenerator) throws IOException {
        getLog().info("Rendering schema index");
        VelocityContext velocityContext = new VelocityContext();
        populateVelocityContextBase(velocityContext, prismContext, pathGenerator, null, ".");
        velocityContext.put(VELOCITY_CONTEXT_VAR_SCHEMA_REGISTRY, schemaRegistry);

        Template template = velocityEngine.getTemplate(TEMPLATE_SCHEMA_INDEX_NAME);

        Writer writer = new FileWriter(pathGenerator.prepareSchemaIndexOutputFile());
        template.merge(velocityContext, writer);
        writer.close();
    }

    private void renderSchema(PrismSchema schema, PrismContext prismContext, VelocityEngine velocityEngine, PathGenerator pathGenerator) throws IOException {
        getLog().info("Processing schema: "+schema);
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
        getLog().info("  Processing object definition: "+objectDefinition);

        VelocityContext velocityContext = new VelocityContext();
        populateVelocityContextBase(velocityContext, prismContext, pathGenerator, schema, "../..");
        velocityContext.put(VELOCITY_CONTEXT_VAR_DEFINITION, objectDefinition);

        Template template = velocityEngine.getTemplate(TEMPLATE_OBJECT_DEFINITION_NAME);

        Writer writer = new FileWriter(pathGenerator.prepareObjectDefinitionOutputFile(schema, objectDefinition));
        template.merge(velocityContext, writer);
        writer.close();
    }

    private void renderComplexTypeDefinition(ComplexTypeDefinition typeDefinition, PrismSchema schema, PrismContext prismContext, VelocityEngine velocityEngine, PathGenerator pathGenerator) throws IOException {
        getLog().info("  Processing complex type definition: "+typeDefinition);

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
        getLog().info("Output dir: "+destDir);
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
                getLog().info("SchemaDoc: registering schema file: "+schemaFile);
                if (!schemaFile.exists()) {
                    throw new MojoFailureException("Schema file "+schemaFile+" does not exist");
                }
                schemaRegistry.registerPrismSchemaFile(schemaFile);
            }

            if (catalogFiles != null && catalogFiles.length > 0) {
                for (File catalogFile : catalogFiles) {
                    getLog().info("SchemaDoc: using catalog file: " + catalogFile);
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
