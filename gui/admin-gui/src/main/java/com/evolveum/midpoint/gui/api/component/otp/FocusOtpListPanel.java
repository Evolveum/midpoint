/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.otp;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialsType;

public class FocusOtpListPanel extends BasePanel<FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusOtpListPanel.class);

    private static final String DOT_CLASS = FocusOtpListPanel.class.getName() + ".";
    private static final String OPERATION_SAVE_OTP_CREDENTIALS = DOT_CLASS + "saveOtpCredentials";

    private static final String ID_FORM = "form";
    private static final String ID_OTP = "otp";
    private static final String ID_SAVE = "save";

    private LoadableModel<PrismObjectWrapper<FocusType>> objectWrapperModel;

    public FocusOtpListPanel(String id, IModel<FocusType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initModels();
        initLayout();
    }

    private void initModels() {
        setOutputMarkupId(true);

        objectWrapperModel = new LoadableModel<>(false) {

            @Override
            protected PrismObjectWrapper<FocusType> load() {
                PrismObject object = getModelObject().asPrismObject();
                PageBase page = WebComponentUtil.getPageBase(getPage());

                PrismObjectWrapperFactory<? extends FocusType> factory = page.findObjectWrapperFactory(object.getDefinition());
                Task task = page.createSimpleTask("createWrapper");
                OperationResult result = task.getResult();

                try {
                    return factory.createObjectWrapper(object, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot create wrapper for {} \nReason: {]", e, object, e.getMessage());
                    result.recordFatalError("Cannot create wrapper for " + object + ", because: " + e.getMessage(), e);
                    page.showResult(result);
                    throw page.redirectBackViaRestartResponseException();
                }
            }
        };

    }

    private void initLayout() {
        MidpointForm<Void> form = new MidpointForm<>(ID_FORM);
        add(form);

        PrismContainerWrapperModel<FocusType, OtpCredentialType> credentialModel =
                PrismContainerWrapperModel.fromContainerWrapper(
                        objectWrapperModel,
                        ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_OTPS, OtpCredentialsType.F_TOTP),
                        () -> getPageBase());

        OtpListPanel otp = new OtpListPanel(ID_OTP, getModel(), credentialModel, null);
        form.add(otp);

        form.add(new AjaxSubmitLink(ID_SAVE) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onSavePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                onSaveError(target);
            }
        });
    }

    protected void onSavePerformed(AjaxRequestTarget target) {
        PageBase page = getPageBase();

        Task task = page.createSimpleTask(OPERATION_SAVE_OTP_CREDENTIALS);
        OperationResult result = task.getResult();

        try {
            ObjectDelta<?> delta = objectWrapperModel.getObject().getObjectDelta();
            if (delta.isEmpty()) {
                page.warn(getString("FocusOtpListPanel.noChanges"));
                target.add(getFeedbackPanel());
                return;
            }

            WebModelServiceUtils.save(delta, result, task, getParentPage());
        } catch (CommonException ex) {
            LoggingUtils.logException(LOGGER, "Cannot save OTP credentials, reason: {}", ex, ex.getMessage());
            page.error(getString("FocusOtpListPanel.errorSavingOtpCredentials", ex.getMessage()));
        } finally {
            result.computeStatusIfUnknown();
            if (result.isSuccess()) {
                objectWrapperModel.reset();
            }
        }

        page.showResult(result);
        target.add(getFeedbackPanel());
        target.add(this);
    }

    private void onSaveError(AjaxRequestTarget target) {
        target.add(getFeedbackPanel());
    }
}
