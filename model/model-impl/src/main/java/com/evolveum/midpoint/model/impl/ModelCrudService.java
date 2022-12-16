/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Simple version of model service exposing CRUD-like operations. This is common facade for webservice and REST services.
 * It takes care of all the "details" of externalized objects such as applying correct definitions and so on.
 *
 * Other methods (not strictly related to CRUD operations) requiring some pre-processing may come here later; if needed
 * for both WS and REST services.
 *
 * @author Radovan Semancik
 *
 */
@Component
public class ModelCrudService {

    private static final String CLASS_NAME_WITH_DOT = ModelCrudService.class.getName() + ".";
    private static final String ADD_OBJECT = CLASS_NAME_WITH_DOT + "addObject";
    private static final String MODIFY_OBJECT = CLASS_NAME_WITH_DOT + "modifyObject";
    private static final String DELETE_OBJECT = CLASS_NAME_WITH_DOT + "deleteObject";

    private static final Trace LOGGER = TraceManager.getTrace(ModelCrudService.class);

    @Autowired ModelService modelService;
    @Autowired TaskService taskService;
    @Autowired PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") RepositoryService repository;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    public <T extends ObjectType> PrismObject<T> getObject(Class<T> clazz, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return modelService.getObject(clazz, oid, options, task, parentResult);
    }

    public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return modelService.searchObjects(type, query, options, task, parentResult);
    }

    /**
     * <p>
     * Add new object.
     * </p>
     * <p>
     * The OID provided in the input message may be empty. In that case the OID
     * will be assigned by the implementation of this method and it will be
     * provided as return value.
     * </p>
     * <p>
     * This operation should fail if such object already exists (if object with
     * the provided OID already exists).
     * </p>
     * <p>
     * The operation may fail if provided OID is in an unusable format for the
     * storage. Generating own OIDs and providing them to this method is not
     * recommended for normal operation.
     * </p>
     * <p>
     * Should be atomic. Should not allow creation of two objects with the same
     * OID (even if created in parallel).
     * </p>
     * <p>
     * The operation may fail if the object to be created does not conform to
     * the underlying schema of the storage system or the schema enforced by the
     * implementation.
     * </p>
     *
     * @param object
     *            object to create
     * @param parentResult
     *            parent OperationResult (in/out)
     * @return OID assigned to the created object
     * @throws ObjectAlreadyExistsException
     *             object with specified identifiers already exists, cannot add
     * @throws ObjectNotFoundException
     *             object required to complete the operation was not found (e.g.
     *             appropriate connector or resource definition)
     * @throws SchemaException
     *             error dealing with resource schema, e.g. created object does
     *             not conform to schema
     * @throws ExpressionEvaluationException
     *                 evaluation of expression associated with the object has failed
     * @throws CommunicationException
     * @throws ConfigurationException
     * @throws PolicyViolationException
     *                 Policy violation was detected during processing of the object
     * @throws IllegalArgumentException
     *             wrong OID format, etc.
     * @throws SystemException
     *             unknown error from underlying layers or other unexpected
     *             state
     */
    @SuppressWarnings("JavaDoc")
    public <T extends ObjectType> String addObject(PrismObject<T> object, ModelExecuteOptions options, Task task,
            OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        Validate.notNull(object, "Object must not be null.");
        Validate.notNull(parentResult, "Result type must not be null.");

        object.checkConsistence();

        T objectType = object.asObjectable();
        prismContext.adopt(objectType);

        OperationResult result = parentResult.createSubresult(ADD_OBJECT);
        result.addParam(OperationResult.PARAM_OBJECT, object);

        ModelImplUtils.resolveReferences(object, repository, false, false, EvaluationTimeType.IMPORT, true, result);

        String oid;
        RepositoryCache.enterLocalCaches(cacheConfigurationManager);
        try {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Entering addObject with {}", object);
                LOGGER.trace(object.debugDump());
            }

            if (options == null) {
                if (StringUtils.isNotEmpty(objectType.getVersion())) {
                    options = ModelExecuteOptions.create().overwrite();
                }
            }

            ObjectDelta<T> objectDelta = DeltaFactory.Object.createAddDelta(object);
            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);

            Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges = modelService.executeChanges(deltas, options, task, result);

            oid = ObjectDeltaOperation.findAddDeltaOid(executedChanges, object);
            result.computeStatus();
            result.cleanupResult();

        } catch (ExpressionEvaluationException | SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | SecurityViolationException | ConfigurationException | RuntimeException ex) {
            ModelImplUtils.recordFatalError(result, ex);
            throw ex;
        } finally {
            RepositoryCache.exitLocalCaches();
        }

        return oid;
    }

    /**
     * <p>
     * Deletes object with specified OID.
     * </p>
     * <p>
     * Must fail if object with specified OID does not exist. Should be atomic.
     * </p>
     *
     * @param oid
     *            OID of object to delete
     * @param parentResult
     *            parent OperationResult (in/out)
     * @throws ObjectNotFoundException
     *             specified object does not exist
     * @throws IllegalArgumentException
     *             wrong OID format, described change is not applicable
     * @throws CommunicationException
     *             Communication problem was detected during processing of the object
     * @throws ConfigurationException
     *             Configuration problem was detected during processing of the object
     * @throws PolicyViolationException
     *                Policy violation was detected during processing of the object
     * @throws SystemException
     *             unknown error from underlying layers or other unexpected
     *             state
     */
    public <T extends ObjectType> void deleteObject(Class<T> clazz, String oid, ModelExecuteOptions options, Task task,
            OperationResult parentResult) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        Validate.notNull(clazz, "Class must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(parentResult, "Result type must not be null.");

        OperationResult result = parentResult.createSubresult(DELETE_OBJECT);
        result.addParam(OperationResult.PARAM_OID, oid);

        RepositoryCache.enterLocalCaches(cacheConfigurationManager);

        try {
            ObjectDelta<T> objectDelta = prismContext.deltaFactory().object().create(clazz, ChangeType.DELETE);
            objectDelta.setOid(oid);

            LOGGER.trace("Deleting object with oid {}.", oid);

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
            modelService.executeChanges(deltas, options, task, result);

            result.recordSuccess();

        } catch (ObjectNotFoundException | CommunicationException | RuntimeException | SecurityViolationException ex) {
            ModelImplUtils.recordFatalError(result, ex);
            throw ex;
        } catch (ObjectAlreadyExistsException | ExpressionEvaluationException ex) {
            ModelImplUtils.recordFatalError(result, ex);
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            RepositoryCache.exitLocalCaches();
        }
    }

    /**
     * <p>
     * Modifies object using relative change description.
     * </p>
     * <p>
     * Must fail if user with provided OID does not exist. Must fail if any of
     * the described changes cannot be applied. Should be atomic.
     * </p>
     * <p>
     * If two or more modify operations are executed in parallel, the operations
     * should be merged. In case that the operations are in conflict (e.g. one
     * operation adding a value and the other removing the same value), the
     * result is not deterministic.
     * </p>
     * <p>
     * The operation may fail if the modified object does not conform to the
     * underlying schema of the storage system or the schema enforced by the
     * implementation.
     * </p>
     *
     * @param parentResult
     *            parent OperationResult (in/out)
     * @throws ObjectNotFoundException
     *             specified object does not exist
     * @throws SchemaException
     *             resulting object would violate the schema
     * @throws ExpressionEvaluationException
     *                 evaluation of expression associated with the object has failed
     * @throws CommunicationException
     *              Communication problem was detected during processing of the object
     * @throws ObjectAlreadyExistsException
     *                 If the account or another "secondary" object already exists and cannot be created
     * @throws PolicyViolationException
     *                 Policy violation was detected during processing of the object
     * @throws IllegalArgumentException
     *             wrong OID format, described change is not applicable
     * @throws SystemException
     *             unknown error from underlying layers or other unexpected
     *             state
     */
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
            Collection<? extends ItemDelta> modifications, ModelExecuteOptions options, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {

        Validate.notNull(modifications, "Object modification must not be null.");
        Validate.notEmpty(oid, "Change oid must not be null or empty.");
        Validate.notNull(parentResult, "Result type must not be null.");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Modifying object with oid {}", oid);
            LOGGER.trace(DebugUtil.debugDump(modifications));
        }

        ItemDeltaCollectionsUtil.checkConsistence(modifications, ConsistencyCheckScope.THOROUGH);
        // TODO: check definitions, but tolerate missing definitions in <attributes>

        OperationResult result = parentResult.createSubresult(MODIFY_OBJECT);
        result.addArbitraryObjectCollectionAsParam("modifications", modifications);

        RepositoryCache.enterLocalCaches(cacheConfigurationManager);

        try {

            ObjectDelta<T> objectDelta = prismContext.deltaFactory().object().createModifyDelta(oid, modifications, type);
            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
            modelService.executeChanges(deltas, options, task, result);

            result.computeStatus();

        } catch (ExpressionEvaluationException | ObjectNotFoundException ex) {
            LOGGER.error("model.modifyObject failed: {}", ex.getMessage(), ex);
            result.recordFatalError(ex);
            throw ex;
        } catch (SchemaException | ConfigurationException | SecurityViolationException | RuntimeException ex) {
            ModelImplUtils.recordFatalError(result, ex);
            throw ex;
        } finally {
            RepositoryCache.exitLocalCaches();
        }
    }
}
