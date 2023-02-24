/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.IconComponent;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class VisualizationPanel extends BasePanel<VisualizationDto> {

    private static final long serialVersionUID = 1L;

    public static final String ID_ICON = "icon";
    public static final String ID_OVERVIEW = "overview";
    public static final String ID_MINIMIZE = "minimize";
    private static final String ID_HEADER_PANEL = "headerPanel";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_WRAPPER_DISPLAY_NAME = "wrapperDisplayName";
    private static final String ID_NAME_LINK = "nameLink";
    private static final String ID_CHANGE_TYPE = "changeType";
    private static final String ID_OBJECT_TYPE = "objectType";
    private static final String ID_BODY = "body";
    private static final String ID_WARNING = "warning";
    private static final String ID_VISUALIZATION = "visualization";

    private final boolean advanced;

    private final boolean showOperationalItems;
    private boolean operationalItemsVisible = false;

    private IModel<String> overviewModel;

    public VisualizationPanel(String id, @NotNull IModel<VisualizationDto> model) {
        this(id, model, false, true);
    }

    public VisualizationPanel(String id, @NotNull IModel<VisualizationDto> model, boolean showOperationalItems, boolean advanced) {
        super(id, model);

        this.advanced = advanced;
        this.showOperationalItems = showOperationalItems;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initModels();
        initLayout();

        if (!advanced && overviewModel.getObject() != null) {
            getModelObject().setMinimized(true);
        }
    }

    private void initModels() {
        overviewModel = () -> {
            Visualization visualization = getModelObject().getVisualization();
            if (visualization == null || visualization.getName() == null) {
                return null;
            }

            LocalizableMessage msg = visualization.getName().getOverview();
            String translated = msg != null ? LocalizationUtil.translateMessage(msg) : null;
            if (translated == null) {
                return null;
            }

            // only allow <b>XXX</b> to be unescaped to allow some form of highlighting
            translated = Strings.escapeMarkup(translated).toString();
            translated = translated.replaceAll("&lt;b&gt;", "<b>");
            translated = translated.replaceAll("&lt;/b&gt;", "</b>");

            return translated;
        };
    }

    private void initLayout() {
        setOutputMarkupId(true);

        add(AttributeAppender.append("class", "card card-outline-left"));
        add(AttributeModifier.append("class", () -> {
            VisualizationDto dto = getModelObject();

            if (dto.getBoxClassOverride() != null) {
                return dto.getBoxClassOverride();
            }

            ChangeType change = dto.getChangeType();

            return change != null ? VisualizationUtil.createChangeTypeCssClassForOutlineCard(change) : null;
        }));

        final VisibleBehaviour visibleIfNotWrapper = new VisibleBehaviour(() -> !getModelObject().isWrapper());
        final VisibleBehaviour visibleIfWrapper = new VisibleBehaviour(() -> getModelObject().isWrapper());

        final IModel<VisualizationDto> model = getModel();

        final WebMarkupContainer headerPanel = new WebMarkupContainer(ID_HEADER_PANEL);
        headerPanel.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                headerOnClickPerformed(target, model);
            }
        });
        add(headerPanel);

        IModel<String> iconModel = () -> {
            Visualization visualization = getModelObject().getVisualization();
            if (visualization == null || visualization.getSourceValue() == null) {
                return null;
            }

            PrismContainerValue value = visualization.getSourceValue();
            QName type = value.getTypeName();
            String icon = WebComponentUtil.createDefaultBlackIcon(type);

            return StringUtils.isNotEmpty(icon) ? "mr-1 " + icon : null;
        };

        IconComponent icon = new IconComponent(ID_ICON, iconModel);
        icon.add(new VisibleBehaviour(() -> iconModel.getObject() != null));
        headerPanel.add(icon);

        final Label overview = new Label(ID_OVERVIEW, overviewModel);
        overview.setEscapeModelStrings(false);
        overview.add(new VisibleBehaviour(() -> overviewModel.getObject() != null));
        headerPanel.add(overview);

        WebMarkupContainer fullDescription = new WebMarkupContainer("fullDescription");
        fullDescription.add(new VisibleBehaviour(() -> overviewModel.getObject() == null));
        headerPanel.add(fullDescription);

        final Label wrapperDisplayName = new Label(ID_WRAPPER_DISPLAY_NAME, () -> {
            WrapperVisualization visualization = ((WrapperVisualization) getModelObject().getVisualization());
            return LocalizationUtil.translateMessage(visualization.getName().getDisplayName());
        });
        wrapperDisplayName.add(visibleIfWrapper);
        fullDescription.add(wrapperDisplayName);

        final Label changeType = new Label(ID_CHANGE_TYPE, new ChangeTypeModel(getModel()));
        changeType.add(visibleIfNotWrapper);
        fullDescription.add(changeType);

        final Label objectType = new Label(ID_OBJECT_TYPE, new ObjectTypeModel(getModel()));
        objectType.add(visibleIfNotWrapper);
        fullDescription.add(objectType);

        final AjaxButton nameLink = new AjaxButton(ID_NAME_LINK, () -> getModelObject().getName()) {

            @Override
            protected void disableLink(ComponentTag tag) {
                super.disableLink(tag);

                tag.setName("span");
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.STOP);
                attributes.setPreventDefault(true);
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                PrismContainerValue<?> value = VisualizationPanel.this.getModelObject().getVisualization().getSourceValue();
                if (value != null && value.getParent() instanceof PrismObject) {
                    PrismObject<? extends ObjectType> object = (PrismObject<? extends ObjectType>) value.getParent();
                    WebComponentUtil.dispatchToObjectDetailsPage(ObjectTypeUtil.createObjectRef(object, getPageBase().getPrismContext()), getPageBase(), false);
                }
            }
        };
        nameLink.add(new VisibleEnableBehaviour(() -> !getModelObject().isWrapper(), () -> isExistingViewableObject() && isAutorized()));
        fullDescription.add(nameLink);

        final Label description = new Label(ID_DESCRIPTION, () -> getModelObject().getDescription());
        description.add(visibleIfNotWrapper);
        fullDescription.add(description);

        final Label warning = new Label(ID_WARNING);
        warning.add(new VisibleBehaviour(() -> getModelObject().getVisualization().isBroken()));
        warning.add(new TooltipBehavior());
        headerPanel.add(warning);

        final AjaxIconButton minimize = new AjaxIconButton(ID_MINIMIZE,
                () -> getModelObject().isMinimized() ? GuiStyleConstants.CLASS_ICON_EXPAND : GuiStyleConstants.CLASS_ICON_COLLAPSE,
                () -> getModelObject().isMinimized() ? getString("prismOptionButtonPanel.maximize") : getString("prismOptionButtonPanel.minimize")) {

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.STOP);
                attributes.setPreventDefault(true);
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                headerOnClickPerformed(target, VisualizationPanel.this.getModel());
            }
        };
        minimize.add(new VisibleBehaviour(() -> !getModelObject().getVisualization().isEmpty()));
        headerPanel.add(minimize);

        final WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.add(new VisibleBehaviour(() -> {
            VisualizationDto dto = getModelObject();
            return !dto.isMinimized() && (!dto.getItems().isEmpty() || !dto.getPartialVisualizations().isEmpty());
        }));
        add(body);

        final SimpleVisualizationPanel visualization = new SimpleVisualizationPanel(ID_VISUALIZATION, getModel(), showOperationalItems, advanced);
        visualization.setRenderBodyOnly(true);
        body.add(visualization);
    }

    protected boolean isExistingViewableObject() {
        final Visualization visualization = getModelObject().getVisualization();
        final PrismContainerValue<?> value = visualization.getSourceValue();

        if (value == null || !(value.getParent() instanceof PrismObject)) {
            return false;
        }

        PrismObject obj = (PrismObject) value.getParent();

        return WebComponentUtil.hasDetailsPage(obj) &&
                obj.getOid() != null && (visualization.getSourceDelta() == null || !visualization.getSourceDelta().isAdd());
    }

    public void headerOnClickPerformed(AjaxRequestTarget target, IModel<VisualizationDto> model) {
        VisualizationDto dto = model.getObject();
        dto.setMinimized(!dto.isMinimized());
        target.add(this);
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

    private static class ChangeTypeModel implements IModel<String> {

        private IModel<VisualizationDto> visualization;

        public ChangeTypeModel(@NotNull IModel<VisualizationDto> visualization) {
            this.visualization = visualization;
        }

        @Override
        public String getObject() {
            ChangeType changeType = visualization.getObject().getVisualization().getChangeType();
            if (changeType == null) {
                return null;
            }

            return LocalizationUtil.translate(WebComponentUtil.createEnumResourceKey(changeType));
        }
    }

    private static class ObjectTypeModel implements IModel<String> {

        private IModel<VisualizationDto> visualization;

        public ObjectTypeModel(@NotNull IModel<VisualizationDto> visualization) {
            this.visualization = visualization;
        }

        @Override
        public String getObject() {
            PrismContainerDefinition<?> def = visualization.getObject().getVisualization().getSourceDefinition();
            if (!(def instanceof PrismObjectDefinition)) {
                return null;
            }

            return LocalizationUtil.translate(SchemaConstants.OBJECT_TYPE_KEY_PREFIX + def.getTypeName().getLocalPart());
        }
    }
}
