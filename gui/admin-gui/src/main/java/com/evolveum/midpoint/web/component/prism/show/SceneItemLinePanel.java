/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class SceneItemLinePanel extends BasePanel<SceneItemLineDto> {

    private static final String ID_NAME_CONTAINER = "nameContainer";
    private static final String ID_NAME = "name";
    private static final String ID_OLD_VALUE_CONTAINER = "oldValueContainer";
    private static final String ID_OLD_VALUE_IMAGE = "oldValueImage";
    private static final String ID_OLD_VALUE = "oldValue";
    private static final String ID_NEW_VALUE_CONTAINER = "newValueContainer";
    private static final String ID_NEW_VALUE_IMAGE = "newValueImage";
    private static final String ID_NEW_VALUE = "newValue";

    private static final Trace LOGGER = TraceManager.getTrace(SceneItemLinePanel.class);

    public SceneItemLinePanel(String id, IModel<SceneItemLineDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer nameCell = new WebMarkupContainer(ID_NAME_CONTAINER);
        nameCell.add(new AttributeModifier("rowspan",
                new PropertyModel<Integer>(getModel(), SceneItemLineDto.F_NUMBER_OF_LINES)));

        Label label = new Label(ID_NAME, createStringResource("${name}", getModel()));
        nameCell.add(label);
        nameCell.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModelObject().isFirst();
            }
        });
        add(nameCell);

        WebMarkupContainer oldValueCell = new WebMarkupContainer(ID_OLD_VALUE_CONTAINER);
        oldValueCell.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModelObject().isNullEstimatedOldValues() || getModelObject().isDelta();
            }
        });
        Component sivp;
        if (getModelObject().isNullEstimatedOldValues()){
            sivp = new Label(ID_OLD_VALUE, createStringResource("SceneItemLinePanel.unknownLabel"));
        } else {
            sivp = new SceneItemValuePanel(ID_OLD_VALUE,
                new PropertyModel<>(getModel(), SceneItemLineDto.F_OLD_VALUE));
        }
        sivp.setRenderBodyOnly(true);
        oldValueCell.add(sivp);

        ImagePanel oldValueImagePanel = new ImagePanel(ID_OLD_VALUE_IMAGE, Model.of(GuiStyleConstants.CLASS_MINUS_CIRCLE_DANGER),
                createStringResource("SceneItemLinePanel.removedValue"));
        oldValueImagePanel.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return getModelObject().getOldValue() != null &&
                        getModelObject().getOldValue().getSourceValue() != null &&
                        !getModelObject().isNullEstimatedOldValues();
            }
        });
        oldValueCell.add(oldValueImagePanel);

        add(oldValueCell);

        IModel<String> newValueIconModel;
        IModel<String> newValueTitleModel;
        if (getModelObject().isNullEstimatedOldValues()){
            if (getModelObject().isAdd()){
                newValueIconModel = Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE_SUCCESS);
                newValueTitleModel = createStringResource("SceneItemLinePanel.addedValue");
            } else if (getModelObject().isDelete()){
                newValueIconModel = Model.of(GuiStyleConstants.CLASS_MINUS_CIRCLE_DANGER);
                newValueTitleModel = createStringResource("SceneItemLinePanel.removedValue");
            } else if (getModelObject().isReplace()){
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
                newValueIconModel = !getModelObject().isDelta() && getModelObject().isDeltaScene() ?
                        Model.of(GuiStyleConstants.CLASS_CIRCLE_FULL) :
                        Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE_SUCCESS);
                newValueTitleModel = !getModelObject().isDelta() && getModelObject().isDeltaScene() ?
                        createStringResource("SceneItemLinePanel.unchangedValue")
                        : createStringResource("SceneItemLinePanel.addedValue");
            }
        }

        WebMarkupContainer newValueCell = new WebMarkupContainer(ID_NEW_VALUE_CONTAINER);
        sivp = new SceneItemValuePanel(ID_NEW_VALUE,
            new PropertyModel<>(getModel(), SceneItemLineDto.F_NEW_VALUE));
        sivp.setRenderBodyOnly(true);
        newValueCell.add(sivp);
        newValueCell.add(new AttributeModifier("colspan", new IModel<Integer>() {
            @Override
            public Integer getObject() {
                return !getModelObject().isDelta() && !getModelObject().isNullEstimatedOldValues() && getModelObject().isDeltaScene() ? 2 : 1;
            }
        }));
        newValueCell.add(new AttributeModifier("align", new IModel<String>() {
            @Override
            public String getObject() {
                return !getModelObject().isDelta() && !getModelObject().isNullEstimatedOldValues() && getModelObject().isDeltaScene() ? "center" : null;
            }
        }));

        ImagePanel newValueImagePanel = new ImagePanel(ID_NEW_VALUE_IMAGE, newValueIconModel,
                newValueTitleModel);
        newValueImagePanel.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return getModelObject().getNewValue() != null &&
                        getModelObject().getNewValue().getSourceValue() != null;
            }
        });
//        newValueImagePanel.add(new AttributeAppender("style",
//                !getModelObject().isDelta() && getModelObject().isDeltaScene() ?
//                        ""
//                        : "float: left; margin-right: 5px;"));
        newValueCell.add(newValueImagePanel);

        add(newValueCell);
    }

}
