/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

import java.util.*;
import java.util.function.Function;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.api.hooks.ReadHook;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.task.api.TaskManager;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author semancik
 *
 */
@Component
public class ModelObjectResolver implements ObjectResolver {

    @Autowired private transient ProvisioningService provisioning;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired private transient PrismContext prismContext;

    @Autowired private transient TaskManager taskManager;

    @Autowired(required = false)
    private transient WorkflowManager workflowManager;

    @Autowired(required = false)
    private transient HookRegistry hookRegistry;

    private static final Trace LOGGER = TraceManager.getTrace(ModelObjectResolver.class);

    @Override
    public <O extends ObjectType> O resolve(ObjectReferenceType ref, Class<O> expectedType, Collection<SelectorOptions<GetOperationOptions>> options,
            String contextDescription, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        String oid = ref.getOid();
        Class<?> typeClass = null;
        QName typeQName = ref.getType();
        if (typeQName != null) {
            typeClass = prismContext.getSchemaRegistry().determineCompileTimeClass(typeQName);
        }
        if (typeClass != null && expectedType.isAssignableFrom(typeClass)) {
            expectedType = (Class<O>) typeClass;
        }
        return getObject(expectedType, oid, options, task, result);
    }

    public <O extends ObjectType> PrismObject<O> resolve(PrismReferenceValue refVal, String string, Task task, OperationResult result) throws ObjectNotFoundException {
        return resolve(refVal, string, null, task, result);
    }

    public <O extends ObjectType> PrismObject<O> resolve(PrismReferenceValue refVal, String string, GetOperationOptions options, Task task,
            OperationResult result) throws ObjectNotFoundException {
        String oid = refVal.getOid();
        if (oid == null) {
            // e.g. for targetName-only references
            //noinspection unchecked
            return refVal.getObject();
        }
        Class<?> typeClass = ObjectType.class;
        QName typeQName = refVal.getTargetType();
        if (typeQName == null && refVal.getParent() != null && refVal.getParent().getDefinition() != null) {
            PrismReferenceDefinition refDef = (PrismReferenceDefinition) refVal.getParent().getDefinition();
            typeQName = refDef.getTargetTypeName();
        }
        if (typeQName != null) {
            typeClass = prismContext.getSchemaRegistry().determineCompileTimeClass(typeQName);
        }
        return (PrismObject<O>) (getObjectSimple((Class<O>)typeClass, oid, options, task, result)).asPrismObject();
    }

    public <T extends ObjectType> T getObjectSimple(Class<T> clazz, String oid, GetOperationOptions options, Task task,
            OperationResult result) throws ObjectNotFoundException {
        try {
            return getObject(clazz, oid, SelectorOptions.createCollection(options), task, result);
        } catch (SystemException | ObjectNotFoundException ex) {
            throw ex;
        } catch (CommonException ex) {
            LoggingUtils.logException(LOGGER, "Error resolving object with oid {}", ex, oid);
            // Add to result only a short version of the error, the details will be in subresults
            result.recordFatalError(
                    "Couldn't get object with oid '" + oid + "': "+ex.getErrorTypeMessage(), ex);
            throw new SystemException("Error resolving object with oid '" + oid + "': "+ex.getMessage(), ex);
        }
    }

    @Override
    @NotNull
    public <T extends ObjectType> T getObject(Class<T> clazz, String oid, Collection<SelectorOptions<GetOperationOptions>> options, Task task,
            OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        T objectType;
        try {
            PrismObject<T> object;
            ObjectTypes.ObjectManager manager = ObjectTypes.getObjectManagerForClass(clazz);
            switch (defaultIfNull(manager, ObjectTypes.ObjectManager.REPOSITORY)) {
                case PROVISIONING:
                    object = provisioning.getObject(clazz, oid, options, task, result);
                    if (object == null) {
                        throw new SystemException("Got null result from provisioning.getObject while looking for "+clazz.getSimpleName()
                                +" with OID "+oid+"; using provisioning implementation "+provisioning.getClass().getName());
                    }
                    break;
                case TASK_MANAGER:
                    object = taskManager.getObject(clazz, oid, options, result);
                    if (object == null) {
                        throw new SystemException("Got null result from taskManager.getObject while looking for "+clazz.getSimpleName()
                                +" with OID "+oid+"; using task manager implementation "+taskManager.getClass().getName());
                    }
                    break;
                default:
                    object = cacheRepositoryService.getObject(clazz, oid, options, result);
                    if (object == null) {
                        throw new SystemException("Got null result from repository.getObject while looking for "+clazz.getSimpleName()
                                +" with OID "+oid+"; using repository implementation "+cacheRepositoryService.getClass().getName());
                    }
            }
            objectType = object.asObjectable();
            if (!clazz.isInstance(objectType)) {
                throw new ObjectNotFoundException(
                        "Bad object type returned for referenced oid '" + oid + "'. Expected '" + clazz + "', but was '"
                                + objectType.getClass() + "'.");
            }

            if (hookRegistry != null) {
                for (ReadHook hook : hookRegistry.getAllReadHooks()) {
                    hook.invoke(object, options, task, result);
                }
            }
        } catch (RuntimeException | Error ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error resolving object with oid {}, expected type was {}.", ex, oid, clazz);
            throw new SystemException("Error resolving object with oid '" + oid + "': "+ex.getMessage(), ex);
        }
        return objectType;
    }

    @Override
    public <O extends ObjectType> void searchIterative(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<O> handler, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (ObjectTypes.isClassManagedByProvisioning(type)) {
            provisioning.searchObjectsIterative(type, query, options, handler, task, parentResult);
        } else {
            cacheRepositoryService.searchObjectsIterative(type, query, handler, options, true, parentResult);        // TODO pull up into resolver interface
        }
    }

    @Override
    public <O extends ObjectType> SearchResultList<PrismObject<O>> searchObjects(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (ObjectTypes.isClassManagedByProvisioning(type)) {
            return provisioning.searchObjects(type, query, options, task, parentResult);
        } else {
            return cacheRepositoryService.searchObjects(type, query, options, parentResult);
        }
    }

    public <O extends ObjectType> Integer countObjects(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (ObjectTypes.isClassManagedByProvisioning(type)) {
            return provisioning.countObjects(type, query, options, task, parentResult);
        } else {
            return cacheRepositoryService.countObjects(type, query, options, parentResult);
        }
    }

    public PrismObject<SystemConfigurationType> getSystemConfiguration(OperationResult result) throws ObjectNotFoundException, SchemaException {
        PrismObject<SystemConfigurationType> config = cacheRepositoryService.getObject(SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result);

        if (LOGGER.isTraceEnabled()) {
            if (config == null) {
                LOGGER.warn("No system configuration object");
            } else {
                LOGGER.trace("System configuration version read from repo: " + config.getVersion());
            }
        }
        return config;
    }

    public <O extends ObjectType, R extends ObjectType> PrismObject<R> searchOrgTreeWidthFirstReference(PrismObject<O> object,
            Function<PrismObject<OrgType>, ObjectReferenceType> function, String shortDesc, Task task, OperationResult result) throws SchemaException {
        if (object == null) {
            LOGGER.trace("No object provided. Cannost find security policy specific for an object.");
            return null;
        }
        PrismReference orgRef = object.findReference(ObjectType.F_PARENT_ORG_REF);
        if (orgRef == null) {
            return null;
        }
        List<PrismReferenceValue> orgRefValues = orgRef.getValues();
        List<PrismObject<OrgType>> orgs = new ArrayList<>();
        PrismObject<R> resultObject = null;

        for (PrismReferenceValue orgRefValue : orgRefValues) {
            if (orgRefValue != null) {

                try {
                    PrismObject<OrgType> org = resolve(orgRefValue, "resolving parent org ref", null, null, result);
                    orgs.add(org);
                    ObjectReferenceType ref = function.apply(org);

                    if (ref != null) {
                        PrismObject<R> resolvedObject;
                        try {
                            resolvedObject = resolve(ref.asReferenceValue(), shortDesc, task, result);
                        } catch (ObjectNotFoundException ex) {
                            // Just log the error, but do not fail on that. Failing would prohibit login
                            // and that may mean the misconfiguration could not be easily fixed.
                            LOGGER.warn("Cannot find object {} referenced in {} while resolving {}", orgRefValue.getOid(), object, shortDesc);
                            continue;
                        }
                        if (resolvedObject != null) {
                            if (resultObject == null) {
                                resultObject = resolvedObject;
                            } else if (!StringUtils.equals(resolvedObject.getOid(), resultObject.getOid())) {
                                throw new SchemaException(
                                        "Found more than one object (" + resolvedObject + ", " + resultObject + ") while " + shortDesc);
                            }
                        }
                    }

                } catch (ObjectNotFoundException ex) {
                    // Just log the error, but do not fail on that. Failing would prohibit login
                    // and that may mean the misconfiguration could not be easily fixed.
                    LOGGER.warn("Cannot find organization {} referenced in {}", orgRefValue.getOid(), object);
                    result.muteLastSubresultError();
                }
            }
        }

        if (resultObject != null) {
            return resultObject;
        }

        // go deeper
        for (PrismObject<OrgType> org : orgs) {
            PrismObject<R> val = searchOrgTreeWidthFirstReference((PrismObject<O>) org, function, shortDesc, task, result);
            if (val != null) {
                return val;
            }
        }

        return null;
    }

    public <R,O extends ObjectType> R searchOrgTreeWidthFirst(PrismObject<O> object,
            Function<PrismObject<OrgType>, R> function, Task task, OperationResult result) {
        PrismReference orgRef = object.findReference(ObjectType.F_PARENT_ORG_REF);
        if (orgRef == null) {
            return null;
        }
        List<PrismReferenceValue> orgRefValues = orgRef.getValues();
        List<PrismObject<OrgType>> orgs = new ArrayList<>();

        for (PrismReferenceValue orgRefValue : orgRefValues) {
            if (orgRefValue != null) {

                try {
                    PrismObject<OrgType> org = resolve(orgRefValue, "resolving parent org ref", null, null, result);
                    orgs.add(org);
                    R val = function.apply(org);

                    if (val != null) {
                        return val;
                    }
                } catch (ObjectNotFoundException ex) {
                    // Just log the error, but do not fail on that. Failing would prohibit login
                    // and that may mean the misconfiguration could not be easily fixed.
                    LOGGER.warn("Cannot find organization {} referenced in {}", orgRefValue.getOid(), object);
                }
            }
        }

        // go deeper
        for (PrismObject<OrgType> orgType : orgs) {
            R val = searchOrgTreeWidthFirst((PrismObject<O>) orgType, function, task, result);
            if (val != null) {
                return val;
            }
        }

        return null;
    }

    @Experimental
    @Override
    public void resolveAllReferences(Collection<PrismContainerValue<?>> pcvs, Object taskObject, OperationResult result) {
        Session session = openResolutionSession(null);
        Task task = (Task) taskObject;
        ConfigurableVisitor<?> visitor = new ConfigurableVisitor() {
            @Override
            public boolean shouldVisitEmbeddedObjects() {
                // This is to avoid endless recursion when resolving cases: A parent case has references to its children
                // whereas child cases have references to their parent. We could deal with this using SmartVisitable
                // but that would be overkill for the basic use - it would resolve much more than needed. So we simply stop
                // visiting embedded objects because in the basic use case we are simply not interested in them.
                // See also MID-6171.
                //
                // Should we need this feature we will add an option or create a separate method
                // e.g. "resolveAllReferencesDeeply".
                return false;
            }

            @Override
            public void visit(Visitable visitable) {
                if (visitable instanceof PrismReferenceValue) {
                    resolveReference((PrismReferenceValue) visitable, "resolving object reference", session, task, result);
                }
            }
        };
        pcvs.forEach(pcv -> pcv.accept(visitor));
    }

    @Override
    public void resolveReference(PrismReferenceValue prv, String contextDescription,
            Session session, Object taskObject, OperationResult result) {
        Task task = (Task) taskObject;
        String oid = prv.getOid();
        if (oid == null) {
            // nothing to do
        } else if (prv.getObject() != null) {
            if (!session.contains(oid)) {
                session.put(oid, prv.getObject());
            }
        } else {
            PrismObject<?> object = session.get(oid);
            if (object == null) {
                try {
                    object = resolve(prv, "resolving object reference", session.getOptions(), task, result);
                    session.put(oid, object);
                } catch (Throwable t) {
                    LoggingUtils.logException(LOGGER, "Couldn't resolve reference {}", t, prv);
                    // but let's continue (hoping the issue is already recorded in the operation result)
                }
            }
            prv.setObject(object);
        }
    }
}
