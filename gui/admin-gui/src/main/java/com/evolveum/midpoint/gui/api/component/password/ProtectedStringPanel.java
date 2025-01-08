/*
 * Copyright (C) 2018-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.input.validator.ProtectedStringValidatorForKeyField;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ExternalDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Arrays;

/**
 * Panel for ProtectedStringType that contains choices for 'clean' password panel and configuration of secret provider
 *
 * @author skublik
 */
public class ProtectedStringPanel extends BasePanel<PrismPropertyValueWrapper<ProtectedStringType>> {
    private static final long serialVersionUID = 1L;

    private enum State {
        CLEAR_PASSWORD, PROVIDER
    }

    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_CLEAR_PASSWORD_CHECK = "clearPasswordCheck";
    private static final String ID_CLEAR_PASSWORD_LABEL = "clearPasswordLabel";
    private static final String ID_PROVIDER_CHECK = "providerCheck";
    private static final String ID_PROVIDER_LABEL = "providerLabel";
    private static final String ID_CLEAR_PASSWORD_CONTAINER = "clearPasswordContainer";
    private static final String ID_CLEAR_PASSWORD_PANEL = "clearPasswordPanel";
    private static final String ID_PROVIDER_CONTAINER = "providerContainer";
    private static final String ID_PROVIDER_PANEL = "providerPanel";

    private final IModel<State> currentState;
    private FeedbackAlerts feedback = null;
    private final boolean showProviderPanel;

    private final boolean showOneLinePasswordPanel;
    private final boolean useGlobalValuePolicy;

    public ProtectedStringPanel(
            String id,
            IModel<PrismPropertyValueWrapper<ProtectedStringType>> model,
            boolean showProviderPanel,
            boolean showOneLinePasswordPanel,
            boolean useGlobalValuePolicy) {
        super(id, model);

        this.showProviderPanel = showProviderPanel;
        this.showOneLinePasswordPanel = showOneLinePasswordPanel;
        this.useGlobalValuePolicy = useGlobalValuePolicy;

        if (!showProviderPanel) {
            currentState = Model.of(State.CLEAR_PASSWORD);
        } else {
            ProtectedStringType bean = model.getObject().getRealValue();
            if (bean != null && bean.getExternalData() != null) {
                currentState = Model.of(State.PROVIDER);
            } else {
                currentState = Model.of(State.CLEAR_PASSWORD);
            }
        }
    }

    public void setFeedback(FeedbackAlerts feedback) {
        this.feedback = feedback;
    }

    private FeedbackAlerts getFeedback() {
        return this.feedback;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        IModel<Integer> radioGroupModel = new IModel<>() {
            @Override
            public Integer getObject() {
                return Arrays.asList(State.values()).indexOf(currentState.getObject());
            }

            @Override
            public void setObject(Integer object) {
                State newState = State.values()[object];
                currentState.setObject(newState);

                if (State.PROVIDER.equals(newState)) {
                    addProviderContainer();
                }

                if (State.CLEAR_PASSWORD.equals(newState)) {
                    addCleanPasswordContainer();
                }
            }
        };
        RadioGroup<Integer> radioGroup = new RadioGroup<>(ID_RADIO_GROUP, radioGroupModel);
        radioGroup.add(new VisibleBehaviour(() -> showProviderPanel));

        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(get(ID_CLEAR_PASSWORD_CONTAINER));
                target.add(get(createComponentPath(ID_CLEAR_PASSWORD_CONTAINER, ID_CLEAR_PASSWORD_PANEL)));
                target.add(get(ID_PROVIDER_CONTAINER));
                target.add(get(createComponentPath(ID_PROVIDER_CONTAINER, ID_PROVIDER_PANEL)));
                target.add(ProtectedStringPanel.this);
                target.add(getFeedback());
            }
        });
        add(radioGroup);

        addRadio(radioGroup, ID_CLEAR_PASSWORD_CHECK, ID_CLEAR_PASSWORD_LABEL, 0, "ProtectedStringPanel.cleanPassword");
        addRadio(radioGroup, ID_PROVIDER_CHECK, ID_PROVIDER_LABEL, 1, "ProtectedStringPanel.provider");

        addCleanPasswordContainer();
        addProviderContainer();
    }

    private void addCleanPasswordContainer() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CLEAR_PASSWORD_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(AttributeAppender.append("class", showProviderPanel ? "bg-light rounded p-3" : null));
        container.add(new VisibleBehaviour(() -> State.CLEAR_PASSWORD.equals(currentState.getObject())));
        addOrReplace(container);

        PasswordPropertyPanel panel = new PasswordPropertyPanel(
                ID_CLEAR_PASSWORD_PANEL,
                new ItemRealValueModel<>(getModel()),
                getParentWrapper().isReadOnly(),
                isClearPasswordValueNull(),
                showOneLinePasswordPanel,
                getPrismObjectParentIfExist()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void changePasswordPerformed() {
                PrismValueWrapper<ProtectedStringType> itemModel = getModelObject();
                if (itemModel != null) {
                    itemModel.setStatus(ValueStatus.MODIFIED);
                }
            }

            @Override
            protected boolean isPasswordLimitationPopupVisible() {
                return super.isPasswordLimitationPopupVisible() && useGlobalValuePolicy;
            }

            @Override
            protected boolean isPasswordStrengthBarVisible() {
                return super.isPasswordStrengthBarVisible() && useGlobalValuePolicy;
            }

        };
        panel.setOutputMarkupId(true);
        container.addOrReplace(panel);
    }

    private <O extends ObjectType> PrismObject<O> getPrismObjectParentIfExist() {
        PrismPropertyWrapper<ProtectedStringType> wrapper = getParentWrapper();
        PrismObjectWrapper<ObjectType> parentObjectWrapper = wrapper.findObjectWrapper();
        if (parentObjectWrapper == null) {
            return null;
        }
        return (PrismObject<O>) parentObjectWrapper.getObject();
    }

    private boolean isClearPasswordValueNull() {
        var bean = getModelObject().getRealValue();
        return bean == null || (bean.getEncryptedDataType() == null && bean.getClearValue() == null && bean.getHashedDataType() == null);
    }

    private void addProviderContainer() {
        WebMarkupContainer container = new WebMarkupContainer(ID_PROVIDER_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(new VisibleBehaviour(() -> showProviderPanel && State.PROVIDER.equals(currentState.getObject())));
        container.add(AttributeAppender.append("class", showProviderPanel ? "bg-light rounded p-3" : null));
        addOrReplace(container);

        IModel<ExternalDataType> providerModel = new IModel<>() {
            @Override
            public ExternalDataType getObject() {
                if (getModelObject().getRealValue() == null) {
                    return null;
                }
                return getModelObject().getRealValue().getExternalData();
            }

            @Override
            public void setObject(ExternalDataType object) {
                if (object.getKey() == null && object.getProvider() == null) {
                    return;
                }

                getModelObject().setRealValue(new ProtectedStringType());

                getModelObject().getRealValue().setExternalData(object);
            }
        };

        SecretProviderPanel panel = new SecretProviderPanel(ID_PROVIDER_PANEL, providerModel) {
            @Override
            protected void refreshFeedback(AjaxRequestTarget target) {
                if (getFeedback() != null) {
                    target.add(getFeedback());
                }
            }
        };
        panel.setOutputMarkupId(true);
        container.addOrReplace(panel);
        panel.getKeyTextPanel().add(new ProtectedStringValidatorForKeyField(getModel(), getFeedback()));
        if (getFeedback() != null) {
            getFeedback().setFilter(new ComponentFeedbackMessageFilter(panel.getKeyTextPanel()));
        }
    }

    private void addRadio(RadioGroup<Integer> radioGroup, String checkId, String labelId, int state, String labelKey) {
        Radio<Integer> radio = new Radio<>(checkId, Model.of(state), radioGroup);
        radioGroup.add(radio);
        radioGroup.add(new Label(labelId, getParentPage().createStringResource(labelKey)));

    }

    private PrismPropertyWrapper<ProtectedStringType> getParentWrapper() {
        return getModelObject().getParent();
    }
}
