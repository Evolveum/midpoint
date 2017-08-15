/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.model.impl.importer;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.migrator.Migrator;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.PrismSchemaImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Extension of validator used to import objects to the repository.
 * <p/>
 * In addition to validating the objects the importer also tries to resolve the
 * references and may also do other repository-related stuff.
 *
 * @author Radovan Semancik
 */
@Component
public class ObjectImporter {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectImporter.class);
//    private static final String OPERATION_RESOLVE_REFERENCE = ObjectImporter.class.getName()
//            + ".resolveReference";
    private static final String OPERATION_VALIDATE_DYN_SCHEMA = ObjectImporter.class.getName()
            + ".validateDynamicSchema";


    @Autowired(required = true)
    private Protector protector;
    @Autowired(required = true)
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired(required = true)
    private PrismContext prismContext;
    @Autowired(required = true)
    private TaskManager taskManager;
    @Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
    @Autowired(required = true)
    private ModelService modelService;
    @Autowired(required = true)
    private Clock clock;
    
    private Migrator migrator = new Migrator();

    // this method is responsible for computing the operation result!
    public void importObjects(InputStream input, String language, ImportOptionsType options, Task task, OperationResult parentResult) {
        importObjectsInternal(input, language, options, true, task, parentResult);
    }

    // TODO provide "noRaw" option in ImportOptionsType?
//    public void importObjectsNotRaw(InputStream input, final ImportOptionsType options, final Task task, final OperationResult parentResult) {
//        importObjectsInternal(input, options, false, task, parentResult);
//    }

    private void importObjectsInternal(InputStream input, String language, final ImportOptionsType options,
		    final boolean raw, final Task task, final OperationResult parentResult) {

    	if (options != null) {
		    if (BooleanUtils.isTrue(options.isSummarizeErrors())) {
			    parentResult.setSummarizeErrors(true);
		    }
		    if (BooleanUtils.isTrue(options.isSummarizeSucceses())) {
			    parentResult.setSummarizeSuccesses(true);
		    }
	    }

	    int stopAfterErrors = options != null && options.getStopAfterErrors() != null ?
			    options.getStopAfterErrors() : 0;

	    if (!PrismContext.LANG_XML.equals(language)) {
		    AtomicInteger index = new AtomicInteger(0);
		    AtomicInteger errors = new AtomicInteger(0);
		    AtomicInteger successes = new AtomicInteger(0);
		    PrismParser.ObjectHandler handler = new PrismParser.ObjectHandler() {
			    @Override
			    public boolean handleData(PrismObject<?> object) {
				    OperationResult objectResult = parentResult.createSubresult(OperationConstants.IMPORT_OBJECT);
				    objectResult.addContext("objectNumber", index.incrementAndGet());
				    importParsedObject(object, null, objectResult, options, task, raw);
				    objectResult.computeStatusIfUnknown();

				    if (objectResult.isAcceptable()) {
				    	successes.incrementAndGet();
				    } else {
				    	errors.incrementAndGet();
				    }
				    return stopAfterErrors == 0 || errors.get() < stopAfterErrors;
			    }

			    @Override
			    public boolean handleError(Throwable t) {
				    OperationResult objectResult = parentResult.createSubresult(OperationConstants.IMPORT_OBJECT);
				    objectResult.addContext("objectNumber", index.incrementAndGet());
				    objectResult.recordPartialError("Couldn't parse object", t);
			    	errors.incrementAndGet();
			    	return stopAfterErrors == 0 || errors.get() < stopAfterErrors;
			    }
		    };
		    try {
			    prismContext.parserFor(input).language(language).parseObjectsIteratively(handler);
		    } catch (SchemaException|IOException e) {
			    parentResult.recordFatalError("Couldn't parse objects to be imported: " + e.getMessage(), e);
			    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse objects to be imported", e);
			    return;
		    }
		    parentResult.computeStatus(errors.get() + " errors, " + successes.get() + " passed");
	    } else {
		    EventHandler handler = new EventHandler() {

			    @Override
			    public EventResult preMarshall(Element objectElement, Node postValidationTree, OperationResult objectResult) {
				    return EventResult.cont();
			    }

			    @Override
			    public <T extends Objectable> EventResult postMarshall(PrismObject<T> prismObjectObjectable,
					    Element objectElement, OperationResult objectResult) {
				    return importParsedObject(prismObjectObjectable, objectElement, objectResult, options, task, raw);
			    }

			    @Override
			    public void handleGlobalError(OperationResult currentResult) {
				    // No reaction
			    }
		    };

		    Validator validator = new Validator(prismContext, handler);
		    validator.setVerbose(true);
		    if (options != null) {
			    validator.setValidateSchema(BooleanUtils.isTrue(options.isValidateStaticSchema()));
		    }
		    validator.setStopAfterErrors(stopAfterErrors);
		    validator.validate(input, parentResult, OperationConstants.IMPORT_OBJECT);
	    }
    }

	@NotNull
	private <T extends Objectable> EventResult importParsedObject(PrismObject<T> prismObjectObjectable, Element objectElement,
			OperationResult objectResult, ImportOptionsType options, Task task, boolean raw) {
		LOGGER.debug("Importing object {}", prismObjectObjectable);

		T objectable = prismObjectObjectable.asObjectable();
		if (!(objectable instanceof ObjectType)) {
			String message = "Cannot process type "+objectable.getClass()+" as it is not a subtype of "+ObjectType.class;
			objectResult.recordFatalError(message);
		    LOGGER.error("Import of object {} failed: {}",
		            new Object[]{prismObjectObjectable, message});
		    return EventResult.skipObject(message);
		}
		PrismObject<? extends ObjectType> object = (PrismObject<? extends ObjectType>) prismObjectObjectable;

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("IMPORTING object:\n{}", object.debugDump());
		}

		object = migrator.migrate(object);

		Utils.resolveReferences(object, repository,
				(options == null || options.isReferentialIntegrity() == null) ? false : options.isReferentialIntegrity(),
		        false, EvaluationTimeType.IMPORT, false, prismContext, objectResult);

		objectResult.computeStatus();
		if (!objectResult.isAcceptable()) {
			return EventResult.skipObject(objectResult.getMessage());
		}

		generateIdentifiers(object, repository,  objectResult);

		objectResult.computeStatus();
		if (!objectResult.isAcceptable()) {
			return EventResult.skipObject(objectResult.getMessage());
		}

		if (options != null && BooleanUtils.isTrue(options.isValidateDynamicSchema())) {
		    validateWithDynamicSchemas(object, repository, objectResult);
		}

		objectResult.computeStatus();
		if (!objectResult.isAcceptable()) {
			return EventResult.skipObject(objectResult.getMessage());
		}

		if (options != null && BooleanUtils.isTrue(options.isEncryptProtectedValues())) {
			OperationResult opResult = objectResult.createMinorSubresult(ObjectImporter.class.getName()+".encryptValues");
			try {
				CryptoUtil.encryptValues(protector, object);
				opResult.recordSuccess();
			} catch (EncryptionException e) {
				opResult.recordFatalError(e);
			}
		}

		if (options == null || !BooleanUtils.isTrue(options.isKeepMetadata())) {
			MetadataType metaData = new MetadataType();
				String channel = SchemaConstants.CHANNEL_OBJECT_IMPORT_URI;
				metaData.setCreateChannel(channel);
				metaData.setCreateTimestamp(clock.currentTimeXMLGregorianCalendar());
				if (task.getOwner() != null) {
					metaData.setCreatorRef(ObjectTypeUtil.createObjectRef(task.getOwner()));
				}
				object.asObjectable().setMetadata(metaData);
		}

		objectResult.computeStatus();
		if (!objectResult.isAcceptable()) {
			return EventResult.skipObject(objectResult.getMessage());
		}

		try {
			importObjectToRepository(object, options, raw, task, objectResult);
		    LOGGER.info("Imported object {}", object);
		} catch (SchemaException e) {
		    objectResult.recordFatalError("Schema violation: "+e.getMessage(), e);
		    LOGGER.error("Import of object {} failed: Schema violation: {}", object, e.getMessage(), e);
		} catch (ObjectAlreadyExistsException e) {
			objectResult.recordFatalError("Object already exists: "+e.getMessage(), e);
		    LOGGER.error("Import of object {} failed: Object already exists: {}", object, e.getMessage(), e);
		    LOGGER.error("Object already exists", e);
		} catch (RuntimeException e) {
		    objectResult.recordFatalError("Unexpected problem: "+e.getMessage(), e);
		    LOGGER.error("Import of object {} failed: Unexpected problem: {}", object, e.getMessage(), e);
		} catch (ObjectNotFoundException e) {
			LOGGER.error("Import of object {} failed: Object referred from this object was not found: {}", object, e.getMessage(), e);
		} catch (ExpressionEvaluationException e) {
			LOGGER.error("Import of object {} failed: Expression evaluation error: {}", object, e.getMessage(), e);
		} catch (CommunicationException e) {
			LOGGER.error("Import of object {} failed: Communication error: {}", object, e.getMessage(), e);
		} catch (ConfigurationException e) {
			LOGGER.error("Import of object {} failed: Configuration error: {}", object, e.getMessage(), e);
		} catch (PolicyViolationException e) {
			LOGGER.error("Import of object {} failed: Policy violation: {}", object, e.getMessage(), e);
		} catch (SecurityViolationException e) {
			LOGGER.error("Import of object {} failed: Security violation: {}", object, e.getMessage(), e);
		}

		objectResult.recordSuccessIfUnknown();
		if (objectResult.isAcceptable()) {
		    // Continue import
		    return EventResult.cont();
		} else {
		    return EventResult.skipObject(objectResult.getMessage());
		}
	}

	private <T extends ObjectType> void importObjectToRepository(PrismObject<T> object, ImportOptionsType options, boolean raw,
                                         Task task, OperationResult objectResult) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
				ConfigurationException, PolicyViolationException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException {

        OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName() + ".importObjectToRepository");

        if (options == null) {
        	options = new ImportOptionsType();
        }
        
        if (BooleanUtils.isTrue(options.isKeepOid()) && object.getOid() == null) {
			// Try to check if there is existing object with the same type and name
        	ObjectQuery query = ObjectQueryUtil.createNameQuery(object);
        	List<PrismObject<T>> foundObjects = repository.searchObjects(object.getCompileTimeClass(), query, null, result);
        	if (foundObjects.size() == 1) {
        		String oid = foundObjects.iterator().next().getOid();
        		object.setOid(oid);
        	}
        }
        
        try {
			String oid = addObject(object, BooleanUtils.isTrue(options.isOverwrite()), 
					BooleanUtils.isFalse(options.isEncryptProtectedValues()), raw, task, result);
			
            if (object.canRepresent(TaskType.class)) {
            	taskManager.onTaskCreate(oid, result);
            }
            result.recordSuccess();

        } catch (ObjectAlreadyExistsException e) {            
        	if (BooleanUtils.isTrue(options.isOverwrite() && 
        		BooleanUtils.isNotTrue(options.isKeepOid()) && 
        		object.getOid() == null)) {
     	 	 	// This is overwrite, without keep oid, therefore we do not have conflict on OID
        		// this has to be conflict on name. So try to delete the conflicting object and create new one (with a new OID).
        		result.muteLastSubresultError();
        		ObjectQuery query = ObjectQueryUtil.createNameQuery(object);
            	List<PrismObject<T>> foundObjects = repository.searchObjects(object.getCompileTimeClass(), query, null, result);
            	if (foundObjects.size() == 1) {
            		PrismObject<T> foundObject = foundObjects.iterator().next();
            		String deletedOid = deleteObject(foundObject, repository, result);
         	 	 	if (deletedOid != null) {
         	 	 		if (object.canRepresent(TaskType.class)) {
         	 	 			taskManager.onTaskDelete(deletedOid, result);
         	 	 		}
         	 	 		if (BooleanUtils.isTrue(options.isKeepOid())) {
         	 	 			object.setOid(deletedOid);
         	 	 		}
         	 	 		addObject(object, false, BooleanUtils.isFalse(options.isEncryptProtectedValues()), raw, task, result);
	         	 	 	if (object.canRepresent(TaskType.class)) {
	         	 	 		taskManager.onTaskCreate(object.getOid(), result);
	         	 	 	}
	         	 	 	result.recordSuccess();
         	 	 	} else {
         	 	 		// cannot delete, throw original exception
                        result.recordFatalError("Object already exists, cannot overwrite", e);
                        throw e;
         	 	 	}
            	} else {
            		// Cannot locate conflicting object
            		String message = "Conflicting object already exists but it was not possible to precisely locate it, "+foundObjects.size()+" objects with same name exist";
            		result.recordFatalError(message, e);
                 	throw new ObjectAlreadyExistsException(message, e);
            	}
        	} else {
        		result.recordFatalError(e);
             	throw e;
        	}
        } catch (ObjectNotFoundException | ExpressionEvaluationException | CommunicationException
				| ConfigurationException | PolicyViolationException | SecurityViolationException | SchemaException e) {
        	result.recordFatalError("Cannot import " + object + ": "+e.getMessage(), e);
        	throw e;
        } catch (RuntimeException ex){
        	result.recordFatalError("Couldn't import object: " + object +". Reason: " + ex.getMessage(), ex);
        	throw ex;
        }
    }

    private <T extends ObjectType> String addObject(PrismObject<T> object, boolean overwrite, boolean noCrypt, boolean raw,
    		Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
    	
    	ObjectDelta<T> delta = ObjectDelta.createAddDelta(object);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
		ModelExecuteOptions options = new ModelExecuteOptions();
		options.setRaw(raw);
		if (overwrite) {
			options.setOverwrite(true);
		}
		if (noCrypt) {
			options.setNoCrypt(true);
		}
		
		modelService.executeChanges(deltas, options, task, parentResult);
    	
		return deltas.iterator().next().getOid();
    }

    /**
     * @return OID of the deleted object or null (if nothing was deleted)
     */
    private <T extends ObjectType> String deleteObject(PrismObject<T> object, RepositoryService repository, OperationResult objectResult) throws SchemaException {
        try {
            repository.deleteObject(object.getCompileTimeClass(), object.getOid(), objectResult);
        } catch (ObjectNotFoundException e) {
            // Cannot delete. The conflicting thing was obviously not OID. Just throw the original exception
            return null;
        }
        // deleted
        return object.getOid();
    }

    private <T extends ObjectType> void validateWithDynamicSchemas(PrismObject<T> object, RepositoryService repository,
		    OperationResult objectResult) {

        // TODO: check extension schema (later)
    	OperationResult result = objectResult.createSubresult(OPERATION_VALIDATE_DYN_SCHEMA);
        if (object.canRepresent(ConnectorType.class)) {
            ConnectorType connector = (ConnectorType) object.asObjectable();
            checkSchema(connector.getSchema(), "connector", result);
            result.computeStatus("Connector schema error");
            result.recordSuccessIfUnknown();

        } else if (object.canRepresent(ResourceType.class)) {


            // Only two object types have XML snippets that conform to the dynamic schema

        	PrismObject<ResourceType> resource = (PrismObject<ResourceType>)object;
            ResourceType resourceType = resource.asObjectable();
            PrismContainer<ConnectorConfigurationType> configurationContainer = ResourceTypeUtil.getConfigurationContainer(resource);
            if (configurationContainer == null || configurationContainer.isEmpty()) {
                // Nothing to check
                result.recordWarning("The resource has no configuration");
                return;
            }

            // Check the resource configuration. The schema is in connector, so fetch the connector first
            String connectorOid = resourceType.getConnectorRef().getOid();
            if (StringUtils.isBlank(connectorOid)) {
                result.recordFatalError("The connector reference (connectorRef) is null or empty");
                return;
            }

            PrismObject<ConnectorType> connector;
            ConnectorType connectorType;
            try {
                connector = repository.getObject(ConnectorType.class, connectorOid, null, result);
                connectorType = connector.asObjectable();
            } catch (ObjectNotFoundException e) {
                // No connector, no fun. We can't check the schema. But this is referential integrity problem.
                // Mark the error ... there is nothing more to do
                result.recordFatalError("Connector (OID:" + connectorOid + ") referenced from the resource is not in the repository", e);
                return;
            } catch (SchemaException e) {
                // Probably a malformed connector. To be kind of robust, lets allow the import.
                // Mark the error ... there is nothing more to do
                result.recordPartialError("Connector (OID:" + connectorOid + ") referenced from the resource has schema problems: " + e.getMessage(), e);
                LOGGER.error("Connector (OID:{}) referenced from the imported resource \"{}\" has schema problems: {}", new Object[]{connectorOid, resourceType.getName(), e.getMessage(), e});
                return;
            }
            
            Element connectorSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(connector);
            PrismSchema connectorSchema;
            if (connectorSchemaElement == null) {
            	// No schema to validate with
            	result.recordSuccessIfUnknown();
            	return;
            }
			try {
				connectorSchema = PrismSchemaImpl.parse(connectorSchemaElement, true, "schema for " + connector, prismContext);
			} catch (SchemaException e) {
				result.recordFatalError("Error parsing connector schema for " + connector + ": "+e.getMessage(), e);
				return;
			}
            QName configContainerQName = new QName(connectorType.getNamespace(), ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
    		PrismContainerDefinition<ConnectorConfigurationType> configContainerDef = connectorSchema.findContainerDefinitionByElementName(configContainerQName);
    		if (configContainerDef == null) {
    			result.recordFatalError("Definition of configuration container " + configContainerQName + " not found in the schema of of " + connector);
                return;
    		}
            
            try {
				configurationContainer.applyDefinition(configContainerDef);
			} catch (SchemaException e) {
				result.recordFatalError("Configuration error in " + resource + ": "+e.getMessage(), e);
                return;
			}

            // now we check for raw data - their presence means e.g. that there is a connector property that is unknown in connector schema (applyDefinition does not scream in such a case!)
            try {
                configurationContainer.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);        // require definitions and prohibit raw
            } catch (IllegalStateException e) {
                // TODO do this error checking and reporting in a cleaner and more user-friendly way
                result.recordFatalError("Configuration error in " + resource + " (probably incorrect connector property, see the following error): " + e.getMessage(), e);
                return;
            }
            
            // Also check integrity of the resource schema
            checkSchema(resourceType.getSchema(), "resource", result);

            result.computeStatus("Dynamic schema error");
            

        } else if (object.canRepresent(ShadowType.class)) {
            // TODO

            //objectResult.computeStatus("Dynamic schema error");
        }

        result.recordSuccessIfUnknown();
    }

    /**
     * Try to parse the schema using schema processor. Report errors.
     */
    private void checkSchema(XmlSchemaType dynamicSchema, String schemaName, OperationResult objectResult) {
        OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName() + ".check" + StringUtils.capitalize(schemaName) + "Schema");

        Element xsdElement = ObjectTypeUtil.findXsdElement(dynamicSchema);

        if (dynamicSchema == null || xsdElement == null) {
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Missing dynamic " + schemaName + " schema");
            return;
        }

        try {
            PrismSchemaImpl.parse(xsdElement, true, schemaName, prismContext);
        } catch (SchemaException e) {
            result.recordFatalError("Error during " + schemaName + " schema integrity check: " + e.getMessage(), e);
            return;
        }
        result.recordSuccess();
    }

    /**
     * Validate the provided XML snippet with schema definition fetched in runtime.
     *
     * @param contentElements DOM tree to validate
     * @param elementRef      the "correct" name of the root element
     * @param dynamicSchema   dynamic schema
     */
    private PrismContainer validateDynamicSchema(List<Object> contentElements, QName elementRef,
                                                    XmlSchemaType dynamicSchema, String schemaName, OperationResult objectResult) {
        OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName() + ".validate" + StringUtils.capitalize(schemaName) + "Schema");

        Element xsdElement = ObjectTypeUtil.findXsdElement(dynamicSchema);
        if (xsdElement == null) {
        	result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No "+schemaName+" schema present");
        	return null;
        }

        com.evolveum.midpoint.prism.schema.PrismSchema schema;
        try {
            schema = com.evolveum.midpoint.prism.schema.PrismSchemaImpl.parse(xsdElement, true, schemaName, prismContext);
        } catch (SchemaException e) {
            result.recordFatalError("Error during " + schemaName + " schema parsing: " + e.getMessage(), e);
            LOGGER.trace("Validation error: {}" + e.getMessage());
            return null;
        }

        PrismContainerDefinition containerDefinition = schema.findItemDefinition(elementRef, PrismContainerDefinition.class);

        PrismContainer propertyContainer = null;

        result.recordSuccess();
        return propertyContainer;

    }

    private <T extends ObjectType> void generateIdentifiers(PrismObject<T> object, RepositoryService repository,
			OperationResult objectResult) {
		if (object.canRepresent(TaskType.class)) {
			TaskType task = (TaskType)object.asObjectable();
			if (task.getTaskIdentifier() == null || task.getTaskIdentifier().isEmpty()) {
				task.setTaskIdentifier(lightweightIdentifierGenerator.generate().toString());
			}
		}
	}
    
}

