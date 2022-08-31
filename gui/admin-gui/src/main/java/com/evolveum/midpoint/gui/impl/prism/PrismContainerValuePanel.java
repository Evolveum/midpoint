/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author katka
 *
 */
public class PrismContainerValuePanel<C extends Containerable, CVW extends PrismContainerValueWrapper<C>> extends BasePanel<CVW>{

    private static final long serialVersionUID = 1L;

    protected static final String ID_LABEL = "label";
    protected static final String ID_LABEL_CONTAINER = "labelContainer";
    protected static final String ID_HELP = "help";


    private static final String ID_SORT_PROPERTIES = "sortProperties";
    private static final String ID_SHOW_METADATA = "showMetadata";
    private static final String ID_ADD_CHILD_CONTAINER = "addChildContainer";
    private static final String ID_REMOVE_CONTAINER = "removeContainer";

    private static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseButton";
    private static final String ID_PROPERTIES_LABEL = "propertiesLabel";
    private static final String ID_SHOW_EMPTY_BUTTON = "showEmptyButton";

    private ItemVisibilityHandler visibilityHandler;

    public PrismContainerValuePanel(String id, IModel<CVW> model, ItemVisibilityHandler visibilityHandler) {
        super(id, model);
        this.visibilityHandler = visibilityHandler;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {

        initHeader();
        initValues();

    }

    private void initHeader() {
        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
        labelContainer.setOutputMarkupId(true);

        add(labelContainer);

        LoadableDetachableModel<String> headerLabelModel = getLabelModel();
        AjaxButton labelComponent = new AjaxButton(ID_LABEL, headerLabelModel) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                onExpandClick(target);
            }
        };
        labelComponent.setOutputMarkupId(true);
        labelComponent.setOutputMarkupPlaceholderTag(true);
        labelComponent.add(AttributeAppender.append("style", "cursor: pointer;"));
        labelContainer.add(labelComponent);

        labelContainer.add(getHelpLabel());

        initButtons();

        add(new VisibleBehaviour(() -> getModelObject() != null && ValueStatus.DELETED != getModelObject().getStatus()));
        //TODO always visible if isObject
    }

    protected LoadableDetachableModel<String> getLabelModel() {
        return getPageBase().createStringResource(getModel().getObject().getDisplayName());
    }

    private void initValues() {

        createNonContainersPanel();

        createContainersPanel();

    }

    private <PV extends PrismValue, I extends Item<PV, ID>, ID extends ItemDefinition<I>, IW extends ItemWrapper> void createNonContainersPanel() {
        WebMarkupContainer propertiesLabel = new WebMarkupContainer(ID_PROPERTIES_LABEL);
        propertiesLabel.setOutputMarkupId(true);

        IModel<List<IW>> nonContainerWrappers = createNonContainerWrappersModel();

        ListView<IW> properties = new ListView<IW>("properties", nonContainerWrappers) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<IW> item) {
                populateNonContainer(item);
            }
        };
//        properties.setReuseItems(true);
        properties.setOutputMarkupId(true);
        add(propertiesLabel);
           propertiesLabel.add(properties);

        AjaxButton labelShowEmpty = new AjaxButton(ID_SHOW_EMPTY_BUTTON) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                onShowEmptyClick(target);
            }

            @Override
            public IModel<?> getBody() {
                return getNameOfShowEmptyButton();
            }
        };
        labelShowEmpty.setOutputMarkupId(true);
        labelShowEmpty.add(AttributeAppender.append("style", "cursor: pointer;"));
        labelShowEmpty.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return nonContainerWrappers.getObject() != null && !nonContainerWrappers.getObject().isEmpty()
                        && getModelObject().isExpanded();// && !model.getObject().isShowEmpty();
            }
        });
        add(labelShowEmpty);
    }

    private <PV extends PrismValue, I extends Item<PV, ID>, ID extends ItemDefinition<I>, IW extends ItemWrapper> void createContainersPanel() {
        ListView<PrismContainerWrapper> containers = new ListView<PrismContainerWrapper>("containers", new PropertyModel<>(getModel(), "containers")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<PrismContainerWrapper> item) {
                populateContainer(item);
            }
        };

        containers.setReuseItems(true);
        containers.setOutputMarkupId(true);
        add(containers);

    }

    private <IW extends ItemWrapper> IModel<List<IW>> createNonContainerWrappersModel() {
        return new IModel<List<IW>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public List<IW> getObject() {
                return getNonContainerWrappers();
            }
        };
    }

    private <IW extends ItemWrapper> List<IW> getNonContainerWrappers() {
        CVW containerValueWrapper = getModelObject();
        List<? extends ItemWrapper<?, ?, ?, ?>> nonContainers = containerValueWrapper.getNonContainers();
        return (List<IW>) nonContainers;
    }

    private <IW extends ItemWrapper> void populateNonContainer(ListItem<IW> item) {
        item.setOutputMarkupId(true);
        IW itemWrapper = item.getModelObject();
        try {
            QName typeName = itemWrapper.getTypeName();
            if(item.getModelObject() instanceof ResourceAttributeWrapper) {
                typeName = new QName("ResourceAttributeDefinition");
            }

            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder().visibilityHandler(visibilityHandler);
            Panel panel = getPageBase().initItemPanel("property", typeName, item.getModel(), builder.build());
            panel.add(AttributeAppender.replace("style", () -> getModelObject().isExpanded() ? "" : "display:none"));

            panel.setOutputMarkupId(true);
            item.add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                //TODO decide if we need to set enable behavior here
                //see also ItemPanel.getEnableBehaviourOfValuePanel(), may be it's enough
//                @Override
//                public boolean isEnabled() {
//                    return !item.getModelObject().isReadOnly();
//                }

                @Override
                public boolean isVisible() {
                    CVW parent = PrismContainerValuePanel.this.getModelObject();
                    return item.getModelObject().isVisible(parent, visibilityHandler);
                }
            });
            item.add(panel);
        } catch (SchemaException e1) {
            throw new SystemException("Cannot instantiate " + itemWrapper.getTypeName());
        }

        item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));
    }

    private void populateContainer(ListItem<PrismContainerWrapper> container) {
        container.setOutputMarkupId(true);
        PrismContainerWrapper itemWrapper = container.getModelObject();
        try {
            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder().visibilityHandler(visibilityHandler);
            Panel panel = getPageBase().initItemPanel("container", itemWrapper.getTypeName(), container.getModel(), builder.build());
            panel.add(AttributeAppender.replace("style", () -> getModelObject().isExpanded() ? "" : "display:none"));

            panel.setOutputMarkupId(true);
            container.add(new VisibleBehaviour(() -> {
                CVW parent = PrismContainerValuePanel.this.getModelObject();
                return container.getModelObject().isVisible(parent, visibilityHandler);
            }));
            container.add(panel);
        } catch (SchemaException e) {
            throw new SystemException("Cannot instantiate panel for: " + itemWrapper.getDisplayName());
        }

    }

     private StringResourceModel getNameOfShowEmptyButton() {
            return getPageBase().createStringResource("ShowEmptyButton.showMore.${showEmpty}", getModel());

        }

        private void onShowEmptyClick(AjaxRequestTarget target) {

            PrismContainerValueWrapper<C> wrapper = getModelObject();
            wrapper.setShowEmpty(!wrapper.isShowEmpty());
            refreshPanel(target);
        }

    private <IW extends ItemWrapper<?,?,?,?>> IModel<String> createStyleClassModel(final IModel<IW> wrapper) {
        return new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                ItemWrapper<?, ?, ?,?> property = wrapper.getObject();
                return property.isStripe() ? "stripe" : null;
            }
        };
    }

    private void initButtons() {
        initExpandCollapseButton();
        initMetadataButton();
        initSortButton();
        initAddMoreButton();
        initRemoveButton();
    }

    private void onExpandClick(AjaxRequestTarget target) {

        CVW wrapper = getModelObject();
        wrapper.setExpanded(!wrapper.isExpanded());
        refreshPanel(target);
    }

    protected Label getHelpLabel() {

        Label help = new Label(ID_HELP);
        help.add(AttributeModifier.replace("title", LambdaModel.of(getModel(), CVW::getHelpText)));
        help.add(new InfoTooltipBehavior());
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getModelObject().getHelpText())));
        help.setOutputMarkupId(true);
        return help;
    }

    private void initMetadataButton() {
        ToggleIconButton<String> showMetadataButton = new ToggleIconButton<String>(ID_SHOW_METADATA,
                GuiStyleConstants.CLASS_ICON_SHOW_METADATA, GuiStyleConstants.CLASS_ICON_SHOW_METADATA) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onShowMetadataClicked(target);
            }

            @Override
            public boolean isOn() {
                return PrismContainerValuePanel.this.getModelObject().isShowMetadata();
            }


        };
        showMetadataButton.add(new AttributeModifier("title", new StringResourceModel("PrismContainerValuePanel.showMetadata.${showMetadata}", getModel())));
//                return PrismContainerValueHeaderPanel.this.getModelObject() == null ? "" : (PrismContainerValueHeaderPanel.this.getModelObject().isShowMetadata() ?
//                        createStringResource("PrismObjectPanel.hideMetadata").getString() :
//                        createStringResource("PrismObjectPanel.showMetadata").getString());
        showMetadataButton.add(new VisibleBehaviour(() -> getModelObject().hasMetadata() && shouldBeButtonsShown()));
        showMetadataButton.setOutputMarkupId(true);
        showMetadataButton.setOutputMarkupPlaceholderTag(true);
        add(showMetadataButton);

    }

    private void initSortButton() {
        ToggleIconButton<String> sortPropertiesButton = new ToggleIconButton<String>(ID_SORT_PROPERTIES,
                GuiStyleConstants.CLASS_ICON_SORT_ALPHA_ASC, GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onSortClicked(target);
            }

            @Override
            public boolean isOn() {
                return PrismContainerValuePanel.this.getModelObject().isSorted();
            }
        };
        sortPropertiesButton.add(new VisibleBehaviour(() -> shouldBeButtonsShown()));
        sortPropertiesButton.setOutputMarkupId(true);
        sortPropertiesButton.setOutputMarkupPlaceholderTag(true);
        add(sortPropertiesButton);
    }

    private void initAddMoreButton() {

         AjaxLink<String> addChildContainerButton = new AjaxLink<String>(ID_ADD_CHILD_CONTAINER, new StringResourceModel("PrismContainerValuePanel.addMore")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    initMoreContainersPopup(target);
                }
            };

            addChildContainerButton.add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isEnabled() {
                    if (getModelObject() != null) {
                        if(getModelObject().getParent() != null) {
                            return !getModelObject().getParent().isReadOnly();
                        } else {
                            return !getModelObject().isReadOnly();
                        }
                    }
                    return false;
                }

                @Override
                public boolean isVisible() {
                    return shouldBeButtonsShown() && getModelObject()!= null && getModelObject().isHeterogenous();
                }
            });
            addChildContainerButton.setOutputMarkupId(true);
            addChildContainerButton.setOutputMarkupPlaceholderTag(true);
            add(addChildContainerButton);

    }

    private void initMoreContainersPopup(AjaxRequestTarget parentTarget) {


        ListContainersPopup<C, CVW> listContainersPopup = new ListContainersPopup<C, CVW>(getPageBase().getMainPopupBodyId(), getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void processSelectedChildren(AjaxRequestTarget target, List<PrismContainerDefinition<?>> selected) {
                prepareNewContainers(target, selected);
            }

        };
        listContainersPopup.setOutputMarkupId(true);

        getPageBase().showMainPopup(listContainersPopup, parentTarget);
    }

    private void prepareNewContainers(AjaxRequestTarget target, List<PrismContainerDefinition<?>> containers) {
        getPageBase().hideMainPopup(target);

        Task task = getPageBase().createSimpleTask("Create child containers");
        WrapperContext ctx = new WrapperContext(task, task.getResult());
        containers.forEach(container -> {
            try {
                ItemWrapper iw = getPageBase().createItemWrapper(container, getModelObject(), ctx);
                if (iw != null) {
                    getModelObject().addItem(iw);
                }
            } catch (SchemaException e) {
                OperationResult result = ctx.getResult();
                result.recordFatalError(createStringResource("PrismContainerValuePanel.message.prepareNewContainers.fatalError", container).getString(), e);
                getPageBase().showResult(ctx.getResult());
            }
        });

        refreshPanel(target);

    }

    private void initRemoveButton() {
        AjaxLink<Void> removeContainerButton = new AjaxLink<Void>(ID_REMOVE_CONTAINER) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeValuePerformed(target);
            }
        };

        removeContainerButton.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                if (getModelObject() != null) {
                    if(getModelObject().getParent() != null) {
                        return !getModelObject().getParent().isReadOnly();
                    } else {
                        return !getModelObject().isReadOnly();
                    }
                }
                return false;
            }

            @Override
            public boolean isVisible() {
                return shouldBeButtonsShown();
            }
        });
        add(removeContainerButton);

    }

    private void removeValuePerformed(AjaxRequestTarget target) {
        CVW containerValueWrapper = getModelObject();
        containerValueWrapper.setStatus(ValueStatus.DELETED);
        refreshPanel(target);
    }

    private boolean shouldBeButtonsShown() {
        return getModelObject().isExpanded();
    }

    private void onSortClicked(AjaxRequestTarget target) {
        CVW wrapper = getModelObject();
        wrapper.setSorted(!wrapper.isSorted());

        //wrapper.sort();

        refreshPanel(target);
    }

    private void onShowMetadataClicked(AjaxRequestTarget target) {
        CVW wrapper = getModelObject();
        wrapper.setShowMetadata(!wrapper.isShowMetadata());
        refreshPanel(target);
    }


    private void refreshPanel(AjaxRequestTarget target) {

        target.add(PrismContainerValuePanel.this);
        target.add(getPageBase().getFeedbackPanel());
    }

    protected void initExpandCollapseButton() {
        ToggleIconButton<?> expandCollapseButton = new ToggleIconButton<Void>(ID_EXPAND_COLLAPSE_BUTTON,
                GuiStyleConstants.CLASS_ICON_EXPAND_CONTAINER, GuiStyleConstants.CLASS_ICON_COLLAPSE_CONTAINER) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onExpandClick(target);
            }

            @Override
            public boolean isOn() {
                return PrismContainerValuePanel.this.getModelObject().isExpanded();
            }
        };
        expandCollapseButton.setOutputMarkupId(true);
        add(expandCollapseButton);
    }
}
