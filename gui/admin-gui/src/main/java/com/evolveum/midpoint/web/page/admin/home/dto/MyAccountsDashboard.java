/*
 * Copyright (c) 2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.home.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dashboard.Dashboard;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public class MyAccountsDashboard extends Dashboard<ArrayList<AccountItemDto>> {

    private static final Trace LOGGER = TraceManager.getTrace(MyAccountsDashboard.class);

    private static final String DOT_CLASS = MyAccountsDashboard.class.getName() + ".";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";


    public MyAccountsDashboard() {
        super(true, true);
    }

    private void loadObject() {

    }

    private List<SimpleAccountDto> loadAccounts(PrismObject<UserType> prismUser) {
        List<SimpleAccountDto> list = new ArrayList<SimpleAccountDto>();
        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
        Task task = null;//createSimpleTask(OPERATION_LOAD_ACCOUNT);

        List<ObjectReferenceType> references = prismUser.asObjectable().getAccountRef();
        for (ObjectReferenceType reference : references) {
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_ACCOUNT);
            try {

                Collection<SelectorOptions<GetOperationOptions>> options =
                        SelectorOptions.createCollection(AccountShadowType.F_RESOURCE, GetOperationOptions.createResolve());

                PrismObject<AccountShadowType> account = null;//getModelService().getObject(AccountShadowType.class,
                //reference.getOid(), options, task, subResult);
                AccountShadowType accountType = account.asObjectable();

                OperationResultType fetchResult = accountType.getFetchResult();
                if (fetchResult != null && !OperationResultStatusType.SUCCESS.equals(fetchResult.getStatus())) {
                    //showResult(OperationResult.createOperationResult(fetchResult));
                }


                ResourceType resource = accountType.getResource();
                String resourceName = WebMiscUtil.getName(resource);
                list.add(new SimpleAccountDto(WebMiscUtil.getOrigStringFromPoly(accountType.getName()), resourceName));

                subResult.recomputeStatus();
            } catch (Exception ex) {
                subResult.recordFatalError("Couldn't load account.", ex);
                LoggingUtils.logException(LOGGER, "Couldn't load account", ex);
            }
        }
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        if (!result.isSuccess()) {
            // showResult(result);
        }

        return list;
    }
}
