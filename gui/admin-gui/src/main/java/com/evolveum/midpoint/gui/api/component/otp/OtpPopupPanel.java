/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.otp;

import java.io.Serial;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialType;

public class OtpPopupPanel<F extends FocusType> extends BasePanel<PrismContainerValueWrapper<OtpCredentialType>> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_FORM = "form";
    private static final String ID_OTP = "otp";
    private static final String ID_FOOTER_BUTTONS = "footerButtons";
    private static final String ID_CANCEL = "cancel";
    private static final String ID_CONFIRM = "confirm";

    private final IModel<F> focusModel;

    private boolean editMode;

    public OtpPopupPanel(String id, IModel<F> focusModel, IModel<PrismContainerValueWrapper<OtpCredentialType>> model) {
        super(id, model);

        this.focusModel = focusModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        add(form);

        OtpPanel<F> otp = OtpPanel.createPanelForWrapper(ID_OTP, focusModel, getModel());
        otp.setEditMode(editMode);
        form.add(otp);
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public @NotNull Component getFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_FOOTER_BUTTONS, this);
        AjaxLink<?> cancel = new AjaxLink<>(ID_CANCEL) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onCancelPerformed(target);
            }
        };
        footer.add(cancel);

        AjaxSubmitButton confirm = new AjaxSubmitButton(ID_CONFIRM, getConfirmButtonLabel()) {

            @Override
            protected void onError(AjaxRequestTarget target) {
                onSubmitError(target);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onConfirmPerformed(target);
            }
        };
        footer.add(confirm);

        return footer;
    }

    @Override
    public int getWidth() {
        return 600;
    }

    @Override
    public int getHeight() {
        return 600;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("OtpPopupPanel.title");
    }

    public IModel<String> getConfirmButtonLabel() {
        return createStringResource("OtpPopupPanel.verifyAndEnable");
    }

    protected void onCancelPerformed(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    private OtpPanel<F> getOtpPanel() {
        // noinspection unchecked
        return (OtpPanel<F>) get(createComponentPath(ID_FORM, ID_OTP));
    }

    protected void onSubmitError(AjaxRequestTarget target) {
        var panel = getOtpPanel();

        panel.onValidationError(target);
    }

    protected void onConfirmPerformed(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
}
