/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.prism.PrismObject;
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
        Task task = page.createSimpleTask(result.getOperation(), principal);

        PrismObject<T> object = null;
        try {
            object = page.getModelService().getObject(type, oid, options, task, result);
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            result.recordFatalError("WebModelUtils.couldntLoadObject", ex);
            LoggingUtils.logException(LOGGER, "Couldn't load object", ex);
        }
        result.recomputeStatus();

        return object;
    }
}
