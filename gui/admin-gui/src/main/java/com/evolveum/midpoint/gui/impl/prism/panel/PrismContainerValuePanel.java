/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.panel;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.*;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectValueWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemPanelContext;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismContainerPanelContext;
import com.evolveum.midpoint.gui.impl.prism.panel.component.ListContainersPopup;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author katka
 *
 */
public class PrismContainerValuePanel<C extends Containerable, CVW extends PrismContainerValueWrapper<C>> extends PrismValuePanel<C, PrismContainerWrapper<C>, CVW> {

    @Serial private static final long serialVersionUID = 1L;

    protected static final String ID_EXPANDABLE_LABEL_CONTAINER = "expandableLabelContainer";
    protected static final String ID_LABEL = "label";
    protected static final String ID_HELP = "help";
    private static final String ID_SORT_PROPERTIES = "sortProperties";
    private static final String ID_ADD_CHILD_CONTAINER = "addChildContainer";
    protected static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseButton";

    public PrismContainerValuePanel(String id, IModel<CVW> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected <PC extends ItemPanelContext> PC createPanelCtx(IModel<PrismContainerWrapper<C>> wrapper) {
        PrismContainerPanelContext<C> ctx = new PrismContainerPanelContext<>(wrapper);
        ctx.setSettings(getSettings());
        //noinspection unchecked
        return (PC) ctx;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);

        appendClassForAddedOrRemovedItem();

        add(AttributeAppender.append(
                "aria-label",
                () -> {
                    String key = "PrismContainerPanel.container";
                    if (getModelObject().getDefinition() != null && getModelObject().getDefinition().isMultiValue()) {
                        key = "PrismContainerValuePanel.container";
                    }
                    IModel<String> labelModel = getLabelModel();
                    String label = labelModel.getObject();
                    labelModel.detach();
                    return getParentPage().createStringResource(
                                    key, label)
                            .getString();
                }));
    }

    protected void appendClassForAddedOrRemovedItem(){
        add(AttributeModifier.append("class", () -> {
            String cssClasses = "";
            if (getModelObject() != null && ValueStatus.ADDED == getModelObject().getStatus()) {
                cssClasses = " added-value-background";
            }
            if (getModelObject() != null && ValueStatus.DELETED == getModelObject().getStatus()) {
                cssClasses = " removed-value-background";
            }
            return cssClasses;
        }));
    }

    @Override
    protected void addToHeader(WebMarkupContainer header) {
        AjaxButton expandableLabelContainer = new AjaxButton(ID_EXPANDABLE_LABEL_CONTAINER) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onExpandClick(target);
            }
        };
        expandableLabelContainer.setOutputMarkupId(true);
        expandableLabelContainer.setOutputMarkupPlaceholderTag(true);
        expandableLabelContainer.add(AttributeModifier.append("aria-expanded", getExpandedContainerValueModel()));
        expandableLabelContainer.add(AttributeModifier.append("aria-label", getExpandableLabelContainerAriaLabelModel()));
        header.add(expandableLabelContainer);

        LoadableDetachableModel<String> headerLabelModel = getLabelModel();
        Label labelComponent = new Label(ID_LABEL, headerLabelModel);
        labelComponent.setOutputMarkupId(true);
        labelComponent.setOutputMarkupPlaceholderTag(true);
        expandableLabelContainer.add(labelComponent);

        expandableLabelContainer.add(createExpandCollapseButton());

        header.add(getHelpLabel());

        initButtons(header);

        //commented due to wcag issue "Accessible labels are confusing"
//        header.add(AttributeAppender.append(
//                "aria-label",
//                () -> {
//                    String key = "PrismContainerPanel.header";
//                    if (getModelObject().getDefinition().isMultiValue()) {
//                        key = "PrismContainerValuePanel.header";
//                    }
//                    return getParentPage().createStringResource(
//                                    key, headerLabelModel.getObject())
//                            .getString();
//                }));

        //TODO always visible if isObject
    }

    protected IModel<String> getLabelModel() {
        return createStringResource("${displayName}", getModel());
    }

    @Override
    protected Component createDefaultPanel(String id) {
        throw new IllegalArgumentException("Cannot create default panel");
    }

    @Override
    protected <PV extends PrismValue> PV createNewValue(PrismContainerWrapper<C> itemWrapper) {
        //noinspection unchecked
        return (PV) itemWrapper.getItem().createNewValue();
    }

    private void initButtons(WebMarkupContainer header) {
        header.add(createSortButton());
        header.add(createAddMoreButton());
    }

    protected void onExpandClick(AjaxRequestTarget target) {
        CVW wrapper = getModelObject();
        wrapper.setExpanded(!wrapper.isExpanded());
        refreshPanel(target);
    }

    protected Label getHelpLabel() {
        Label help = new Label(ID_HELP);
        help.add(AttributeModifier.replace("title", new PropertyModel<>(getModel(), "helpText")));
        help.add(new InfoTooltipBehavior() {

            @Override
            public IModel<String> createAriaLabelModel() {
                return getParentPage().createStringResource("PrismContainerValuePanel.helpTooltip", getLabelModel().getObject());
            }
        });
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getModelObject().getHelpText()) && shouldBeButtonsShown()));
        help.setOutputMarkupId(true);
        return help;
    }

    private ToggleIconButton<String> createSortButton() {
        ToggleIconButton<String> sortPropertiesButton = new ToggleIconButton<>(ID_SORT_PROPERTIES,
                GuiStyleConstants.CLASS_ICON_SORT_ALPHA_ASC, GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onSortClicked(target);
            }

            @Override
            public boolean isOn() {
                return PrismContainerValuePanel.this.getModelObject().isSorted();
            }
        };
        sortPropertiesButton.add(AttributeAppender.append("data-tooltip", this::getSortButtonTitle));
        sortPropertiesButton.add(AttributeAppender.append("aria-label", this::getSortButtonTitle));
        sortPropertiesButton.add(new VisibleBehaviour(this::shouldBeButtonsShown));
        sortPropertiesButton.setOutputMarkupId(true);
        sortPropertiesButton.setOutputMarkupPlaceholderTag(true);
        return sortPropertiesButton;
    }

    private String getSortButtonTitle() {
        return getModelObject().isSorted() ? getString("PrismObjectPanel.sortPropertiesByOrder")
                : getString("PrismObjectPanel.sortPropertiesByName");
    }

    private AjaxLink createAddMoreButton() {

         AjaxLink<String> addChildContainerButton = new AjaxLink<>(ID_ADD_CHILD_CONTAINER, new StringResourceModel("PrismContainerValuePanel.addMore")) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    initMoreContainersPopup(target);
                }
            };

            addChildContainerButton.add(new VisibleEnableBehaviour(
                    this::isAddChildContainerButtonVisible, this::isAddChildContainerButtonEnable));
            addChildContainerButton.setOutputMarkupId(true);
            addChildContainerButton.setOutputMarkupPlaceholderTag(true);
            return addChildContainerButton;
    }

    private boolean isAddChildContainerButtonVisible() {
        return shouldBeButtonsShown() && getModelObject()!= null && getModelObject().isHeterogenous() &&
                !getModelObject().isVirtual();
    }

    private boolean isAddChildContainerButtonEnable() {
        if (getModelObject() != null) {
            if(getModelObject().getParent() != null) {
                return !getModelObject().getParent().isReadOnly();
            } else {
                return !getModelObject().isReadOnly();
            }
        }
        return false;
    }

    private void initMoreContainersPopup(AjaxRequestTarget parentTarget) {


        ListContainersPopup<C, CVW> listContainersPopup = new ListContainersPopup<>(getPageBase().getMainPopupBodyId(), getModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void processSelectedChildren(AjaxRequestTarget target, List<PrismContainerDefinition<?>> selected) {
                prepareNewContainers(target, selected);
            }

        };
        listContainersPopup.setOutputMarkupId(true);

        getPageBase().showMainPopup(listContainersPopup, parentTarget);
    }

    private void prepareNewContainers(AjaxRequestTarget target, List<PrismContainerDefinition<?>> containers) {
        PageAdminLTE parentPage = WebComponentUtil.getPage(PrismContainerValuePanel.this, PageAdminLTE.class);
        Task task = parentPage.createSimpleTask("Create child containers");
        WrapperContext ctx = new WrapperContext(task, task.getResult());
        ctx.setCreateIfEmpty(true);
        containers.forEach(container -> {
            try {
                ItemWrapper iw = parentPage.createItemWrapper(container, getModelObject(), ctx);
                if (iw != null) {
                    getModelObject().addItem(iw);
                }
            } catch (SchemaException e) {
                OperationResult result = ctx.getResult();
                result.recordFatalError(createStringResource("PrismContainerValuePanel.message.prepareNewContainers.fatalError", container).getString(), e);
                showResult(ctx.getResult());
            }
        });

        refreshPanel(target);

    }

    private boolean shouldBeButtonsShown() {
        return isContainerExpanded();
    }

    private void onSortClicked(AjaxRequestTarget target) {
        CVW wrapper = getModelObject();
        wrapper.setSorted(!wrapper.isSorted());
        target.add(getValuePanel());
        target.add(getSortButton());
        target.add(getFeedbackPanel());
    }

    private ToggleIconButton<Void> getSortButton() {
        return (ToggleIconButton) get(createComponentPath(ID_MAIN_CONTAINER, ID_HEADER_CONTAINER, ID_SORT_PROPERTIES));
    }

    public void refreshPanel(AjaxRequestTarget target) {
        target.add(PrismContainerValuePanel.this);
        target.add(getFeedbackPanel());
    }

    protected WebMarkupContainer createExpandCollapseButton() {
        WebMarkupContainer expandCollapseButton = new WebMarkupContainer(ID_EXPAND_COLLAPSE_BUTTON);
        expandCollapseButton.setOutputMarkupId(true);
        expandCollapseButton.add(AttributeModifier.append("class",
                () -> isContainerExpanded() ?
                GuiStyleConstants.CLASS_ICON_COLLAPSE_CONTAINER : GuiStyleConstants.CLASS_ICON_EXPAND_CONTAINER));
        return expandCollapseButton;
    }

    @Override
    protected void remove(CVW valueToRemove, AjaxRequestTarget target) throws SchemaException {
        throw new UnsupportedOperationException("Must be implemented in calling panel");
    }

    @Override
    protected boolean isRemoveButtonVisible() {
        return super.isRemoveButtonVisible() && isContainerExpanded() && !(getModelObject() instanceof PrismObjectValueWrapper)
                && !getModelObject().isVirtual();
    }

    private boolean isContainerExpanded() {
        return getModelObject().isExpanded();
    }

    private LoadableDetachableModel<Boolean> getExpandedContainerValueModel() {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected Boolean load() {
                return isContainerExpanded();
            }
        };
    }

    private LoadableDetachableModel<String> getExpandableLabelContainerAriaLabelModel() {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return getString(
                        isContainerExpanded() ? "PrismObjectPanel.collapseContainer" : "PrismObjectPanel.expandContainer");
            }
        };
    }
}
