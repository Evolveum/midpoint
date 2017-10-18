package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
import java.util.Collection;

@PageDescriptor(url = "/admin/case", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminCases.AUTH_CASES_ALL,
                label = PageAdminCases.AUTH_CASES_ALL_LABEL,
                description = PageAdminCases.AUTH_CASES_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASE_URL,
                label = "PageCase.auth.case.label",
                description = "PageCase.auth.case.description")})
public class PageCase  extends PageAdminCases {

    private static final Trace LOGGER = TraceManager.getTrace(PageCase.class);
    private static final String DOT_CLASS = PageCase.class.getName() + ".";
    private static final String OPERATION_LOAD_CASE = DOT_CLASS + "loadCase";
    private static final String OPERATION_SAVE_CASE = DOT_CLASS + "saveCase";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CASE = "case";
    private static final String ID_BACK_BUTTON = "backButton";
    private static final String ID_SAVE_BUTTON = "saveButton";

    private LoadableModel<ObjectWrapper<CaseType>> caseModel;

    public PageCase() {
        initialize();
    }

    public PageCase(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize();
    }
    private void initialize(){
        caseModel = new LoadableModel<ObjectWrapper<CaseType>>(false) {

            @Override
            protected ObjectWrapper<CaseType> load() {
                return loadCase();
            }
        };
        initLayout();
    }

    private ObjectWrapper<CaseType> loadCase() {
        Task task = createSimpleTask(OPERATION_LOAD_CASE);
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
                CaseType.F_OBJECT_REF, GetOperationOptions.createResolve());

        Boolean emptyCase = !isEditingFocus();
        PrismObject<CaseType> caseInstance = null;
        try {
            if (emptyCase) {
                LOGGER.trace("Loading case: New case (creating)");
                CaseType newCase = new CaseType();
                getMidpointApplication().getPrismContext().adopt(newCase);
                caseInstance = newCase.asPrismObject();
            } else {
                String oid = getObjectOidParameter();

                caseInstance = WebModelServiceUtils.loadObject(CaseType.class, oid, options,
                        PageCase.this, task, result);

                if (caseInstance == null) {
                    LOGGER.trace("caseInstance:[oid]={} was null", oid);
                    getSession().error(getString("pageCase.message.cantEditCase"));
                    showResult(result);
                    throw new RestartResponseException(PageCases.class);
                }
                LOGGER.debug("CASE WORK ITEMS: {}", caseInstance.asObjectable().getWorkItem());
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get case.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load case", ex);
        }

        if (caseInstance == null) {
            if (isEditingFocus()) {
                getSession().error(getString("pageAdminFocus.message.cantEditFocus"));
            } else {
                getSession().error(getString("pageAdminFocus.message.cantNewFocus"));
            }
            throw new RestartResponseException(PageCaseWorkItemsAll.class);
        }

        ObjectWrapper<CaseType> wrapper;
        ObjectWrapperFactory owf = new ObjectWrapperFactory(this);
        ContainerStatus status = isEditingFocus() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
        try {
            wrapper = owf.createObjectWrapper("PageCase.details", null, caseInstance, status, task);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get case.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load case", ex);
            wrapper = owf.createObjectWrapper("PageCase.details", null, caseInstance, null, null, status);
        }
        wrapper.setShowEmpty(emptyCase);

        return wrapper;
    }

    private void initLayout() {
        LOGGER.trace("initLayout()");
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        PrismObjectPanel<CaseType> caseForm = new PrismObjectPanel<CaseType>(ID_CASE, caseModel, new PackageResourceReference(
                ImgResources.class, ImgResources.HDD_PRISM), mainForm, this);
        mainForm.add(caseForm);

        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {
        AjaxButton back = new AjaxButton(ID_BACK_BUTTON, createStringResource("pageCase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(back);
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE_BUTTON, createStringResource("pageCase.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(save);
    }

    private String getObjectOidParameter() {
        LOGGER.debug("PAGE CASE getObjectOidParameter");
        PageParameters parameters = getPageParameters();
        LOGGER.trace("Page parameters: {}", parameters);
        StringValue oidValue = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        LOGGER.trace("OID parameter: {}", oidValue);
        if (oidValue == null) {
            return null;
        }
        String oid = oidValue.toString();
        if (StringUtils.isBlank(oid)) {
            return null;
        }
        return oid;
    }

    private boolean isEditingFocus() {
        LOGGER.debug("PAGE CASE isEditingFocus");
        return getObjectOidParameter() != null;
    }

    private void savePerformed(AjaxRequestTarget target) {
        LOGGER.debug("Saving case changes.");

        OperationResult result = new OperationResult(OPERATION_SAVE_CASE);
        try {
            WebComponentUtil.revive(caseModel, getPrismContext());
            ObjectWrapper wrapper = caseModel.getObject();
            ObjectDelta<CaseType> delta = wrapper.getObjectDelta();
            if (delta == null) {
                return;
            }
            if (delta.getPrismContext() == null) {
                getPrismContext().adopt(delta);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Case delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
            }

            if (delta.isEmpty()) {
                return;
            }
            WebComponentUtil.encryptCredentials(delta, true, getMidpointApplication());

            Task task = createSimpleTask(OPERATION_SAVE_CASE);
            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
            deltas.add(delta);

            getModelService().executeChanges(deltas, null, task, result);
            result.recomputeStatus();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save case.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save case", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
            showResult(result);

            redirectBack();
        }
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        redirectBack();
    }

}
