/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.xml.namespace.QName;

import org.hibernate.*;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryObjectDiagnosticData;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RShadow;
import com.evolveum.midpoint.repo.sql.data.common.any.*;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.query2.QueryEngine2;
import com.evolveum.midpoint.repo.sql.query2.RQueryImpl;
import com.evolveum.midpoint.repo.sql.query2.hqm.QueryParameterValue;
import com.evolveum.midpoint.repo.sql.util.*;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author lazyman, mederly
 */
@Component
public class ObjectRetriever {

    public static final String CLASS_DOT = ObjectRetriever.class.getName() + ".";

    private static final Trace LOGGER = TraceManager.getTrace(ObjectRetriever.class);
    private static final Trace LOGGER_PERFORMANCE = TraceManager.getTrace(SqlRepositoryServiceImpl.PERFORMANCE_LOG_NAME);

    public static final String NULL_OID_MARKER = "###null-oid###";     // brutal hack (TODO)

    @Autowired private LookupTableHelper lookupTableHelper;
    @Autowired private CertificationCaseHelper caseHelper;
    @Autowired private CaseManagementHelper caseManagementHelper;
    @Autowired private BaseHelper baseHelper;
    @Autowired private NameResolutionHelper nameResolutionHelper;
    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private ExtItemDictionary extItemDictionary;
    @Autowired
    @Qualifier("repositoryService")
    private RepositoryService repositoryService;

    public <T extends ObjectType> PrismObject<T> getObjectAttempt(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        LOGGER_PERFORMANCE.debug("> get object {}, oid={}", type.getSimpleName(), oid);
        PrismObject<T> objectType = null;

        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();

            objectType = getObjectInternal(session, type, oid, options, false, result);

            session.getTransaction().commit();
        } catch (ObjectNotFoundException ex) {
            GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
            baseHelper.rollbackTransaction(session, ex, result, !GetOperationOptions.isAllowNotFound(rootOptions));
            throw ex;
        } catch (SchemaException ex) {
            baseHelper.rollbackTransaction(session, ex, "Schema error while getting object with oid: "
                    + oid + ". Reason: " + ex.getMessage(), result, true);
            throw ex;
        } catch (DtoTranslationException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Get object:\n{}", objectType != null ? objectType.debugDump(3) : null);
        }

        return objectType;
    }

    public <T extends ObjectType> PrismObject<T> getObjectInternal(Session session, Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options,
            boolean lockForUpdate, OperationResult operationResult)
            throws ObjectNotFoundException, SchemaException, DtoTranslationException {

        boolean lockedForUpdateViaHibernate = false;
        boolean lockedForUpdateViaSql = false;

        LockOptions lockOptions = new LockOptions();
        //todo fix lock for update!!!!!
        if (lockForUpdate) {
            if (getConfiguration().isLockForUpdateViaHibernate()) {
                lockOptions.setLockMode(LockMode.PESSIMISTIC_WRITE);
                lockedForUpdateViaHibernate = true;
            } else if (getConfiguration().isLockForUpdateViaSql()) {
                LOGGER.trace("Trying to lock object {} for update (via SQL)", oid);
                long time = System.currentTimeMillis();
                NativeQuery q = session.createNativeQuery("select oid from m_object where oid = ? for update");
                q.setParameter(1, oid);
                Object result = q.uniqueResult();
                if (result == null) {
                    return throwObjectNotFoundException(type, oid);
                }
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Locked via SQL (in {} ms)", System.currentTimeMillis() - time);
                }
                lockedForUpdateViaSql = true;
            }
        }

        if (LOGGER.isTraceEnabled()) {
            if (lockedForUpdateViaHibernate) {
                LOGGER.trace("Getting object {} with locking for update (via hibernate)", oid);
            } else if (lockedForUpdateViaSql) {
                LOGGER.trace("Getting object {}, already locked for update (via SQL)", oid);
            } else {
                LOGGER.trace("Getting object {} without locking for update", oid);
            }
        }

        GetObjectResult fullObject = null;
        if (!lockForUpdate) {
            Query query = session.getNamedQuery("get.object");
            query.setParameter("oid", oid);
            query.setResultTransformer(GetObjectResult.RESULT_STYLE.getResultTransformer());
            query.setLockOptions(lockOptions);

            fullObject = (GetObjectResult) query.uniqueResult();
        } else {
            // we're doing update after this get, therefore we load full object right now
            // (it would be loaded during merge anyway)
            // this just loads object to hibernate session, probably will be removed later. Merge after this get
            // will be faster. Read and use object only from fullObject column.
            // todo remove this later [lazyman]
            Class clazz = ClassMapper.getHQLTypeClass(type);

            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery(clazz);
            cq.where(cb.equal(cq.from(clazz).get("oid"), oid));

            Query query = session.createQuery(cq);
            query.setLockOptions(lockOptions);

            RObject obj = (RObject) query.uniqueResult();

            if (obj != null) {
                fullObject = new GetObjectResult(obj.getOid(), obj.getFullObject());
            }
        }

        LOGGER.trace("Got it.");
        if (fullObject == null) {
            throwObjectNotFoundException(type, oid);
        }

        LOGGER.trace("Transforming data to JAXB type.");
        PrismObject<T> prismObject = updateLoadedObject(fullObject, type, oid, options, null, session, operationResult);
        validateObjectType(prismObject, type);

        // this was implemented to allow report parsing errors as warnings to upper layers;
        // however, it causes problems when serialization problems are encountered: in such cases, we put
        // FATAL_ERROR to the result here, and it should be then removed or muted (which is a complication)
        // -- so, as the parsing errors are not implemented, we disabled this code as well

        //            subResult.computeStatusIfUnknown();
        //            if (subResult.isWarning() || subResult.isError() || subResult.isInProgress()) {
        //                prismObject.asObjectable().setFetchResult(subResult.createOperationResultType());
        //            }

        return prismObject;
    }

    protected SqlRepositoryConfiguration getConfiguration() {
        return baseHelper.getConfiguration();
    }

    private <T extends ObjectType> PrismObject<T> throwObjectNotFoundException(Class<T> type, String oid)
            throws ObjectNotFoundException {
        throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '" + oid
                + "' was not found.", null, oid);
    }

    public <F extends FocusType> PrismObject<F> searchShadowOwnerAttempt(String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) {
        LOGGER_PERFORMANCE.debug("> search shadow owner for oid={}", shadowOid);
        PrismObject<F> owner = null;
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();
            LOGGER.trace("Selecting account shadow owner for account {}.", shadowOid);
            Query query = session.getNamedQuery("searchShadowOwner.getOwner");
            query.setParameter("oid", shadowOid);
            query.setResultTransformer(GetObjectResult.RESULT_STYLE.getResultTransformer());

            @SuppressWarnings({ "unchecked", "raw" })
            List<GetObjectResult> focuses = query.list();
            LOGGER.trace("Found {} focuses, transforming data to JAXB types.", focuses != null ? focuses.size() : 0);

            if (focuses == null || focuses.isEmpty()) {
                // account shadow owner was not found
                return null;
            } else if (focuses.size() > 1) {
                LOGGER.warn("Found {} owners for shadow oid {}, returning first owner.", focuses.size(), shadowOid);
            }

            GetObjectResult focus = focuses.get(0);
            owner = updateLoadedObject(focus, (Class<F>) FocusType.class, null, options, null, session, result);

            session.getTransaction().commit();

        } catch (SchemaException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
        }

        return owner;
    }

    public PrismObject<UserType> listAccountShadowOwnerAttempt(String accountOid, OperationResult result) {
        LOGGER_PERFORMANCE.debug("> list account shadow owner oid={}", accountOid);
        PrismObject<UserType> userType = null;
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();
            Query query = session.getNamedQuery("listAccountShadowOwner.getUser");
            query.setParameter("oid", accountOid);
            query.setResultTransformer(GetObjectResult.RESULT_STYLE.getResultTransformer());

            @SuppressWarnings({ "unchecked", "raw" })
            List<GetObjectResult> users = query.list();
            LOGGER.trace("Found {} users, transforming data to JAXB types.", users != null ? users.size() : 0);

            if (users == null || users.isEmpty()) {
                // account shadow owner was not found
                return null;
            }

            if (users.size() > 1) {
                LOGGER.warn("Found {} users for account oid {}, returning first user. [interface change needed]", users.size(), accountOid);
            }

            GetObjectResult user = users.get(0);
            userType = updateLoadedObject(user, UserType.class, null, null, null, session, result);

            session.getTransaction().commit();
        } catch (SchemaException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
        }

        return userType;
    }

    public <T extends ObjectType> int countObjectsAttempt(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) {
        LOGGER_PERFORMANCE.debug("> count objects {}", type.getSimpleName());

        int count = 0;

        Session session = null;
        try {
            Class<? extends RObject> hqlType = ClassMapper.getHQLTypeClass(type);

            session = baseHelper.beginReadOnlyTransaction();
            Number longCount;
            query = refineAssignmentHolderQuery(type, query);
            if (query == null || query.getFilter() == null) {
                // this is 5x faster than count with 3 inner joins, it can probably improved also for queries which
                // filters uses only properties from concrete entities like RUser, RRole by improving interpreter [lazyman]
                // note: distinct can be ignored here, as there is no filter, so no joins
                NativeQuery sqlQuery = session.createNativeQuery("SELECT COUNT(*) FROM " + RUtil.getTableName(hqlType, session));
                longCount = (Number) sqlQuery.uniqueResult();
            } else {
                RQuery rQuery;
                QueryEngine2 engine = new QueryEngine2(getConfiguration(), extItemDictionary, prismContext, relationRegistry);
                rQuery = engine.interpret(query, type, options, true, session);

                longCount = (Number) rQuery.uniqueResult();
            }
            LOGGER.trace("Found {} objects.", longCount);
            count = longCount != null ? longCount.intValue() : 0;

            session.getTransaction().commit();
        } catch (QueryException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
        }

        return count;
    }

    //TODO copied from QueryInterpreter, remove if full support for searching AssignmentHolderType is implemented MID-5579

    /**
     * Both ObjectType and AssignmentHolderType are mapped to RObject. So when searching for AssignmentHolderType it is not sufficient to
     * query this table. This method hacks this situation a bit by introducing explicit type filter for AssignmentHolderType.
     */
    private ObjectQuery refineAssignmentHolderQuery(Class<? extends Containerable> type, ObjectQuery query) {
        if (!type.equals(AssignmentHolderType.class)) {
            return query;
        }
        if (query == null) {
            query = prismContext.queryFactory().createQuery();
        }
        query.setFilter(prismContext.queryFactory().createType(AssignmentHolderType.COMPLEX_TYPE, query.getFilter()));
        return query;
    }

    public <C extends Containerable> int countContainersAttempt(Class<C> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) {
        boolean cases = AccessCertificationCaseType.class.equals(type);
        boolean workItems = AccessCertificationWorkItemType.class.equals(type);
        boolean caseWorkItems = CaseWorkItemType.class.equals(type);
        if (!cases && !workItems && !caseWorkItems) {
            throw new UnsupportedOperationException("Only AccessCertificationCaseType or AccessCertificationWorkItemType or CaseWorkItemType is supported here now.");
        }

        LOGGER_PERFORMANCE.debug("> count containers {}", type.getSimpleName());
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();

            QueryEngine2 engine = new QueryEngine2(getConfiguration(), extItemDictionary, prismContext, relationRegistry);
            RQuery rQuery = engine.interpret(query, type, options, true, session);
            Number longCount = (Number) rQuery.uniqueResult();
            LOGGER.trace("Found {} objects.", longCount);

            session.getTransaction().commit();
            return longCount != null ? longCount.intValue() : 0;
        } catch (QueryException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
            throw new AssertionError("Shouldn't get here; previous method call should throw an exception.");
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
        }
    }

    @NotNull
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjectsAttempt(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
        LOGGER_PERFORMANCE.debug("> search objects {}", type.getSimpleName());
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();
            RQuery rQuery;

            QueryEngine2 engine = new QueryEngine2(getConfiguration(), extItemDictionary, prismContext, relationRegistry);
            rQuery = engine.interpret(query, type, options, false, session);

            @SuppressWarnings({ "unchecked", "raw" })
            List<GetObjectResult> queryResult = rQuery.list();
            LOGGER.trace("Found {} objects, translating to JAXB.", queryResult != null ? queryResult.size() : 0);

            List<PrismObject<T>> list = queryResultToPrismObjects(queryResult, type, options, session, result);
            session.getTransaction().commit();
            return new SearchResultList<>(list);

        } catch (QueryException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
            throw new IllegalStateException("shouldn't get here");
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
        }
    }

    @NotNull
    private <T extends ObjectType> List<PrismObject<T>> queryResultToPrismObjects(List<GetObjectResult> objects, Class<T> type,
            Collection<SelectorOptions<GetOperationOptions>> options,
            Session session, OperationResult result) throws SchemaException {
        List<PrismObject<T>> rv = new ArrayList<>();
        if (objects != null) {
            for (GetObjectResult object : objects) {
                String oid = object.getOid();
                Holder<PrismObject<T>> partialValueHolder = new Holder<>();
                PrismObject<T> prismObject;
                try {
                    prismObject = updateLoadedObject(object, type, oid, options, partialValueHolder, session, result);
                } catch (Throwable t) {
                    if (!partialValueHolder.isEmpty()) {
                        prismObject = partialValueHolder.getValue();
                    } else {
                        prismObject = prismContext.createObject(type);
                        prismObject.setOid(oid);
                        prismObject.asObjectable().setName(PolyStringType.fromOrig("Unreadable object"));
                    }
                    result.recordFatalError("Couldn't retrieve " + type + " " + oid + ": " + t.getMessage(), t);
                    prismObject.asObjectable().setFetchResult(result.createOperationResultType());
                }
                rv.add(prismObject);
            }
        }
        return rv;
    }

    public <C extends Containerable> SearchResultList<C> searchContainersAttempt(Class<C> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {

        boolean cases = AccessCertificationCaseType.class.equals(type);
        boolean workItems = AccessCertificationWorkItemType.class.equals(type);
        boolean caseWorkItems = CaseWorkItemType.class.equals(type);
        if (!cases && !workItems && !caseWorkItems) {
            throw new UnsupportedOperationException("Only AccessCertificationCaseType or AccessCertificationWorkItemType or CaseWorkItemType is supported here now.");
        }

        LOGGER_PERFORMANCE.debug("> search containers {}", type.getSimpleName());
        List<C> list = new ArrayList<>();
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();

            QueryEngine2 engine = new QueryEngine2(getConfiguration(), extItemDictionary, prismContext, relationRegistry);
            RQuery rQuery = engine.interpret(query, type, options, false, session);

            if (cases) {
                @SuppressWarnings({ "unchecked", "raw" })
                List<GetContainerableResult> items = rQuery.list();
                LOGGER.trace("Found {} items (cases), translating to JAXB.", items.size());
                Map<String, PrismObject<AccessCertificationCampaignType>> campaignsCache = new HashMap<>();
                for (GetContainerableResult item : items) {
                    @SuppressWarnings({ "raw", "unchecked" })
                    C value = (C) caseHelper.updateLoadedCertificationCase(item, campaignsCache, options, session, result);
                    list.add(value);
                }
            } else if (workItems) {
                @SuppressWarnings({ "unchecked", "raw" })
                List<GetCertificationWorkItemResult> items = rQuery.list();
                LOGGER.trace("Found {} work items, translating to JAXB.", items.size());
                Map<String, PrismContainerValue<AccessCertificationCaseType>> casesCache = new HashMap<>();
                Map<String, PrismObject<AccessCertificationCampaignType>> campaignsCache = new HashMap<>();
                for (GetCertificationWorkItemResult item : items) {
                    //LOGGER.trace("- {}", item);
                    @SuppressWarnings({ "raw", "unchecked" })
                    C value = (C) caseHelper.updateLoadedCertificationWorkItem(item, casesCache, campaignsCache, options, engine, session, result);
                    list.add(value);
                }
            } else {
                assert caseWorkItems;
                @SuppressWarnings({ "unchecked", "raw" })
                List<GetContainerableIdOnlyResult> items = rQuery.list();
                LOGGER.trace("Found {} items (case work items), translating to JAXB.", items.size());
                Map<String, PrismObject<CaseType>> casesCache = new HashMap<>();

                for (GetContainerableIdOnlyResult item : items) {
                    try {
                        @SuppressWarnings({ "raw", "unchecked" })
                        C value = (C) caseManagementHelper.updateLoadedCaseWorkItem(item, casesCache, options, session, result);
                        list.add(value);
                    } catch (ObjectNotFoundException | DtoTranslationException e) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve case work item for {}", e, item);
                    }
                }
            }

            nameResolutionHelper.resolveNamesIfRequested(session, PrismContainerValue.asPrismContainerValues(list), options);

            session.getTransaction().commit();
        } catch (QueryException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
        }

        list.forEach(c -> ObjectTypeUtil.normalizeAllRelations(c.asPrismContainerValue(), relationRegistry));
        return new SearchResultList<>(list);
    }

    /**
     * This method provides object parsing from String and validation.
     */
    private <T extends ObjectType> PrismObject<T> updateLoadedObject(GetObjectResult result, Class<T> type,
            String oid, Collection<SelectorOptions<GetOperationOptions>> options,
            Holder<PrismObject<T>> partialValueHolder,
            Session session, OperationResult operationResult) throws SchemaException {

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        boolean raw = GetOperationOptions.isRaw(rootOptions);

        byte[] fullObject = result.getFullObject();
        String xml = RUtil.getXmlFromByteArray(fullObject, getConfiguration().isUseZip());
        PrismObject<T> prismObject;
        try {
            // "Postel mode": be tolerant what you read. We need this to tolerate (custom) schema changes
            ParsingContext parsingContext = prismContext.createParsingContextForCompatibilityMode();
            prismObject = prismContext.parserFor(xml).language(SqlRepositoryServiceImpl.DATA_LANGUAGE).context(parsingContext).parse();
            if (parsingContext.hasWarnings()) {
                LOGGER.warn("Object {} parsed with {} warnings", ObjectTypeUtil.toShortString(prismObject), parsingContext.getWarnings().size());
                // TODO enable if needed
//                for (String warning : parsingContext.getWarnings()) {
//                    operationResult.createSubresult("parseObject").recordWarning(warning);
//                }
            }
        } catch (SchemaException | RuntimeException | Error e) {
            // This is a serious thing. We have corrupted XML in the repo. This may happen even
            // during system init. We want really loud and detailed error here.
            LOGGER.error("Couldn't parse object {} {}: {}: {}\n{}",
                    type.getSimpleName(), oid, e.getClass().getName(), e.getMessage(), xml, e);
            throw e;
        }
        attachDiagDataIfRequested(prismObject, fullObject, options);
        if (prismObject.getCompileTimeClass() != null && FocusType.class.isAssignableFrom(prismObject.getCompileTimeClass())) {
            if (SelectorOptions.hasToLoadPath(FocusType.F_JPEG_PHOTO, options)) {
                Query query = session.getNamedQuery("get.focusPhoto");
                query.setParameter("oid", prismObject.getOid());
                byte[] photo = (byte[]) query.uniqueResult();
                if (photo != null) {
                    PrismProperty<byte[]> photoProperty = prismObject.findOrCreateProperty(FocusType.F_JPEG_PHOTO);
                    photoProperty.setRealValue(photo);
                    photoProperty.setIncomplete(false);
                } else {
                    setPropertyNullAndComplete(prismObject, FocusType.F_JPEG_PHOTO);
                }
            }
        } else if (ShadowType.class.equals(prismObject.getCompileTimeClass())) {
            //we store it because provisioning now sends it to repo, but it should be transient
            prismObject.removeContainer(ShadowType.F_ASSOCIATION);

            if (raw) {
                LOGGER.debug("Loading definitions for shadow attributes.");
                Class[] classes = GetObjectResult.EXT_COUNT_CLASSES;
                for (Class aClass : classes) {
                    // TODO restrict classes to only those that are currently present in the object
                    applyShadowAttributeDefinitions(aClass, prismObject, session);
                }
                LOGGER.debug("Definitions for attributes loaded.");
            } else {
                LOGGER.debug("Not loading definitions for shadow attributes, raw=false");
            }
        } else if (LookupTableType.class.equals(prismObject.getCompileTimeClass())) {
            lookupTableHelper.updateLoadedLookupTable(prismObject, options, session);
        } else if (AccessCertificationCampaignType.class.equals(prismObject.getCompileTimeClass())) {
            caseHelper.updateLoadedCampaign(prismObject, options, session);
        } else if (TaskType.class.equals(prismObject.getCompileTimeClass())) {
            if (SelectorOptions.hasToLoadPath(TaskType.F_RESULT, options)) {
                Query query = session.getNamedQuery("get.taskResult");
                query.setParameter("oid", prismObject.getOid());
                byte[] opResult = (byte[]) query.uniqueResult();
                if (opResult != null) {
                    String xmlResult = RUtil.getXmlFromByteArray(opResult, true);
                    OperationResultType resultType = prismContext.parserFor(xmlResult).parseRealValue(OperationResultType.class);

                    PrismProperty<OperationResultType> resultProperty = prismObject.findOrCreateProperty(TaskType.F_RESULT);
                    resultProperty.setRealValue(resultType);
                    resultProperty.setIncomplete(false);

                    prismObject.setPropertyRealValue(TaskType.F_RESULT_STATUS, resultType.getStatus());
                } else {
                    setPropertyNullAndComplete(prismObject, TaskType.F_RESULT);
                }
            }
        }
        if (getConfiguration().isEnableIndexOnlyItems()) {
            loadIndexOnlyItemsIfNeeded(prismObject, options, raw, session);
        }

        if (partialValueHolder != null) {
            partialValueHolder.setValue(prismObject);
        }
        nameResolutionHelper.resolveNamesIfRequested(session, prismObject.getValue(), options);
        validateObjectType(prismObject, type);

        ObjectTypeUtil.normalizeAllRelations(prismObject, relationRegistry);
        return prismObject;
    }

    private <T extends ObjectType> void setPropertyNullAndComplete(PrismObject<T> prismObject, ItemPath path) {
        PrismProperty<?> photoProperty = prismObject.findProperty(path);
        if (photoProperty != null) {
            photoProperty.setRealValue(null);
            photoProperty.setIncomplete(false);
        }
    }

    private <T extends ObjectType> void loadIndexOnlyItemsIfNeeded(PrismObject<T> prismObject,
            Collection<SelectorOptions<GetOperationOptions>> options, boolean raw, Session session) throws SchemaException {
        List<SelectorOptions<GetOperationOptions>> retrieveOptions = SelectorOptions.filterRetrieveOptions(options);
        LOGGER.trace("loadIndexOnlyItemsIfNeeded: retrieval options = {}", retrieveOptions);
        if (retrieveOptions.isEmpty()) {
            return;
        }

        RObject<?> rObject = null;
        for (ItemDefinition<?> itemDefinition : getIndexOnlyExtensionItems(prismObject)) {
            if (SelectorOptions.hasToLoadPath(ItemPath.create(ObjectType.F_EXTENSION, itemDefinition.getItemName()),
                    retrieveOptions, false)) {
                LOGGER.trace("We have to load index-only extension item {}", itemDefinition);
                if (rObject == null) {
                    rObject = session.load(RObject.class, prismObject.getOid());
                }
                RExtItem extItemDef = extItemDictionary.findItemByDefinition(itemDefinition);
                if (extItemDef == null) {
                    LOGGER.warn("No ext item definition for {}", itemDefinition);
                } else if (extItemDef.getId() == null) {
                    throw new IllegalStateException("Ext item definition with no ID: " + extItemDef);
                } else {
                    Collection<? extends ROExtValue> values = RAnyConverter.getExtValues(rObject, extItemDef, itemDefinition);
                    if (!values.isEmpty()) {
                        if (itemDefinition instanceof PrismPropertyDefinition) {
                            //noinspection unchecked
                            PrismProperty<Object> item = ((PrismPropertyDefinition<Object>) itemDefinition).instantiate();
                            for (ROExtValue value : values) {
                                if (value.getOwnerType() == RObjectExtensionType.EXTENSION) {   // just for sure
                                    item.addRealValue(value.getValue());
                                }
                            }
                            PrismContainerValue<?> extensionPcv = prismObject.getOrCreateExtension().getValue();
                            extensionPcv.removeProperty(itemDefinition.getItemName());      // temporary hack
                            extensionPcv.add(item);
                        } else {
                            throw new UnsupportedOperationException(
                                    "Non-property index-only items are not supported: " + itemDefinition);
                        }
                    } else {
                        // this is to remove "incomplete" flag and/or any obsolete information
                        PrismContainerValue<?> extensionPcv = prismObject.getExtensionContainerValue();
                        if (extensionPcv != null) {
                            extensionPcv.removeProperty(itemDefinition.getItemName());
                        }
                    }
                }
            }
        }
        // todo what about extension items that are not part of object extension definition?
        //  we should utilize 'incomplete' flag

        // attributes
        Class<T> compileTimeClass = prismObject.getCompileTimeClass();
        LOGGER.trace("Object class: {}", compileTimeClass);
        if (ShadowType.class.equals(compileTimeClass)) {
            boolean getAllAttributes = SelectorOptions.hasToLoadPath(ShadowType.F_ATTRIBUTES, retrieveOptions, false);
            LOGGER.trace("getAllAttributes = {}", getAllAttributes);
            if (getAllAttributes) {
                if (rObject == null) {
                    rObject = session.load(RShadow.class, prismObject.getOid());
                }
                LOGGER.trace("Going to fetch attributes from {}", rObject);
                //noinspection unchecked
                PrismObject<ShadowType> shadowObject = (PrismObject<ShadowType>) prismObject;
                retrieveAllAttributeValues(shadowObject, rObject.getBooleans(), raw);
                retrieveAllAttributeValues(shadowObject, rObject.getDates(), raw);
                retrieveAllAttributeValues(shadowObject, rObject.getLongs(), raw);
                retrieveAllAttributeValues(shadowObject, rObject.getPolys(), raw);
                retrieveAllAttributeValues(shadowObject, rObject.getReferences(), raw);
                retrieveAllAttributeValues(shadowObject, rObject.getStrings(), raw);
            } else {
                // fetching of individual attributes is not supported yet
            }
        }
    }

    private void retrieveAllAttributeValues(PrismObject<ShadowType> shadowObject,
            Collection<? extends ROExtValue<?>> dbCollection, boolean raw) throws SchemaException {
        PrismContainer<Containerable> attributeContainer = shadowObject.findOrCreateContainer(ShadowType.F_ATTRIBUTES);
        // Hack: let's ignore values of attributes that already exist in this container
        Set<QName> existingCompleteAttributeNames = attributeContainer.getValue().getItems().stream()
                .filter(item -> !item.isIncomplete())
                .map(Item::getElementName)
                .collect(Collectors.toSet());
        LOGGER.trace("existingAttributeNames = {}", existingCompleteAttributeNames);
        for (ROExtValue<?> rValue : dbCollection) {
            if (rValue.getOwnerType() == RObjectExtensionType.ATTRIBUTES) {
                LOGGER.trace("- processing {}", rValue);
                RExtItem extItem = extItemDictionary.getItemById(rValue.getItemId());
                if (extItem == null) {
                    LOGGER.warn("Couldn't get definition for extItem ID {} for value of {} in {} -- it will not be fetched",
                            rValue.getItemId(), rValue, shadowObject);
                } else {
                    ItemName extItemName = RUtil.stringToQName(extItem.getName());
                    if (!QNameUtil.matchAny(extItemName, existingCompleteAttributeNames)) {
                        PrismProperty attribute = attributeContainer.findProperty(extItemName);
                        if (attribute == null) {
                            if (raw) {
                                attribute = ((PrismPropertyDefinition) createDynamicDefinition(extItem, extItemName))
                                        .instantiate();
                            } else {
                                attribute = prismContext.itemFactory().createProperty(extItemName);
                            }
                            attributeContainer.add(attribute);
                        }
                        //noinspection unchecked
                        attribute.addRealValue(rValue.getValue());      // no polystring nor reference is expected here
                        attribute.setIncomplete(false);
                    }
                }
            }
        }
    }

    private <T extends ObjectType> Collection<ItemDefinition<?>> getIndexOnlyExtensionItems(PrismObject<T> prismObject) {
        List<ItemDefinition<?>> rv = new ArrayList<>();
        if (prismObject.getDefinition() != null) {
            PrismContainerDefinition<?> extensionDefinition = prismObject.getDefinition().getExtensionDefinition();
            if (extensionDefinition != null) {
                for (ItemDefinition definition : extensionDefinition.getDefinitions()) {
                    if (definition.isIndexOnly()) {
                        rv.add(definition);
                    }
                }
            }
        }
        return rv;
    }

    private void applyShadowAttributeDefinitions(Class<? extends RAnyValue> anyValueType,
            PrismObject object, Session session) throws SchemaException {

        PrismContainer attributes = object.findContainer(ShadowType.F_ATTRIBUTES);

        Query query = session.getNamedQuery("getDefinition." + anyValueType.getSimpleName());
        query.setParameter("oid", object.getOid());
        query.setParameter("ownerType", RObjectExtensionType.ATTRIBUTES);

        @SuppressWarnings({ "unchecked", "raw" })
        List<Integer> identifiers = query.list();
        if (identifiers == null || identifiers.isEmpty()) {
            return;
        }

        for (Integer extItemId : identifiers) {
            if (extItemId == null) {
                // Just skip. Cannot throw exceptions here. Otherwise we
                // could break raw reading.
                continue;
            }
            RExtItem extItem = extItemDictionary.getItemById(extItemId);
            if (extItem == null) {
                continue;
            }
            ItemName name = RUtil.stringToQName(extItem.getName());
            Item item = attributes.findItem(name);
            if (item != null && item.getDefinition() == null) {
                MutableItemDefinition<?> def = createDynamicDefinition(extItem, name);
                //noinspection unchecked
                item.applyDefinition(def, true);
            }
        }
    }

    @NotNull
    private MutableItemDefinition<?> createDynamicDefinition(RExtItem extItem, ItemName name) {
        QName type = RUtil.stringToQName(extItem.getType());
        RItemKind rValType = extItem.getKind();
        MutableItemDefinition<?> def;
        if (rValType == RItemKind.PROPERTY) {
            def = prismContext.definitionFactory().createPropertyDefinition(name, type);
        } else if (rValType == RItemKind.REFERENCE) {
            def = prismContext.definitionFactory().createReferenceDefinition(name, type);
        } else {
            throw new UnsupportedOperationException("Unsupported value type " + rValType);
        }
        def.setMinOccurs(0);
        def.setMaxOccurs(-1);
        def.setRuntimeSchema(true);
        def.setDynamic(true);
        return def;
    }

    private <T extends ObjectType> void validateObjectType(PrismObject<T> prismObject, Class<T> type)
            throws SchemaException {
        if (prismObject == null || !type.isAssignableFrom(prismObject.getCompileTimeClass())) {
            throw new SchemaException("Expected to find '" + type.getSimpleName() + "' but found '"
                    + prismObject.getCompileTimeClass().getSimpleName() + "' (" + prismObject.toDebugName()
                    + "). Bad OID in a reference?");
        }
        if (InternalsConfig.consistencyChecks) {
            prismObject.checkConsistence();
        }
        if (InternalsConfig.readEncryptionChecks) {
            CryptoUtil.checkEncrypted(prismObject);
        }
    }

    public <T extends ObjectType> String getVersionAttempt(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException {
        LOGGER_PERFORMANCE.debug("> get version {}, oid={}", type.getSimpleName(), oid);

        String version = null;
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();
            Query query = session.getNamedQuery("getVersion");
            query.setParameter("oid", oid);

            Number versionLong = (Number) query.uniqueResult();
            if (versionLong == null) {
                throw new ObjectNotFoundException("Object '" + type.getSimpleName()
                        + "' with oid '" + oid + "' was not found.");
            }
            version = versionLong.toString();
            session.getTransaction().commit();
        } catch (RuntimeException ex) {
            baseHelper.handleGeneralRuntimeException(ex, session, result);
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
        }

        return version;
    }

    public <T extends ObjectType> void searchObjectsIterativeAttempt(Class<T> type, ObjectQuery query, ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result, Set<String> retrievedOids) {
        Set<String> newlyRetrievedOids = new HashSet<>();
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();
            RQuery rQuery;
            QueryEngine2 engine = new QueryEngine2(getConfiguration(), extItemDictionary, prismContext, relationRegistry);
            rQuery = engine.interpret(query, type, options, false, session);

            try (ScrollableResults results = rQuery.scroll(ScrollMode.FORWARD_ONLY)) {
                Iterator<GetObjectResult> iterator = new ScrollableResultsIterator<>(results);
                while (iterator.hasNext()) {
                    GetObjectResult object = iterator.next();

                    if (retrievedOids.contains(object.getOid())) {
                        continue;
                    }

                    // TODO treat exceptions encountered within the next call
                    PrismObject<T> prismObject = updateLoadedObject(object, type, null, options, null, session, result);

                    /*
                     *  We DO NOT store OIDs directly into retrievedOids, because this would mean that any duplicated results
                     *  would get eliminated from processing. While this is basically OK, it would break existing behavior,
                     *  and would lead to inconsistencies between e.g. "estimated total" vs "progress" in iterative tasks.
                     *  Such inconsistencies could happen also in the current approach with retrievedOids/newlyRetrievedOids,
                     *  but are much less likely.
                     *  TODO reconsider this in the future - i.e. if it would not be beneficial to skip duplicate processing of objects
                     *  TODO what about memory requirements of this data structure (consider e.g. millions of objects)
                     */
                    newlyRetrievedOids.add(object.getOid());

                    if (!handler.handle(prismObject, result)) {
                        break;
                    }
                }
            }

            session.getTransaction().commit();
        } catch (SchemaException | QueryException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
            retrievedOids.addAll(newlyRetrievedOids);
        }
    }

    public <T extends ObjectType> void searchObjectsIterativeByPaging(Class<T> type, ObjectQuery query,
            ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws SchemaException {

        try {
            ObjectQuery pagedQuery = query != null ? query.clone() : prismContext.queryFactory().createQuery();

            int offset;
            int remaining;
            final int batchSize = getConfiguration().getIterativeSearchByPagingBatchSize();

            ObjectPaging paging = pagedQuery.getPaging();

            if (paging == null) {
                paging = prismContext.queryFactory().createPaging(0, 0);        // counts will be filled-in later
                pagedQuery.setPaging(paging);
                offset = 0;
                remaining = repositoryService.countObjects(type, query, options, result);
            } else {
                offset = paging.getOffset() != null ? paging.getOffset() : 0;
                remaining = paging.getMaxSize() != null ? paging.getMaxSize() : repositoryService.countObjects(type, query, options, result) - offset;
            }

            main:
            while (remaining > 0) {
                paging.setOffset(offset);
                paging.setMaxSize(remaining < batchSize ? remaining : batchSize);

                List<PrismObject<T>> objects = repositoryService.searchObjects(type, pagedQuery, options, result);

                for (PrismObject<T> object : objects) {
                    if (!handler.handle(object, result)) {
                        break main;
                    }
                }

                if (objects.size() == 0) {
                    break;                      // should not occur, but let's check for this to avoid endless loops
                }
                offset += objects.size();
                remaining -= objects.size();
            }
        } finally {
            if (result.isUnknown()) {
                result.computeStatus();
            }
            result.setSummarizeSuccesses(true);
            result.summarize();
        }
    }

    /**
     * Strictly-sequential version of paged search.
     * <p>
     * Assumptions:
     * - During processing of returned object(s), any objects can be added, deleted or modified.
     * <p>
     * Guarantees:
     * - We return each object that existed in the moment of search start:
     * - exactly once if it was not deleted in the meanwhile,
     * - at most once otherwise.
     * - However, we may or may not return any objects that were added during the processing.
     * <p>
     * Constraints:
     * - There can be no ordering prescribed. We use our own ordering.
     * - We also disallow any explicit paging - except for maxSize setting.
     * <p>
     * Implementation is very simple - we fetch objects ordered by OID, and remember last OID fetched.
     * Obviously no object will be present in output more than once.
     * Objects that are not deleted will be there exactly once, provided their oid is not changed.
     */
    public <T extends ObjectType> void searchObjectsIterativeByPagingStrictlySequential(
            Class<T> type, ObjectQuery query, ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws SchemaException {

        try {
            if (!SqlRepositoryServiceImpl.isCustomPagingOkWithPagedSeqIteration(query)) {
                throw new IllegalArgumentException("Externally specified paging is not supported on strictly sequential "
                        + "iterative search. Query = " + query);
            }
            Integer maxSize;
            ObjectQuery pagedQuery;
            if (query != null) {
                maxSize = query.getPaging() != null ? query.getPaging().getMaxSize() : null;
                pagedQuery = query.clone();
            } else {
                maxSize = null;
                pagedQuery = prismContext.queryFactory().createQuery();
            }

            String lastOid = null;
            final int batchSize = getConfiguration().getIterativeSearchByPagingBatchSize();

            ObjectPaging paging = prismContext.queryFactory().createPaging();
            pagedQuery.setPaging(paging);
            main:
            for (; ; ) {
                paging.setCookie(lastOid != null ? lastOid : NULL_OID_MARKER);
                paging.setMaxSize(Math.min(batchSize, defaultIfNull(maxSize, Integer.MAX_VALUE)));

                List<PrismObject<T>> objects = repositoryService.searchObjects(type, pagedQuery, options, result);

                for (PrismObject<T> object : objects) {
                    lastOid = object.getOid();
                    if (!handler.handle(object, result)) {
                        break main;
                    }
                }
                if (objects.size() == 0 || objects.size() < paging.getMaxSize()) {
                    break;
                }
                if (maxSize != null) {
                    maxSize -= objects.size();
                    if (maxSize <= 0) {
                        break;
                    }
                }
            }
        } finally {
            if (result.isUnknown()) {
                result.computeStatus();
            }
        }
    }

    public <T extends ObjectType> void searchObjectsIterativeByFetchAll(Class<T> type, ObjectQuery query,
            ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws SchemaException {
        try {
            SearchResultList<PrismObject<T>> objects = repositoryService.searchObjects(type, query, options, result);
            for (PrismObject<T> object : objects) {
                if (!handler.handle(object, result)) {
                    break;
                }
            }
        } finally {
            if (result.isUnknown()) {
                result.computeStatus();
            }
            result.setSummarizeSuccesses(true);
            result.summarize();
        }
    }

    public boolean isAnySubordinateAttempt(String upperOrgOid, Collection<String> lowerObjectOids) {
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();

            Query query;
            if (lowerObjectOids.size() == 1) {
                query = session.getNamedQuery("isAnySubordinateAttempt.oneLowerOid");
                query.setParameter("dOid", lowerObjectOids.iterator().next());
            } else {
                query = session.getNamedQuery("isAnySubordinateAttempt.moreLowerOids");
                query.setParameterList("dOids", lowerObjectOids);
            }
            query.setParameter("aOid", upperOrgOid);

            Number number = (Number) query.uniqueResult();
            session.getTransaction().commit();

            return number != null && number.longValue() != 0L;
        } catch (RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, null);
        } finally {
            baseHelper.cleanupSessionAndResult(session, null);
        }

        throw new SystemException("isAnySubordinateAttempt failed somehow, this really should not happen.");
    }

    public RepositoryQueryDiagResponse executeQueryDiagnosticsRequest(RepositoryQueryDiagRequest request, OperationResult result) {
        LOGGER_PERFORMANCE.debug("> execute query diagnostics {}", request);

        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();       // beware, not all databases support read-only transactions!

            final String implementationLevelQuery;
            final Map<String, RepositoryQueryDiagResponse.ParameterValue> implementationLevelQueryParameters;
            final org.hibernate.Query query;
            final boolean isMidpointQuery = request.getImplementationLevelQuery() == null;
            if (isMidpointQuery) {
                QueryEngine2 engine = new QueryEngine2(getConfiguration(), extItemDictionary, prismContext, relationRegistry);
                RQueryImpl rQuery = (RQueryImpl) engine.interpret(request.getQuery(), request.getType(), request.getOptions(), false, session);
                query = rQuery.getQuery();
                implementationLevelQuery = query.getQueryString();
                implementationLevelQueryParameters = new HashMap<>();
                for (Map.Entry<String, QueryParameterValue> entry : rQuery.getQuerySource().getParameters().entrySet()) {
                    implementationLevelQueryParameters.put(entry.getKey(),
                            new RepositoryQueryDiagResponse.ParameterValue(entry.getValue().getValue(), entry.getValue().toString()));
                }
            } else {
                implementationLevelQuery = (String) request.getImplementationLevelQuery();
                implementationLevelQueryParameters = new HashMap<>();
                query = session.createQuery(implementationLevelQuery);
            }

            List<?> objects = request.isTranslateOnly() ? null : query.list();
            if (isMidpointQuery && objects != null) {
                // raw GetObjectResult instances are useless outside repo-sql-impl module, so we'll convert them to objects
                @SuppressWarnings("unchecked")
                List<GetObjectResult> listOfGetObjectResults = (List<GetObjectResult>) objects;
                objects = queryResultToPrismObjects(listOfGetObjectResults, request.getType(), null, session, result);
            }

            RepositoryQueryDiagResponse response = new RepositoryQueryDiagResponse(objects, implementationLevelQuery, implementationLevelQueryParameters);
            session.getTransaction().rollback();
            return response;
        } catch (SchemaException | QueryException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
            throw new IllegalStateException("shouldn't get here");
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
        }
    }

    void attachDiagDataIfRequested(PrismValue value, byte[] fullObject, Collection<SelectorOptions<GetOperationOptions>> options) {
        if (GetOperationOptions.isAttachDiagData(SelectorOptions.findRootOptions(options))) {
            value.setUserData(RepositoryService.KEY_DIAG_DATA, new RepositoryObjectDiagnosticData(getLength(fullObject)));
        }
    }

    private void attachDiagDataIfRequested(Item<?, ?> item, byte[] fullObject, Collection<SelectorOptions<GetOperationOptions>> options) {
        if (GetOperationOptions.isAttachDiagData(SelectorOptions.findRootOptions(options))) {
            item.setUserData(RepositoryService.KEY_DIAG_DATA, new RepositoryObjectDiagnosticData(getLength(fullObject)));
        }
    }
}
