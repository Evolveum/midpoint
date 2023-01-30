/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SimulationResultsPanel extends MainObjectListPanel<SimulationResultType> {

    public SimulationResultsPanel(String id, ContainerPanelConfigurationType config) {
        super(id, SimulationResultType.class, null, config);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_SIMULATION_RESULTS;
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, SimulationResultType object) {
        PageParameters params = new PageParameters();
        params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, object.getOid());

        getPageBase().navigateToNext(PageSimulationResult.class, params);
    }

    @Override
    protected List<Component> createToolbarButtonsList(String buttonId) {
        return new ArrayList<>();
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return super.createInlineMenu();
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultType>, String> createCustomExportableColumn(IModel<String> columnDisplayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
        ItemPath path = WebComponentUtil.getPath(customColumn);
        if (DefaultColumnUtils.SIMULATION_RESULTS_DURATION.equivalent(path)) {
            return new LambdaColumn<>(createStringResource("ProcessedObjectsPanel.duration"), row -> {
                SimulationResultType result = row.getValue();

                XMLGregorianCalendar start = result.getStartTimestamp();
                if (start == null) {
                    return null;
                }

                XMLGregorianCalendar end = result.getEndTimestamp();
                if (end == null) {
                    // todo null
                    return null;
                }

                return null;
            });
        }

        if (ItemPath.create(SimulationResultType.F_ROOT_TASK_REF, TaskType.F_EXECUTION_STATE).equivalent(path)) {
            return new AbstractColumn<>(createStringResource("ProcessedObjectsPanel.duration")) {

                @Override
                public void populateItem(Item<ICellPopulator<SelectableBean<SimulationResultType>>> item, String id, IModel<SelectableBean<SimulationResultType>> model) {
                    Label label = new Label(id, () -> {
                        return "vilko";
                    });
                    label.add(AttributeAppender.replace("class", () -> "badge badge-success"));
                    item.add(label);
                }
            };
        }

        return super.createCustomExportableColumn(columnDisplayModel, customColumn, expression);
    }
}
