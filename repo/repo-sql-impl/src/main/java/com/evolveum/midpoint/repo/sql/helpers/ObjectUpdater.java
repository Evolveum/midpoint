/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.sql.SerializationRelatedException;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.PrismIdentifierGenerator;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;

/**
 * @author lazyman, mederly
 */

@Component
public class ObjectUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectUpdater.class);
    private static final Trace LOGGER_PERFORMANCE = TraceManager.getTrace(SqlRepositoryServiceImpl.PERFORMANCE_LOG_NAME);

	@Autowired
	@Qualifier("repositoryService")
	private RepositoryService repositoryService;

    @Autowired
    private BaseHelper baseHelper;

    @Autowired
    private ObjectRetriever objectRetriever;

    @Autowired
    private LookupTableHelper lookupTableHelper;

    @Autowired
    private CertificationCaseHelper caseHelper;

    @Autowired
    private OrgClosureManager closureManager;

    @Autowired
    private ObjectDeltaUpdater objectDeltaUpdater;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ExtItemDictionary extItemDictionary;

    public <T extends ObjectType> String addObjectAttempt(PrismObject<T> object, RepoAddOptions options,
            OperationResult result) throws ObjectAlreadyExistsException, SchemaException {

        LOGGER_PERFORMANCE.debug("> add object {}, oid={}, overwrite={}",
                object.getCompileTimeClass().getSimpleName(), object.getOid(), options.isOverwrite());

        String oid = null;
        Session session = null;
        OrgClosureManager.Context closureContext = null;
        // it is needed to keep the original oid for example for import options. if we do not keep it
        // and it was null it can bring some error because the oid is set when the object contains orgRef
        // or it is org. and by the import we do not know it so it will be trying to delete non-existing object
        String originalOid = object.getOid();
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Object\n{}", object.debugDump());
            }
            ObjectTypeUtil.normalizeAllRelations(object);

            LOGGER.trace("Translating JAXB to data type.");
            PrismIdentifierGenerator.Operation operation = options.isOverwrite() ?
                    PrismIdentifierGenerator.Operation.ADD_WITH_OVERWRITE :
                    PrismIdentifierGenerator.Operation.ADD;
            PrismIdentifierGenerator<T> idGenerator = new PrismIdentifierGenerator<>(operation);
            
            session = baseHelper.beginTransaction();

            RObject rObject = createDataObjectFromJAXB(object, idGenerator);

            closureContext = closureManager.onBeginTransactionAdd(session, object, options.isOverwrite());

            if (options.isOverwrite()) {
                oid = overwriteAddObjectAttempt(object, rObject, originalOid, session, closureContext, result);
            } else {
                oid = nonOverwriteAddObjectAttempt(object, rObject, originalOid, session, closureContext);
            }
            session.getTransaction().commit();

            LOGGER.trace("Saved object '{}' with oid '{}'", object.getCompileTimeClass().getSimpleName(), oid);

            object.setOid(oid);
        } catch (PersistenceException ex) {
            ConstraintViolationException constEx = findConstraintViolationException(ex);
            if (constEx == null) {
                baseHelper.handleGeneralException(ex, session, result);
                throw new AssertionError("shouldn't be here");
            }

            handleConstraintViolationException(session, constEx, result);
            baseHelper.rollbackTransaction(session, constEx, result, true);

            LOGGER.debug("Constraint violation occurred (will be rethrown as ObjectAlreadyExistsException).", constEx);
            // we don't know if it's only name uniqueness violation, or something else,
            // therefore we're throwing it always as ObjectAlreadyExistsException revert
            // to the original oid and prevent of unexpected behaviour (e.g. by import with overwrite option)
            if (StringUtils.isEmpty(originalOid)) {
                object.setOid(null);
            }
            String constraintName = constEx.getConstraintName();
            // Breaker to avoid long unreadable messages
            if (constraintName != null && constraintName.length() > SqlRepositoryServiceImpl.MAX_CONSTRAINT_NAME_LENGTH) {
                constraintName = null;
            }
            throw new ObjectAlreadyExistsException("Conflicting object already exists"
                    + (constraintName == null ? "" : " (violated constraint '" + constraintName + "')"), constEx);
        } catch (ObjectAlreadyExistsException | SchemaException ex) {
            baseHelper.rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (DtoTranslationException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
        } finally {
            cleanupClosureAndSessionAndResult(closureContext, session, result);
        }

        return oid;
    }

    private ConstraintViolationException findConstraintViolationException(PersistenceException ex) {
        if (ex instanceof ConstraintViolationException) {
            return (ConstraintViolationException) ex;
        }

        if (ex.getCause() instanceof ConstraintViolationException) {
            return (ConstraintViolationException) ex.getCause();
        }

        return null;
    }

    private <T extends ObjectType> String overwriteAddObjectAttempt(PrismObject<T> object, RObject rObject,
			String originalOid, Session session, OrgClosureManager.Context closureContext, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException, DtoTranslationException {

        PrismObject<T> oldObject = null;

        //check if object already exists, find differences and increment version if necessary
        Collection<? extends ItemDelta> modifications = null;
        if (originalOid != null) {
            try {
                oldObject = objectRetriever.getObjectInternal(session, object.getCompileTimeClass(), originalOid, null, true, result);
                ObjectDelta<T> delta = oldObject.diff(object, false, true);
                modifications = delta.getModifications();

                LOGGER.trace("overwriteAddObjectAttempt: originalOid={}, modifications={}", originalOid, modifications);

                //we found existing object which will be overwritten, therefore we increment version
                Integer version = RUtil.getIntegerFromString(oldObject.getVersion());
                version = (version == null) ? 0 : ++version;

                rObject.setVersion(version);
//            } catch (QueryException ex) {
//                baseHelper.handleGeneralCheckedException(ex, session, null);
            } catch (ObjectNotFoundException ex) {
                //it's ok that object was not found, therefore we won't be overwriting it
            }
        }

        updateFullObject(rObject, object);

        RObject merged = objectDeltaUpdater.update(object, rObject, session);
        lookupTableHelper.addLookupTableRows(session, rObject, oldObject != null);
        caseHelper.addCertificationCampaignCases(session, rObject, oldObject != null);

        if (closureManager.isEnabled()) {
            OrgClosureManager.Operation operation;
            if (modifications == null) {
                operation = OrgClosureManager.Operation.ADD;
                modifications = createAddParentRefDelta(object);
            } else {
                operation = OrgClosureManager.Operation.MODIFY;
            }
            closureManager.updateOrgClosure(oldObject, modifications, session, merged.getOid(), object.getCompileTimeClass(),
                    operation, closureContext);
        }
        return merged.getOid();
    }

    private <T extends ObjectType> List<ReferenceDelta> createAddParentRefDelta(PrismObject<T> object) {
        PrismReference parentOrgRef = object.findReference(ObjectType.F_PARENT_ORG_REF);
        if (parentOrgRef == null || parentOrgRef.isEmpty()) {
            return new ArrayList<>();
        }

        PrismObjectDefinition def = object.getDefinition();
        ReferenceDelta delta = ReferenceDelta.createModificationAdd(new ItemPath(ObjectType.F_PARENT_ORG_REF),
                def, parentOrgRef.getClonedValues());

        return Arrays.asList(delta);
    }

    public <T extends ObjectType> void updateFullObject(RObject object, PrismObject<T> savedObject)
            throws DtoTranslationException, SchemaException {
        LOGGER.trace("Updating full object xml column start.");
        savedObject.setVersion(Integer.toString(object.getVersion()));

        // Deep cloning for object transformation - we don't want to return object "changed" by save.
        // Its' because we're removing some properties during save operation and if save fails,
        // overwrite attempt (for example using object importer) might try to delete existing object
        // and then try to save this object one more time.
        String xml = prismContext.xmlSerializer().serialize(savedObject);
        savedObject = prismContext.parseObject(xml);

        if (FocusType.class.isAssignableFrom(savedObject.getCompileTimeClass())) {
            savedObject.removeProperty(FocusType.F_JPEG_PHOTO);
        } else if (LookupTableType.class.equals(savedObject.getCompileTimeClass())) {
            savedObject.removeContainer(LookupTableType.F_ROW);
        } else if (AccessCertificationCampaignType.class.equals(savedObject.getCompileTimeClass())) {
            savedObject.removeContainer(AccessCertificationCampaignType.F_CASE);
        } else if (TaskType.class.isAssignableFrom(savedObject.getCompileTimeClass())) {
            savedObject.removeProperty(TaskType.F_RESULT);
        }

        xml = prismContext.xmlSerializer().serialize(savedObject);
        byte[] fullObject = RUtil.getByteArrayFromXml(xml, getConfiguration().isUseZip());

        object.setFullObject(fullObject);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Updating full object xml column finished. Xml:\n{}", xml);
        }
    }

    protected SqlRepositoryConfiguration getConfiguration() {
        return baseHelper.getConfiguration();
    }

    private <T extends ObjectType> String nonOverwriteAddObjectAttempt(PrismObject<T> object, RObject rObject,
                                                                       String originalOid, Session session, OrgClosureManager.Context closureContext)
            throws ObjectAlreadyExistsException, SchemaException, DtoTranslationException {

        // check name uniqueness (by type)
        if (StringUtils.isNotEmpty(originalOid)) {
            LOGGER.trace("Checking oid uniqueness.");
            //todo improve this table name bullshit
            Class hqlType = ClassMapper.getHQLTypeClass(object.getCompileTimeClass());
            NativeQuery query = session.createNativeQuery("select count(*) from "
                    + RUtil.getTableName(hqlType, session) + " where oid=:oid");
            query.setParameter("oid", object.getOid());

            Number count = (Number) query.uniqueResult();
            if (count != null && count.longValue() > 0) {
                throw new ObjectAlreadyExistsException("Object '" + object.getCompileTimeClass().getSimpleName()
                        + "' with oid '" + object.getOid() + "' already exists.");
            }
        }

        updateFullObject(rObject, object);

        LOGGER.trace("Saving object (non overwrite).");
        String oid = (String) session.save(rObject);
        lookupTableHelper.addLookupTableRows(session, rObject, false);
        caseHelper.addCertificationCampaignCases(session, rObject, false);

        if (closureManager.isEnabled()) {
            Collection<ReferenceDelta> modifications = createAddParentRefDelta(object);
            closureManager.updateOrgClosure(null, modifications, session, oid, object.getCompileTimeClass(),
                    OrgClosureManager.Operation.ADD, closureContext);
        }

        return oid;
    }

    public <T extends ObjectType> Object deleteObjectAttempt(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException {
        LOGGER_PERFORMANCE.debug("> delete object {}, oid={}", new Object[]{type.getSimpleName(), oid});
        Session session = null;
        OrgClosureManager.Context closureContext = null;
        try {
            session = baseHelper.beginTransaction();

            closureContext = closureManager.onBeginTransactionDelete(session, type, oid);

            Class clazz = ClassMapper.getHQLTypeClass(type);

            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery(clazz);
            cq.where(cb.equal(cq.from(clazz).get("oid"), oid));

            Query query = session.createQuery(cq);
            RObject object = (RObject) query.uniqueResult();
            if (object == null) {
                throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '" + oid
                        + "' was not found.", null, oid);
            }

            closureManager.updateOrgClosure(null, null, session, oid, type, OrgClosureManager.Operation.DELETE, closureContext);

            session.delete(object);
            if (LookupTableType.class.equals(type)) {
                lookupTableHelper.deleteLookupTableRows(session, oid);
            }
            if (AccessCertificationCampaignType.class.equals(type)) {
                caseHelper.deleteCertificationCampaignCases(session, oid);
            }

            session.getTransaction().commit();
        } catch (ObjectNotFoundException ex) {
            baseHelper.rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
        } finally {
            cleanupClosureAndSessionAndResult(closureContext, session, result);
        }
        return null;            // just because of executeAttempts wrapper
    }

    public <T extends ObjectType> void modifyObjectAttempt(Class<T> type, String oid,
			Collection<? extends ItemDelta> modifications, ModificationPrecondition<T> precondition,
			RepoModifyOptions modifyOptions, OperationResult result, SqlRepositoryServiceImpl sqlRepositoryService)
		    throws ObjectNotFoundException,
		    SchemaException, ObjectAlreadyExistsException, SerializationRelatedException, PreconditionViolationException {

        // clone - because some certification and lookup table related methods manipulate this collection and even their constituent deltas
        // TODO clone elements only if necessary
        modifications = CloneUtil.cloneCollectionMembers(modifications);
        //modifications = new ArrayList<>(modifications);

        LOGGER.debug("Modifying object '{}' with oid '{}'.", type.getSimpleName(), oid);
        LOGGER_PERFORMANCE.debug("> modify object {}, oid={}, modifications={}", type.getSimpleName(), oid, modifications);
	    LOGGER.trace("Modifications:\n{}", DebugUtil.debugDumpLazily(modifications));

        Session session = null;
        OrgClosureManager.Context closureContext = null;
        try {
            session = baseHelper.beginTransaction();

            closureContext = closureManager.onBeginTransactionModify(session, type, oid, modifications);

            Collection<? extends ItemDelta> lookupTableModifications = lookupTableHelper.filterLookupTableModifications(type, modifications);
            Collection<? extends ItemDelta> campaignCaseModifications = caseHelper.filterCampaignCaseModifications(type, modifications);

            if (!modifications.isEmpty() || RepoModifyOptions.isExecuteIfNoChanges(modifyOptions)) {

                // JpegPhoto (RFocusPhoto) is a special kind of entity. First of all, it is lazily loaded, because photos are really big.
                // Each RFocusPhoto naturally belongs to one RFocus, so it would be appropriate to set orphanRemoval=true for focus-photo
                // association. However, this leads to a strange problem when merging in-memory RFocus object with the database state:
                // If in-memory RFocus object has no photo associated (because of lazy loading), then the associated RFocusPhoto is deleted.
                //
                // To prevent this behavior, we've set orphanRemoval to false. Fortunately, the remove operation on RFocus
                // seems to be still cascaded to RFocusPhoto. What we have to implement ourselves, however, is removal of RFocusPhoto
                // _without_ removing of RFocus. In order to know whether the photo has to be removed, we have to retrieve
                // its value, apply the delta (e.g. if the delta is a DELETE VALUE X, we have to know whether X matches current
                // value of the photo), and if the resulting value is empty, we have to manually delete the RFocusPhoto instance.
                //
                // So the first step is to retrieve the current value of photo - we obviously do this only if the modifications
                // deal with the jpegPhoto property.
                Collection<SelectorOptions<GetOperationOptions>> options;
                boolean containsFocusPhotoModification = FocusType.class.isAssignableFrom(type) && containsPhotoModification(modifications);
                if (containsFocusPhotoModification) {
                    options = Collections.singletonList(SelectorOptions.create(FocusType.F_JPEG_PHOTO, GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
                } else {
                    options = null;
                }

                // get object
                PrismObject<T> prismObject = objectRetriever.getObjectInternal(session, type, oid, options, true, result);
                if (precondition != null && !precondition.holds(prismObject)) {
                	throw new PreconditionViolationException("Modification precondition does not hold for " + prismObject);
                }
	            sqlRepositoryService.invokeConflictWatchers(w -> w.beforeModifyObject(prismObject));
                // apply diff
				LOGGER.trace("OBJECT before:\n{}", prismObject.debugDumpLazily());
                PrismObject<T> originalObject = null;
                if (closureManager.isEnabled()) {
                    originalObject = prismObject.clone();
                }

                // old implementation start
//                ItemDelta.applyTo(modifications, prismObject);
//                LOGGER.trace("OBJECT after:\n{}", prismObject.debugDumpLazily());
//                // Continuing the photo treatment: should we remove the (now obsolete) focus photo?
//                // We have to test prismObject at this place, because updateFullObject (below) removes photo property from the prismObject.
//                boolean shouldPhotoBeRemoved = containsFocusPhotoModification && ((FocusType) prismObject.asObjectable()).getJpegPhoto() == null;
//
//                // merge and update object
//                LOGGER.trace("Translating JAXB to data type.");
//                ObjectTypeUtil.normalizeAllRelations(prismObject);
//                RObject rObject = createDataObjectFromJAXB(prismObject, PrismIdentifierGenerator.Operation.MODIFY);
//                rObject.setVersion(rObject.getVersion() + 1);
//
//                updateFullObject(rObject, prismObject);
//                LOGGER.trace("Starting merge.");
//                session.merge(rObject);
                // old implementation end

                // new implementation start
                RObject rObject = objectDeltaUpdater.modifyObject(type, oid, modifications, prismObject, session);

				LOGGER.trace("OBJECT after:\n{}", prismObject.debugDumpLazily());
                // Continuing the photo treatment: should we remove the (now obsolete) focus photo?
                // We have to test prismObject at this place, because updateFullObject (below) removes photo property from the prismObject.
                boolean shouldPhotoBeRemoved = containsFocusPhotoModification && ((FocusType) prismObject.asObjectable()).getJpegPhoto() == null;

                updateFullObject(rObject, prismObject);

                LOGGER.trace("Starting save.");
                session.save(rObject);
                LOGGER.trace("Save finished.");
                // new implementation end

                if (closureManager.isEnabled()) {
                    closureManager.updateOrgClosure(originalObject, modifications, session, oid, type, OrgClosureManager.Operation.MODIFY, closureContext);
                }

                // JpegPhoto cleanup: As said before, if a focus has to have no photo (after modifications are applied),
                // we have to remove the photo manually.
                if (shouldPhotoBeRemoved) {
                    Query query = session.createQuery("delete RFocusPhoto where ownerOid = :oid");
                    query.setParameter("oid", prismObject.getOid());
                    query.executeUpdate();
                    LOGGER.trace("Focus photo for {} was deleted", prismObject.getOid());
                }
            }

            if (LookupTableType.class.isAssignableFrom(type)) {
                lookupTableHelper.updateLookupTableData(session, oid, lookupTableModifications);
            }
            if (AccessCertificationCampaignType.class.isAssignableFrom(type)) {
                caseHelper.updateCampaignCases(session, oid, campaignCaseModifications, modifyOptions);
            }

            LOGGER.trace("Before commit...");
            session.getTransaction().commit();
            LOGGER.trace("Committed!");
        } catch (ObjectNotFoundException | SchemaException ex) {
            baseHelper.rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (PersistenceException ex) {
            ConstraintViolationException constEx = findConstraintViolationException(ex);
	        if (constEx != null) {
		        handleConstraintViolationException(session, constEx, result);
		        baseHelper.rollbackTransaction(session, constEx, result, true);
		        LOGGER.debug("Constraint violation occurred (will be rethrown as ObjectAlreadyExistsException).", constEx);
		        // we don't know if it's only name uniqueness violation, or something else,
		        // therefore we're throwing it always as ObjectAlreadyExistsException

		        //todo improve (we support only 5 DB, so we should probably do some hacking in here)
		        throw new ObjectAlreadyExistsException(constEx);
	        } else {
	            baseHelper.handleGeneralException(ex, session, result);
	        }
        } catch (DtoTranslationException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
        } finally {
            cleanupClosureAndSessionAndResult(closureContext, session, result);
            LOGGER.trace("Session cleaned up.");
        }
    }

    private <T extends ObjectType> boolean containsPhotoModification(Collection<? extends ItemDelta> modifications) {
        ItemPath photoPath = new ItemPath(FocusType.F_JPEG_PHOTO);
        for (ItemDelta delta : modifications) {
            ItemPath path = delta.getPath();
            if (path.isEmpty()) {
                throw new UnsupportedOperationException("Focus cannot be modified via empty-path modification");
            } else if (photoPath.isSubPathOrEquivalent(path)) { // actually, "subpath" variant should not occur
                return true;
            }
        }

        return false;
    }

    private void cleanupClosureAndSessionAndResult(final OrgClosureManager.Context closureContext, final Session session, final OperationResult result) {
        if (closureContext != null) {
            closureManager.cleanUpAfterOperation(closureContext, session);
        }
        baseHelper.cleanupSessionAndResult(session, result);
    }

    private void handleConstraintViolationException(Session session, ConstraintViolationException ex, OperationResult result) {

        // BRUTAL HACK - in PostgreSQL, concurrent changes in parentRefOrg sometimes cause the following exception
        // "duplicate key value violates unique constraint "XXXX". This is *not* an ObjectAlreadyExistsException,
        // more likely it is a serialization-related one.
        //
        // TODO: somewhat generalize this approach - perhaps by retrying all operations not dealing with OID/name uniqueness
        //
        // see MID-4698

        SQLException sqlException = baseHelper.findSqlException(ex);
        if (sqlException != null) {
            SQLException nextException = sqlException.getNextException();
            LOGGER.debug("ConstraintViolationException = {}; SQL exception = {}; embedded SQL exception = {}", ex, sqlException, nextException);
            String[] okStrings = new String[] {
                    "Violation of PRIMARY KEY constraint 'PK__m_org_cl__",
                    "Violation of PRIMARY KEY constraint 'PK__m_refere__",
                    "Violation of PRIMARY KEY constraint 'PK__m_assign__",
                    "Violation of PRIMARY KEY constraint 'PK__m_operat__",
                    "is not present in table \"m_ext_item\"",
                    "duplicate key value violates unique constraint \"m_org_closure_pkey\"",
                    "duplicate key value violates unique constraint \"m_reference_pkey\"",
                    "duplicate key value violates unique constraint \"m_assignment_pkey\"",
                    "duplicate key value violates unique constraint \"m_operation_execution_pkey\""     // TODO resolve more intelligently (and completely!)
            };
            String msg1;
            if (sqlException.getMessage() != null) {
                msg1 = sqlException.getMessage();
            } else {
                msg1 = "";
            }
            String msg2;
            if (nextException != null && nextException.getMessage() != null) {
                msg2 = nextException.getMessage();
            } else {
                msg2 = "";
            }
            for (String okString : okStrings) {
                if (msg1.contains(okString) || msg2.contains(okString)) {
                    baseHelper.rollbackTransaction(session, ex, result, false);
                    throw new SerializationRelatedException(ex);
                }
            }
        }
    }

    public <T extends ObjectType> RObject createDataObjectFromJAXB(PrismObject<T> prismObject, PrismIdentifierGenerator<T> idGenerator)
            throws SchemaException {

        IdGeneratorResult generatorResult = idGenerator.generate(prismObject);

        T object = prismObject.asObjectable();

        RObject rObject;
        Class<? extends RObject> clazz = ClassMapper.getHQLTypeClass(object.getClass());
        try {
            rObject = clazz.newInstance();
            Method method = clazz.getMethod("copyFromJAXB", object.getClass(), clazz,
                    RepositoryContext.class, IdGeneratorResult.class);
            method.invoke(clazz, object, rObject, new RepositoryContext(repositoryService, prismContext, extItemDictionary), generatorResult);
        } catch (Exception ex) {
            SerializationRelatedException serializationException = ExceptionUtil.findCause(ex, SerializationRelatedException.class);
            if (serializationException != null) {
                throw serializationException;
            }
            String message = ex.getMessage();
            if (StringUtils.isEmpty(message) && ex.getCause() != null) {
                message = ex.getCause().getMessage();
            }
            throw new SchemaException(message, ex);
        }

        return rObject;
    }

}
