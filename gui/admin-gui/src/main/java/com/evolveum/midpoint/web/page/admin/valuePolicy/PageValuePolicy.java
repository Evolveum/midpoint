/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.valuePolicy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
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
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.valuePolicy.component.ValuePolicyBasicPanel;
import com.evolveum.midpoint.web.page.admin.valuePolicy.component.ValuePolicySummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * Created by matus on 9/11/2017.
 */

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/valuepolicy",
                        matchUrlForSecurity = "/admin/valuepolicy")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_VALUE_POLICIES_ALL_URL,
                        label = "PageAdminValuePolicies.auth.valuePoliciesAll.label",
                        description = "PageAdminValuePolicies.auth.valuePoliciesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_VALUE_POLICY_URL,
                        label = "PageValuePolicy.auth.valuePolcy.label",
                        description = "PageValuePolicy.auth.valuePolicy.description")
        })

public class PageValuePolicy extends PageAdmin {

    private static final long serialVersionUID = 1L;


    private static final Trace LOGGER = TraceManager.getTrace(PageValuePolicy.class);

    private static final String DOT_CLASS = PageValuePolicy.class.getName() + ".";

    private static final String OPERATION_LOAD_DEFINITION = DOT_CLASS + "loadDefinition";
    private static final String OPERATION_LOAD_VALUEPOLICY = DOT_CLASS + "loadValuePolicy";
    private static final String OPERATION_SAVE_VALUEPOLICY = DOT_CLASS + "saveValuePolicy";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_SUMMARY_PANEL = "summaryPanel";
    private static final String ID_TAB_PANEL = "tabPanel";
   // private static final String ID_VALUE_POLICY_BASIC_DETAIL = "valuePolicyBasic";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";

    private static final String ID_BACK_BUTTON = "backButton";
    private static final String ID_SAVE_BUTTON = "saveButton";


    private LoadableModel<PrismObjectWrapper<ValuePolicyType>> valuePolicyModel;
    private String policyOid;

    public PageValuePolicy(PageParameters parameters) {
        if (parameters != null){
            policyOid = parameters.get(OnePageParameterEncoder.PARAMETER).toString();
            getPageParameters().overwriteWith(parameters);
        }
        initModels();
        initLayout();
    }

    private void initModels() {
        valuePolicyModel = new LoadableModel<PrismObjectWrapper<ValuePolicyType>>(false) {
            @Override
            protected PrismObjectWrapper<ValuePolicyType> load() {
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

    private PrismObjectWrapper<ValuePolicyType> createValuePolicy() throws SchemaException {
        Task task = createSimpleTask(OPERATION_LOAD_VALUEPOLICY);

        PrismObject<ValuePolicyType> valuePolicyObject  = getPrismContext().createObject(ValuePolicyType.class);

        PrismObjectWrapperFactory<ValuePolicyType> owf = getRegistry().getObjectWrapperFactory(valuePolicyObject.getDefinition());
        WrapperContext context = new WrapperContext(task, task.getResult());
        PrismObjectWrapper<ValuePolicyType> valuePolicyWrapper = owf.createObjectWrapper(valuePolicyObject, ItemStatus.ADDED, context);

//        PrismObjectWrapper<ValuePolicyType> valuePolicyWrapper = ObjectWrapperUtil.createObjectWrapper("","", valuePolicyObject, ContainerStatus.ADDING,task, WebComponentUtil.getPageBase(this));

        return valuePolicyWrapper;
    }

    private PrismObjectWrapper<ValuePolicyType> loadValuePolicy(String policyOid) {
        Task task = createSimpleTask(OPERATION_LOAD_DEFINITION);
        OperationResult result = task.getResult();
        PrismObjectWrapper<ValuePolicyType> valuePolicyWrapper = null;
        try {
            PrismObject<ValuePolicyType> valuePolicyObject =
                    WebModelServiceUtils.loadObject(ValuePolicyType.class, policyOid,
                            PageValuePolicy.this, task, result);
            PrismObjectWrapperFactory<ValuePolicyType> owf = getRegistry().getObjectWrapperFactory(valuePolicyObject.getDefinition());
            WrapperContext context = new WrapperContext(task, result);
            valuePolicyWrapper = owf.createObjectWrapper(valuePolicyObject, ItemStatus.NOT_CHANGED, context);

            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get definition", ex);
            result.recordFatalError(getString("PageValuePolicy.message.loadValuePolicy.fatalError"), ex);
        }
        result.recomputeStatus();

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }
        return valuePolicyWrapper;
    }

    protected void initLayout() {
        // TODO should be used if valuePolicyObject is edited
        ValuePolicySummaryPanel summaryPanel = new ValuePolicySummaryPanel(ID_SUMMARY_PANEL,
                Model.of(valuePolicyModel.getObject().getObject().asObjectable()), this);
        add(summaryPanel);

        MidpointForm mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);


       // List<ItemPath> itemPath = new ArrayList<>();
       // itemPath.add(ItemPath.EMPTY_PATH);

        // itemPath.add(prismContext.path(ValuePolicyType.F_STRING_POLICY));


       //PrismPanel<ValuePolicyType> valuePolicyForm = new PrismPanel<>(ID_VALUE_POLICY_BASIC_DETAIL, new ContainerWrapperListFromObjectWrapperModel<ValuePolicyType,ValuePolicyType>(valuePolicyModel, itemPath),null, mainForm, null, this);

       // mainForm.add(valuePolicyForm);
        initTabs(mainForm);
        initButtons(mainForm);

    }
    private void initTabs(MidpointForm mainForm){
        List<ITab> tabs = new ArrayList<>();
            PageBase baseParameter = this;
        tabs.add(new AbstractTab(createStringResource("PageValuePolicy.basic")) {
            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new ValuePolicyBasicPanel(panelId,mainForm,valuePolicyModel);
            }
        });

      //  tabs.add(new AbstractTab(createStringResource("PageValuePolicy.stringPolicy")) {
       //     @Override
        //    public WebMarkupContainer getPanel(String panelId) {
          //      return new ValuePolicyStringPoliciesPanel(panelId,mainForm,valuePolicyModel,baseParameter);
           // }
       // });
        TabbedPanel tabPanel = WebComponentUtil.createTabPanel(ID_TAB_PANEL, this, tabs, null);
        mainForm.add(tabPanel);

    }
    private void initButtons(MidpointForm mainForm){
        AjaxButton backButton = new AjaxButton(ID_BACK_BUTTON,createStringResource("PageValuePolicy.button.back")){
            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }
        };
        mainForm.add(backButton);

        AjaxSubmitButton saveButton = new AjaxSubmitButton(ID_SAVE_BUTTON,createStringResource("PageValuePolicy.button.save")) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                savePerformed(target);
            }
        };
        mainForm.add(saveButton);
    }

    private void savePerformed(AjaxRequestTarget target) {
        LOGGER.debug("Saving value policy.");

        OperationResult result = new OperationResult(OPERATION_SAVE_VALUEPOLICY);
        try {
            WebComponentUtil.revive(valuePolicyModel, getPrismContext());
            PrismObjectWrapper<ValuePolicyType> wrapper = valuePolicyModel.getObject();
            ObjectDelta<ValuePolicyType> delta = wrapper.getObjectDelta();
            if (delta == null) {
                return;
            }
            if (delta.getPrismContext() == null) {
                getPrismContext().adopt(delta);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Computed value policy delta:\n{}", new Object[]{delta.debugDump(3)});
            }

            if (delta.isEmpty()) {
                return;
            }

            Task task = createSimpleTask(OPERATION_SAVE_VALUEPOLICY);
            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
            deltas.add(delta);
            getModelService().executeChanges(deltas, null, task, result);
            result.recomputeStatus();
        } catch (Exception ex) {
            result.recordFatalError(getString("PageValuePolicy.message.savePerformed.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save value policy", ex);
        }
        if (!result.isSuccess()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
            showResult(result);
            redirectBack();
        }
    }

}
