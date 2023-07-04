/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/authorizationPlayground",
                        matchUrlForSecurity = "/admin/config/authorizationPlayground")
        },
        action = {
        @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_AUTHORIZATION_PLAYGROUND_URL,
                label = "PageAuthorizationPlayground.auth.mapping.label",
                description = "PageAuthorizationPlayground.auth.mapping.description")
        }, experimental = true)
@Experimental
public class PageAuthorizationPlayground extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageAuthorizationPlayground.class);

    private static final String DOT_CLASS = PageAuthorizationPlayground.class.getName() + ".";

    private static final String OP_EVALUATE_AUTHORIZATIONS = DOT_CLASS + "evaluateAuthorizations";

    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_SUBJECT_OID = "subjectOid";

    private static final String ID_ADDITIONAL_AUTHORIZATIONS = "additionalAuthorizations";

    private static final String ID_TYPE = "type";
    private static final String ID_OBJECT_FILTER = "objectFilter";

    private static final String ID_OBJECT_OID = "objectOid";

    private static final String ID_SAMPLE = "sample";

    private static final String ID_SELECTOR_TRACING = "selectorTracing";

    private static final String ID_EXECUTE = "execute";

    private static final String ID_RESULT_TEXT = "resultText";
    private static final String ID_COMPUTATION_TEXT = "computationText";

    private static final String SAMPLES_DIR = "authorization-samples";
    private static final List<String> SAMPLES = List.of(
            "no-autz",
            "full-autz",
            "read-modify-caribbean"
            // TODO add other ones
    );

    private final IModel<String> additionalAuthorizationsModel =
            new Model<>("<additionalAuthorizations>\n</additionalAuthorizations>");

    private final IModel<QName> typeModel = new Model<>(UserType.COMPLEX_TYPE);
    private final IModel<String> filterModel = new Model<>();
    private final IModel<ObjectReferenceType> objectModel = Model.of(new ObjectReferenceType());
    private final IModel<ObjectReferenceType> subjectModel = Model.of(new ObjectReferenceType());

    private final IModel<Boolean> selectorTracingModel = Model.of(false);

    private final IModel<String> resultModel = new Model<>();
    private final IModel<String> computationModel = new Model<>();

    public PageAuthorizationPlayground() {
        initLayout();
    }

    private void initLayout() {
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        mainForm.add(new ValueChoosePanel<>(ID_SUBJECT_OID, subjectModel) {

            @Override
            protected <O extends ObjectType> Class<O> getDefaultType(List<QName> supportedTypes) {
                return (Class<O>) WebComponentUtil.qnameToClass(PrismContext.get(), typeModel.getObject());
            }

            @Override
            public List<QName> getSupportedTypes() {
                return WebComponentUtil.createObjectTypeList();
            }
        });

        mainForm.add(new ValueChoosePanel<>(ID_OBJECT_OID, objectModel));

        var additionalAuthorizationsEditor = new AceEditor(ID_ADDITIONAL_AUTHORIZATIONS, additionalAuthorizationsModel);
        additionalAuthorizationsEditor.setHeight(400);
        additionalAuthorizationsEditor.setResizeToMaxHeight(false);
        mainForm.add(additionalAuthorizationsEditor);

        mainForm.add(new DropDownChoicePanel<>(ID_TYPE, typeModel, () -> WebComponentUtil.createObjectTypeList(), new QNameChoiceRenderer()));

        var filterEditor = new AceEditor(ID_OBJECT_FILTER, filterModel);
        filterEditor.setHeight(400);
        filterEditor.setResizeToMaxHeight(false);
        mainForm.add(filterEditor);

//        mainForm.add(new TextField<>(ID_OBJECT_OID, objectOidModel));

        mainForm.add(new CheckBox(ID_SELECTOR_TRACING, selectorTracingModel));

        mainForm.add(
                new AjaxSubmitButton(ID_EXECUTE, createStringResource("PageAuthorizationPlayground.button.evaluate")) {
                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        target.add(getFeedbackPanel());
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        evaluatePerformed(target);
                    }
                });

        mainForm.add(new SamplesChoice(
                ID_SAMPLE, SAMPLES, "PageAuthorizationPlayground.sample") {
            @Override
            protected void update(String sampleName, AjaxRequestTarget target) {
                additionalAuthorizationsModel.setObject(readResource(sampleFile(sampleName, "additional-authorizations")));
                resultModel.setObject("");
                computationModel.setObject("");
                target.add(PageAuthorizationPlayground.this);
            }
        });

        AceEditor resultText = new AceEditor(ID_RESULT_TEXT, resultModel);
        resultText.setReadonly(true);
        resultText.setHeight(300);
        resultText.setResizeToMaxHeight(false);
        resultText.setMode(null);
        mainForm.add(resultText);

        AceEditor computationText = new AceEditor(ID_COMPUTATION_TEXT, computationModel);
        computationText.setReadonly(true);
        computationText.setHeight(1000);
        computationText.setResizeToMaxHeight(false);
        computationText.setMode(null);
        mainForm.add(computationText);
    }

    private static String sampleFile(String sampleName, String type) {
        return SAMPLES_DIR + "/" + sampleName + "." + type + ".xml.data";
    }

    private void evaluatePerformed(AjaxRequestTarget target) {
        Task task = createSimpleTask(OP_EVALUATE_AUTHORIZATIONS);
        OperationResult result = new OperationResult(OP_EVALUATE_AUTHORIZATIONS);

        try {
            var request = createRequestRaw();

            if (request == null) {
                warn(getString("PageAuthorizationPlayground.message.noInputProvided"));
                target.add(getFeedbackPanel());
                return;
            }

            setSubjectRef(request);
            addExplicitAuthorizations(request);
            setTracing(request);

            var response = getModelDiagnosticService().evaluateAuthorizations(request, task, result);

            resultModel.setObject(response.getResult());
            computationModel.setObject(response.getComputation());

        } catch (CommonException | RuntimeException e) {
            result.recordException(e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't evaluate authorizations", e);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            resultModel.setObject(sw.toString());
        } finally {
            result.computeStatus();
        }

        showResult(result);
        target.add(this);
    }

    /** Returns request without adornments like extra authorizations etc. */
    private AuthorizationEvaluationRequestType createRequestRaw() throws SchemaException {
        ObjectReferenceType objectOid = objectModel.getObject();
        if (objectOid != null) {
            return new AuthorizationEvaluationAccessDecisionRequestType()
                    .objectRef(objectOid);
        }

        QName typeName = typeModel.getObject();
        if (typeName != null) {
            return new AuthorizationEvaluationFilterProcessingRequestType()
                    .type(typeName)
                    .filter(createFilterBean());
        }

        return null;
    }

    private SearchFilterType createFilterBean() throws SchemaException {
        String filterString = filterModel.getObject();
        if (!StringUtils.isEmpty(filterString)) {
            return getPrismContext().parserFor(filterString).xml().parseRealValue(SearchFilterType.class);
        } else {
            return null;
        }
    }

    private void setSubjectRef(AuthorizationEvaluationRequestType request) {
        ObjectReferenceType subjectOid = subjectModel.getObject();
        if (StringUtils.isNotEmpty(subjectOid.getOid())) {
            request.setSubjectRef(subjectOid);
        }
    }

    private void addExplicitAuthorizations(AuthorizationEvaluationRequestType request) throws SchemaException {
        String autz = additionalAuthorizationsModel.getObject();
        if (StringUtils.isNotEmpty(autz)) {
            var additional = getPrismContext().parserFor(autz).xml().parseRealValue(AdditionalAuthorizationsType.class);
            request.getAdditionalAuthorization().addAll(
                    CloneUtil.cloneCollectionMembers(
                            additional.getAuthorization()));
        }
    }

    private void setTracing(AuthorizationEvaluationRequestType request) {
        request.tracing(new AuthorizationEvaluationTracingOptionsType()
                .selectorTracingEnabled(selectorTracingModel.getObject()));
    }
}
