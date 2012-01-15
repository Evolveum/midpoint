/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.init;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class SchemaRegistryFactory {
	
	public static String SCHEMA_DIR_NAME = "schema";
	private static final Trace LOGGER = TraceManager.getTrace(SchemaRegistryFactory.class);
	
	private String schemaDirectoryPath;
	private MidpointConfiguration midpointConfiguration;
	private SchemaRegistry schemaRegistry;
	
	SchemaRegistryFactory() {
		this.schemaDirectoryPath = null;
	}
	
	SchemaRegistryFactory(String schemaDirectoryPath) {
		this.schemaDirectoryPath = schemaDirectoryPath;
	}

	public MidpointConfiguration getMidpointConfiguration() {
		return midpointConfiguration;
	}

	public void setMidpointConfiguration(MidpointConfiguration midpointConfiguration) {
		this.midpointConfiguration = midpointConfiguration;
	}

	public void init() {
		schemaRegistry = new SchemaRegistry();
		File schemaDir = null;
		if (schemaDirectoryPath != null) {
			schemaDir = new File(schemaDirectoryPath);
		} else  if (midpointConfiguration != null) {
			String midpointHomePath = midpointConfiguration.getMidpointHome();
			if (midpointHomePath != null) {
				schemaDir = new File(midpointHomePath, SCHEMA_DIR_NAME);
			}
		}
		if (schemaDir != null && schemaDir.isDirectory()) {
			LOGGER.info("Reading schemas from {}",schemaDir);
			try {
				schemaRegistry.registerMidPointSchemasFromDirectory(schemaDir);
			} catch (FileNotFoundException e) {
				LOGGER.error("Error reading schemas from {}: file not found: {}", new Object[] {
						schemaDir, e.getMessage(), e});
				throw new SystemException("Error reading schemas from "+schemaDir+": "+e.getMessage(),e);
			} catch (SchemaException e) {
				LOGGER.error("Error reading schemas from {}: schema error: {}", new Object[] {
						schemaDir, e.getMessage(), e});
				throw new SystemException("Error reading schemas from "+schemaDir+": "+e.getMessage(),e);
			}
		}
		try {
			schemaRegistry.initialize();
		} catch (SchemaException e) {
			LOGGER.error("Error initializing schema registry: schema error: {}", e.getMessage(), e);
			throw new SystemException("Error initializing schema registry: schema error: "+e.getMessage(),e);
		} catch (SAXException e) {
			LOGGER.error("Error initializing schema registry: XML parsing error: {}", e.getMessage(), e);
			throw new SystemException("Error initializing schema registry: XML parsing error: "+e.getMessage(),e);
		} catch (IOException e) {
			LOGGER.error("Error initializing schema registry: IO error: {}", e.getMessage(), e);
			throw new SystemException("Error initializing schema registry: IO error: "+e.getMessage(),e);
		}
	}
	
	public SchemaRegistry getSchemaRegistry() {
		return schemaRegistry;
	}

}
