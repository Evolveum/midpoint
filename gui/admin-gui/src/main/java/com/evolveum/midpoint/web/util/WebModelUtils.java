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

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import java.util.Collection;

/**
 * @author lazyman
 */
public class WebModelUtils {

    private static final Trace LOGGER = TraceManager.getTrace(WebModelUtils.class);

    public static <T extends ObjectType> PrismObject<T> loadObjectAsync(Class<T> type, String oid, OperationResult result,
                                                                        PageBase page, PrismObject<UserType> principal) {
        return loadObject(type, oid, null, result, page, principal);
    }

    public static <T extends ObjectType> PrismObject<T> loadObjectAsync(Class<T> type, String oid,
                                                                        Collection<SelectorOptions<GetOperationOptions>> options,
                                                                        OperationResult result, PageBase page, PrismObject<UserType> principal) {
        return loadObject(type, oid, options, result, page, principal);
    }

    public static <T extends ObjectType> PrismObject<T> loadObject(Class<T> type, String oid, OperationResult result,
                                                                   PageBase page) {
        return loadObject(type, oid, null, result, page);
    }

    public static <T extends ObjectType> PrismObject<T> loadObject(Class<T> type, String oid,
                                                                   Collection<SelectorOptions<GetOperationOptions>> options,
                                                                   OperationResult result, PageBase page) {

        return loadObject(type, oid, null, result, page, null);
    }

    private static <T extends ObjectType> PrismObject<T> loadObject(Class<T> type, String oid,
                                                                    Collection<SelectorOptions<GetOperationOptions>> options,
                                                                    OperationResult result, PageBase page, PrismObject<UserType> principal) {
        LOGGER.debug("Loading {} with oid {}, options {}", new Object[]{type.getSimpleName(), oid, options});

        PrismObject<T> object = null;
        try {
            Task task = page.createSimpleTask(result.getOperation(), principal);
            object = page.getModelService().getObject(type, oid, options, task, result);
        } catch (Exception ex) {
            result.recordFatalError("WebModelUtils.couldntLoadObject", ex);
            LoggingUtils.logException(LOGGER, "Couldn't load object", ex);
        } finally {
            result.computeStatus();
        }

        LOGGER.debug("Loaded with result {}", new Object[]{result});

        return object;
    }

    public static <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult result,
                                                           PageBase page) {
        deleteObject(type, oid, result, null, page, null);
    }

    public static <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult result,
                                                           ModelExecuteOptions options,
                                                           PageBase page, PrismObject<UserType> principal) {
        LOGGER.debug("Deleting {} with oid {}, options {}", new Object[]{type.getSimpleName(), oid, options});
        try {
            Task task = page.createSimpleTask(result.getOperation(), principal);

            ObjectDelta delta = new ObjectDelta(type, ChangeType.DELETE, page.getPrismContext());
            delta.setOid(oid);

            page.getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), options, task, result);
        } catch (Exception ex) {
            result.recordFatalError("WebModelUtils.couldntDeleteObject", ex);
            LoggingUtils.logException(LOGGER, "Couldn't delete object", ex);
        } finally {
            result.computeStatus();
        }

        LOGGER.debug("Deleted with result {}", new Object[]{result});
    }
}
