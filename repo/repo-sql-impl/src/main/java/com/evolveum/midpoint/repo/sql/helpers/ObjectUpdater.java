/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.persistence.PersistenceException;

import com.evolveum.midpoint.prism.path.PathSet;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.sql.RestartOperationRequestedException;
import com.evolveum.midpoint.repo.sql.SerializationRelatedException;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.helpers.delta.ObjectDeltaUpdater;
import com.evolveum.midpoint.repo.sql.util.*;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.HANDLED_ERROR;

@Component
public class ObjectUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectUpdater.class);
    private static final Trace LOGGER_PERFORMANCE = TraceManager.getTrace(SqlRepositoryServiceImpl.PERFORMANCE_LOG_NAME);

    @Autowired
    @Qualifier("repositoryService")
    private RepositoryService repositoryService;

    @Autowired private BaseHelper baseHelper;
    @Autowired private ObjectRetriever objectRetriever;
    @Autowired private LookupTableHelper lookupTableHelper;
    @Autowired private CertificationCaseHelper caseHelper;
    @Autowired private OrgClosureManager closureManager;
    @Autowired private ObjectDeltaUpdater objectDeltaUpdater;
    @Autowired private PrismContext prismContext;
    @Autowired private SchemaService schemaService;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private ExtItemDictionary extItemDictionary;

    public <T extends ObjectType> String addObjectAttempt(
            PrismObject<T> object, RepoAddOptions options, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {

        String classSimpleName = object.getCompileTimeClass() != null
                ? object.getCompileTimeClass().getSimpleName()
                : "(unknown class)";
        LOGGER_PERFORMANCE.debug("> add object {}, oid={}, overwrite={}",
                classSimpleName, object.getOid(), options.isOverwrite());

        String oid = null;
        Session session = null;
        OrgClosureManager.Context closureContext = null;
        // it is needed to keep the original oid for example for import options. if we do not keep it
        // and it was null it can bring some error because the oid is set when the object contains orgRef
        // or it is org. and by the import we do not know it so it will be trying to delete non-existing object
        String originalOid = object.getOid();
        try {
            LOGGER.trace("Object\n{}", object.debugDumpLazily());
            ObjectTypeUtil.normalizeAllRelations(object, relationRegistry);

            LOGGER.trace("Translating JAXB to data type.");
            PrismIdentifierGenerator.Operation operation = options.isOverwrite() ?
                    PrismIdentifierGenerator.Operation.ADD_WITH_OVERWRITE :
                    PrismIdentifierGenerator.Operation.ADD;
            PrismIdentifierGenerator idGenerator = new PrismIdentifierGenerator(operation);

            session = baseHelper.beginTransaction();

            RObject rObject = createDataObjectFromJAXB(object, idGenerator);

            // ignore options.isOverwrite() here, it's not used
            closureContext = closureManager.onBeginTransactionAdd(session, object, options.isOverwrite());

            if (options.isOverwrite()) {
                oid = overwriteAddObjectAttempt(object, rObject, originalOid, session, closureContext);
            } else {
                oid = nonOverwriteAddObjectAttempt(object, rObject, originalOid, session, closureContext);
            }
            session.getTransaction().commit();

            LOGGER.trace("Saved object '{}' with oid '{}'", classSimpleName, oid);

            object.setOid(oid);
        } catch (PersistenceException ex) {
            ConstraintViolationException constEx = ExceptionUtil.findCause(ex, ConstraintViolationException.class);
            if (constEx == null) {
                baseHelper.handleGeneralException(ex, session, result);
                throw new AssertionError("shouldn't be here");
            }
            // TODO use this throughout overwriteAddObjectAttempt to collect information about no-fetch insertion attempts
            AttemptContext attemptContext = new AttemptContext();
            handleConstraintViolationExceptionSpecialCases(constEx, session, attemptContext, result);
            baseHelper.rollbackTransaction(session, constEx, result, FATAL_ERROR);

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
            baseHelper.rollbackTransaction(session, ex, result, FATAL_ERROR);
            throw ex;
        } catch (DtoTranslationException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
        } finally {
            cleanupClosureAndSessionAndResult(closureContext, session, result);
        }

        return oid;
    }

    private <T extends ObjectType> String overwriteAddObjectAttempt(
            PrismObject<T> object, RObject rObject, String originalOid, Session session,
            OrgClosureManager.Context closureContext)
            throws SchemaException, DtoTranslationException {

        PrismObject<T> oldObject = null;

        //check if object already exists, find differences and increment version if necessary
        Collection<? extends ItemDelta<?, ?>> modifications = null;
        if (originalOid != null) {
            try {
                oldObject = objectRetriever.getObjectInternal(session, object.getCompileTimeClass(), originalOid, null, true);
                object.setUserData(RepositoryService.KEY_ORIGINAL_OBJECT, oldObject);
                ObjectDelta<T> delta = oldObject.diff(object, EquivalenceStrategy.LITERAL);
                modifications = delta.getModifications();

                LOGGER.trace("overwriteAddObjectAttempt: originalOid={}, modifications={}", originalOid, modifications);

                //we found existing object which will be overwritten, therefore we increment version
                Integer version = RUtil.getIntegerFromString(oldObject.getVersion());
                version = (version == null) ? 0 : ++version;

                rObject.setVersion(version);
            } catch (ObjectNotFoundException ex) {
                //it's ok that object was not found, therefore we won't be overwriting it
            }
        }

        updateFullObject(rObject, object);

        RObject merged = (RObject) session.merge(rObject);
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

        PrismObjectDefinition<?> def = object.getDefinition();
        ReferenceDelta delta = prismContext.deltaFactory().reference().createModificationAdd(ObjectType.F_PARENT_ORG_REF,
                def, parentOrgRef.getClonedValues());

        return Collections.singletonList(delta);
    }

    <T extends ObjectType> void updateFullObject(RObject object, PrismObject<T> savedObject) throws SchemaException {
        LOGGER.trace("Updating full object xml column start.");
        savedObject.setVersion(Integer.toString(object.getVersion()));

        PathSet itemsToSkip = new PathSet();
        Class<T> compileTimeClass = savedObject.getCompileTimeClass();
        assert compileTimeClass != null;
        if (FocusType.class.isAssignableFrom(compileTimeClass)) {
            itemsToSkip.add(FocusType.F_JPEG_PHOTO);
        } else if (LookupTableType.class.equals(compileTimeClass)) {
            itemsToSkip.add(LookupTableType.F_ROW);
        } else if (AccessCertificationCampaignType.class.equals(compileTimeClass)) {
            itemsToSkip.add(AccessCertificationCampaignType.F_CASE);
        } else if (TaskType.class.isAssignableFrom(compileTimeClass)) {
            itemsToSkip.add(TaskType.F_RESULT);
        }

        String xml = prismContext.serializerFor(getConfiguration().getFullObjectFormat())
                .itemsToSkip(itemsToSkip)
                .options(SerializationOptions
                        .createSerializeReferenceNamesForNullOids()
                        .skipIndexOnly(true)
                        .skipTransient(true))
                .serialize(savedObject);
        byte[] fullObject = RUtil.getBytesFromSerializedForm(xml, getConfiguration().isUseZip());

        object.setFullObject(fullObject);

        LOGGER.trace("Updating full object xml column finished. Xml:\n{}", xml);
    }

    protected SqlRepositoryConfiguration getConfiguration() {
        return baseHelper.getConfiguration();
    }

    private <T extends ObjectType> String nonOverwriteAddObjectAttempt(PrismObject<T> object, RObject rObject,
            String originalOid, Session session, OrgClosureManager.Context closureContext)
            throws ObjectAlreadyExistsException, SchemaException {

        // check name uniqueness (by type)
        if (StringUtils.isNotEmpty(originalOid)) {
            LOGGER.trace("Checking oid uniqueness.");
            // TODO improve this table name nonsense
            Class<?> hqlType = ClassMapper.getHQLTypeClass(object.getCompileTimeClass());
            NativeQuery<?> query = session.createNativeQuery("select count(*) from "
                    + RUtil.getTableName(hqlType, session) + " where oid=:oid");
            query.setParameter("oid", object.getOid());

            Number count = (Number) query.uniqueResult();
            if (count != null && count.longValue() > 0) {
                //noinspection ConstantConditions
                throw new ObjectAlreadyExistsException("Object '" + object.getCompileTimeClass().getSimpleName()
                        + "' with oid '" + object.getOid() + "' already exists.");
            }
        }

        updateFullObject(rObject, object);

        LOGGER.trace("Saving object (non overwrite).");
        session.persist(rObject);
        lookupTableHelper.addLookupTableRows(session, rObject, false);
        caseHelper.addCertificationCampaignCases(session, rObject, false);

        String oid = rObject.getOid();
        if (oid == null) {
            throw new IllegalStateException("OID was not assigned to the object added");
        }

        if (closureManager.isEnabled()) {
            Collection<ReferenceDelta> modifications = createAddParentRefDelta(object);
            closureManager.updateOrgClosure(null, modifications, session, oid, object.getCompileTimeClass(),
                    OrgClosureManager.Operation.ADD, closureContext);
        }

        return oid;
    }

    public <T extends ObjectType> DeleteObjectResult deleteObjectAttempt(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException {
        LOGGER_PERFORMANCE.debug("> delete object {}, oid={}", type.getSimpleName(), oid);
        Session session = null;
        OrgClosureManager.Context closureContext = null;
        try {
            session = baseHelper.beginTransaction();

            Class<? extends RObject> clazz = ClassMapper.getHQLTypeClass(type);
            RObject object = session.get(clazz, oid);
            if (object == null) {
                throw new ObjectNotFoundException(
                        "Object of type '" + type.getSimpleName() + "' with oid '" + oid + "' was not found.", clazz, oid);
            }
            Class<? extends ObjectType> actualType = ClassMapper.getObjectTypeForHQLType(object.getClass()).getClassDefinition();

            closureContext = closureManager.onBeginTransactionDelete(session, actualType, oid);
            closureManager.updateOrgClosure(null, null, session, oid, actualType, OrgClosureManager.Operation.DELETE, closureContext);

            session.delete(object);
            if (LookupTableType.class.equals(actualType)) {
                lookupTableHelper.deleteLookupTableRows(session, oid);
            }
            if (AccessCertificationCampaignType.class.equals(actualType)) {
                caseHelper.deleteCertificationCampaignCases(session, oid);
            }

            session.getTransaction().commit();
            return new DeleteObjectResult(
                    RUtil.getSerializedFormFromBytes(object.getFullObject()));
        } catch (ObjectNotFoundException ex) {
            baseHelper.rollbackTransaction(session, ex, result, FATAL_ERROR); // TODO might be handled error instead?
            throw ex;
        } catch (RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
            throw new AssertionError("Should not get here");
        } finally {
            cleanupClosureAndSessionAndResult(closureContext, session, result);
        }
    }

    /**
     * @param externalSession If non-null, this session is used to execute the operation. Note that usual commit/rollback is
     * issued even if external session is present. We assume we are the last element of the processing in the session.
     */
    public <T extends ObjectType> ModifyObjectResult<T> modifyObjectAttempt(
            Class<T> type, String oid, Collection<? extends ItemDelta<?, ?>> originalModifications,
            ModificationPrecondition<T> precondition, RepoModifyOptions originalModifyOptions,
            int attempt, OperationResult result, SqlRepositoryServiceImpl sqlRepositoryService,
            boolean noFetchExtensionValueInsertionForbidden, Session externalSession)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
            SerializationRelatedException, PreconditionViolationException {

        RepoModifyOptions modifyOptions = adjustExtensionValuesHandling(originalModifyOptions, noFetchExtensionValueInsertionForbidden);
        AttemptContext attemptContext = new AttemptContext();

        // clone - because some certification and lookup table related methods manipulate this collection and even their constituent deltas
        // TODO clone elements only if necessary
        Collection<? extends ItemDelta<?, ?>> modifications =
                CloneUtil.cloneCollectionMembers(originalModifications);
        //modifications = new ArrayList<>(modifications);

        LOGGER.debug("Modifying object '{}' with oid '{}' (attempt {}) (adjusted options: {})", type.getSimpleName(), oid, attempt, modifyOptions);
        LOGGER_PERFORMANCE.debug("> modify object {}, oid={} (attempt {}), modifications={}", type.getSimpleName(), oid, attempt, modifications);
        LOGGER.trace("Modifications:\n{}", DebugUtil.debugDumpLazily(modifications));
        LOGGER.trace("noFetchExtensionValueInsertionForbidden: {}", noFetchExtensionValueInsertionForbidden);

        Session session = externalSession;
        OrgClosureManager.Context closureContext = null;
        try {
            if (session == null) {
                session = baseHelper.beginTransaction();
            }

            closureContext = closureManager.onBeginTransactionModify(session, type, oid, modifications);

            Collection<? extends ItemDelta<?, ?>> lookupTableModifications = lookupTableHelper.filterLookupTableModifications(type, modifications);
            Collection<? extends ItemDelta<?, ?>> campaignCaseModifications = caseHelper.filterCampaignCaseModifications(type, modifications);

            ModifyObjectResult<T> rv;

            boolean reindex = RepoModifyOptions.isForceReindex(modifyOptions);
            if (!modifications.isEmpty() || reindex) {

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
                //
                // TODO handling of "externally stored" items (focus.jpegPhoto, task.result, lookupTable.row, ...)
                //  is a kind of ugly magic. It needs to be reviewed and fixed.
                GetOperationOptionsBuilder optionsBuilder = schemaService.getOperationOptionsBuilder();
                boolean containsFocusPhotoModification = FocusType.class.isAssignableFrom(type) && containsPhotoModification(modifications);
                if (containsFocusPhotoModification) {
                    LOGGER.trace("Setting 'retrieve' option on jpegPhoto for object fetching because containsFocusPhotoModification=true");
                    optionsBuilder = optionsBuilder.item(FocusType.F_JPEG_PHOTO).retrieve();
                }
                if (reindex) {
                    LOGGER.trace("Setting 'raw' option for object fetching because reindex is being applied");
                    optionsBuilder = optionsBuilder.root().raw();
                    if (TaskType.class.isAssignableFrom(type) || ShadowType.class.isAssignableFrom(type)) {
                        // Certification campaigns and lookup tables treat their externally stored items (cases, rows)
                        // in a different way that collides with the use of "retrieve" option. TODO resolve this!
                        LOGGER.trace("Setting 'retrieve' option for object fetching because reindex is being applied");
                        optionsBuilder = optionsBuilder.root().retrieve();
                    } else {
                        LOGGER.trace("Setting 'retrieve' option for c:extension for object fetching because reindex is being applied");
                        optionsBuilder = optionsBuilder.item(ObjectType.F_EXTENSION).retrieve();        // index-only items can be also here
                    }
                }

                // get object
                PrismObject<T> prismObject = objectRetriever.getObjectInternal(session, type, oid, optionsBuilder.build(), true);
                if (precondition != null && !precondition.holds(prismObject)) {
                    throw new PreconditionViolationException("Modification precondition does not hold for " + prismObject);
                }
                sqlRepositoryService.invokeConflictWatchers(w -> w.beforeModifyObject(prismObject));
                // apply diff
                LOGGER.trace("OBJECT before:\n{}", prismObject.debugDumpLazily());
                PrismObject<T> originalObject = prismObject.clone();

                boolean shouldPhotoBeRemoved;
                if (reindex) {
                    // old implementation start
                    ItemDeltaCollectionsUtil.applyTo(modifications, prismObject);
                    LOGGER.trace("OBJECT after:\n{}", prismObject.debugDumpLazily());
                    // Continuing the photo treatment: should we remove the (now obsolete) focus photo?
                    // We have to test prismObject at this place, because updateFullObject (below) removes photo property from the prismObject.
                    shouldPhotoBeRemoved = containsFocusPhotoModification && ((FocusType) prismObject.asObjectable()).getJpegPhoto() == null;

                    // merge and update object
                    LOGGER.trace("Translating JAXB to data type.");
                    ObjectTypeUtil.normalizeAllRelations(prismObject, relationRegistry);
                    PrismIdentifierGenerator idGenerator = new PrismIdentifierGenerator(PrismIdentifierGenerator.Operation.MODIFY);
                    RObject rObject = createDataObjectFromJAXB(prismObject, idGenerator);
                    rObject.setVersion(rObject.getVersion() + 1);

                    updateFullObject(rObject, prismObject);
                    LOGGER.trace("Starting merge.");
                    session.merge(rObject);
                    // old implementation end
                } else {
                    // new implementation start
                    RObject rObject = objectDeltaUpdater.modifyObject(type, oid, modifications, prismObject, modifyOptions, session, attemptContext);

                    LOGGER.trace("OBJECT after:\n{}", prismObject.debugDumpLazily());
                    // Continuing the photo treatment: should we remove the (now obsolete) focus photo?
                    // We have to test prismObject at this place, because updateFullObject (below) removes photo property from the prismObject.
                    shouldPhotoBeRemoved =
                            containsFocusPhotoModification && ((FocusType) prismObject.asObjectable()).getJpegPhoto() == null;

                    updateFullObject(rObject, prismObject);

                    LOGGER.trace("Starting save.");
                    session.save(rObject);
                    LOGGER.trace("Save finished.");
                    // new implementation end
                }

                if (closureManager.isEnabled()) {
                    closureManager.updateOrgClosure(originalObject, modifications, session, oid, type, OrgClosureManager.Operation.MODIFY, closureContext);
                }

                // JpegPhoto cleanup: As said before, if a focus has to have no photo (after modifications are applied),
                // we have to remove the photo manually.
                if (shouldPhotoBeRemoved) {
                    Query<?> query = session.createQuery("delete RFocusPhoto where ownerOid = :oid");
                    query.setParameter("oid", prismObject.getOid());
                    query.executeUpdate();
                    LOGGER.trace("Focus photo for {} was deleted", prismObject.getOid());
                }
                rv = new ModifyObjectResult<>(originalObject, prismObject, originalModifications);
            } else {
                rv = new ModifyObjectResult<>(originalModifications);
            }

            if (LookupTableType.class.isAssignableFrom(type)) {
                lookupTableHelper.updateLookupTableData(session, oid, lookupTableModifications);
            }
            if (AccessCertificationCampaignType.class.isAssignableFrom(type)) {
                caseHelper.updateCampaignCases(session, oid, campaignCaseModifications, modifyOptions);
            }

            LOGGER.trace("Before commit...");
            session.getTransaction().commit();
            LOGGER.trace("Committed! (at attempt {})", attempt);
            return rv;
        } catch (ObjectNotFoundException | SchemaException ex) {
            baseHelper.rollbackTransaction(session, ex, result, FATAL_ERROR);
            throw ex;
        } catch (PersistenceException ex) {
            ConstraintViolationException constEx = ExceptionUtil.findCause(ex, ConstraintViolationException.class);
            if (constEx != null) {
                handleConstraintViolationExceptionSpecialCases(constEx, session, attemptContext, result);
                baseHelper.rollbackTransaction(session, constEx, result, FATAL_ERROR);
                LOGGER.debug("Constraint violation occurred (will be rethrown as ObjectAlreadyExistsException).", constEx);
                // we don't know if it's only name uniqueness violation, or something else,
                // therefore we're throwing it always as ObjectAlreadyExistsException

                //todo improve (we support only 5 DB, so we should probably do some hacking in here)
                throw new ObjectAlreadyExistsException(constEx);
            } else {
                baseHelper.handleGeneralException(ex, session, result);
                throw new AssertionError("Shouldn't get here");
            }
        } catch (DtoTranslationException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
            throw new AssertionError("Shouldn't get here");
        } finally {
            cleanupClosureAndSessionAndResult(closureContext, session, result);
            LOGGER.trace("Session cleaned up.");
        }
    }

    private RepoModifyOptions adjustExtensionValuesHandling(RepoModifyOptions options,
            boolean noFetchExtensionValueInsertionForbidden) {
        RepoModifyOptions rv = options != null ? options.clone() : new RepoModifyOptions();
        SqlRepositoryConfiguration config = getConfiguration();
        rv.setUseNoFetchExtensionValuesInsertion(config.isEnableNoFetchExtensionValuesInsertion() &&
                !noFetchExtensionValueInsertionForbidden &&
                !Boolean.FALSE.equals(rv.getUseNoFetchExtensionValuesInsertion()));
        // TODO implement more complex heuristics when the options come with null value for no-fetch deletion
        //  (e.g. doing that for extensions having index-only values, and by comparing # of values to deleted
        //  with overall # of values)
        rv.setUseNoFetchExtensionValuesDeletion(config.isEnableNoFetchExtensionValuesDeletion() &&
                !Boolean.FALSE.equals(rv.getUseNoFetchExtensionValuesDeletion()));
        return rv;
    }

    private boolean containsPhotoModification(Collection<? extends ItemDelta<?, ?>> modifications) {
        for (ItemDelta<?, ?> delta : modifications) {
            ItemPath path = delta.getPath();
            if (path.isEmpty()) {
                throw new UnsupportedOperationException("Focus cannot be modified via empty-path modification");
            } else if (FocusType.F_JPEG_PHOTO.isSubPathOrEquivalent(path)) { // actually, "subpath" variant should not occur
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

    public <T extends ObjectType> ModifyObjectResult<T> modifyObjectDynamicallyAttempt(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> getOptions,
            RepositoryService.ModificationsSupplier<T> modificationsSupplier,
            RepoModifyOptions modifyOptions, int attempt, OperationResult result,
            SqlRepositoryServiceImpl sqlRepositoryService, boolean noFetchExtensionValueInsertionForbidden)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        LOGGER_PERFORMANCE.debug("> modify object dynamically {}, oid={}", type.getSimpleName(), oid);

        try (Session session = baseHelper.beginTransaction()) {

            PrismObject<T> objectBefore;
            Collection<? extends ItemDelta<?, ?>> modifications;
            try {
                objectBefore = objectRetriever.getObjectInternal(session, type, oid, getOptions, true);
                LOGGER.trace("Object retrieved:\n{}", objectBefore.debugDumpLazily(1));

                // Intentionally within this try-catch block because this call must be covered by proper exception handling.
                modifications = modificationsSupplier.get(objectBefore.asObjectable());
                LOGGER.trace("Modifications computed:\n{}", DebugUtil.debugDumpLazily(modifications, 1));
            } catch (ObjectNotFoundException ex) {
                GetOperationOptions rootOptions = SelectorOptions.findRootOptions(getOptions);
                baseHelper.rollbackTransaction(session, ex, result,
                        GetOperationOptions.isAllowNotFound(rootOptions) ? HANDLED_ERROR : FATAL_ERROR);
                throw ex;
            } catch (SchemaException ex) {
                baseHelper.rollbackTransaction(session, ex, "Schema error while getting object with oid: "
                        + oid + ". Reason: " + ex.getMessage(), result, FATAL_ERROR);
                throw ex;
            } catch (DtoTranslationException | RuntimeException ex) {
                baseHelper.handleGeneralException(ex, session, result);
                throw new AssertionError("shouldn't be here");
            }

            if (modifications.isEmpty() && !RepoModifyOptions.isForceReindex(modifyOptions)) {
                LOGGER.debug("Modification list is empty, nothing was modified.");
                session.getTransaction().commit();
                result.recordStatus(OperationResultStatus.SUCCESS, "Computed modification list is empty");
                return new ModifyObjectResult<>(objectBefore, objectBefore, modifications);
            }

            try {
                // TODO: eliminate redundant getObjectInternal call in modifyObjectAttempt
                return modifyObjectAttempt(type, oid, modifications, null, modifyOptions, attempt, result, sqlRepositoryService,
                        noFetchExtensionValueInsertionForbidden, session);
            } catch (PreconditionViolationException e) {
                throw new SystemException("Unexpected PreconditionViolationException: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Handles serialization-related cases and no-fetch extension value insertion collisions.
     */
    private void handleConstraintViolationExceptionSpecialCases(ConstraintViolationException ex, Session session,
            AttemptContext attemptContext, OperationResult result) {
        if (attemptContext.noFetchExtensionValueInsertionAttempted && isNoFetchExtensionValueInsertionException(ex)) {
            throw new RestartOperationRequestedException("Suspecting no-fetch extension value insertion attempt causing "
                    + "ConstraintViolationException; restarting with no-fetch insertion disabled", true);
        } else if (baseHelper.isSerializationRelatedConstraintViolationException(ex)) {
            baseHelper.rollbackTransaction(session, ex, result, null);
            throw new SerializationRelatedException(ex);
        }
    }

    private boolean isNoFetchExtensionValueInsertionException(
            @SuppressWarnings("unused") ConstraintViolationException ex) {
        return true; // keep things safe
    }

    public <T extends ObjectType> RObject createDataObjectFromJAXB(PrismObject<T> prismObject, PrismIdentifierGenerator idGenerator)
            throws SchemaException {

        IdGeneratorResult generatorResult = idGenerator.generate(prismObject);

        T object = prismObject.asObjectable();

        RObject rObject;
        Class<? extends RObject> clazz = ClassMapper.getHQLTypeClass(object.getClass());
        try {
            rObject = clazz.getConstructor().newInstance();
            // Note that methods named "copyFromJAXB" that were _not_ called from this point were renamed e.g. to "fromJaxb",
            // in order to avoid confusion with dynamically called "copyFromJAXB" method.
            Method method = clazz.getMethod("copyFromJAXB", object.getClass(), clazz,
                    RepositoryContext.class, IdGeneratorResult.class);
            method.invoke(clazz, object, rObject, new RepositoryContext(repositoryService, prismContext, relationRegistry,
                    extItemDictionary, baseHelper.getConfiguration()), generatorResult);
        } catch (Exception ex) {
            SerializationRelatedException serializationException = ExceptionUtil.findCause(ex, SerializationRelatedException.class);
            if (serializationException != null) {
                throw serializationException;
            }
            ConstraintViolationException cve = ExceptionUtil.findCause(ex, ConstraintViolationException.class);
            if (cve != null && baseHelper.isSerializationRelatedConstraintViolationException(cve)) {
                throw cve;
            }
            String message = ex.getMessage();
            if (StringUtils.isEmpty(message) && ex.getCause() != null) {
                message = ex.getCause().getMessage();
            }
            throw new SchemaException(message, ex);
        }

        return rObject;
    }

    /**
     * Gathers things relevant to the whole attempt.
     */
    @Experimental
    public static class AttemptContext {
        public boolean noFetchExtensionValueInsertionAttempted;
    }
}
