/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryObjectDiagnosticData;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.ObjectPagingAfterOid;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.any.RValueType;
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
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.NativeQuery;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;

import static org.apache.commons.lang3.ArrayUtils.getLength;

/**
 * @author lazyman, mederly
 */
@Component
public class ObjectRetriever {

	public static final String CLASS_DOT = ObjectRetriever.class.getName() + ".";
	public static final String OPERATION_GET_OBJECT_INTERNAL = CLASS_DOT + "getObjectInternal";

    private static final Trace LOGGER = TraceManager.getTrace(ObjectRetriever.class);
    private static final Trace LOGGER_PERFORMANCE = TraceManager.getTrace(SqlRepositoryServiceImpl.PERFORMANCE_LOG_NAME);

    @Autowired private LookupTableHelper lookupTableHelper;
	@Autowired private CertificationCaseHelper caseHelper;
	@Autowired private BaseHelper baseHelper;
	@Autowired private NameResolutionHelper nameResolutionHelper;
	@Autowired private PrismContext prismContext;
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
				q.setString(0, oid);
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
			query.setString("oid", oid);
			query.setResultTransformer(GetObjectResult.RESULT_STYLE.getResultTransformer());
			query.setLockOptions(lockOptions);

			fullObject = (GetObjectResult) query.uniqueResult();
		} else {
			// we're doing update after this get, therefore we load full object right now
			// (it would be loaded during merge anyway)
			// this just loads object to hibernate session, probably will be removed later. Merge after this get
			// will be faster. Read and use object only from fullObject column.
			// todo remove this later [lazyman]
			Criteria criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
			criteria.add(Restrictions.eq("oid", oid));

			criteria.setLockMode(lockOptions.getLockMode());
			RObject obj = (RObject) criteria.uniqueResult();

			if (obj != null) {
				fullObject = new GetObjectResult(obj.getOid(), obj.getFullObject(), obj.getStringsCount(), obj.getLongsCount(),
						obj.getDatesCount(), obj.getReferencesCount(), obj.getPolysCount(), obj.getBooleansCount());
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

		//			subResult.computeStatusIfUnknown();
		//			if (subResult.isWarning() || subResult.isError() || subResult.isInProgress()) {
		//				prismObject.asObjectable().setFetchResult(subResult.createOperationResultType());
		//			}

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
            query.setString("oid", shadowOid);
            query.setResultTransformer(GetObjectResult.RESULT_STYLE.getResultTransformer());

			@SuppressWarnings({"unchecked", "raw"})
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

    public PrismObject<UserType> listAccountShadowOwnerAttempt(String accountOid, OperationResult result)
            throws ObjectNotFoundException {
        LOGGER_PERFORMANCE.debug("> list account shadow owner oid={}", accountOid);
        PrismObject<UserType> userType = null;
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();
            Query query = session.getNamedQuery("listAccountShadowOwner.getUser");
            query.setString("oid", accountOid);
            query.setResultTransformer(GetObjectResult.RESULT_STYLE.getResultTransformer());

			@SuppressWarnings({"unchecked", "raw"})
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
            if (query == null || query.getFilter() == null) {
            	if (GetOperationOptions.isDistinct(SelectorOptions.findRootOptions(options))) {
            		throw new UnsupportedOperationException("Distinct option is not supported here");	// TODO
				}
                // this is 5x faster than count with 3 inner joins, it can probably improved also for queries which
                // filters uses only properties from concrete entities like RUser, RRole by improving interpreter [lazyman]
                NativeQuery sqlQuery = session.createNativeQuery("SELECT COUNT(*) FROM " + RUtil.getTableName(hqlType, session));
                longCount = (Number) sqlQuery.uniqueResult();
            } else {
                RQuery rQuery;
				QueryEngine2 engine = new QueryEngine2(getConfiguration(), prismContext);
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

    public <C extends Containerable> int countContainersAttempt(Class<C> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) {
		boolean cases = AccessCertificationCaseType.class.equals(type);
		boolean workItems = AccessCertificationWorkItemType.class.equals(type);
		if (!cases && !workItems) {
			throw new UnsupportedOperationException("Only AccessCertificationCaseType or AccessCertificationWorkItemType is supported here now.");
		}

		LOGGER_PERFORMANCE.debug("> count containers {}", type.getSimpleName());
		Session session = null;
		try {
			session = baseHelper.beginReadOnlyTransaction();

			QueryEngine2 engine = new QueryEngine2(getConfiguration(), prismContext);
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

			QueryEngine2 engine = new QueryEngine2(getConfiguration(), prismContext);
			rQuery = engine.interpret(query, type, options, false, session);

			@SuppressWarnings({"unchecked", "raw"})
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
        if (!cases && !workItems) {
            throw new UnsupportedOperationException("Only AccessCertificationCaseType or AccessCertificationWorkItemType is supported here now.");
        }

        LOGGER_PERFORMANCE.debug("> search containers {}", type.getSimpleName());
        List<C> list = new ArrayList<>();
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();

            QueryEngine2 engine = new QueryEngine2(getConfiguration(), prismContext);
            RQuery rQuery = engine.interpret(query, type, options, false, session);

            if (cases) {
				@SuppressWarnings({"unchecked", "raw"})
				List<GetContainerableResult> items = rQuery.list();
				LOGGER.trace("Found {} items (cases), translating to JAXB.", items.size());
				Map<String,PrismObject<AccessCertificationCampaignType>> campaignsCache = new HashMap<>();
				for (GetContainerableResult item : items) {
					@SuppressWarnings({ "raw", "unchecked" })
					C value = (C) caseHelper.updateLoadedCertificationCase(item, campaignsCache, options, session, result);
					list.add(value);
				}
			} else {
            	assert workItems;
            	@SuppressWarnings({"unchecked", "raw"})
				List<GetCertificationWorkItemResult> items = rQuery.list();
				LOGGER.trace("Found {} work items, translating to JAXB.", items.size());
				Map<String,PrismContainerValue<AccessCertificationCaseType>> casesCache = new HashMap<>();
				Map<String,PrismObject<AccessCertificationCampaignType>> campaignsCache = new HashMap<>();
				for (GetCertificationWorkItemResult item : items) {
					//LOGGER.trace("- {}", item);
					@SuppressWarnings({ "raw", "unchecked" })
					C value = (C) caseHelper.updateLoadedCertificationWorkItem(item, casesCache, campaignsCache, options, engine, session, result);
					list.add(value);
				}
			}

			nameResolutionHelper.resolveNamesIfRequested(session, PrismContainerValue.asPrismContainerValues(list), options);

			session.getTransaction().commit();
        } catch (QueryException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
        }

        list.forEach(c -> ObjectTypeUtil.normalizeAllRelations(c.asPrismContainerValue()));
        return new SearchResultList<>(list);
    }

    /**
     * This method provides object parsing from String and validation.
     */
    private <T extends ObjectType> PrismObject<T> updateLoadedObject(GetObjectResult result, Class<T> type,
    		String oid, Collection<SelectorOptions<GetOperationOptions>> options,
			Holder<PrismObject<T>> partialValueHolder,
			Session session, OperationResult operationResult) throws SchemaException {

		byte[] fullObject = result.getFullObject();
		String xml = RUtil.getXmlFromByteArray(fullObject, getConfiguration().isUseZip());
        PrismObject<T> prismObject;
        try {
            // "Postel mode": be tolerant what you read. We need this to tolerate (custom) schema changes
			ParsingContext parsingContext = ParsingContext.forMode(XNodeProcessorEvaluationMode.COMPAT);
            prismObject = prismContext.parserFor(xml).context(parsingContext).parse();
			if (parsingContext.hasWarnings()) {
				LOGGER.warn("Object {} parsed with {} warnings", ObjectTypeUtil.toShortString(prismObject), parsingContext.getWarnings().size());
				// TODO enable if needed
//				for (String warning : parsingContext.getWarnings()) {
//					operationResult.createSubresult("parseObject").recordWarning(warning);
//				}
			}
        } catch (SchemaException | RuntimeException | Error e) {
        	// This is a serious thing. We have corrupted XML in the repo. This may happen even
        	// during system init. We want really loud and detailed error here.
            LOGGER.error("Couldn't parse object {} {}: {}: {}\n{}",
            		type.getSimpleName(), oid, e.getClass().getName(), e.getMessage(), xml, e);
            throw e;
        }
        attachDiagDataIfRequested(prismObject, fullObject, options);
        if (FocusType.class.isAssignableFrom(prismObject.getCompileTimeClass())) {
            if (SelectorOptions.hasToLoadPath(FocusType.F_JPEG_PHOTO, options)) {
                //todo improve, use user.hasPhoto flag and take options into account [lazyman]
                //this is called only when options contains INCLUDE user/jpegPhoto
                Query query = session.getNamedQuery("get.focusPhoto");
                query.setString("oid", prismObject.getOid());
                byte[] photo = (byte[]) query.uniqueResult();
                if (photo != null) {
                    PrismProperty property = prismObject.findOrCreateProperty(FocusType.F_JPEG_PHOTO);
                    property.setRealValue(photo);
                }
            }
        } else if (ShadowType.class.equals(prismObject.getCompileTimeClass())) {
            //we store it because provisioning now sends it to repo, but it should be transient
            prismObject.removeContainer(ShadowType.F_ASSOCIATION);

            LOGGER.debug("Loading definitions for shadow attributes.");

            Short[] counts = result.getCountProjection();
            Class[] classes = GetObjectResult.EXT_COUNT_CLASSES;

            for (int i = 0; i < classes.length; i++) {
                if (counts[i] == null || counts[i] == 0) {
                    continue;
                }

                applyShadowAttributeDefinitions(classes[i], prismObject, session);
            }
            LOGGER.debug("Definitions for attributes loaded. Counts: {}", Arrays.toString(counts));
        } else if (LookupTableType.class.equals(prismObject.getCompileTimeClass())) {
            lookupTableHelper.updateLoadedLookupTable(prismObject, options, session);
        } else if (AccessCertificationCampaignType.class.equals(prismObject.getCompileTimeClass())) {
            caseHelper.updateLoadedCampaign(prismObject, options, session);
        }

        if (partialValueHolder != null) {
        	partialValueHolder.setValue(prismObject);
		}
        nameResolutionHelper.resolveNamesIfRequested(session, prismObject.getValue(), options);
        validateObjectType(prismObject, type);

        ObjectTypeUtil.normalizeAllRelations(prismObject);
		return prismObject;
    }


    private void applyShadowAttributeDefinitions(Class<? extends RAnyValue> anyValueType,
                                                 PrismObject object, Session session) throws SchemaException {

        PrismContainer attributes = object.findContainer(ShadowType.F_ATTRIBUTES);

        Query query = session.getNamedQuery("getDefinition." + anyValueType.getSimpleName());
        query.setParameter("oid", object.getOid());
        query.setParameter("ownerType", RObjectExtensionType.ATTRIBUTES);

		@SuppressWarnings({"unchecked", "raw"})
		List<Object[]> values = query.list();
        if (values == null || values.isEmpty()) {
            return;
        }

        for (Object[] value : values) {
            QName name = RUtil.stringToQName((String) value[0]);
            QName type = RUtil.stringToQName((String) value[1]);
            Item item = attributes.findItem(name);

            if (item == null) {
            	// Just skip. Cannot throw exceptions here. Otherwise we
            	// could break raw reading.
            	continue;
            }

            // A switch statement used to be here
            // but that caused strange trouble with OpenJDK. This if-then-else works.
            if (item.getDefinition() == null) {
                RValueType rValType = (RValueType) value[2];
                if (rValType == RValueType.PROPERTY) {
                    PrismPropertyDefinitionImpl<Object> def = new PrismPropertyDefinitionImpl<>(name, type, object.getPrismContext());
                    def.setMinOccurs(0);
                    def.setMaxOccurs(-1);
                    item.applyDefinition(def, true);
                } else if (rValType == RValueType.REFERENCE) {
                    PrismReferenceDefinitionImpl def = new PrismReferenceDefinitionImpl(name, type, object.getPrismContext());
	                def.setMinOccurs(0);
	                def.setMaxOccurs(-1);
                    item.applyDefinition(def, true);
                } else {
                    throw new UnsupportedOperationException("Unsupported value type " + rValType);
                }
            }
        }
    }

    public <T extends ShadowType> List<PrismObject<T>> listResourceObjectShadowsAttempt(
            String resourceOid, Class<T> resourceObjectShadowType, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        LOGGER_PERFORMANCE.debug("> list resource object shadows {}, for resource oid={}",
                resourceObjectShadowType.getSimpleName(), resourceOid);
        List<PrismObject<T>> list = new ArrayList<>();
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();
            Query query = session.getNamedQuery("listResourceObjectShadows");
            query.setString("oid", resourceOid);
            query.setResultTransformer(GetObjectResult.RESULT_STYLE.getResultTransformer());

			@SuppressWarnings({"unchecked", "raw"})
			List<GetObjectResult> shadows = query.list();
            LOGGER.debug("Query returned {} shadows, transforming to JAXB types.", shadows != null ? shadows.size() : 0);

            if (shadows != null) {
                for (GetObjectResult shadow : shadows) {
                    PrismObject<T> prismObject = updateLoadedObject(shadow, resourceObjectShadowType, null, null, null, session, result);
                    list.add(prismObject);
                }
            }
            session.getTransaction().commit();
        } catch (SchemaException | RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, result);
        } finally {
            baseHelper.cleanupSessionAndResult(session, result);
        }

        return list;
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
            throws ObjectNotFoundException, SchemaException {
        LOGGER_PERFORMANCE.debug("> get version {}, oid={}", type.getSimpleName(), oid);

        String version = null;
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();
            Query query = session.getNamedQuery("getVersion");
            query.setString("oid", oid);

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
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result, Set<String> retrievedOids)
			throws SchemaException {
		Set<String> newlyRetrievedOids = new HashSet<>();
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();
            RQuery rQuery;
			QueryEngine2 engine = new QueryEngine2(getConfiguration(), prismContext);
			rQuery = engine.interpret(query, type, options, false, session);

            ScrollableResults results = rQuery.scroll(ScrollMode.FORWARD_ONLY);
            try {
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
            } finally {
                if (results != null) {
                    results.close();
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
                                                                      ResultHandler<T> handler,
                                                                      Collection<SelectorOptions<GetOperationOptions>> options,
                                                                      OperationResult result)
            throws SchemaException {

        try {
            ObjectQuery pagedQuery = query != null ? query.clone() : new ObjectQuery();

            int offset;
            int remaining;
            final int batchSize = getConfiguration().getIterativeSearchByPagingBatchSize();

            ObjectPaging paging = pagedQuery.getPaging();

            if (paging == null) {
                paging = ObjectPaging.createPaging(0, 0);        // counts will be filled-in later
                pagedQuery.setPaging(paging);
                offset = 0;
                remaining = repositoryService.countObjects(type, query, options, result);
            } else {
                offset = paging.getOffset() != null ? paging.getOffset() : 0;
                remaining = paging.getMaxSize() != null ? paging.getMaxSize() : repositoryService.countObjects(type, query, options, result) - offset;
            }

main:       while (remaining > 0) {
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
     *
     * Assumptions:
     *  - During processing of returned object(s), any objects can be added, deleted or modified.
     *
     * Guarantees:
     *  - We return each object that existed in the moment of search start:
     *     - exactly once if it was not deleted in the meanwhile,
     *     - at most once otherwise.
     *  - However, we may or may not return any objects that were added during the processing.
     *
     * Constraints:
     *  - There can be no ordering prescribed. We use our own ordering.
     *  - Moreover, for simplicity we disallow any explicit paging.
     *
     *  Implementation is very simple - we fetch objects ordered by OID, and remember last OID fetched.
     *  Obviously no object will be present in output more than once.
     *  Objects that are not deleted will be there exactly once, provided their oid is not changed.
     */
    public <T extends ObjectType> void searchObjectsIterativeByPagingStrictlySequential(
            Class<T> type, ObjectQuery query, ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws SchemaException {

        try {
            ObjectQuery pagedQuery = query != null ? query.clone() : new ObjectQuery();

            String lastOid = "";
            final int batchSize = getConfiguration().getIterativeSearchByPagingBatchSize();

            if (pagedQuery.getPaging() != null) {
                throw new IllegalArgumentException("Externally specified paging is not supported on strictly sequential iterative search.");
            }

            ObjectPagingAfterOid paging = new ObjectPagingAfterOid();
            pagedQuery.setPaging(paging);
main:       for (;;) {
                paging.setOidGreaterThan(lastOid);
                paging.setMaxSize(batchSize);

                List<PrismObject<T>> objects = repositoryService.searchObjects(type, pagedQuery, options, result);

                for (PrismObject<T> object : objects) {
                    lastOid = object.getOid();
                    if (!handler.handle(object, result)) {
                        break main;
                    }
                }

                if (objects.size() == 0) {
                    break;
                }
            }
        } finally {
            if (result.isUnknown()) {
                result.computeStatus();
            }
        }
    }

    public boolean isAnySubordinateAttempt(String upperOrgOid, Collection<String> lowerObjectOids) {
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();

            Query query;
            if (lowerObjectOids.size() == 1) {
                query = session.getNamedQuery("isAnySubordinateAttempt.oneLowerOid");
                query.setString("dOid", lowerObjectOids.iterator().next());
            } else {
                query = session.getNamedQuery("isAnySubordinateAttempt.moreLowerOids");
                query.setParameterList("dOids", lowerObjectOids);
            }
            query.setString("aOid", upperOrgOid);

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
			final Query query;
			final boolean isMidpointQuery = request.getImplementationLevelQuery() == null;
			if (isMidpointQuery) {
				QueryEngine2 engine = new QueryEngine2(getConfiguration(), prismContext);
				RQueryImpl rQuery = (RQueryImpl) engine.interpret(request.getQuery(), request.getType(), null, false, session);
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

	private void attachDiagDataIfRequested(Item item, byte[] fullObject, Collection<SelectorOptions<GetOperationOptions>> options) {
		if (GetOperationOptions.isAttachDiagData(SelectorOptions.findRootOptions(options))) {
			item.setUserData(RepositoryService.KEY_DIAG_DATA, new RepositoryObjectDiagnosticData(getLength(fullObject)));
		}
	}
}
