/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 */
public class WebModelUtils {

    private static final Trace LOGGER = TraceManager.getTrace(WebModelUtils.class);

    private static final String DOT_CLASS = WebModelUtils.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_SAVE_OBJECT = DOT_CLASS + "saveObject";

    public static <T extends ObjectType> PrismObject<T> loadObject(Class<T> type, String oid, OperationResult result,
                                                                   PageBase page) {
        return loadObject(type, oid, null, result, page);
    }

    public static <T extends ObjectType> PrismObject<T> loadObject(Class<T> type, String oid,
                                                                   Collection<SelectorOptions<GetOperationOptions>> options,
                                                                   OperationResult result, PageBase page) {

        return loadObject(type, oid, options, result, page, null);
    }

    private static <T extends ObjectType> PrismObject<T> loadObject(Class<T> type, String oid,
                                                                    Collection<SelectorOptions<GetOperationOptions>> options,
                                                                    OperationResult result, PageBase page, PrismObject<UserType> principal) {
        LOGGER.debug("Loading {} with oid {}, options {}", new Object[]{type.getSimpleName(), oid, options});

        OperationResult subResult;
        if (result != null) {
            subResult = result.createMinorSubresult(OPERATION_LOAD_OBJECT);
        } else {
            subResult = new OperationResult(OPERATION_LOAD_OBJECT);
        }
        PrismObject<T> object = null;
        try {
            Task task = page.createSimpleTask(subResult.getOperation(), principal);
            object = page.getModelService().getObject(type, oid, options, task, subResult);
        } catch (Exception ex) {
            subResult.recordFatalError("WebModelUtils.couldntLoadObject", ex);
            LoggingUtils.logException(LOGGER, "Couldn't load object", ex);
        } finally {
            subResult.computeStatus();
        }

        if (result == null && WebMiscUtil.showResultInPage(subResult)) {
            page.showResultInSession(subResult);
        }

        LOGGER.debug("Loaded with result {}", new Object[]{subResult});

        return object;
    }

    public static <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
                                                                            OperationResult result, PageBase page) {
        return searchObjects(type, query, null, result, page, null);
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
            Task task = page.createSimpleTask(subResult.getOperation(), principal);
            List<PrismObject<T>> list = page.getModelService().searchObjects(type, query, options, task, subResult);
            if (list != null) {
                objects.addAll(list);
            }
        } catch (Exception ex) {
            subResult.recordFatalError("WebModelUtils.couldntSearchObjects", ex);
            LoggingUtils.logException(LOGGER, "Couldn't search objects", ex);
        } finally {
            subResult.computeStatus();
        }

        if (result == null && WebMiscUtil.showResultInPage(subResult)) {
            page.showResultInSession(subResult);
        }

        LOGGER.debug("Loaded ({}) with result {}", new Object[]{objects.size(), subResult});

        return objects;
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
            Task task = page.createSimpleTask(result.getOperation(), principal);

            ObjectDelta delta = new ObjectDelta(type, ChangeType.DELETE, page.getPrismContext());
            delta.setOid(oid);

            page.getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), options, task, subResult);
        } catch (Exception ex) {
            subResult.recordFatalError("WebModelUtils.couldntDeleteObject", ex);
            LoggingUtils.logException(LOGGER, "Couldn't delete object", ex);
        } finally {
            subResult.computeStatus();
        }

        if (result == null && WebMiscUtil.showResultInPage(subResult)) {
            page.showResultInSession(subResult);
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
        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH,
                GetOperationOptions.createRetrieve(RetrieveOption.DEFAULT)));
        return options;
    }

    public static void save(ObjectDelta delta, OperationResult result, PageBase page) {
        save(delta, null, result, page);
    }

    public static void save(ObjectDelta delta, ModelExecuteOptions options, OperationResult result, PageBase page) {
        save(WebMiscUtil.createDeltaCollection(delta), options, result, page);
    }

    public static void save(Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options,
                            OperationResult result, PageBase page) {
        LOGGER.debug("Saving deltas {}, options {}", new Object[]{deltas, options});

        OperationResult subResult;
        if (result != null) {
            subResult = result.createMinorSubresult(OPERATION_SAVE_OBJECT);
        } else {
            subResult = new OperationResult(OPERATION_SAVE_OBJECT);
        }

        try {
            Task task = page.createSimpleTask(result.getOperation());
            page.getModelService().executeChanges(deltas, options, task, result);
        } catch (Exception ex) {
            subResult.recordFatalError("WebModelUtils.couldntSearchObjects", ex);
            LoggingUtils.logException(LOGGER, "Couldn't search objects", ex);
        } finally {
            subResult.computeStatus();
        }

        if (result == null && WebMiscUtil.showResultInPage(subResult)) {
            page.showResultInSession(subResult);
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
}
