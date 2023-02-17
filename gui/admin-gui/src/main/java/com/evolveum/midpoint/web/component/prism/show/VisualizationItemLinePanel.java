/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.gui.api.component.IconComponent;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class VisualizationItemLinePanel extends BasePanel<VisualizationItemLineDto> {

    private static final long serialVersionUID = 1L;

    private static final String ID_NAME_CONTAINER = "nameContainer";
    private static final String ID_NAME = "name";
    private static final String ID_OLD_VALUE_CONTAINER = "oldValueContainer";
    private static final String ID_OLD_VALUE_IMAGE = "oldValueImage";
    private static final String ID_OLD_VALUE = "oldValue";
    private static final String ID_NEW_VALUE_CONTAINER = "newValueContainer";
    private static final String ID_NEW_VALUE_IMAGE = "newValueImage";
    private static final String ID_NEW_VALUE = "newValue";

    public VisualizationItemLinePanel(String id, IModel<VisualizationItemLineDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer nameCell = new WebMarkupContainer(ID_NAME_CONTAINER);
        nameCell.add(new AttributeModifier("rowspan", () -> getModelObject().getNumberOfLines()));

        Label label = new Label(ID_NAME, createStringResource("${name}", getModel()));
        nameCell.add(label);
        nameCell.add(new VisibleBehaviour(() -> getModelObject().isFirst()));
        add(nameCell);

        WebMarkupContainer oldValueCell = new WebMarkupContainer(ID_OLD_VALUE_CONTAINER);
        oldValueCell.add(new VisibleBehaviour(() -> getModelObject().isNullEstimatedOldValues() || getModelObject().isDelta()));

        Component sivp;
        if (getModelObject().isNullEstimatedOldValues()) {
            sivp = new Label(ID_OLD_VALUE, createStringResource("SceneItemLinePanel.unknownLabel"));
        } else {
            sivp = new VisualizationItemValuePanel(ID_OLD_VALUE, () -> getModel().getObject().getOldValue());
        }
        sivp.setRenderBodyOnly(true);
        oldValueCell.add(sivp);

        IconComponent oldValueImagePanel = new IconComponent(ID_OLD_VALUE_IMAGE, Model.of(GuiStyleConstants.CLASS_MINUS_CIRCLE_DANGER),
                createStringResource("SceneItemLinePanel.removedValue"));
        oldValueImagePanel.add(new VisibleBehaviour(
                () -> {
                    VisualizationItemValue val = getModelObject().getOldValue();
                    return val != null && val.getSourceValue() != null && !getModelObject().isNullEstimatedOldValues();
                }));
        oldValueCell.add(oldValueImagePanel);

        add(oldValueCell);

        IModel<String> newValueIconModel;
        IModel<String> newValueTitleModel;
        if (getModelObject().isNullEstimatedOldValues()) {
            if (getModelObject().isAdd()) {
                newValueIconModel = Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE_SUCCESS);
                newValueTitleModel = createStringResource("SceneItemLinePanel.addedValue");
            } else if (getModelObject().isDelete()) {
                newValueIconModel = Model.of(GuiStyleConstants.CLASS_MINUS_CIRCLE_DANGER);
                newValueTitleModel = createStringResource("SceneItemLinePanel.removedValue");
            } else if (getModelObject().isReplace()) {
                newValueIconModel = Model.of(GuiStyleConstants.CLASS_CIRCLE_FULL);
                newValueTitleModel = createStringResource("SceneItemLinePanel.unchangedValue");
            } else {
                newValueIconModel = Model.of("");
                newValueTitleModel = Model.of("");
            }
        } else {
            if (getModelObject().isDescriptive()) {
                newValueIconModel = Model.of("");
                newValueTitleModel = Model.of("");
            } else {
                newValueIconModel = !getModelObject().isDelta() && getModelObject().isDeltaVisualization() ?
                        Model.of(GuiStyleConstants.CLASS_CIRCLE_FULL) :
                        Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE_SUCCESS);
                newValueTitleModel = !getModelObject().isDelta() && getModelObject().isDeltaVisualization() ?
                        createStringResource("SceneItemLinePanel.unchangedValue")
                        : createStringResource("SceneItemLinePanel.addedValue");
            }
        }

        WebMarkupContainer newValueCell = new WebMarkupContainer(ID_NEW_VALUE_CONTAINER);
        sivp = new VisualizationItemValuePanel(ID_NEW_VALUE, () -> getModel().getObject().getNewValue());
        sivp.setRenderBodyOnly(true);
        newValueCell.add(sivp);
        newValueCell.add(AttributeModifier.replace("colspan",
                () -> !getModelObject().isDelta() && !getModelObject().isNullEstimatedOldValues() && getModelObject().isDeltaVisualization() ? 2 : 1));
        newValueCell.add(AttributeModifier.replace("align",
                () -> !getModelObject().isDelta() && !getModelObject().isNullEstimatedOldValues() && getModelObject().isDeltaVisualization() ? "center" : null));

        IconComponent newValueImagePanel = new IconComponent(ID_NEW_VALUE_IMAGE, newValueIconModel, newValueTitleModel);
        newValueImagePanel.add(new VisibleBehaviour(() -> {
            VisualizationItemValue val = getModelObject().getNewValue();
            return val != null && val.getSourceValue() != null;
        }));
        newValueCell.add(newValueImagePanel);

        add(newValueCell);
    }
}
