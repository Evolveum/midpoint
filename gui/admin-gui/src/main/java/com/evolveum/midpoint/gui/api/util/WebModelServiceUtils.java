/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.util;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;

import java.util.*;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ThreadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.CompiledUserProfile;
import com.evolveum.midpoint.model.api.authentication.MidPointUserProfilePrincipal;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.AuthorizationException;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;

import javax.xml.namespace.QName;

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
    private static final String OPERATION_GET_SYSTEM_CONFIG = DOT_CLASS + "getSystemConfiguration";
    private static final String OPERATION_LOAD_FLOW_POLICY = DOT_CLASS + "loadFlowPolicy";

    public static String resolveReferenceName(ObjectReferenceType ref, PageBase page) {
        return resolveReferenceName(ref, page, false);
    }

    public static String resolveReferenceName(ObjectReferenceType ref, PageBase page, boolean translate) {
        Task task = page.createSimpleTask(WebModelServiceUtils.class.getName() + ".resolveReferenceName");
        return resolveReferenceName(ref, page, task, task.getResult(), translate);
    }

    public static String resolveReferenceName(ObjectReferenceType ref, PageBase page, Task task, OperationResult result) {
        return resolveReferenceName(ref, page, task, result, false);
    }

    public static String resolveReferenceName(ObjectReferenceType ref, PageBase page, Task task, OperationResult result, boolean translate) {
        if (ref == null) {
            return null;
        }
        if (ref.getTargetName() != null) {
            return translate ? page.getLocalizationService().translate(ref.getTargetName().toPolyString(), page.getLocale(), true)
                    : ref.getTargetName().getOrig();
        }
        if (StringUtils.isEmpty(ref.getOid()) || ref.getType() == null){
            return null;
        }
        PrismObject<ObjectType> object = resolveReferenceNoFetch(ref, page, task, result);
        if (object == null) {
            return ref.getOid();
        } else {
            ref.asReferenceValue().setObject(object);
            return WebComponentUtil.getName(object, translate);
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

        try {
            List<PrismObject<O>> objects = searchObjects(type, null, result, page);
            result.recomputeStatus();
            List<ObjectReferenceType> references = new ArrayList<>();

            for(PrismObject<O> object: objects){
                referenceMap.put(object.getOid(), WebComponentUtil.getName(object));
                references.add(ObjectTypeUtil.createObjectRef(object, page.getPrismContext()));

            }
            return references;
        } catch (Exception e){
            result.recordFatalError(page.createStringResource("WebModelUtils.couldntLoadPasswordPolicies").getString(), e);
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
            ObjectDelta<TaskType> delta = DeltaFactory.Object.createAddDelta(taskToRun.asPrismObject());
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
//            error(pageBase.getString("pageUsers.message.nothingSelected") + e.getMessage());
            parentResult.recordFatalError(pageBase.createStringResource("WebModelUtils.couldntRunTask", e.getMessage()).getString(), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't run task " + e.getMessage(), e);
            return null;
        }

    }

    public static void runTask(Collection<TaskType> tasksToRun, Task operationalTask, OperationResult parentResult, PageBase pageBase){
//        try {

            for (TaskType taskToRun : tasksToRun){
                runTask(tasksToRun, operationalTask, parentResult, pageBase);
            }

//            }
//            ObjectDelta<TaskType> delta = ObjectDelta.createAddDelta(taskToRun.asPrismObject());
//            pageBase.getPrismContext().adopt(delta);
//            pageBase.getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), null,
//                    operationalTask, parentResult);
//            parentResult.recordInProgress();
//            parentResult.setBackgroundTaskOid(delta.getOid());
//            pageBase.showResult(parentResult);
//            return delta.getOid();
//        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
//                | ExpressionEvaluationException | CommunicationException | ConfigurationException
//                | PolicyViolationException | SecurityViolationException e) {
//            // TODO Auto-generated catch block
////            error(pageBase.getString("pageUsers.message.nothingSelected") + e.getMessage());
//            parentResult.recordFatalError(pageBase.createStringResource("WebModelUtils.couldntRunTask", e.getMessage()).getString(), e);
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't run task " + e.getMessage(), e);
//            return null;
//        }

    }

    public static <O extends ObjectType> PrismObject<O> loadObject(PrismReferenceValue objectRef, QName expectedTargetType, PageBase pageBase, Task task, OperationResult result) {
        if (objectRef == null) {
            return null;
        }

        if (QNameUtil.match(expectedTargetType, objectRef.getTargetType())) {
            Class<O> type = pageBase.getPrismContext().getSchemaRegistry().determineClassForType(objectRef.getTargetType());
            PrismObject<O> resourceType = WebModelServiceUtils.loadObject(type, objectRef.getOid(), GetOperationOptions.createNoFetchCollection(), pageBase, task, result);
            return resourceType;
        }

        return null;
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
                subResult.recordFatalError(page.createStringResource("WebModelUtils.couldntLoadObject").getString(), e);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", e);
            }
        } catch (Exception ex) {
            subResult.recordFatalError(page.createStringResource("WebModelUtils.couldntLoadObject").getString(), ex);
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

    @NotNull
    public static <T extends ObjectType> List<PrismObject<T>> searchObjects(
            Class<T> type, ObjectQuery query, OperationResult result, PageBase page) {
        return searchObjects(type, query, null, result, page, null);
    }

    @NotNull
    public static <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result, PageBase page) {
        return searchObjects(type, query, options, result, page, null);
    }

    @NotNull
    public static <T extends ObjectType> List<PrismObject<T>> searchObjects(
            Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result, PageBase page, PrismObject<UserType> principal) {
        LOGGER.debug("Searching {} with oid {}, options {}", type.getSimpleName(), query, options);

        OperationResult subResult;
        if (result != null) {
            subResult = result.createMinorSubresult(OPERATION_SEARCH_OBJECTS);
        } else {
            subResult = new OperationResult(OPERATION_SEARCH_OBJECTS);
        }
        List<PrismObject<T>> objects = new ArrayList<>();
        try {
            Task task = createSimpleTask(subResult.getOperation(), principal, page.getTaskManager());
            List<PrismObject<T>> list = page.getModelService().searchObjects(type, query, options, task, subResult);
            if (list != null) {
                objects.addAll(list);
            }
        } catch (Exception ex) {
            subResult.recordFatalError(page.createStringResource("WebModelUtils.couldntSearchObjects").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't search objects", ex);
        } finally {
            subResult.computeStatus();
        }

        if (result == null && WebComponentUtil.showResultInPage(subResult)) {
            page.showResult(subResult);
        }

        LOGGER.debug("Loaded ({}) with result {}", objects.size(), subResult);

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
            parentResult.recordFatalError(page.createStringResource("WebModelUtils.couldntCountObjects").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count objects", ex);
        }

         LOGGER.debug("Count objects with result {}", parentResult);
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
        LOGGER.debug("Deleting {} with oid {}, options {}", type.getSimpleName(), oid, options);

        OperationResult subResult;
        if (result != null) {
            subResult = result.createMinorSubresult(OPERATION_DELETE_OBJECT);
        } else {
            subResult = new OperationResult(OPERATION_DELETE_OBJECT);
        }
        try {
            Task task = createSimpleTask(result.getOperation(), principal, page.getTaskManager());

            ObjectDelta delta = page.getPrismContext().deltaFactory().object().create(type, ChangeType.DELETE);
            delta.setOid(oid);

            page.getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), options, task, subResult);
        } catch (Exception ex) {
            subResult.recordFatalError(page.createStringResource("WebModelUtils.couldntDeleteObject").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete object", ex);
        } finally {
            subResult.computeStatus();
        }

        if (result == null && WebComponentUtil.showResultInPage(subResult)) {
            page.showResult(subResult);
        }

        LOGGER.debug("Deleted with result {}", result);
    }

    public static Collection<SelectorOptions<GetOperationOptions>> createOptionsForParentOrgRefs(GetOperationOptionsBuilder builder) {
        return builder
                .item(ObjectType.F_PARENT_ORG_REF).retrieve()
                .build();
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
        LOGGER.debug("Saving deltas {}, options {}", deltas, options);

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

        LOGGER.debug("Saved with result {}", subResult);
    }

    public static <T extends ObjectType> ObjectDelta<T> createActivationAdminStatusDelta(
            Class<T> type, String oid, boolean enabled, PrismContext context) {

        ItemPath path = SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;
        ActivationStatusType status = enabled ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED;
        ObjectDelta objectDelta = context.deltaFactory().object().createModificationReplaceProperty(type, oid, path,
                status);

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
                    //session in tests is null
                        if (ThreadContext.getSession() == null) {
                            return MidPointApplication.getDefaultLocale();
                        }

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
        MidPointUserProfilePrincipal principal = SecurityUtils.getPrincipalUser();
        if (principal != null && user == null) {
            user = principal.getUser();
        }
        String timeZone;

        if (user != null && StringUtils.isNotEmpty(user.getTimezone())) {
            timeZone = user.getTimezone();
        } else {
            timeZone = principal != null && principal.getCompiledUserProfile() != null ?
                    principal.getCompiledUserProfile().getDefaultTimezone() : "";
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
        return createSimpleTask(operation, null, owner, manager);
    }

    public static Task createSimpleTask(String operation, String channel, PrismObject<UserType> owner, TaskManager manager) {
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
        if (channel == null) {
            task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);
        } else {
            task.setChannel(channel);
        }

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

    public static Collection<SelectorOptions<GetOperationOptions>> createLookupTableRetrieveOptions(SchemaHelper schemaHelper) {
        return schemaHelper.getOperationOptionsBuilder()
                .item(LookupTableType.F_ROW)
                .retrieveQuery()
                    .asc(LookupTableRowType.F_LABEL)
                .end()
                .build();
    }

    public static ActivationStatusType getAssignmentEffectiveStatus(String lifecycleStatus, ActivationType activationType, PageBase pageBase){
        return pageBase.getModelInteractionService().getAssignmentEffectiveStatus(lifecycleStatus, activationType);
    }

    public static void assumePowerOfAttorney(PrismObject<UserType> donor,
            ModelInteractionService modelInteractionService, TaskManager taskManager, OperationResult parentResult) {
        Task task = taskManager.createTaskInstance();
        OperationResult result = OperationResult.createSubResultOrNewResult(parentResult, OPERATION_ASSUME_POWER_OF_ATTORNEY);

        try {
            modelInteractionService.assumePowerOfAttorney(donor, task, result);
        } catch (CommonException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't assume power of attorney", ex);
            result.recordFatalError("WebModelUtils.couldntAssumePowerAttorney", ex);
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
            result.recordFatalError("WebModelUtils.couldntDropPowerAttorney", ex);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public static boolean isEnableExperimentalFeature(Task task, ModelServiceLocator pageBase) {
        OperationResult result = task.getResult();

        ModelInteractionService mInteractionService = pageBase.getModelInteractionService();

        CompiledUserProfile adminGuiConfig = null;
        try {
            adminGuiConfig = mInteractionService.getCompiledUserProfile(task, result);
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

    public static boolean isEnableExperimentalFeature(ModelInteractionService modelInteractionService, Task task, OperationResult result) {
        CompiledUserProfile adminGuiConfig = null;
        try {
            adminGuiConfig = modelInteractionService.getCompiledUserProfile(task, result);
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

    public static boolean isEnableExperimentalFeature(ModelServiceLocator pageBase) {
        Task task = pageBase.createSimpleTask("Load admin gui config");
        return isEnableExperimentalFeature(task, pageBase);

    }

    public static AccessCertificationConfigurationType getCertificationConfiguration(PageBase pageBase) {
        OperationResult result = new OperationResult(WebModelServiceUtils.class.getName() + ".getCertificationConfiguration");
        try {
            return pageBase.getModelInteractionService().getCertificationConfiguration(result);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot load certification configuration", t);
            return null;
        }
    }

    public static String translateMessage(OperationResult result, ModelServiceLocator page) {
        LocalizationService service = page.getLocalizationService();
        Locale locale = page.getLocale();

        return service.translate(result.getUserFriendlyMessage(), locale);
    }

    public static boolean isPostAuthenticationEnabled(TaskManager taskManager, ModelInteractionService modelInteractionService) {
        MidPointPrincipal midpointPrincipal = SecurityUtils.getPrincipalUser();
        if (midpointPrincipal != null) {
            UserType user = midpointPrincipal.getUser();
            Task task = taskManager.createTaskInstance(OPERATION_LOAD_FLOW_POLICY);
            OperationResult parentResult = new OperationResult(OPERATION_LOAD_FLOW_POLICY);
            RegistrationsPolicyType registrationPolicyType;
            try {
                registrationPolicyType = modelInteractionService.getFlowPolicy(user.asPrismObject(), task, parentResult);
                if (registrationPolicyType == null) {
                    return false;
                }
                SelfRegistrationPolicyType postAuthenticationPolicy = registrationPolicyType.getPostAuthentication();
                if (postAuthenticationPolicy == null) {
                    return false;
                }
                String requiredLifecycleState = postAuthenticationPolicy.getRequiredLifecycleState();
                if (StringUtils.isNotBlank(requiredLifecycleState) && requiredLifecycleState.equals(user.getLifecycleState())) {
                    return true;
                }
            } catch (CommonException e) {
                LoggingUtils.logException(LOGGER, "Cannot determine post authentication policies", e);
            }
        }
        return false;
    }

    public static PrismObject<SystemConfigurationType> loadSystemConfigurationAsPrismObject(PageBase pageBase, Task task, OperationResult result) {
        PrismObject<SystemConfigurationType> systemConfig = loadObject(
            SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null,
            pageBase, task, result);

        return systemConfig;
    }

}
