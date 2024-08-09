/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationAttemptDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationBehavioralDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BehaviorType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;

import java.io.Serial;

public class LockoutStatusPanel extends BasePanel<PrismPropertyWrapper<LockoutStatusType>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";
    private static final String ID_LABEL = "label";
    private static final String ID_BUTTON = "button";

    IModel<PrismContainerWrapper<BehaviorType>> behaviorModel;
    PrismContainerWrapper<BehaviorType> bb = null;

    public LockoutStatusPanel(String id, IModel<PrismPropertyWrapper<LockoutStatusType>> model,
            IModel<PrismContainerWrapper<BehaviorType>> behaviorModel) {
        super(id, model);
        this.behaviorModel = behaviorModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        Label label = new Label(ID_LABEL, getLabelModel());
        container.add(label);

        AjaxButton button = new AjaxButton(ID_BUTTON, getButtonModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                lockoutStatusChangePerformed(target);
            }
        };
        container.add(button);
    }

    private void lockoutStatusChangePerformed(AjaxRequestTarget target) {
        if (isModified()) {
            resetToInitialValue();
        } else {
            resetToNormalState();
        }
        target.add(LockoutStatusPanel.this.get(ID_CONTAINER));
    }

    private void resetToInitialValue() {
        PrismPropertyValueWrapper<LockoutStatusType> value = getLockoutStateValueWrapper();
        if (value != null) {
            value.setRealValue(value.getOldValue().getRealValue());
            value.setStatus(ValueStatus.NOT_CHANGED);
        }
        resetFailedAttemptsValue(true);
    }

    private void resetToNormalState() {
        PrismPropertyValueWrapper<LockoutStatusType> value = getLockoutStateValueWrapper();
        if (value != null) {
            value.setRealValue(LockoutStatusType.NORMAL);
            value.setStatus(ValueStatus.MODIFIED);
        }
        resetFailedAttemptsValue(false);
    }

    private void resetFailedAttemptsValue(boolean toInitialValue) {
        PrismContainerWrapper<BehaviorType> behavior = behaviorModel.getObject();
        if (behavior != null) {
            try {
                PrismContainerWrapper<AuthenticationBehavioralDataType> authWrapper =
                        behavior.findContainer(BehaviorType.F_AUTHENTICATION);
                if (authWrapper != null) {
                    authWrapper.getValues().forEach(bv -> {
                        if (toInitialValue) {
                            bv.setRealValue(bv.getOldValue().getRealValue());
                        } else {
                            PrismProperty<AuthenticationAttemptDataType> authAttempt =
                                    bv.getNewValue().findProperty(AuthenticationBehavioralDataType.F_AUTHENTICATION_ATTEMPT);
                            if (authAttempt != null) {
                                authAttempt.getValues().forEach(authAttemptValue -> {
                                    try {
                                        if (authAttemptValue.getRealValue() != null) {
                                            authAttemptValue.getRealValue().setFailedAttempts(0);
                                        }
                                    } catch (Exception e) {
//                                    nothing to do
                                    }
                                });
                            }
                        }
                    });
                }
            } catch (SchemaException e) {
                //nothing to do
            }
        }

    }

    private IModel<String> getButtonModel() {
        return () -> {
            String key = isModified() ? "LockoutStatusPanel.undoButtonLabel" : "LockoutStatusPanel.unlockButtonLabel";
            return getString(key);
        };
    }

    private IModel<String> getLabelModel() {
        return () -> {
            LockoutStatusType lockoutStatus = getLockoutStateRealValue();

            String labelValue = lockoutStatus == null ?
                    getString("LockoutStatusType.UNDEFINED") : getString(LocalizationUtil.createKeyForEnum(lockoutStatus));

            if (isModified()) {
                labelValue += " " + getString("LockoutStatusPanel.changesSaving");
            }

            return labelValue;
        };
    }

    private boolean isModified() {
        PrismPropertyValueWrapper<LockoutStatusType> value = getLockoutStateValueWrapper();
        return value != null && ValueStatus.MODIFIED.equals(value.getStatus());
    }

    private LockoutStatusType getLockoutStateRealValue() {
        PrismPropertyValueWrapper<LockoutStatusType> value = getLockoutStateValueWrapper();
        return value != null ? value.getRealValue() : null;
    }

    private PrismPropertyValueWrapper<LockoutStatusType> getLockoutStateValueWrapper() {
        PrismPropertyWrapper<LockoutStatusType> lockoutStatusWrapper = getModelObject();
        PrismPropertyValueWrapper<LockoutStatusType> value = null;
        try {
            value = lockoutStatusWrapper != null ? lockoutStatusWrapper.getValue() : null;
        } catch (Exception e) {
            //nothing to do
        }
        return value;
    }
}
