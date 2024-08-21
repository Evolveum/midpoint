/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.input.LifecycleStatePanel;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTilePanel;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

public class MappingTilePanel<C extends Containerable> extends TemplateTilePanel<PrismContainerValueWrapper<C>, MappingTile<PrismContainerValueWrapper<C>>> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingTilePanel.class);
    private static final String ID_CONFIGURE_BUTTON = "configureButton";
    private static final String ID_LIFECYCLE_STATE = "lifecycleState";
    private static final String ID_HELP = "help";
    private static final String ID_REMOVE_BUTTON = "removeButton";

    public MappingTilePanel(String id, IModel<MappingTile<PrismContainerValueWrapper<C>>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        getIcon().add(new VisibleBehaviour(MappingTilePanel.this::isIconVisible));

        initLifeCycleStatePanel();
    }

    protected boolean isIconVisible() {
        return true;
    }

    private void initLifeCycleStatePanel() {
//        try {
            IModel<PrismPropertyWrapper<String>> model = () -> {
                try {
                    return getModelObject().getValue().findProperty(MappingType.F_LIFECYCLE_STATE);
                } catch (SchemaException e) {
                    LOGGER.debug("Couldn't find property " + MappingType.F_LIFECYCLE_STATE);
                }
                return null;
            };

            add(new LifecycleStatePanel(ID_LIFECYCLE_STATE, model));
    }

    @Override
    protected void initLayout() {
        super.initLayout();

        add(AttributeAppender.replace(
                "class",
                "card selectable col-12 catalog-tile-panel d-flex flex-column align-items-center px-3 pb-3 pt-3 h-100 mb-0 btn"));

        boolean isConfigurable = true;
        if (getModelObject().getValue().getItems().size() == 1) {
            @NotNull ItemName itemName = getModelObject().getValue().getItems().iterator().next().getItemName();
            if (itemName.equivalent(MappingType.F_LIFECYCLE_STATE)) {
                isConfigurable = false;
            }
        }

        boolean finalIsConfigurable = isConfigurable;
        AjaxIconButton configureButton = new AjaxIconButton(
                ID_CONFIGURE_BUTTON,
                Model.of("fa fa-gear"),
                createStringResource("MappingTilePanel.button.settings")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onConfigureClick(target, MappingTilePanel.this.getModelObject());
            }

            @Override
            protected void onInitialize() {
                super.onInitialize();

                if (!finalIsConfigurable) {
                    add(AttributeAppender.replace(
                            "title",
                            PageBase.createStringResourceStatic("MappingTilePanel.disabledConfiguration")));
                }
            }
        };
        configureButton.showTitleAsLabel(true);
        if (!isConfigurable) {
            configureButton.add(AttributeAppender.append("class", "disabled"));
        }
        add(configureButton);

        Label help = new Label(ID_HELP);
        IModel<String> helpModel = new PropertyModel<>(getModel(), "help");
        help.add(AttributeModifier.replace("title", createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
        help.add(new InfoTooltipBehavior());
        help.add(new VisibleBehaviour(this::isHelpTextVisible));
        add(help);

        AjaxIconButton removeButton = new AjaxIconButton(
                ID_REMOVE_BUTTON,
                Model.of("fa fa-trash"),
                createStringResource("MappingTilePanel.button.remove")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onRemovePerformed(MappingTilePanel.this.getModelObject().getValue(), target);
            }
        };
        removeButton.showTitleAsLabel(true);
        add(removeButton);
    }

    protected void onRemovePerformed(PrismContainerValueWrapper<? extends Containerable> value, AjaxRequestTarget target) {
        if (value.getStatus() == ValueStatus.ADDED) {
            PrismContainerWrapper wrapper = value.getParent();
            if (wrapper != null) {
                wrapper.getValues().remove(value);
            }
        } else {
            value.setStatus(ValueStatus.DELETED);
        }
    }

    protected boolean isHelpTextVisible() {
        return getModelObject() != null && StringUtils.isNotEmpty(getModelObject().getHelp());
    }

    protected <T extends PrismContainerValueWrapper<? extends Containerable>> void onConfigureClick(AjaxRequestTarget target, MappingTile<T> modelObject) {
    }

    @Override
    protected boolean addClickBehaviour() {
        return false;
    }
}
