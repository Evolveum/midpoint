/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * Created by honchar
 */
public class ExpressionPropertyHeaderPanel extends ItemHeaderPanel<PrismPropertyValue<ExpressionType>, PrismProperty<ExpressionType>,
        PrismPropertyDefinition<ExpressionType>, PrismPropertyWrapper<ExpressionType>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_REMOVE_BUTTON = "removeButton";
    private static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseButton";

    private boolean isExpanded;

    public ExpressionPropertyHeaderPanel(String id, IModel<PrismPropertyWrapper<ExpressionType>> model) {
        super(id, model);
        isExpanded = model.getObject() != null && CollectionUtils.isNotEmpty(model.getObject().getValues());
    }

    @Override
    protected void initButtons() {
        AjaxLink<Void> addButton = new AjaxLink<Void>(ID_ADD_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addExpressionValuePerformed(target);
            }
        };
        addButton.add(new VisibleBehaviour(() -> isExpressionValueEmpty()));
        add(addButton);

        AjaxLink<Void> removeButton = new AjaxLink<Void>(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeExpressionValuePerformed(target);
            }
        };
        removeButton.add(new VisibleBehaviour(() -> !isExpressionValueEmpty()));
        add(removeButton);

        ToggleIconButton<?> expandCollapseButton = new ToggleIconButton<Void>(ID_EXPAND_COLLAPSE_BUTTON,
                GuiStyleConstants.CLASS_ICON_EXPAND_CONTAINER, GuiStyleConstants.CLASS_ICON_COLLAPSE_CONTAINER) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onExpandClick(target);
            }

            @Override
            public boolean isOn() {
                return ExpressionPropertyHeaderPanel.this.getModelObject() != null && isExpanded;
            }
        };
        expandCollapseButton.setOutputMarkupId(true);
        add(expandCollapseButton);
    }

    @Override
    protected Component createTitle(IModel<String> label) {
        AjaxButton labelComponent = new AjaxButton(ID_LABEL, label) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                onExpandClick(target);
            }
        };
        labelComponent.setOutputMarkupId(true);
        labelComponent.add(AttributeAppender.append("style", "cursor: pointer;"));
        return labelComponent;
    }

    private boolean isExpressionValueEmpty(){
        if (getModelObject() == null || CollectionUtils.isEmpty(getModelObject().getValues())){
            return true;
        }
        List<PrismPropertyValueWrapper<ExpressionType>> valueWrappers = getModelObject().getValues();
        for(PrismPropertyValueWrapper<ExpressionType> expressionValueWrapper : valueWrappers){
            if (expressionValueWrapper.getOldValue() != null && expressionValueWrapper.getOldValue().getValue() != null){
                return false;
            }
        }
        return true;
    }

    protected void onExpandClick(AjaxRequestTarget target) {
        isExpanded = !isExpanded;
    }

    protected void addExpressionValuePerformed(AjaxRequestTarget target){

    }

    protected void removeExpressionValuePerformed(AjaxRequestTarget target){

    }

    /**
     * @author katka
     *
     */
    public static class PrismContainerHeaderPanel<C extends Containerable> extends BasePanel<PrismContainerWrapper<C>> { //ItemHeaderPanel<PrismContainerWrapper<C>>{

        private static final long serialVersionUID = 1L;

        private static final String ID_ADD_BUTTON = "addButton";
        private static final String ID_EXPAND_COLLAPSE_FRAGMENT = "expandCollapseFragment";
        private static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseButton";

        public PrismContainerHeaderPanel(String id, IModel<PrismContainerWrapper<C>> model) {
            super(id, model);

        }

        @Override
        protected void onInitialize() {
            super.onInitialize();
            initLayout();
        }

        private void initLayout() {
            setOutputMarkupId(true);

            initButtons();
    //        initHeaderLabel();
        }

    //    @Override
        protected void initButtons() {

            add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return isContainerMultivalue();
                }
            });

             AjaxLink<?> addButton = new AjaxLink<Void>(ID_ADD_BUTTON) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addValue(target);
                    }
                };
                addButton.add(new VisibleEnableBehaviour() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        return isAddButtonVisible();
                    }
                });

                add(addButton);

        }

        protected boolean isAddButtonVisible() {
            return getModelObject().isExpanded();
        }

        private void addValue(AjaxRequestTarget target) {
    //        ContainerWrapperFactory cwf = new ContainerWrapperFactory(getPageBase());
    //        PrismContainerWrapper<C> containerWrapper = getModelObject();
    //        Task task = getPageBase().createSimpleTask("Creating new container");
    //        ContainerValueWrapper<C> newContainerValue = cwf.createContainerValueWrapper(containerWrapper,
    //                containerWrapper.getItem().createNewValue(), containerWrapper.getObjectStatus(), ValueStatus.ADDED,
    //                containerWrapper.getPath(), task);
    //        newContainerValue.setShowEmpty(true, false);
    //        getModelObject().addValue(newContainerValue);
    //        onButtonClick(target);
        }

        private boolean isContainerMultivalue(){
            return true;// getModelObject().isVisible() && getModelObject().getItemDefinition().isMultiValue();
        }


        public String getLabel() {
            return getModelObject() != null ? getModelObject().getDisplayName() : "";
        }

    //    @Override
    //    protected WebMarkupContainer initExpandCollapseButton(String contentAreaId) {
    //        Fragment expandCollapseFragment = new Fragment(contentAreaId, ID_EXPAND_COLLAPSE_FRAGMENT, this);
    //
    //        ToggleIconButton<?> expandCollapseButton = new ToggleIconButton<Void>(ID_EXPAND_COLLAPSE_BUTTON,
    //                GuiStyleConstants.CLASS_ICON_EXPAND_CONTAINER, GuiStyleConstants.CLASS_ICON_COLLAPSE_CONTAINER) {
    //
    //            private static final long serialVersionUID = 1L;
    //
    //            @Override
    //            public void onClick(AjaxRequestTarget target) {
    //                onExpandClick(target);
    //            }
    //
    //            @Override
    //            public boolean isOn() {
    //                return PrismContainerHeaderPanel.this.getModelObject().isExpanded();
    //            }
    //        };
    //        expandCollapseButton.setOutputMarkupId(true);
    //
    //        expandCollapseFragment.add(expandCollapseButton);
    //
    //        return expandCollapseFragment;
    //    }
    //
    //    private void onExpandClick(AjaxRequestTarget target) {
    //
    //        PrismContainerWrapper<C> wrapper = getModelObject();
    //        wrapper.setExpanded(!wrapper.isExpanded());
    //        onButtonClick(target);
    //    }
    //
    //    @Override
    //    protected void initHeaderLabel(){
    //        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
    //        labelContainer.setOutputMarkupId(true);
    //
    //        add(labelContainer);
    //
    //        String displayName = getLabel();
    //        if (org.apache.commons.lang3.StringUtils.isEmpty(displayName)) {
    //            displayName = "displayName.not.set";
    //        }
    //
    //        StringResourceModel headerLabelModel = createStringResource(displayName);
    //        AjaxButton labelComponent = new AjaxButton(ID_LABEL, headerLabelModel) {
    //            private static final long serialVersionUID = 1L;
    //            @Override
    //            public void onClick(AjaxRequestTarget target) {
    //                onExpandClick(target);
    //            }
    //        };
    //        labelComponent.setOutputMarkupId(true);
    //        labelComponent.add(AttributeAppender.append("style", "cursor: pointer;"));
    //        labelContainer.add(labelComponent);
    //
    //        labelContainer.add(getHelpLabel());
    //
    //    }
    //
    //    @Override
    //    protected String getHelpText() {
    //        return WebComponentUtil.loadHelpText(new Model<ContainerWrapperImpl<C>>(getModelObject()), PrismContainerHeaderPanel.this);
    //    }
    //
    //    @Override
    //    protected boolean isVisibleHelpText() {
    //        return true;
    //    }

    }
}
