/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangePanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class VariableBindingDefinitionTypePanel extends BasePanel<VariableBindingDefinitionType> {

    private static final String ID_PATH_PANEL = "pathPanel";
    private static final String ID_RANGE = "range";
    private static final String ID_RANGE_PANEL = "rangePanel";
    private final boolean showRangePanel;
    private final IModel<PrismContainerValueWrapper<MappingType>> mappingValueModel;

    public VariableBindingDefinitionTypePanel(String id, IModel<VariableBindingDefinitionType> model) {
        this(id, model, false, null);
    }

    public VariableBindingDefinitionTypePanel(
            String id,
            IModel<VariableBindingDefinitionType> model,
            boolean showRangePanel,
            IModel<PrismContainerValueWrapper<MappingType>> mappingValueModel) {
        super(id, model);
        this.showRangePanel = showRangePanel;
        this.mappingValueModel = mappingValueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(AttributeModifier.append("class", "d-flex flex-column"));

        ItemPathPanel pathPanel = new ItemPathPanel(ID_PATH_PANEL, createPathModel()) {

            @Override
            protected void onUpdate(ItemPathDto itemPathDto) {
                ItemPath newPath = getModelObject().toItemPath();
                ItemPathType newPathtype = null;
                if (newPath != null) {
                    newPathtype = new ItemPathType(newPath);
                }

                VariableBindingDefinitionType var = VariableBindingDefinitionTypePanel.this.getModelObject();
                if (var == null) {
                    var = new VariableBindingDefinitionType();
                    VariableBindingDefinitionTypePanel.this.getModel().setObject(var);
                }
                VariableBindingDefinitionTypePanel.this.getModelObject().setPath(newPathtype);
            }
        };
        pathPanel.setOutputMarkupId(true);
        add(pathPanel);

        createRangePanel();
    }

    private void createRangePanel() {
        WebMarkupContainer rangeContainer = new WebMarkupContainer(ID_RANGE);
        add(rangeContainer);
        rangeContainer.setOutputMarkupId(true);
        rangeContainer.add(new VisibleBehaviour(VariableBindingDefinitionTypePanel.this::isRangeVisible));

        rangeContainer.add(mappingValueModel != null
                ? new MappingRangePanel(ID_RANGE_PANEL, mappingValueModel) {

                    @Override
                    protected boolean isHeaderVisible() {
                        return false;
                    }
                }
                : new WebMarkupContainer(ID_RANGE_PANEL));
    }

    private boolean isRangeVisible() {
        return showRangePanel;
    }

    private ItemPathDto createPathModel() {
        VariableBindingDefinitionType variable = getModelObject();
        if (variable == null) {
            return new ItemPathDto();
        }
        return new ItemPathDto(variable.getPath());
    }
}
