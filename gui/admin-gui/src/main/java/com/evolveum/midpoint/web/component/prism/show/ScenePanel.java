/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
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
    public static final String ID_OPTION_BUTTONS = "optionButtons";
    public static final String ID_HEADER_PANEL = "headerPanel";
    public static final String ID_HEADER_DESCRIPTION = "description";
    public static final String ID_HEADER_WRAPPER_DISPLAY_NAME = "wrapperDisplayName";
    public static final String ID_HEADER_NAME_LABEL = "nameLabel";
    public static final String ID_HEADER_NAME_LINK = "nameLink";
    public static final String ID_HEADER_CHANGE_TYPE = "changeType";
    public static final String ID_HEADER_OBJECT_TYPE = "objectType";
    public static final String ID_BODY = "body";
    public static final String ID_OLD_VALUE_LABEL = "oldValueLabel";
    public static final String ID_NEW_VALUE_LABEL = "newValueLabel";
    public static final String ID_VALUE_LABEL = "valueLabel";

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
    protected void onInitialize(){
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
        box.add(AttributeModifier.append("class", new IModel<String>() {

            @Override
            public String getObject() {
                SceneDto dto = model.getObject();

                if (dto.getBoxClassOverride() != null) {
                    return dto.getBoxClassOverride();
                }

                if (dto.getChangeType() == null) {
                    return null;
                }

                switch (dto.getChangeType()) {
                    case ADD:
                        return "box-success";
                    case DELETE:
                        return "box-danger";
                    case MODIFY:
                        return "box-info";
                    default:
                        return null;
                }
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
        //headerChangeType.setRenderBodyOnly(true);
        Label headerObjectType = new Label(ID_HEADER_OBJECT_TYPE, new ObjectTypeModel());
        //headerObjectType.setRenderBodyOnly(true);

        IModel<String> nameModel = new IModel<String>() {
            @Override
            public String getObject() {
                return model.getObject().getName(ScenePanel.this);
            }
        };
        Label headerNameLabel = new Label(ID_HEADER_NAME_LABEL, nameModel);
        LinkPanel headerNameLink = new LinkPanel(ID_HEADER_NAME_LINK, nameModel) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PrismContainerValue<?> value = getModelObject().getScene().getSourceValue();
                if (value != null && value.getParent() instanceof PrismObject) {
                    PrismObject<? extends ObjectType> object = (PrismObject<? extends ObjectType>) value.getParent();
                    WebComponentUtil.dispatchToObjectDetailsPage(ObjectTypeUtil.createObjectRef(object, getPageBase().getPrismContext()), getPageBase(), false);
                }
            }
        };
        Label headerDescription = new Label(ID_HEADER_DESCRIPTION, new IModel<String>() {
            @Override
            public String getObject() {
                return model.getObject().getDescription(ScenePanel.this);
            }
        });
        Label headerWrapperDisplayName = new Label(ID_HEADER_WRAPPER_DISPLAY_NAME,
                new IModel<String>() {
                    @Override
                    public String getObject() {
                        String key = ((WrapperScene) getModelObject().getScene()).getDisplayNameKey();
                        Object[] parameters = ((WrapperScene) getModelObject().getScene()).getDisplayNameParameters();
                        return new StringResourceModel(key, this).setModel(null)
                                .setDefaultValue(key)
                                .setParameters(parameters).getObject();
                    }
                });

        headerPanel.add(headerChangeType);
        headerPanel.add(headerObjectType);
        headerPanel.add(headerNameLabel);
        headerPanel.add(headerNameLink);
        headerPanel.add(headerDescription);
        headerPanel.add(headerWrapperDisplayName);

        headerChangeType.add(createHeaderOnClickBehaviour(model));
        headerObjectType.add(createHeaderOnClickBehaviour(model));
        headerNameLabel.add(createHeaderOnClickBehaviour(model));
        headerDescription.add(createHeaderOnClickBehaviour(model));
        headerWrapperDisplayName.add(createHeaderOnClickBehaviour(model));

        VisibleEnableBehaviour visibleIfNotWrapper = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !getModelObject().isWrapper();
            }
        };
        VisibleEnableBehaviour visibleIfWrapper = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModelObject().isWrapper();
            }
        };
        VisibleEnableBehaviour visibleIfExistingObjectAndAuthorized = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                if (getModelObject().isWrapper()) {
                    return false;
                }
                return isExistingViewableObject() && isAutorized();
            }
        };
        VisibleEnableBehaviour visibleIfNotWrapperAndNotExistingObjectAndNotAuthorized = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                if (getModelObject().isWrapper()) {
                    return false;
                }
                return !isExistingViewableObject() || !isAutorized();
            }
        };
        headerChangeType.add(visibleIfNotWrapper);
        headerObjectType.add(visibleIfNotWrapper);
        headerNameLabel.add(visibleIfNotWrapperAndNotExistingObjectAndNotAuthorized);
        headerNameLink.add(visibleIfExistingObjectAndAuthorized);
        headerDescription.add(visibleIfNotWrapper);
        headerWrapperDisplayName.add(visibleIfWrapper);

        WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                SceneDto wrapper = model.getObject();
                return !wrapper.isMinimized();
            }
        });
        box.add(body);

        WebMarkupContainer itemsTable = new WebMarkupContainer(ID_ITEMS_TABLE);
        itemsTable.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !model.getObject().getItems().isEmpty();
            }
        });
        WebMarkupContainer oldValueLabel = new WebMarkupContainer(ID_OLD_VALUE_LABEL);
        oldValueLabel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return model.getObject().containsDeltaItems();
            }
        });
        itemsTable.add(oldValueLabel);
        WebMarkupContainer newValueLabel = new WebMarkupContainer(ID_NEW_VALUE_LABEL);
        newValueLabel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return model.getObject().containsDeltaItems();
            }
        });
        itemsTable.add(newValueLabel);
        WebMarkupContainer valueLabel = new WebMarkupContainer(ID_VALUE_LABEL);
        valueLabel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !model.getObject().containsDeltaItems();
            }
        });
        itemsTable.add(valueLabel);
        ListView<SceneItemDto> items = new ListView<SceneItemDto>(ID_ITEMS,
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

        ListView<SceneDto> partialScenes = new ListView<SceneDto>(ID_PARTIAL_SCENES,
            new PropertyModel<>(model, SceneDto.F_PARTIAL_SCENES)) {

            @Override
            protected void populateItem(ListItem<SceneDto> item) {
                ScenePanel panel = new ScenePanel(ID_PARTIAL_SCENE, item.getModel()){
                    private static final long serialVersionUID = 1L;
                    @Override
                    protected boolean isOperationalItemsVisible(){
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

    protected boolean isExistingViewableObject() {
        final Scene scene = getModelObject().getScene();
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
            Scene scene = getModel().getObject().getScene();
            PrismContainerDefinition<?> def = scene.getSourceDefinition();
            if (def == null) {
                return "";
            }
            if (def instanceof PrismObjectDefinition) {
                return PageBase.createStringResourceStatic(ScenePanel.this, SchemaConstants.OBJECT_TYPE_KEY_PREFIX +def.getTypeName().getLocalPart()).getObject();
            } else {
                return "";
            }
        }
    }

    private void setOperationalItemsVisible(boolean operationalItemsVisible){
        this.operationalItemsVisible = operationalItemsVisible;
    }

    protected boolean isOperationalItemsVisible(){
        return operationalItemsVisible;
    }

    private IModel<?> getShowOperationalItemsLinkLabel(){
        return operationalItemsVisible ? PageBase.createStringResourceStatic(ScenePanel.this, "ScenePanel.hideOperationalItemsLink")
                : PageBase.createStringResourceStatic(ScenePanel.this, "ScenePanel.showOperationalItemsLink");
    }

    private boolean isOperationalPartialScene(IModel<SceneDto> sceneDtoModel){
        if (sceneDtoModel == null || sceneDtoModel.getObject() == null){
            return false;
        }
        return sceneDtoModel.getObject().getScene().isOperational();
    }

    private boolean isOperationalItem(IModel<SceneItemDto> sceneDtoModel){
        if (sceneDtoModel == null || sceneDtoModel.getObject() == null){
            return false;
        }
        return sceneDtoModel.getObject().isOperational();
    }

    private boolean isAutorized() {
        Scene scene = getModelObject().getScene();
        PrismContainerValue<?> value = scene.getSourceValue();
        if (value == null || !(value.getParent() instanceof PrismObject)) {
            return true;
        }

        Class<? extends ObjectType> clazz = ((PrismObject<? extends ObjectType>) value.getParent()).getCompileTimeClass();

        return WebComponentUtil.isAuthorized(clazz);
    }
}
