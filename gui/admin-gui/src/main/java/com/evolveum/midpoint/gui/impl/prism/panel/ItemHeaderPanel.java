/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author katka
 *
 */
public abstract class ItemHeaderPanel<V extends PrismValue, I extends Item<V, ID>, ID extends ItemDefinition<I>, IW extends ItemWrapper> extends BasePanel<IW> {
    private static final long serialVersionUID = 1L;


    protected static final String ID_LABEL = "label";
    protected static final String ID_LABEL_CONTAINER = "labelContainer";
    protected static final String ID_HELP = "help";
    private static final String ID_EXPERIMENTAL = "experimental";
    private static final String ID_DEPRECATED = "deprecated";
    private static final String ID_REQUIRED = "required";

    private static final String ID_ADD_BUTTON = "add";
    private static final String ID_REMOVE_BUTTON = "remove";


    private static final Trace LOGGER = TraceManager.getTrace(ItemHeaderPanel.class);


    public ItemHeaderPanel(String id, IModel<IW> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {

        setOutputMarkupId(true);

//        add(initExpandCollapseButton(ID_EXPAND_COLLAPSE_CONTAINER));
        initButtons();
        initHeaderLabel();

    }

//    protected WebMarkupContainer initExpandCollapseButton(String contentAreaId){
//        return new WebMarkupContainer(contentAreaId);
//    }

    protected void initHeaderLabel(){

        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
        labelContainer.setOutputMarkupId(true);
//        labelContainer.add(new AttributeModifier("class", getLabelCssClass()));
        add(labelContainer);

        createTitle(labelContainer);
        createHelpText(labelContainer);
        createExperimentalTooltip(labelContainer);
        createDeprecated(labelContainer);
        createRequeired(labelContainer);
//        createOutbound(labelContainer);

        //TODO: pending operations
    }

    protected WebMarkupContainer getLabelContainer() {
        return (WebMarkupContainer)get(ID_LABEL_CONTAINER);
    }

    private void createTitle(WebMarkupContainer labelContainer) {
        Component displayName = createTitle(new PropertyModel<>(getModel(), "displayName"));//.of(getModel(), IW::getDisplayName));
        displayName.add(new AttributeModifier("style", getDeprecatedCss()));

        labelContainer.add(displayName);

    }

    protected abstract Component createTitle(IModel<String> model);

    private void createHelpText(WebMarkupContainer labelContainer) {

        Label help = new Label(ID_HELP);
        IModel<String> helpModel = new PropertyModel<String>(getModel(), "help");
        help.add(AttributeModifier.replace("title",createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
        help.add(new InfoTooltipBehavior());
        help.add(new VisibleBehaviour(() -> getModelObject() != null && StringUtils.isNotEmpty(getModelObject().getHelp())));
        labelContainer.add(help);
    }

    private void createExperimentalTooltip(WebMarkupContainer labelContainer) {
        Label experimental = new Label(ID_EXPERIMENTAL);

        experimental.add(new InfoTooltipBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return "fa fa-fw  fa-lightbulb-o text-warning";
            }


        });
        experimental.add(AttributeModifier.replace("title", createStringResource("ItemHeaderPanel.experimentalFeature")));
        experimental.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().isExperimental()));
        labelContainer.add(experimental);

    }

    private void createDeprecated(WebMarkupContainer labelContainer) {
        Label deprecated = new Label(ID_DEPRECATED);
        deprecated.add(AttributeModifier.replace("deprecated", new PropertyModel<>(getModel(), "deprecatedSince")));
        deprecated.add(new InfoTooltipBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return "fa fa-fw fa-warning text-warning";
            }


        });
        deprecated.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().isDeprecated()));
        labelContainer.add(deprecated);
    }

    private void createRequeired(WebMarkupContainer labelContainer) {
        WebMarkupContainer required = new WebMarkupContainer(ID_REQUIRED);
        required.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().isMandatory()));
        labelContainer.add(required);
    }

    public IModel<String> getDeprecatedCss() {
        return () -> getModelObject() != null && getModelObject().isDeprecated() ? "text-decoration: line-through;" : "text-decoration: none;";
    }


    ///OLD
    protected void initButtons() {
        AjaxLink<Void> addButton = new AjaxLink<Void>(ID_ADD_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValue(target);
            }
        };
        addButton.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return isButtonEnabled();
            }

            @Override
            public boolean isVisible() {
                return isAddButtonVisible();
            }
        });
        add(addButton);

        AjaxLink<Void> removeButton = new AjaxLink<Void>(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeItem(target);
            }
        };
        removeButton.add(new VisibleBehaviour(() -> isButtonEnabled()));
        add(removeButton);
    }

    private void addValue(AjaxRequestTarget target) {
        IW parentWrapper = getModelObject();
        try {
            parentWrapper.add(createNewValue(parentWrapper), getPageBase());
        } catch (SchemaException e) {
            getSession().error(getString("ItemHeaderPanel.value.add.failed", e.getMessage()));
            LOGGER.error("Failed to add new value for {}, reason: {}", parentWrapper, e.getMessage(), e);
            target.add(getPageBase().getFeedbackPanel());
        }
        refreshPanel(target);
    }

    private void removeItem(AjaxRequestTarget target) {
        try {
            getModelObject().removeAll(getPageBase());
        } catch (SchemaException e) {
            LOGGER.error("Cannot remove value: {}", getModelObject());
            getSession().error("Cannot remove value "+ getModelObject());
            target.add(getPageBase().getFeedbackPanel());

        }
        refreshPanel(target);
    }

    protected abstract V createNewValue(IW parent);
    protected abstract void refreshPanel(AjaxRequestTarget target);


    protected boolean isAddButtonVisible() {
        return getModelObject() != null && getModelObject().isMultiValue();
    }

    protected boolean isButtonEnabled() {
        return getModelObject() != null && !getModelObject().isReadOnly() && getModelObject().isMultiValue();
    }


}
