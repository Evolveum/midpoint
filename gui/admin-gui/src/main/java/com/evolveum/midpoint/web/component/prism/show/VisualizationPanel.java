/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization.VisualizationGuiUtil;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class VisualizationPanel extends BasePanel<VisualizationDto> {

    private static final long serialVersionUID = 1L;

    private static final String ID_OPTION_BUTTONS = "optionButtons";
    private static final String ID_HEADER_PANEL = "headerPanel";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_WRAPPER_DISPLAY_NAME = "wrapperDisplayName";
    private static final String ID_NAME_LABEL = "nameLabel";
    private static final String ID_NAME_LINK = "nameLink";
    private static final String ID_CHANGE_TYPE = "changeType";
    private static final String ID_OBJECT_TYPE = "objectType";
    private static final String ID_BODY = "body";
    private static final String ID_WARNING = "warning";
    private static final String ID_VISUALIZATION = "visualization";

    private final boolean showOperationalItems;
    private boolean operationalItemsVisible = false;

    public VisualizationPanel(String id, @NotNull IModel<VisualizationDto> model) {
        this(id, model, false);
    }

    public VisualizationPanel(String id, @NotNull IModel<VisualizationDto> model, boolean showOperationalItems) {
        super(id, model);

        this.showOperationalItems = showOperationalItems;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setOutputMarkupId(true);
        initLayout();
    }

    private AjaxEventBehavior createHeaderOnClickBehaviour(final IModel<VisualizationDto> model) {
        return new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                headerOnClickPerformed(target, model);
            }
        };
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "card card-outline-left"));

        final IModel<VisualizationDto> model = getModel();

        add(AttributeModifier.append("class", () -> {
            VisualizationDto dto = model.getObject();

            if (dto.getBoxClassOverride() != null) {
                return dto.getBoxClassOverride();
            }

            ChangeType change = dto.getChangeType();

            return change != null ? VisualizationGuiUtil.createChangeTypeCssClassForOutlineCard(change) : null;
        }));

        WebMarkupContainer headerPanel = new WebMarkupContainer(ID_HEADER_PANEL);
        add(headerPanel);

        headerPanel.add(new VisualizationButtonPanel(ID_OPTION_BUTTONS, model) {
            @Override
            public void minimizeOnClick(AjaxRequestTarget target) {
                headerOnClickPerformed(target, model);
            }
        });

        Label headerChangeType = new Label(ID_CHANGE_TYPE, new ChangeTypeModel());
        Label headerObjectType = new Label(ID_OBJECT_TYPE, new ObjectTypeModel());

        IModel<String> nameModel = () -> model.getObject().getName(VisualizationPanel.this);

        Label headerNameLabel = new Label(ID_NAME_LABEL, nameModel);
        AjaxLinkPanel headerNameLink = new AjaxLinkPanel(ID_NAME_LINK, nameModel) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PrismContainerValue<?> value = getModelObject().getVisualization().getSourceValue();
                if (value != null && value.getParent() instanceof PrismObject) {
                    PrismObject<? extends ObjectType> object = (PrismObject<? extends ObjectType>) value.getParent();
                    WebComponentUtil.dispatchToObjectDetailsPage(ObjectTypeUtil.createObjectRef(object, getPageBase().getPrismContext()), getPageBase(), false);
                }
            }
        };
        Label headerDescription = new Label(ID_DESCRIPTION, () -> model.getObject().getDescription(VisualizationPanel.this));
        Label headerWrapperDisplayName = new Label(ID_WRAPPER_DISPLAY_NAME, () -> {
            WrapperVisualization visualization = ((WrapperVisualization) getModelObject().getVisualization());
            String key = visualization.getDisplayNameKey();
            Object[] parameters = visualization.getDisplayNameParameters();
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
        warning.add(new VisibleBehaviour(() -> getModelObject().getVisualization().isBroken()));
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
            VisualizationDto dto = getModelObject();
            return !dto.isMinimized() && (!dto.getItems().isEmpty() || !dto.getPartialVisualizations().isEmpty());
        }));
        add(body);

        SimpleVisualizationPanel visualization = new SimpleVisualizationPanel(ID_VISUALIZATION, getModel(), showOperationalItems);
        visualization.setRenderBodyOnly(true);
        body.add(visualization);
    }

    protected boolean isExistingViewableObject() {
        final Visualization visualization = getModelObject().getVisualization();
        final PrismContainerValue<?> value = visualization.getSourceValue();
        return value != null &&
                value.getParent() instanceof PrismObject &&
                WebComponentUtil.hasDetailsPage((PrismObject) value.getParent()) &&
                ((PrismObject) value.getParent()).getOid() != null &&
                (visualization.getSourceDelta() == null || !visualization.getSourceDelta().isAdd());
    }

    public void headerOnClickPerformed(AjaxRequestTarget target, IModel<VisualizationDto> model) {
        VisualizationDto dto = model.getObject();
        dto.setMinimized(!dto.isMinimized());
        target.add(this);
    }

    private class ChangeTypeModel implements IModel<String> {

        @Override
        public String getObject() {
            ChangeType changeType = getModel().getObject().getVisualization().getChangeType();
            if (changeType == null) {
                return "";
            }
            return WebComponentUtil.createLocalizedModelForEnum(changeType, VisualizationPanel.this).getObject();
        }
    }

    private class ObjectTypeModel implements IModel<String> {

        @Override
        public String getObject() {
            Visualization visualization = getModel().getObject().getVisualization();
            PrismContainerDefinition<?> def = visualization.getSourceDefinition();
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

    private boolean isAutorized() {
        Visualization visualization = getModelObject().getVisualization();
        PrismContainerValue<?> value = visualization.getSourceValue();
        if (value == null || !(value.getParent() instanceof PrismObject)) {
            return true;
        }

        Class<? extends ObjectType> clazz = ((PrismObject<? extends ObjectType>) value.getParent()).getCompileTimeClass();

        return WebComponentUtil.isAuthorized(clazz);
    }
}
