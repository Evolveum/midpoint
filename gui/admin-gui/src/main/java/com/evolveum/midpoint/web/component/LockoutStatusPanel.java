/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerValuePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.util.List;

public class LockoutStatusPanel extends BasePanel<PrismPropertyWrapper<LockoutStatusType>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(LockoutStatusPanel.class);

    private static final String ID_CONTAINER = "container";
    private static final String ID_LABEL = "label";
    private static final String ID_BUTTON = "button";

    IModel<PrismContainerWrapper<BehaviorType>> behaviorModel;
    IModel<PrismContainerWrapper<ActivationType>> activationModel;

    public LockoutStatusPanel(String id, IModel<PrismPropertyWrapper<LockoutStatusType>> model,
            IModel<PrismContainerWrapper<BehaviorType>> behaviorModel, IModel<PrismContainerWrapper<ActivationType>> activationModel) {
        super(id, model);
        this.behaviorModel = behaviorModel;
        this.activationModel = activationModel;
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
        reloadActivationContainerPanel(target);
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
        resetActivationFailedAttemptsValue(toInitialValue);
        resetAuthenticationFailedAttemptsValue(toInitialValue);
    }

    private void resetActivationFailedAttemptsValue(boolean toInitialValue) {
        PrismContainerWrapper<ActivationType> activation = activationModel.getObject();
        if (activation != null) {
            try {
                PrismPropertyWrapper<XMLGregorianCalendar> lockoutExpiration =
                        activation.findProperty(ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP);
                if (lockoutExpiration == null || lockoutExpiration.getValue() == null) {
                    return;
                }
                if (toInitialValue) {
                    lockoutExpiration.getValue().setRealValue(lockoutExpiration.getValue().getOldValue().getRealValue());
                } else {
                    lockoutExpiration.getValue().setRealValue(null);
                }

            } catch (SchemaException e) {
                LOGGER.error("Cannot reset failed attempts value for activation: {}", e.getMessage(), e);
            }
        }

    }

    private void resetAuthenticationFailedAttemptsValue(boolean toInitialValue) {
        PrismContainerWrapper<BehaviorType> behavior = behaviorModel.getObject();
        if (behavior != null) {
            try {
                PrismContainerWrapper<AuthenticationBehavioralDataType> authWrapper =
                        behavior.findContainer(BehaviorType.F_AUTHENTICATION);
                if (authWrapper == null) {
                    return;
                }
                if (toInitialValue) {
                    //TODO probably it will be needed to set also failed attempts as in old value
                    authWrapper.getValues().forEach(bv -> bv.setRealValue(bv.getOldValue().getRealValue()));
                    return;
                } else {
                    List<PrismContainerValueWrapper<AuthenticationBehavioralDataType>> values = authWrapper.getValues();
                    for (PrismContainerValueWrapper<AuthenticationBehavioralDataType> value: values) {
                        PrismPropertyWrapper<AuthenticationAttemptDataType> authAttempt = value.findProperty(AuthenticationBehavioralDataType.F_AUTHENTICATION_ATTEMPT);
                        if (authAttempt == null) {
                            continue;
                        }
                        authAttempt.getValues().forEach(authAttemptValue -> {
                            try {
                                if (authAttemptValue.getRealValue() != null) {
                                    authAttemptValue.getRealValue().setFailedAttempts(0);
                                }
                            } catch (Exception e) {
                                LOGGER.error("Cannot reset failed attempts value for authentication attempt: {}", e.getMessage(), e);
                            }
                        });

                    }

                }

            } catch (SchemaException e) {
                LOGGER.error("Cannot reset failed attempts value for authentication attempt: {}", e.getMessage(), e);
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
            LOGGER.error("Cannot get lockout state value: {}", e.getMessage(), e);
        }
        return value;
    }

    //todo hack; after lockout status is reset, lockout expiration is also updated;
    //therefore the whole panel should be reloaded. implement custom panel for this later
    private void reloadActivationContainerPanel(AjaxRequestTarget target) {
        Component containerPanel = findParent(PrismContainerValuePanel.class);
        if (containerPanel != null) {
            target.add(containerPanel);
        }
    }
}
