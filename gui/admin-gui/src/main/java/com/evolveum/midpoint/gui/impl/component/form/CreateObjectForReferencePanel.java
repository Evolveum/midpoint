/*
 * Copyright (C) 2018-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.form;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.message.Callout;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.factory.wrapper.PrismContainerWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormDefaultContainerablePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.visit.IVisitor;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Panel for reference that allow creating of new object for reference
 *
 * @author skublik
 */
public class CreateObjectForReferencePanel<R extends Referencable> extends BasePanel<PrismReferenceValueWrapperImpl<R>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(CreateObjectForReferencePanel.class);

    private enum State {
        NO_VALUE, USE_EXISTING, CREATE_NEW
    }

    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_NO_VALUE_CHECK = "noValueCheck";
    private static final String ID_NO_VALUE_LABEL = "noValueLabel";
    private static final String ID_USE_EXISTING_CHECK = "useExistingCheck";
    private static final String ID_USE_EXISTING_LABEL = "useExistingLabel";
    private static final String ID_CREATE_NEW_CHECK = "createNewCheck";
    private static final String ID_CREATE_NEW_LABEL = "createNewLabel";
    private static final String ID_USE_EXISTING_CONTAINER = "useExistingContainer";
    private static final String ID_USE_EXISTING_INPUT = "useExistingInput";
    private static final String ID_CREATE_NEW_CONTAINER = "createNewContainer";
    private static final String ID_CREATE_NEW_CALLOUT = "createNewCallout";
    private static final String ID_CREATE_NEW_FORM = "createNewForm";

    private IModel<State> currentState = Model.of();
    private FeedbackAlerts feedback = null;
    private final ContainerPanelConfigurationType config;
    private final boolean isHeaderVisible;

    public CreateObjectForReferencePanel(
            String id,
            IModel<PrismReferenceValueWrapperImpl<R>> model,
            ContainerPanelConfigurationType config,
            boolean isHeaderVisible) {
        super(id, model);
        Referencable bean = model.getObject().getRealValue();
        if (model.getObject().existNewObjectModel()) {
            currentState.setObject(State.CREATE_NEW);
        } else if (bean == null || bean.getOid() == null) {
            currentState.setObject(State.NO_VALUE);
        } else {
            currentState.setObject(State.USE_EXISTING);
        }
        this.config = config;
        this.isHeaderVisible = isHeaderVisible;
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

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        ((WebMarkupContainer)get(ID_CREATE_NEW_CONTAINER)).visitChildren(VerticalFormDefaultContainerablePanel.class,
                (IVisitor<VerticalFormDefaultContainerablePanel, Void>) (container, visit) -> container.getFormContainer().add(
                        AttributeModifier.replace("class", "card-body mb-0 p-3")));

        ((WebMarkupContainer)get(ID_CREATE_NEW_CONTAINER)).visitChildren(VerticalFormPrismContainerPanel.class,
                (IVisitor<VerticalFormPrismContainerPanel, Void>) (container, visit) -> container.getContainer().add(
                        AttributeModifier.replace("class", "m-0")));
    }

    private ContainerPanelConfigurationType getContainerConfiguration() {
        return config;
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
                getModelObject().setRealValue(null);
                if (!State.CREATE_NEW.equals(newState)) {
                    getModelObject().resetNewObjectModel();
                }
            }
        };
        RadioGroup<Integer> radioGroup = new RadioGroup<>(ID_RADIO_GROUP, radioGroupModel);
        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (State.CREATE_NEW.equals(currentState.getObject())) {
                    addCreateNewContainer();
                }

                target.add(get(ID_USE_EXISTING_CONTAINER));
                target.add(get(createComponentPath(ID_USE_EXISTING_CONTAINER, ID_USE_EXISTING_INPUT)));
                target.add(get(ID_CREATE_NEW_CONTAINER));
                target.add(get(createComponentPath(ID_CREATE_NEW_CONTAINER, ID_CREATE_NEW_FORM)));
                target.add(CreateObjectForReferencePanel.this);
            }
        });
        add(radioGroup);

        addRadio(radioGroup, ID_NO_VALUE_CHECK, ID_NO_VALUE_LABEL, 0, "CreateObjectForReferencePanel.noValue");
        addRadio(radioGroup, ID_USE_EXISTING_CHECK, ID_USE_EXISTING_LABEL, 1, "CreateObjectForReferencePanel.useExisting");
        Radio<Integer> radio = addRadio(radioGroup, ID_CREATE_NEW_CHECK, ID_CREATE_NEW_LABEL, 2, "CreateObjectForReferencePanel.createNew");
        radio.add(new VisibleBehaviour(() -> getModelObject().isEditEnabled()));

        addUseExistingContainer();
        addCreateNewContainer();
    }

    private void addUseExistingContainer() {
        WebMarkupContainer container = new WebMarkupContainer(ID_USE_EXISTING_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(new VisibleBehaviour(() -> State.USE_EXISTING.equals(currentState.getObject())));
        add(container);

        ReferenceAutocompletePanel<R> panel = new ReferenceAutocompletePanel<R>(ID_USE_EXISTING_INPUT, new ItemRealValueModel<>(getModel())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectFilter createCustomFilter() {
                return getParentWrapper().getFilter(getPageBase());
            }

            @Override
            protected Set<SearchItemType> getSpecialSearchItem() {
                return getParentWrapper().getPredefinedSearchItem();
            }

            @Override
            protected <O extends ObjectType> void choosePerformed(AjaxRequestTarget target, O object) {
                super.choosePerformed(target, object);
                getBaseFormComponent().validate();
                FeedbackAlerts feedback = getFeedback();
                if (feedback != null) {
                    target.add(feedback);
                }
            }

            @Override
            protected boolean isButtonLabelVisible() {
                return true;
            }

            @Override
            public List<QName> getSupportedTypes() {
                List<QName> targetTypeList = getParentWrapper().getTargetTypes();
                if (targetTypeList == null || WebComponentUtil.isAllNulls(targetTypeList)) {
                    return Arrays.asList(ObjectType.COMPLEX_TYPE);
                }
                return targetTypeList;
            }

            @Override
            protected <O extends ObjectType> Class<O> getDefaultType(List<QName> supportedTypes) {
                if (AbstractRoleType.COMPLEX_TYPE.equals(getParentWrapper().getTargetTypeName())) {
                    return (Class<O>) RoleType.class;
                } else {
                    return super.getDefaultType(supportedTypes);
                }
            }
        };
        panel.setOutputMarkupId(true);
        container.add(panel);
    }

    private void addCreateNewContainer() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CREATE_NEW_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(new VisibleBehaviour(() -> State.CREATE_NEW.equals(currentState.getObject())));
        addOrReplace(container);

        Callout callout = Callout.createInfoCallout(
                ID_CREATE_NEW_CALLOUT,
                getPageBase().createStringResource("CreateObjectForReferencePanel.note", getTypeTranslation()));
        container.add(callout);

        WebMarkupContainer panel;
        if (State.CREATE_NEW.equals(currentState.getObject())) {

            OperationResult result = new OperationResult("createNewObjectWrapper");
            LoadableModel<PrismObjectWrapper<ObjectType>> model =
                    getModelObject().getNewObjectModel(getContainerConfiguration(), getPageBase(), result).getObjectWrapperModel();
            if (result.isFatalError()) {
                getPageBase().showResult(result);
            }

            panel = new VerticalFormPanel(
                    ID_CREATE_NEW_FORM,
                    model,
                    new ItemPanelSettingsBuilder()
                            .mandatoryHandler(this::checkMandatory)
                            .build(),
                    getContainerConfiguration()) {
                @Override
                protected boolean isHeaderVisible(IModel model) {
                    return isHeaderOfCreateObjectVisible(model);
                }

                //                @Override
//                protected boolean isHeaderVisible(IModel<PrismContainerWrapper>) {
//                    return isHeaderOfCreateObjectVisible();
//                }

                @Override
                protected boolean isShowEmptyButtonVisible() {
                    return false;
                }

                @Override
                protected String getClassForPrismContainerValuePanel() {
                    return "bg-light border-0 rounded";
                }

                @Override
                protected String getCssForHeader() {
                    return "border border-bottom-0 border-left-0 border-right-0 rounded-0 p-2 pl-3 pr-3 mb-0 btn w-100";
                }

                @Override
                protected String getCssClassForFormContainerOfValuePanel() {
                    return "card-body mb-0 p-3";
                }
            };
        } else {
            panel = new WebMarkupContainer(ID_CREATE_NEW_FORM);
        }
        panel.setOutputMarkupId(true);
        container.addOrReplace(panel);
    }

    private boolean checkMandatory(ItemWrapper<?, ?> itemWrapper) {
        if (itemWrapper.getItemName().equals(ResourceType.F_NAME)) {
            return true;
        }
        return itemWrapper.isMandatory();
    }

    private Radio<Integer> addRadio(RadioGroup<Integer> radioGroup, String checkId, String labelId, int state, String labelKey) {
        Radio<Integer> radio = new Radio<>(checkId, Model.of(state), radioGroup);
        radioGroup.add(radio);
        radioGroup.add(new Label(labelId, getPageBase().createStringResource(
                labelKey, getTypeTranslation())));

        return radio;
    }

    private String getTypeTranslation() {
        List<QName> types = getParentWrapper().getTargetTypes();
        ObjectTypes type = ObjectTypes.OBJECT;
        if (types.size() == 1 && !QNameUtil.match(types.get(0), ObjectType.COMPLEX_TYPE)) {
            type = ObjectTypes.getObjectTypeFromTypeQName(types.get(0));
        }
        return getPageBase().createStringResource(type).getString().toLowerCase();
    }

    private PrismReferenceWrapper<ObjectReferenceType> getParentWrapper() {
        return getModelObject().getParent();
    }

    /**
     * Return panel for selecting of existing object
     */
    public Component getReferencePanel() {
        return get(createComponentPath(ID_USE_EXISTING_CONTAINER, ID_USE_EXISTING_INPUT));
    }

    protected boolean isHeaderOfCreateObjectVisible(IModel<PrismContainerWrapper> model) {
        if(isHeaderVisible) {
            if (model != null && model.getObject() != null
                    && PrismContainerWrapperFactoryImpl.VIRTUAL_CONTAINER.equals(model.getObject().getPath())
                    && PrismContainerWrapperFactoryImpl.VIRTUAL_CONTAINER.getLocalPart().equals(model.getObject().getDisplayName())) {
                return false;
            }
            return true;
        }
        return false;
    }
}
