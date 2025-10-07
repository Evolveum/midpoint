/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author katka
 */
public abstract class ItemHeaderPanel<V extends PrismValue, I extends Item<V, ID>, ID extends ItemDefinition<I>, IW extends ItemWrapper> extends BasePanel<IW> {
    private static final long serialVersionUID = 1L;

    protected static final String ID_LABEL = "label";
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
        initButtons();
        initHeaderLabel();

    }

    protected void initHeaderLabel() {
        createTitle();
        createHelpText();
        createExperimentalTooltip();
        createDeprecated();
        createRequired(ID_REQUIRED);

        //TODO: pending operations
    }

    private void createTitle() {
        Component displayName = createTitle(createLabelModel());
        displayName.add(new AttributeModifier("style", getDeprecatedCss())); //TODO create deprecated css class?

        add(displayName);

    }

    public IModel<String> createLabelModel() {
        return new PropertyModel<>(getModel(), "displayName");
    }

    protected abstract Component createTitle(IModel<String> model);

    private void createHelpText() {

        Label help = new Label(ID_HELP);
        IModel<String> helpModel = new PropertyModel<>(getModel(), "help");
        help.add(AttributeModifier.replace("title", createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
        help.add(new InfoTooltipBehavior());
        help.add(new VisibleBehaviour(this::isHelpTextVisible));
        help.add(AttributeAppender.append(
                "aria-label",
                getParentPage().createStringResource("ItemHeaderPanel.helpTooltip", createLabelModel().getObject())));
        add(help);
    }

    protected boolean isHelpTextVisible() {
        return getModelObject() != null && StringUtils.isNotEmpty(getModelObject().getHelp());
    }

    private void createExperimentalTooltip() {
        Label experimental = new Label(ID_EXPERIMENTAL);

        experimental.add(new InfoTooltipBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return "far fa-lightbulb text-warning";
            }
        });
        experimental.add(AttributeModifier.replace("title", createStringResource("ItemHeaderPanel.experimentalFeature")));
        experimental.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().isExperimental()));
        add(experimental);

    }

    private void createDeprecated() {
        Label deprecated = new Label(ID_DEPRECATED);
        deprecated.add(AttributeModifier.replace("title",
                createStringResource("ItemHeaderPanel.deprecated", getModelObject() != null && getModelObject().getDeprecatedSince() != null ? getModelObject().getDeprecatedSince() : "N/A")));
        deprecated.add(new InfoTooltipBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return "fa fa-fw fa-warning text-warning";
            }
        });
        deprecated.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().isDeprecated()));
        add(deprecated);
    }

    protected void createRequired(String id) {
        WebMarkupContainer required = new WebMarkupContainer(id);
        required.add(new VisibleBehaviour(() -> isRequired()));
        add(required);
    }

    protected boolean isRequired() {
        return getModelObject() != null && getModelObject().isMandatory();
    }

    public IModel<String> getDeprecatedCss() {
        return () -> getModelObject() != null && getModelObject().isDeprecated() ? "text-decoration: line-through;" : "text-decoration: none;";
    }

    protected void initButtons() {
        AjaxLink<Void> addButton = new AjaxLink<>(ID_ADD_BUTTON) {
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
        addButton.add(AttributeAppender.append("title", getTitleForAddButton()));
        add(addButton);

        AjaxLink<Void> removeButton = new AjaxLink<>(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeItem(target);
            }
        };
        removeButton.add(new VisibleBehaviour(this::isButtonEnabled));
        removeButton.add(AttributeAppender.append("title", getTitleForRemoveAllButton()));
        add(removeButton);
    }

    protected IModel<String> getTitleForRemoveAllButton() {
        return getParentPage().createStringResource("ItemHeaderPanel.removeAll");
    }

    protected IModel<String> getTitleForAddButton() {
        return getParentPage().createStringResource("ItemHeaderPanel.addValue");
    }

    private void addValue(AjaxRequestTarget target) {
        IW parentWrapper = getModelObject();
        try {
            parentWrapper.addIgnoringEquivalents(createNewValue(parentWrapper), getParentPage());
        } catch (SchemaException e) {
            getSession().error(getString("ItemHeaderPanel.value.add.failed", e.getMessage()));
            LOGGER.error("Failed to add new value for {}, reason: {}", parentWrapper, e.getMessage(), e);
            target.add(getParentPage().getFeedbackPanel());
        }
        refreshPanel(target);
    }

    private void removeItem(AjaxRequestTarget target) {
        try {
            getModelObject().removeAll(getParentPage());
        } catch (SchemaException e) {
            LOGGER.error("Cannot remove value: {}", getModelObject());
            getSession().error("Cannot remove value " + getModelObject());
            target.add(getParentPage().getFeedbackPanel());

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

    public Component getLabelComponent() {
        return get(ID_LABEL);
    }
}
