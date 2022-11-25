/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ScenePanel extends BasePanel<SceneDto> {

    private static final String ID_BOX = "box";
    private static final String STRIPED_CLASS = "striped";
    private static final String ID_ITEMS_TABLE = "itemsTable";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_PARTIAL_SCENES = "partialScenes";
    private static final String ID_PARTIAL_SCENE = "partialScene";
    private static final String ID_SHOW_OPERATIONAL_ITEMS_LINK = "showOperationalItemsLink";

    private static final Trace LOGGER = TraceManager.getTrace(ScenePanel.class);
    private static final String ID_OPTION_BUTTONS = "optionButtons";
    private static final String ID_HEADER_PANEL = "headerPanel";
    private static final String ID_HEADER_DESCRIPTION = "description";
    private static final String ID_HEADER_WRAPPER_DISPLAY_NAME = "wrapperDisplayName";
    private static final String ID_HEADER_NAME_LABEL = "nameLabel";
    private static final String ID_HEADER_NAME_LINK = "nameLink";
    private static final String ID_HEADER_CHANGE_TYPE = "changeType";
    private static final String ID_HEADER_OBJECT_TYPE = "objectType";
    private static final String ID_BODY = "body";
    private static final String ID_OLD_VALUE_LABEL = "oldValueLabel";
    private static final String ID_NEW_VALUE_LABEL = "newValueLabel";
    private static final String ID_VALUE_LABEL = "valueLabel";
    private static final String ID_SORT_PROPERTIES = "sortProperties";
    private static final String ID_WARNING = "warning";

    private boolean showOperationalItems = false;
    private boolean operationalItemsVisible = false;

    public ScenePanel(String id, @NotNull IModel<SceneDto> model) {
        this(id, model, false);
    }

    public ScenePanel(String id, @NotNull IModel<SceneDto> model, boolean showOperationalItems) {
        super(id, model);
        this.showOperationalItems = showOperationalItems;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setOutputMarkupId(true);
        initLayout();
    }

    private AjaxEventBehavior createHeaderOnClickBehaviour(final IModel<SceneDto> model) {
        return new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                headerOnClickPerformed(target, model);
            }
        };
    }

    private void initLayout() {
        final IModel<SceneDto> model = getModel();

        WebMarkupContainer box = new WebMarkupContainer(ID_BOX);
        box.add(AttributeModifier.append("class", () -> {
            SceneDto dto = model.getObject();

            if (dto.getBoxClassOverride() != null) {
                return dto.getBoxClassOverride();
            }

            if (dto.getChangeType() == null) {
                return null;
            }

            switch (dto.getChangeType()) {
                case ADD:
                    return "card-success";
                case DELETE:
                    return "card-danger";
                case MODIFY:
                    return "card-info";
                default:
                    return null;
            }
        }));
        add(box);

        WebMarkupContainer headerPanel = new WebMarkupContainer(ID_HEADER_PANEL);
        box.add(headerPanel);

        headerPanel.add(new SceneButtonPanel(ID_OPTION_BUTTONS, model) {
            @Override
            public void minimizeOnClick(AjaxRequestTarget target) {
                headerOnClickPerformed(target, model);
            }
        });

        Label headerChangeType = new Label(ID_HEADER_CHANGE_TYPE, new ChangeTypeModel());
        Label headerObjectType = new Label(ID_HEADER_OBJECT_TYPE, new ObjectTypeModel());

        IModel<String> nameModel = () -> model.getObject().getName(ScenePanel.this);

        Label headerNameLabel = new Label(ID_HEADER_NAME_LABEL, nameModel);
        AjaxLinkPanel headerNameLink = new AjaxLinkPanel(ID_HEADER_NAME_LINK, nameModel) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PrismContainerValue<?> value = getModelObject().getScene().getSourceValue();
                if (value != null && value.getParent() instanceof PrismObject) {
                    PrismObject<? extends ObjectType> object = (PrismObject<? extends ObjectType>) value.getParent();
                    WebComponentUtil.dispatchToObjectDetailsPage(ObjectTypeUtil.createObjectRef(object, getPageBase().getPrismContext()), getPageBase(), false);
                }
            }
        };
        Label headerDescription = new Label(ID_HEADER_DESCRIPTION, () -> model.getObject().getDescription(ScenePanel.this));
        Label headerWrapperDisplayName = new Label(ID_HEADER_WRAPPER_DISPLAY_NAME, () -> {
            WrapperScene scene = ((WrapperScene) getModelObject().getScene());
            String key = scene.getDisplayNameKey();
            Object[] parameters = scene.getDisplayNameParameters();
            return new StringResourceModel(key, this).setModel(null)
                    .setDefaultValue(key)
                    .setParameters(parameters).getObject();
        });

        headerPanel.add(headerChangeType);
        headerPanel.add(headerObjectType);
        headerPanel.add(headerNameLabel);
        headerPanel.add(headerNameLink);
        headerPanel.add(headerDescription);
        headerPanel.add(headerWrapperDisplayName);

        Label warning = new Label(ID_WARNING);
        warning.add(new VisibleBehaviour(() -> getModelObject().getScene().isBroken()));
        warning.add(new TooltipBehavior());
        headerPanel.add(warning);

        headerChangeType.add(createHeaderOnClickBehaviour(model));
        headerObjectType.add(createHeaderOnClickBehaviour(model));
        headerNameLabel.add(createHeaderOnClickBehaviour(model));
        headerDescription.add(createHeaderOnClickBehaviour(model));
        headerWrapperDisplayName.add(createHeaderOnClickBehaviour(model));

        VisibleBehaviour visibleIfNotWrapper = new VisibleBehaviour(() -> !getModelObject().isWrapper());
        VisibleBehaviour visibleIfWrapper = new VisibleBehaviour(() -> getModelObject().isWrapper());
        VisibleBehaviour visibleIfExistingObjectAndAuthorized = new VisibleBehaviour(() -> {
            if (getModelObject().isWrapper()) {
                return false;
            }
            return isExistingViewableObject() && isAutorized();
        });
        VisibleBehaviour visibleIfNotWrapperAndNotExistingObjectAndNotAuthorized = new VisibleBehaviour(() -> {
            if (getModelObject().isWrapper()) {
                return false;
            }
            return !isExistingViewableObject() || !isAutorized();
        });

        headerChangeType.add(visibleIfNotWrapper);
        headerObjectType.add(visibleIfNotWrapper);
        headerNameLabel.add(visibleIfNotWrapperAndNotExistingObjectAndNotAuthorized);
        headerNameLink.add(visibleIfExistingObjectAndAuthorized);
        headerDescription.add(visibleIfNotWrapper);
        headerWrapperDisplayName.add(visibleIfWrapper);

        WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.add(new VisibleBehaviour(() -> {
            SceneDto wrapper = model.getObject();
            return !wrapper.isMinimized();
        }));
        box.add(body);

        WebMarkupContainer itemsTable = new WebMarkupContainer(ID_ITEMS_TABLE);
        itemsTable.add(new VisibleBehaviour(() -> !model.getObject().getItems().isEmpty()));
        itemsTable.setOutputMarkupId(true);

        ToggleIconButton<String> sortPropertiesButton = new ToggleIconButton<String>(ID_SORT_PROPERTIES,
                GuiStyleConstants.CLASS_ICON_SORT_ALPHA_ASC, GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onSortClicked(model, target);
            }

            @Override
            public boolean isOn() {
                return model.getObject().isSorted();
            }
        };
        sortPropertiesButton.setOutputMarkupId(true);
        sortPropertiesButton.setOutputMarkupPlaceholderTag(true);
        itemsTable.add(sortPropertiesButton);

        WebMarkupContainer oldValueLabel = new WebMarkupContainer(ID_OLD_VALUE_LABEL);
        oldValueLabel.add(new VisibleBehaviour(() -> model.getObject().containsDeltaItems()));
        itemsTable.add(oldValueLabel);

        WebMarkupContainer newValueLabel = new WebMarkupContainer(ID_NEW_VALUE_LABEL);
        newValueLabel.add(new VisibleBehaviour(() -> model.getObject().containsDeltaItems()));
        itemsTable.add(newValueLabel);

        WebMarkupContainer valueLabel = new WebMarkupContainer(ID_VALUE_LABEL);
        valueLabel.add(new VisibleBehaviour(() -> !model.getObject().containsDeltaItems()));
        itemsTable.add(valueLabel);

        ListView<SceneItemDto> items = new ListView<>(ID_ITEMS,
                new PropertyModel<>(model, SceneDto.F_ITEMS)) {

            @Override
            protected void populateItem(ListItem<SceneItemDto> item) {
                SceneItemPanel panel = new SceneItemPanel(ID_ITEM, item.getModel());
                panel.add(new VisibleBehaviour(() -> !isOperationalItem(item.getModel()) || isOperationalItemsVisible()));
                panel.setRenderBodyOnly(true);
                item.add(panel);
            }
        };
        items.setReuseItems(true);
        itemsTable.add(items);
        body.add(itemsTable);

        ListView<SceneDto> partialScenes = new ListView<>(ID_PARTIAL_SCENES,
                new PropertyModel<>(model, SceneDto.F_PARTIAL_SCENES)) {

            @Override
            protected void populateItem(ListItem<SceneDto> item) {
                ScenePanel panel = new ScenePanel(ID_PARTIAL_SCENE, item.getModel()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isOperationalItemsVisible() {
                        ScenePanel parentScenePanel = findParent(ScenePanel.class);
                        if (parentScenePanel != null) {
                            return parentScenePanel.isOperationalItemsVisible();
                        } else {
                            return ScenePanel.this.operationalItemsVisible;
                        }
                    }
                };
                panel.add(new VisibleBehaviour(() -> !isOperationalPartialScene(item.getModel()) || operationalItemsVisible));
                panel.setOutputMarkupPlaceholderTag(true);
                item.add(panel);
            }
        };
        partialScenes.setReuseItems(true);
        body.add(partialScenes);

        AjaxButton showOperationalItemsLink = new AjaxButton(ID_SHOW_OPERATIONAL_ITEMS_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setOperationalItemsVisible(!operationalItemsVisible);
                target.add(ScenePanel.this);
            }

            @Override
            public IModel<?> getBody() {
                return getShowOperationalItemsLinkLabel();
            }
        };
        showOperationalItemsLink.setOutputMarkupId(true);
        showOperationalItemsLink.add(AttributeAppender.append("style", "cursor: pointer;"));
        showOperationalItemsLink.add(new VisibleBehaviour(() -> showOperationalItems));
        body.add(showOperationalItemsLink);
    }

    private void onSortClicked(IModel<SceneDto> model, AjaxRequestTarget target) {
        model.getObject().setSorted(!model.getObject().isSorted());
        target.add(get(getPageBase().createComponentPath(ID_BOX, ID_BODY, ID_ITEMS_TABLE)));
        target.add(get(getPageBase().createComponentPath(ID_BOX, ID_BODY, ID_ITEMS_TABLE, ID_SORT_PROPERTIES)));
    }

    protected boolean isExistingViewableObject() {
        final Visualization scene = getModelObject().getScene();
        final PrismContainerValue<?> value = scene.getSourceValue();
        return value != null &&
                value.getParent() instanceof PrismObject &&
                WebComponentUtil.hasDetailsPage((PrismObject) value.getParent()) &&
                ((PrismObject) value.getParent()).getOid() != null &&
                (scene.getSourceDelta() == null || !scene.getSourceDelta().isAdd());
    }

    public void headerOnClickPerformed(AjaxRequestTarget target, IModel<SceneDto> model) {
        SceneDto dto = model.getObject();
        dto.setMinimized(!dto.isMinimized());
        target.add(this);
    }

    private class ChangeTypeModel implements IModel<String> {

        @Override
        public String getObject() {
            ChangeType changeType = getModel().getObject().getScene().getChangeType();
            if (changeType == null) {
                return "";
            }
            return WebComponentUtil.createLocalizedModelForEnum(changeType, ScenePanel.this).getObject();
        }
    }

    private class ObjectTypeModel implements IModel<String> {

        @Override
        public String getObject() {
            Visualization scene = getModel().getObject().getScene();
            PrismContainerDefinition<?> def = scene.getSourceDefinition();
            if (def == null) {
                return "";
            }
            if (def instanceof PrismObjectDefinition) {
                return PageBase.createStringResourceStatic(SchemaConstants.OBJECT_TYPE_KEY_PREFIX + def.getTypeName().getLocalPart()).getObject();
            } else {
                return "";
            }
        }
    }

    private void setOperationalItemsVisible(boolean operationalItemsVisible) {
        this.operationalItemsVisible = operationalItemsVisible;
    }

    protected boolean isOperationalItemsVisible() {
        return operationalItemsVisible;
    }

    private IModel<?> getShowOperationalItemsLinkLabel() {
        return operationalItemsVisible ? PageBase.createStringResourceStatic("ScenePanel.hideOperationalItemsLink")
                : PageBase.createStringResourceStatic("ScenePanel.showOperationalItemsLink");
    }

    private boolean isOperationalPartialScene(IModel<SceneDto> sceneDtoModel) {
        if (sceneDtoModel == null || sceneDtoModel.getObject() == null) {
            return false;
        }
        return sceneDtoModel.getObject().getScene().isOperational();
    }

    private boolean isOperationalItem(IModel<SceneItemDto> sceneDtoModel) {
        if (sceneDtoModel == null || sceneDtoModel.getObject() == null) {
            return false;
        }
        return sceneDtoModel.getObject().isOperational();
    }

    private boolean isAutorized() {
        Visualization scene = getModelObject().getScene();
        PrismContainerValue<?> value = scene.getSourceValue();
        if (value == null || !(value.getParent() instanceof PrismObject)) {
            return true;
        }

        Class<? extends ObjectType> clazz = ((PrismObject<? extends ObjectType>) value.getParent()).getCompileTimeClass();

        return WebComponentUtil.isAuthorized(clazz);
    }
}
