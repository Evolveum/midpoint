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
package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

/**
 * @author honchar
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/usersView", matchUrlForSecurity = "/admin/usersView")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
                        label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
                        description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_VIEW_URL,
                        label = "PageUsers.auth.users.label",
                        description = "PageUsers.auth.users.description")
        })
public class PageUsersView extends PageUsers {
    private static final long serialVersionUID = 1L;

    public static final String PARAMETER_OBJECT_COLLECTION_TYPE_OID = "collectionOid";

    private static final Trace LOGGER = TraceManager.getTrace(PageUsersView.class);

    private static final String DOT_CLASS = PageUsersView.class.getName() + ".";
    private static final String OPERATION_LOAD_USERS_VIEW_COLLECTION_REF = DOT_CLASS + "loadUsersViewCollectionRef";

    public PageUsersView(){
        super();
    }

    @Override
    protected ObjectFilter getUsersViewFilter(){
        PageParameters parameters = getPageParameters();
        StringValue collectionOidValue = parameters.get(PARAMETER_OBJECT_COLLECTION_TYPE_OID);
        if (collectionOidValue == null || collectionOidValue.isEmpty()){
            return null;
        }
        OperationResult result = new OperationResult(OPERATION_LOAD_USERS_VIEW_COLLECTION_REF);
        Task task = createSimpleTask(OPERATION_LOAD_USERS_VIEW_COLLECTION_REF);
        PrismObject<ObjectCollectionType> collectionObject = WebModelServiceUtils.loadObject(ObjectCollectionType.class, collectionOidValue.toString(),
                this, task, result);
        if (collectionObject == null){
            return null;
        }
        ObjectCollectionType collectionValue = collectionObject.getValue().asObjectable();
        if (!QNameUtil.match(collectionValue.getType(), UserType.COMPLEX_TYPE)){
            return null;
        }
        ObjectFilter filter = null;
        try {
            filter = getQueryConverter().parseFilter(collectionValue.getFilter(), UserType.class);
        } catch (SchemaException ex){
            result.recomputeStatus();
            result.recordFatalError("Couldn't parse filter. Filter: " + collectionValue.getFilter(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse filter. Filter: " + collectionValue.getFilter(), ex);
        }
        return filter;
    }
}
