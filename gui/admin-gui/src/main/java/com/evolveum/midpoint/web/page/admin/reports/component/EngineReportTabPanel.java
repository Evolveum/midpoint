/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ItemWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
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

//    private static final String ID_REPORT_TYPE = "reportType";
    private static final String ID_ENGINES = "engines";
    private static final String ID_ENGINE = "engine";

//    private static final String ID_LABEL_SIZE = "col-md-2";
//    private static final String ID_INPUT_SIZE = "col-md-10";

    public EngineReportTabPanel(String id, IModel<PrismObjectWrapper<ReportType>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        List<ItemWrapper> engines = getEngines();
//        IChoiceRenderer renderer = new ChoiceRenderer<ItemWrapper>("displayName", "typeClass"){
//            @Override
//            public Object getDisplayValue(ItemWrapper object) {
//                if (object.getDisplayName() == null) {
//                    return "";
//                }
//                return getPageBase().createStringResource(object.getDisplayName()).getString();
//            }
//        };
//
//        IModel<ItemWrapper> engine = getActualEngine(engines);
//        DropDownFormGroup reportTypePanel = new DropDownFormGroup<ItemWrapper>(ID_REPORT_TYPE, engine, Model.ofList(engines), renderer,
//                createStringResource("EngineReportTabPanel.TypeOfReport"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
//        reportTypePanel.setOutputMarkupId(true);

        ListView<ItemWrapper> engineView = new ListView<ItemWrapper>(ID_ENGINES, Model.ofList(engines)) {
            @Override
            protected void populateItem(ListItem<ItemWrapper> listItem) {
                PrismContainerWrapperModel<ReportType, AbstractReportEngineConfigurationType> model =
                        PrismContainerWrapperModel.fromContainerWrapper(EngineReportTabPanel.this.getModel(), listItem.getModelObject().getPath());
                SingleContainerPanel enginePanel = new SingleContainerPanel(ID_ENGINE, model, listItem.getModelObject().getTypeName());
//                enginePanel.add(new VisibleEnableBehaviour(){
//
//                    @Override
//                    public boolean isVisible() {
//                        return engine.getObject() != null && listItem.getModelObject().getPath().equivalent(engine.getObject().getPath());
//                    }
//                });
                listItem.add(enginePanel);
            }
        };


        engineView.setOutputMarkupId(true);
        add(engineView);

//        reportTypePanel.getInput().add(new AjaxFormComponentUpdatingBehavior("change"){
//
//            @Override
//            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
////                ajaxRequestTarget.add(reportTypePanel);
//                ajaxRequestTarget.add(EngineReportTabPanel.this);
//            }
//        });
//        reportTypePanel.add(new VisibleEnableBehaviour(){
//            @Override
//            public boolean isVisible() {
//                return engine.getObject() == null;
//            }
//        });
//        add(reportTypePanel);
    }

    private IModel<ItemWrapper> getActualEngine(List<ItemWrapper> engines) {
        for (ItemWrapper engine : engines) {
            if (engine instanceof ItemWrapperImpl && !((ItemWrapperImpl)engine).getOldItem().isEmpty()){
                return Model.of(engine);
            }
        }
        return Model.of();
    }

    private List<ItemWrapper> getEngines() {
        List<ItemWrapper> items = new ArrayList<>();
        for (ItemWrapper item : getModel ().getObject().getValue().getContainers()) {
            if (item.getTypeClass() != null && AbstractReportEngineConfigurationType.class.isAssignableFrom(item.getTypeClass())) {
                items.add(item);
            }
        }
        return items;
    }
}
