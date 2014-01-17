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

package com.evolveum.midpoint.web.page.admin.resources;


import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.util.string.StringValue;

import java.util.Collection;

/**
 * @author lazyman
 */
public class PageAdminResources extends PageAdmin {

    private static final String DOT_CLASS = PageAdminResources.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

    protected static final Trace LOGGER = TraceManager.getTrace(PageAdminResources.class);

    protected boolean isResourceOidAvailable() {
        StringValue resourceOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        return resourceOid != null && StringUtils.isNotEmpty(resourceOid.toString());
    }

    protected String getResourceOid() {
        StringValue resourceOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        return resourceOid != null ? resourceOid.toString() : null;
    }

    protected PrismObject<ResourceType> loadResource(Collection<SelectorOptions<GetOperationOptions>> options) {
        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
        PrismObject<ResourceType> resource = null;

        try {
            Task task = createSimpleTask(OPERATION_LOAD_RESOURCE);
            LOGGER.trace("getObject(resource) oid={}, options={}", getResourceOid(), options);
            resource = getModelService().getObject(ResourceType.class, getResourceOid(), options, task, result);
            result.recomputeStatus();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("getObject(resource) result\n:{}", result.dump());
            }

        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get resource", ex);
            result.recordFatalError("Couldn't get resource, reason: " + ex.getMessage(), ex);
        }

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            if (resource != null) {
                showResult(result);
            } else {
                getSession().error(getString("pageAdminResources.message.cantLoadResource"));
                throw new RestartResponseException(PageResources.class);
            }
        }

        return resource;
    }
}
