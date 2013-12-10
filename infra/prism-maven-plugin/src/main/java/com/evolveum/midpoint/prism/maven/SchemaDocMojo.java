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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @goal schemadoc
 * @requiresDependencyResolution compile
 * @phase package
 */
//@Mojo(name="schemadoc")
public class SchemaDocMojo extends AbstractMojo {
	
	/**
	 * @parameter
	 */
	private File[] schemaFiles;
	
	/**
	 * @parameter default-value="${project.build.directory}/schemadoc" required=true
	 */
	protected File destDir;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info( "SchemaDoc plugin started" );
        
        try {
	        SchemaRegistry schemaRegistry = createSchemaRegistry();
	        
	        for (File schemaFile: schemaFiles) {
	        	getLog().info("SchemaDoc: registering schema file: "+schemaFile);
	        	if (!schemaFile.exists()) {
	        		throw new MojoFailureException("Schema file "+schemaFile+" does not exist");
	        	}
	        	schemaRegistry.registerPrismSchemaFile(schemaFile);
	        }
	        
	        PrismContext context = PrismContext.create(schemaRegistry);
	        context.setDefinitionFactory(new SchemaDefinitionFactory());
	        context.initialize();
	        
	        for (PrismSchema schema: schemaRegistry.getSchemas()) {
	        	getLog().info("Schema: "+schema);
	        }
	        
        } catch (SchemaException e) {
        	throw new MojoFailureException(e.getMessage());
        } catch (FileNotFoundException e) {
        	throw new MojoFailureException(e.getMessage());
        } catch (SAXException e) {
        	throw new MojoFailureException(e.getMessage());
		} catch (IOException e) {
			throw new MojoFailureException(e.getMessage());
		}
        
        getLog().info("Output dir: "+destDir);
        if ( destDir.exists() && !destDir.isDirectory() ) {
        	throw new MojoFailureException("Destination directory is not a directory: "+destDir);
        }
        if (destDir.exists() && !destDir.canWrite()) {
        	throw new MojoFailureException("Destination directory is not writable: "+destDir);
        }
        destDir.mkdirs();
        
        getLog().info( "SchemaDoc plugin finished" );
    }
	
	private SchemaRegistry createSchemaRegistry() throws SchemaException {
		SchemaRegistry schemaRegistry = new SchemaRegistry();
		schemaRegistry.setNamespacePrefixMapper(new GlobalDynamicNamespacePrefixMapper());
		return schemaRegistry;
	}

}
