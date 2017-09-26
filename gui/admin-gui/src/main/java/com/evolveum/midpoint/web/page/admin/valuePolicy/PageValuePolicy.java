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

package com.evolveum.midpoint.web.page.admin.valuePolicy;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.page.admin.valuePolicy.component.ValuePolicyBasicPanel;
import com.evolveum.midpoint.web.page.admin.valuePolicy.component.ValuePolicyDto;
import com.evolveum.midpoint.web.page.admin.valuePolicy.component.ValuePolicySummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Collection;

/**
 * Created by matus on 9/11/2017.
 */

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/valuepolicy", matchUrlForSecurity = "/admin/valuepolicy")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminValuePolicies.AUTH_VALUE_POLICIES_ALL,
                        label = PageAdminValuePolicies.AUTH_VALUE_POLICIES_ALL_LABEL,
                        description = PageAdminValuePolicies.AUTH_VALUE_POLICIES_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_VALUE_POLICY_URL,
                        label = "PageValuePolicy.auth.valuePolcy.label",
                        description = "PageValuePolicy.auth.valuePolicy.description")
        })


public class PageValuePolicy extends PageAdminValuePolicies {

    private static final long serialVersionUID = 1L;


    private static final Trace LOGGER = TraceManager.getTrace(PageValuePolicy.class);

    private static final String DOT_CLASS = PageValuePolicy.class.getName() + ".";

    private static final String OPERATION_LOAD_DEFINITION = DOT_CLASS + "loadDefinition";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BASIC_INFO_CONTAINER = "constructBasicInfoContainer";
    private static final String ID_VALUE_POLICY_NAME = "valuePolicyName";
    private static final String ID_SUMMARY_PANEL = "summaryPanel";
    private static final String ID_BASIC_PANEL = "basicPanel";

    private LoadableModel<ValuePolicyDto> valuePolicyModel;
    private String policyOid;

    public PageValuePolicy(PageParameters parameters) {
        policyOid = parameters.get(OnePageParameterEncoder.PARAMETER).toString();
        getPageParameters().overwriteWith(parameters);
        initModels();
        initLayout();
    }

    private void initModels() {
        valuePolicyModel = new LoadableModel<ValuePolicyDto>(false) {
            @Override
            protected ValuePolicyDto load() {
                if (policyOid != null) {
                    return loadValuePolicy(policyOid);
                } else {
                    try {
                        return createValuePolicy();
                    } catch (SchemaException e) {
                        throw new SystemException(e.getMessage(), e);
                    }
                }
            }
        };
    }

    private ValuePolicyDto createValuePolicy() throws SchemaException {

        ValuePolicyType valuePolicy = getPrismContext().createObjectable(ValuePolicyType.class);

        return new ValuePolicyDto(valuePolicy);
    }

    private ValuePolicyDto loadValuePolicy(String policyOid) {
        Task task = createSimpleTask(OPERATION_LOAD_DEFINITION);
        OperationResult result = task.getResult();
        ValuePolicyDto valuePolicyDto = null;
        try {
            Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
            PrismObject<ValuePolicyType> valuePolicyObject =
                    WebModelServiceUtils.loadObject(ValuePolicyType.class, policyOid, options,
                            PageValuePolicy.this, task, result);
            ValuePolicyType valuePolicyType = PrismObjectValue.asObjectable(valuePolicyObject);
            valuePolicyDto = new ValuePolicyDto(valuePolicyType);
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get definition", ex);
            result.recordFatalError("Couldn't get definition.", ex);
        }
        result.recomputeStatus();

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }
        return valuePolicyDto;
    }


    protected void initLayout() {
        ValuePolicySummaryPanel summaryPanel = new ValuePolicySummaryPanel(ID_SUMMARY_PANEL, new PropertyModel<>(valuePolicyModel, "prismObject"), this);
        add(summaryPanel);

        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);
        ValuePolicyBasicPanel basicPanel = new ValuePolicyBasicPanel(ID_BASIC_PANEL, valuePolicyModel);
        mainForm.add(basicPanel);

    }

}
