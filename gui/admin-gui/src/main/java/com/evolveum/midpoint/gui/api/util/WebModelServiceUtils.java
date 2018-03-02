/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.gui.api.util;

import java.util.*;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.RelationalValueSearchQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.SecurityUtils;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;

/**
 * Utility class that contains methods that interact with ModelService and other
 * midPoint components.
 *
 * @author lazyman
 */
public class WebModelServiceUtils {

    private static final Trace LOGGER = TraceManager.getTrace(WebModelServiceUtils.class);

    private static final String DOT_CLASS = WebModelServiceUtils.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_SAVE_OBJECT = DOT_CLASS + "saveObject";
    private static final String OPERATION_LOAD_OBJECT_REFS = DOT_CLASS + "loadObjectReferences";
    private static final String OPERATION_COUNT_OBJECT = DOT_CLASS + "countObjects";
	private static final String OPERATION_ASSUME_POWER_OF_ATTORNEY = DOT_CLASS + "assumePowerOfAttorney";
	private static final String OPERATION_DROP_POWER_OF_ATTORNEY = DOT_CLASS + "dropPowerOfAttorney";

	public static String resolveReferenceName(ObjectReferenceType ref, PageBase page) {
		Task task = page.createSimpleTask(WebModelServiceUtils.class.getName() + ".resolveReferenceName");
		return resolveReferenceName(ref, page, task, task.getResult());
	}

	public static String resolveReferenceName(ObjectReferenceType ref, PageBase page, Task task, OperationResult result) {
		if (ref == null) {
			return null;
		}
		if (ref.getTargetName() != null) {
			return ref.getTargetName().getOrig();
		}
        PrismObject<ObjectType> object = resolveReferenceNoFetch(ref, page, task, result);
        if (object == null) {
            return ref.getOid();
        } else {
			ref.asReferenceValue().setObject(object);
            return WebComponentUtil.getName(object);
        }
    }

    public static <T extends ObjectType> PrismObject<T> resolveReferenceNoFetch(ObjectReferenceType reference, PageBase page, Task task, OperationResult result) {
        if (reference == null) {
            return null;
        }
        if (reference.asReferenceValue().getObject() != null) {
            return reference.asReferenceValue().getObject();
        }
        PrismContext prismContext = page.getPrismContext();
        if (reference.getType() == null) {
            LOGGER.error("No type in {}", reference);
            return null;
        }
        PrismObjectDefinition<T> definition = prismContext.getSchemaRegistry().findObjectDefinitionByType(reference.getType());
        if (definition == null) {
            LOGGER.error("No definition for {} was found", reference.getType());
            return null;
        }
        if (reference.getOid() == null) {
        	if (reference.getResolutionTime() == EvaluationTimeType.RUN) {
        		// Runtime reference resolution. Ignore it for now. Later we maybe would want to resolve it here.
        		// But it may resolve to several objects ....
        		return null;
        	} else {
        		LOGGER.error("Null OID in reference {}", reference);
        		// Throw an exception instead? Maybe not. We want GUI to be robust.
        		return null;
        	}
        }
        return loadObject(definition.getCompileTimeClass(), reference.getOid(), createNoFetchCollection(), page, task, result);
    }

    public static <O extends ObjectType> List<ObjectReferenceType> createObjectReferenceList(Class<O> type, PageBase page, Map<String, String> referenceMap){
		referenceMap.clear();

        OperationResult result = new OperationResult(OPERATION_LOAD_OBJECT_REFS);
//        Task task = page.createSimpleTask(OPERATION_LOAD_PASSWORD_POLICIES);

        try{
            List<PrismObject<O>> objects = searchObjects(type, null, result, page);
        	result.recomputeStatus();
        	if(objects != null){
        		List<ObjectReferenceType> references = new ArrayList<>();

                for(PrismObject<O> object: objects){
                	referenceMap.put(object.getOid(), WebComponentUtil.getName(object));
                    references.add(ObjectTypeUtil.createObjectRef(object));

                }
                return references;
            }
        } catch (Exception e){
            result.recordFatalError("Couldn't load password policies.", e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load password policies", e);
        }

        // TODO - show error somehow
        // if(!result.isSuccess()){
        //    getPageBase().showResult(result);
        // }

        return null;

    }

    public static String runTask(TaskType taskToRun, Task operationalTask, OperationResult parentResult, PageBase pageBase){
    	try {
			ObjectDelta<TaskType> delta = ObjectDelta.createAddDelta(taskToRun.asPrismObject());
			pageBase.getPrismContext().adopt(delta);
			pageBase.getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), null,
					operationalTask, parentResult);
			parentResult.recordInProgress();
			parentResult.setBackgroundTaskOid(delta.getOid());
			pageBase.showResult(parentResult);
	    	return delta.getOid();
		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
				| ExpressionEvaluationException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {
			// TODO Auto-generated catch block
//			error(pageBase.getString("pageUsers.message.nothingSelected") + e.getMessage());
			parentResult.recordFatalError("Couldn't run task " + e.getMessage(), e);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't run task " + e.getMessage(), e);
			return null;
		}

    }

    public static void runTask(Collection<TaskType> tasksToRun, Task operationalTask, OperationResult parentResult, PageBase pageBase){
//    	try {

    		for (TaskType taskToRun : tasksToRun){
    			runTask(tasksToRun, operationalTask, parentResult, pageBase);
    		}

//    		}
//			ObjectDelta<TaskType> delta = ObjectDelta.createAddDelta(taskToRun.asPrismObject());
//			pageBase.getPrismContext().adopt(delta);
//			pageBase.getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), null,
//					operationalTask, parentResult);
//			parentResult.recordInProgress();
//			parentResult.setBackgroundTaskOid(delta.getOid());
//			pageBase.showResult(parentResult);
//	    	return delta.getOid();
//		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
//				| ExpressionEvaluationException | CommunicationException | ConfigurationException
//				| PolicyViolationException | SecurityViolationException e) {
//			// TODO Auto-generated catch block
////			error(pageBase.getString("pageUsers.message.nothingSelected") + e.getMessage());
//			parentResult.recordFatalError("Couldn't run task " + e.getMessage(), e);
//			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't run task " + e.getMessage(), e);
//			return null;
//		}

    }

	@Nullable
    public static <T extends ObjectType> PrismObject<T> loadObject(ObjectReferenceType objectReference,
			PageBase page, Task task, OperationResult result) {
		Class<T> type = page.getPrismContext().getSchemaRegistry().determineClassForType(objectReference.getType());
        return loadObject(type, objectReference.getOid(), null, page, task, result);
    }

	@Nullable
    public static <T extends ObjectType> PrismObject<T> loadObject(Class<T> type, String oid,
			PageBase page, Task task, OperationResult result) {
        return loadObject(type, oid, null, page, task, result);
    }

	@Nullable
	public static <T extends ObjectType> PrismObject<T> loadObject(Class<T> type, String oid,
			Collection<SelectorOptions<GetOperationOptions>> options,
			PageBase page, Task task, OperationResult result) {
    	return loadObject(type, oid, options, true, page, task, result);
	}
	
	@Nullable
	public static <T extends ObjectType> PrismObject<T> loadObject(Class<T> type, String oid,
			Collection<SelectorOptions<GetOperationOptions>> options, boolean allowNotFound,
			PageBase page, Task task, OperationResult result) {
        LOGGER.debug("Loading {} with oid {}, options {}", type.getSimpleName(), oid, options);

        OperationResult subResult;
        if (result != null) {
            subResult = result.createMinorSubresult(OPERATION_LOAD_OBJECT);
        } else {
            subResult = new OperationResult(OPERATION_LOAD_OBJECT);
        }
        PrismObject<T> object = null;
        try {
        	if (options == null) {
        		options = SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
        	} else {
        		GetOperationOptions getOpts = SelectorOptions.findRootOptions(options);
        		if (getOpts == null) {
        			options.add(new SelectorOptions<>(GetOperationOptions.createResolveNames()));
        		} else {
        			getOpts.setResolveNames(Boolean.TRUE);
        		}
        	}
            object = page.getModelService().getObject(type, oid, options, task, subResult);
        } catch (AuthorizationException e) {
        	// Not authorized to access the object. This is probably caused by a reference that
        	// point to an object that the current user cannot read. This is no big deal.
        	// Just do not display that object.
        	subResult.recordHandledError(e);
        	LOGGER.debug("User {} is not authorized to read {} {}",
                    task.getOwner() != null ? task.getOwner().getName() : null, type.getSimpleName(), oid);
        	return null;
        } catch (ObjectNotFoundException e) {
        	if (allowNotFound) {
				// Object does not exist. It was deleted in the meanwhile, or not created yet. This could happen quite often.
				subResult.recordHandledError(e);
				LOGGER.debug("{} {} does not exist", type.getSimpleName(), oid, e);
				return null;
			} else {
				subResult.recordFatalError("WebModelUtils.couldntLoadObject", e);
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", e);
			}
        } catch (Exception ex) {
            subResult.recordFatalError("WebModelUtils.couldntLoadObject", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
        } finally {
            subResult.computeStatus();
        }
		// TODO reconsider this part: until recently, the condition was always 'false'
        if (WebComponentUtil.showResultInPage(subResult)) {
            page.showResult(subResult);
        }

        LOGGER.debug("Loaded {} with result {}", object, subResult);

        return object;
    }
	
	//TODO consider using modelServiceLocator instead of PageBase in other methods.. Do we even need it? What about showResult? Should it be 
	// here or directly in the page? Consider usability and readabiltiy
	@Nullable
    public static <T extends ObjectType> PrismObject<T> loadObject(ObjectReferenceType objectReference,
    		ModelServiceLocator page, Task task, OperationResult result) {
		Class<T> type = page.getPrismContext().getSchemaRegistry().determineClassForType(objectReference.getType());
		String oid = objectReference.getOid();
		Collection<SelectorOptions<GetOperationOptions>> options = null;
		LOGGER.debug("Loading {} with oid {}, options {}", type.getSimpleName(), oid, options);

        OperationResult subResult;
        if (result != null) {
            subResult = result.createMinorSubresult(OPERATION_LOAD_OBJECT);
        } else {
            subResult = new OperationResult(OPERATION_LOAD_OBJECT);
        }
        PrismObject<T> object = null;
        try {
        	if (options == null) {
        		options = SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
        	} else {
        		GetOperationOptions getOpts = SelectorOptions.findRootOptions(options);
        		if (getOpts == null) {
        			options.add(new SelectorOptions<>(GetOperationOptions.createResolveNames()));
        		} else {
        			getOpts.setResolveNames(Boolean.TRUE);
        		}
        	}
            object = page.getModelService().getObject(type, oid, options, task, subResult);
        } catch (AuthorizationException e) {
        	// Not authorized to access the object. This is probably caused by a reference that
        	// point to an object that the current user cannot read. This is no big deal.
        	// Just do not display that object.
        	subResult.recordHandledError(e);
        	LOGGER.debug("User {} is not authorized to read {} {}",
                    task.getOwner() != null ? task.getOwner().getName() : null, type.getSimpleName(), oid);
        	return null;
        } catch (ObjectNotFoundException e) {
        		// Object does not exist. It was deleted in the meanwhile, or not created yet. This could happen quite often.
				subResult.recordHandledError(e);
				LOGGER.debug("{} {} does not exist", type.getSimpleName(), oid, e);
				return null;
			
        } catch (Exception ex) {
            subResult.recordFatalError("WebModelUtils.couldntLoadObject", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
        } finally {
            subResult.computeStatus();
        }
		// TODO reconsider this part: until recently, the condition was always 'false'
        if (WebComponentUtil.showResultInPage(subResult)) {
        	if (page instanceof PageBase) {
        		((PageBase)page).showResult(subResult);
        	}
        }

        LOGGER.debug("Loaded {} with result {}", object, subResult);

        return object;
    }

    public static boolean isNoFetch(Collection<SelectorOptions<GetOperationOptions>> options) {
    	if (options == null) {
    		return false;
    	}
    	GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
    	if (rootOptions == null) {
    		return false;
    	}
    	return GetOperationOptions.isNoFetch(rootOptions);
    }

    public static <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
                                                                            OperationResult result, PageBase page) {
        return searchObjects(type, query, null, result, page, null);
    }

    public static <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result, PageBase page) {
    	return searchObjects(type, query, options, result, page, null);
    }

    public static <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
                                                                            Collection<SelectorOptions<GetOperationOptions>> options,
                                                                            OperationResult result, PageBase page,
                                                                            PrismObject<UserType> principal) {
        LOGGER.debug("Searching {} with oid {}, options {}", new Object[]{type.getSimpleName(), query, options});

        OperationResult subResult;
        if (result != null) {
            subResult = result.createMinorSubresult(OPERATION_SEARCH_OBJECTS);
        } else {
            subResult = new OperationResult(OPERATION_SEARCH_OBJECTS);
        }
        List<PrismObject<T>> objects = new ArrayList<PrismObject<T>>();
        try {
            Task task = createSimpleTask(subResult.getOperation(), principal, page.getTaskManager());
            List<PrismObject<T>> list = page.getModelService().searchObjects(type, query, options, task, subResult);
            if (list != null) {
                objects.addAll(list);
            }
        } catch (Exception ex) {
            subResult.recordFatalError("WebModelUtils.couldntSearchObjects", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't search objects", ex);
        } finally {
            subResult.computeStatus();
        }

        if (result == null && WebComponentUtil.showResultInPage(subResult)) {
            page.showResult(subResult);
        }

        LOGGER.debug("Loaded ({}) with result {}", new Object[]{objects.size(), subResult});

        return objects;
    }

    public static <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, PageBase page) {
    	LOGGER.debug("Count object: type => {}, query => {}", type, query);
    	Task task = page.createSimpleTask(OPERATION_COUNT_OBJECT);
    	OperationResult parentResult = new OperationResult(OPERATION_COUNT_OBJECT);
    	int count = 0;
    	try {
			count = page.getModelService().countObjects(type, query, null, task, parentResult);
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
				| ConfigurationException | CommunicationException | ExpressionEvaluationException ex) {
			parentResult.recordFatalError("WebModelUtils.couldntCountObjects", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count objects", ex);
		}

         LOGGER.debug("Count objects with result {}", new Object[]{parentResult});
         return count;
    }

    public static <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult result,
                                                           PageBase page) {
        deleteObject(type, oid, null, result, page, null);
    }

    public static <T extends ObjectType> void deleteObject(Class<T> type, String oid, ModelExecuteOptions options,
                                                           OperationResult result, PageBase page) {
        deleteObject(type, oid, options, result, page, null);
    }

    public static <T extends ObjectType> void deleteObject(Class<T> type, String oid, ModelExecuteOptions options,
                                                           OperationResult result, PageBase page,
                                                           PrismObject<UserType> principal) {
        LOGGER.debug("Deleting {} with oid {}, options {}", new Object[]{type.getSimpleName(), oid, options});

        OperationResult subResult;
        if (result != null) {
            subResult = result.createMinorSubresult(OPERATION_DELETE_OBJECT);
        } else {
            subResult = new OperationResult(OPERATION_DELETE_OBJECT);
        }
        try {
            Task task = createSimpleTask(result.getOperation(), principal, page.getTaskManager());

            ObjectDelta delta = new ObjectDelta(type, ChangeType.DELETE, page.getPrismContext());
            delta.setOid(oid);

            page.getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), options, task, subResult);
        } catch (Exception ex) {
            subResult.recordFatalError("WebModelUtils.couldntDeleteObject", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete object", ex);
        } finally {
            subResult.computeStatus();
        }

        if (result == null && WebComponentUtil.showResultInPage(subResult)) {
            page.showResult(subResult);
        }

        LOGGER.debug("Deleted with result {}", new Object[]{result});
    }

    public static Collection<SelectorOptions<GetOperationOptions>> createOptionsForParentOrgRefs() {
        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(ObjectType.F_PARENT_ORG_REF,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        return options;
    }

    public static Collection<SelectorOptions<GetOperationOptions>> createMinimalOptions() {
        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH,
                GetOperationOptions.createRetrieve(RetrieveOption.DEFAULT)));
        return options;
    }

    public static void save(ObjectDelta delta, OperationResult result, PageBase page) {
        save(delta, result, null, page);
    }

    public static void save(ObjectDelta delta, OperationResult result, Task task, PageBase page) {
        save(delta, null, result, task, page);
    }

    public static void save(ObjectDelta delta, ModelExecuteOptions options, OperationResult result, Task task, PageBase page) {
        save(WebComponentUtil.createDeltaCollection(delta), options, result, task, page);
    }


    public static void save(Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options,
                            OperationResult result, Task task, PageBase page) {
        LOGGER.debug("Saving deltas {}, options {}", new Object[]{deltas, options});

        OperationResult subResult;
        if (result != null) {
            subResult = result.createMinorSubresult(OPERATION_SAVE_OBJECT);
        } else {
            subResult = new OperationResult(OPERATION_SAVE_OBJECT);
        }

        try {
            if (task == null) {
            	task = page.createSimpleTask(result.getOperation());
            }

            page.getModelService().executeChanges(deltas, options, task, result);
        } catch (Exception ex) {
            subResult.recordFatalError(ex.getMessage());
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save object", ex);
        } finally {
            subResult.computeStatus();
        }

        if (result == null && WebComponentUtil.showResultInPage(subResult)) {
            page.showResult(subResult);
        }

        LOGGER.debug("Saved with result {}", new Object[]{subResult});
    }

    public static <T extends ObjectType> ObjectDelta<T> createActivationAdminStatusDelta(
            Class<T> type, String oid, boolean enabled, PrismContext context) {

        ItemPath path = new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
        ActivationStatusType status = enabled ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED;
        ObjectDelta objectDelta = ObjectDelta.createModificationReplaceProperty(type, oid, path, context, status);

        return objectDelta;
    }

    public static String getLoggedInUserOid() {
    	MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        Validate.notNull(principal, "No principal");
        if (principal.getOid() == null) {
        	throw new IllegalArgumentException("No OID in principal: "+principal);
        }
        return principal.getOid();
    }

    public static Locale getLocale() {
        return getLocale(null);
    }

    public static Locale getLocale(UserType user) {
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        Locale locale = null;
        if (principal != null) {
            if (user == null) {
                PrismObject<UserType> userPrismObject = principal.getUser().asPrismObject();
                user = userPrismObject == null ? null : userPrismObject.asObjectable();
            }
            if (user != null && user.getPreferredLanguage() != null &&
                    !user.getPreferredLanguage().trim().equals("")) {
                try {
                    locale = LocaleUtils.toLocale(user.getPreferredLanguage());
                } catch (Exception ex) {
                    LOGGER.debug("Error occurred while getting user locale, " + ex.getMessage());
                }
            }
            if (locale != null && MidPointApplication.containsLocale(locale)) {
                return locale;
            } else {
                String userLocale = user != null ? user.getLocale() : null;
                try {
                    locale = userLocale == null ? null : LocaleUtils.toLocale(userLocale);
                } catch (Exception ex) {
                    LOGGER.debug("Error occurred while getting user locale, " + ex.getMessage());
                }
                if (locale != null && MidPointApplication.containsLocale(locale)) {
                    return locale;
                } else {
                    locale = Session.get().getLocale();
                    if (locale == null || !MidPointApplication.containsLocale(locale)) {
                        //default locale for web application
                        return MidPointApplication.getDefaultLocale();
                    }
                    return locale;
                }
            }
        }
        return MidPointApplication.getDefaultLocale();
    }

    public static TimeZone getTimezone() {
        return getTimezone(null);
    }

    public static TimeZone getTimezone(UserType user) {
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        if (principal != null && user == null) {
            user = principal.getUser();
        }
        String timeZone;

        if (user != null && StringUtils.isNotEmpty(user.getTimezone())) {
            timeZone = user.getTimezone();
        } else {
            timeZone = principal != null && principal.getAdminGuiConfiguration() != null ?
                    principal.getAdminGuiConfiguration().getDefaultTimezone() : "";
        }
        try {
            if (timeZone != null) {
                return TimeZone.getTimeZone(timeZone);
            }
        } catch (Exception ex){
            LOGGER.debug("Error occurred while getting user time zone, " + ex.getMessage());
        }
        return null;
    }

    public static Task createSimpleTask(String operation, PrismObject<UserType> owner, TaskManager manager) {
        Task task = manager.createTaskInstance(operation);

        if (owner == null) {
            MidPointPrincipal user = SecurityUtils.getPrincipalUser();
            if (user == null) {
                throw new RestartResponseException(PageLogin.class);
            } else {
                owner = user.getUser().asPrismObject();
            }
        }

        task.setOwner(owner);
        task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);

        return task;
    }

    public static <O extends ObjectType> PrismObject<O> reconstructObject(Class<O> type,
                                 String oid, String eventIdentifier, Task task, OperationResult result){
        try {
            MidPointApplication application = (MidPointApplication) MidPointApplication.get();
            return application.getAuditService().reconstructObject(type, oid, eventIdentifier, task, result);
        } catch (Exception ex){
            LOGGER.debug("Error occurred while reconsructing the object, " + ex.getMessage());
        }
        return null;
    }

	public static Collection<SelectorOptions<GetOperationOptions>> createLookupTableRetrieveOptions() {
		return SelectorOptions.createCollection(LookupTableType.F_ROW,
				GetOperationOptions.createRetrieve(
						new RelationalValueSearchQuery(
								ObjectPaging.createPaging(LookupTableRowType.F_LABEL, OrderDirection.ASCENDING))));
	}

	public static ActivationStatusType getEffectiveStatus(String lifecycleStatus, ActivationType activationType, PageBase pageBase){
        return pageBase.getModelInteractionService().getEffectiveStatus(lifecycleStatus, activationType);
    }

	public static void assumePowerOfAttorney(PrismObject<UserType> donor,
	        ModelInteractionService modelInteractionService, TaskManager taskManager, OperationResult parentResult) {
	    Task task = taskManager.createTaskInstance();
	    OperationResult result = OperationResult.createSubResultOrNewResult(parentResult, OPERATION_ASSUME_POWER_OF_ATTORNEY);

	    try {
	        modelInteractionService.assumePowerOfAttorney(donor, task, result);
	    } catch (CommonException ex) {
	        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't assume power of attorney", ex);
	        result.recordFatalError("Couldn't assume power of attorney", ex);
	    } finally {
	    	result.computeStatusIfUnknown();
	    }
	}

	public static void dropPowerOfAttorney(ModelInteractionService modelInteractionService, TaskManager taskManager, OperationResult parentResult) {
	    Task task = taskManager.createTaskInstance();
		OperationResult result = OperationResult.createSubResultOrNewResult(parentResult, OPERATION_DROP_POWER_OF_ATTORNEY);

	    try {
	        modelInteractionService.dropPowerOfAttorney(task, result);
	    } catch (CommonException ex) {
	        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't drop power of attorney", ex);
	        result.recordFatalError("Couldn't drop power of attorney", ex);
	    } finally {
	    	result.computeStatusIfUnknown();
	    }
	}

	// deduplicate with Action.addIncludeOptionsForExport (ninja module)
	public static void addIncludeOptionsForExportOrView(Collection<SelectorOptions<GetOperationOptions>> options,
			Class<? extends ObjectType> type) {
		// todo fix this brutal hack (related to checking whether to include particular options)
		boolean all = type == null
				|| Objectable.class.equals(type)
				|| com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(type)
				|| ObjectType.class.equals(type);

		if (all || UserType.class.isAssignableFrom(type)) {
			options.add(SelectorOptions.create(UserType.F_JPEG_PHOTO,
					GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
		}
		if (all || LookupTableType.class.isAssignableFrom(type)) {
			options.add(SelectorOptions.create(LookupTableType.F_ROW,
					GetOperationOptions.createRetrieve(
							new RelationalValueSearchQuery(
									ObjectPaging.createPaging(PrismConstants.T_ID, OrderDirection.ASCENDING)))));
		}
		if (all || AccessCertificationCampaignType.class.isAssignableFrom(type)) {
			options.add(SelectorOptions.create(AccessCertificationCampaignType.F_CASE,
					GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
		}
	}

	public static boolean isEnableExperimentalFeature(ModelServiceLocator pageBase) {
		Task task = pageBase.createSimpleTask("Load admin gui config");
		OperationResult result = task.getResult();
		
		ModelInteractionService mInteractionService = pageBase.getModelInteractionService();
		
		AdminGuiConfigurationType adminGuiConfig = null;
		try {
			adminGuiConfig = mInteractionService.getAdminGuiConfiguration(task, result);
			result.recomputeStatus();
			result.recordSuccessIfUnknown();
		} catch (Exception e) {
			LoggingUtils.logException(LOGGER, "Cannot load admin gui config", e);
			result.recordPartialError("Cannot load admin gui config. Reason: " + e.getLocalizedMessage());
			
		}
		
		if (adminGuiConfig == null) {
			return false;
		}
		
		return BooleanUtils.isTrue(adminGuiConfig.isEnableExperimentalFeatures());
		
	}
}
