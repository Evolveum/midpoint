/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ItemWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectBasicPanel;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */

public class EngineReportTabPanel extends ObjectBasicPanel<ReportType> {

    private static final Trace LOGGER = TraceManager.getTrace(EngineReportTabPanel.class);

    private static final String ID_ENGINES = "engines";
    private static final String ID_ENGINE = "engine";

    public EngineReportTabPanel(String id, IModel<PrismObjectWrapper<ReportType>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        ListView<ItemWrapper> engineView = new ListView<ItemWrapper>(ID_ENGINES, Model.ofList(getEngines())) {
            @Override
            protected void populateItem(ListItem<ItemWrapper> listItem) {
                PrismContainerWrapperModel<ReportType, AbstractReportEngineConfigurationType> model =
                        PrismContainerWrapperModel.fromContainerWrapper(EngineReportTabPanel.this.getModel(), listItem.getModelObject().getPath());
                SingleContainerPanel enginePanel = new SingleContainerPanel(ID_ENGINE, model, listItem.getModelObject().getTypeName());
                listItem.add(enginePanel);
            }
        };
        engineView.setOutputMarkupId(true);
        add(engineView);
    }

    private List<ItemWrapper> getEngines() {
        List<ItemWrapper> items = new ArrayList<>();
        PrismObjectValueWrapper<ReportType> value = getModel().getObject().getValue();
        for (ItemDefinition containerDef : getModel().getObject().getValue().getDefinition().getDefinitions()){
            if (!(containerDef instanceof PrismContainerDefinition)) {
                continue;
            }
            if (containerDef.getTypeClass() != null && AbstractReportEngineConfigurationType.class.isAssignableFrom(containerDef.getTypeClass())) {
                try {
                    items.add(value.findContainer(containerDef.getItemName()));
                } catch (SchemaException e) {
                    LOGGER.trace("Couldn't find container with name", e);
                }
            }
        }
        return items;
    }
}
