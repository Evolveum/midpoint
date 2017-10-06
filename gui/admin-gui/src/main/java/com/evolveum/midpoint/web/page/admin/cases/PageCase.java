package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
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
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
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
    private static final String ID_CASE_NAME = "caseName";
    private static final String ID_CASE_DESCRIPTION = "caseDescription";
    private static final String ID_CASE_RESOURCE = "caseResource";
    private static final String ID_CASE_EVENT = "caseEvent";
    private static final String ID_CASE_OUTCOME = "caseOutcome";
    private static final String ID_CASE_CLOSE_TIMESTAMP = "caseCloseTimestamp";
    private static final String ID_CASE_STATE = "caseState";
    private static final String ID_BACK_BUTTON = "backButton";

    //private IModel<ObjectWrapper<CaseType>> caseModel;
    private PrismObject<CaseType> caseInstance;
    private NonEmptyLoadableModel<CaseType> caseModel;
    private static final String ID_PROTECTED_MESSAGE = "protectedMessage";

    public PageCase(PageParameters parameters) {
        caseModel = new NonEmptyLoadableModel<CaseType>(false) {

            @Override
            protected CaseType load() {
                return loadCase(parameters);
            }
        };
        initLayout();
    }

    private CaseType loadCase(PageParameters parameters) {

        Task task = createSimpleTask(OPERATION_LOAD_CASE);
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
                ShadowType.F_RESOURCE, GetOperationOptions.createResolve());

        StringValue oid = parameters != null ? parameters.get(OnePageParameterEncoder.PARAMETER) : null;

        LOGGER.trace("loadCase(parameters[oid]={})", oid);


        caseInstance = WebModelServiceUtils.loadObject(CaseType.class, oid.toString(), options,
                PageCase.this, task, result);

        if (caseInstance == null) {
            LOGGER.trace("caseInstance:[oid]={} was null", oid);
            getSession().error(getString("pageCase.message.cantEditCase"));
            showResult(result);
            throw new RestartResponseException(PageCases.class);
        }else{
            return caseInstance.asObjectable();
        }

//        LOGGER.trace("caseInstance={})", caseInstance);
//        ObjectWrapper wrapper = ObjectWrapperUtil.createObjectWrapper(null, null, caseInstance, ContainerStatus.MODIFYING, task, this);
//        OperationResultType fetchResult = caseInstance.getPropertyRealValue(ShadowType.F_FETCH_RESULT, OperationResultType.class);
//        wrapper.setFetchResult(OperationResult.createOperationResult(fetchResult));
//        wrapper.setShowEmpty(false);
//        return caseInstance;
    }

    private void initLayout() {
        LOGGER.trace("initLayout()");
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        //PrismObjectPanel<CaseType> caseForm = new PrismObjectPanel<CaseType>("case", caseModel, new PackageResourceReference(
        //        ImgResources.class, ImgResources.HDD_PRISM), mainForm, this);
        //mainForm.add(caseForm);

        PropertyModel caseNameProperty = new PropertyModel<>(caseModel, CaseType.F_NAME.getLocalPart());
        mainForm.add(new Label(ID_CASE_NAME, caseNameProperty));
        PropertyModel caseDescriptionProperty = new PropertyModel<>(caseModel, CaseType.F_DESCRIPTION.getLocalPart());
        mainForm.add(new Label(ID_CASE_DESCRIPTION, caseDescriptionProperty));
        PropertyModel caseResourceProperty = new PropertyModel<>(caseModel, CaseType.F_OBJECT_REF.getLocalPart());
        PropertyModel caseResourceNameProperty = new PropertyModel<>(caseResourceProperty, ObjectReferenceType.F_TARGET_NAME.getLocalPart());
        mainForm.add(new Label(ID_CASE_RESOURCE, caseResourceNameProperty));
        PropertyModel caseEventProperty = new PropertyModel<>(caseModel, CaseType.F_EVENT.getLocalPart());
        mainForm.add(new Label(ID_CASE_EVENT, caseEventProperty));
        PropertyModel caseOutcomeProperty = new PropertyModel<>(caseModel, CaseType.F_OUTCOME.getLocalPart());
        mainForm.add(new Label(ID_CASE_OUTCOME, caseOutcomeProperty));
        PropertyModel caseCloseTimestampProperty = new PropertyModel<>(caseModel, CaseType.F_CLOSE_TIMESTAMP.getLocalPart());
        mainForm.add(new Label(ID_CASE_CLOSE_TIMESTAMP, caseCloseTimestampProperty));
        PropertyModel caseStateProperty = new PropertyModel<>(caseModel, CaseType.F_STATE.getLocalPart());
        mainForm.add(new Label(ID_CASE_STATE, caseStateProperty));

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
//        AjaxSubmitButton save = new AjaxSubmitButton("save", createStringResource("pageCase.button.save")) {
//
//            @Override
//            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
//                savePerformed(target);
//            }
//
//            @Override
//            protected void onError(AjaxRequestTarget target, Form<?> form) {
//                target.add(getFeedbackPanel());
//            }
//        };
//        save.add(new VisibleEnableBehaviour() {
//
//            @Override
//            public boolean isVisible() {
//                ObjectWrapper wrapper = caseModel.getObject();
//                return !wrapper.isProtectedAccount();
//            }
//        });
//        mainForm.add(save);
    }

    private void savePerformed(AjaxRequestTarget target) {
        LOGGER.debug("Saving account changes.");

        OperationResult result = new OperationResult(OPERATION_SAVE_CASE);
        try {
            WebComponentUtil.revive(caseModel, getPrismContext());
            ObjectWrapper wrapper = ObjectWrapperUtil.createObjectWrapper(null, null, caseInstance, ContainerStatus.MODIFYING, null, this);
            ObjectDelta<CaseType> delta = wrapper.getObjectDelta();
            if (delta == null) {
                return;
            }
            if (delta.getPrismContext() == null) {
                getPrismContext().adopt(delta);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Account delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
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
