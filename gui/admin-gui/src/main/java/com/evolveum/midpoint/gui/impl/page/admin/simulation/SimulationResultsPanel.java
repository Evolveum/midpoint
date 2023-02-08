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
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

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
        List<InlineMenuItem> items = new ArrayList<>();

        items.add(new ButtonInlineMenuItem(createStringResource("list processed objects")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa-solid fa-eye");
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<SimulationResultType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBean<SimulationResultType> bean = getRowModel().getObject();
                        listProcessedObjectsPerformed(bean.getValue());
                    }
                };
            }
        });
        items.add(new ButtonInlineMenuItem(createStringResource("delete")) {

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa-solid fa-trash-can");
            }

            @Override
            public InlineMenuItemAction initAction() {
                return null;
            }
        });
        items.add(new InlineMenuItem(createStringResource("Delete processed objects")) {

            @Override
            public InlineMenuItemAction initAction() {
                return null;
            }
        });

        return items;
    }

    @Override
    protected List<IColumn<SelectableBean<SimulationResultType>, String>> createDefaultColumns() {
        List<IColumn<SelectableBean<SimulationResultType>, String>> columns = super.createDefaultColumns();
        columns.add(new LambdaColumn<>(createStringResource("ProcessedObjectsPanel.duration"), row -> PageSimulationResult.createResultDurationText(row.getValue(), this)));
        columns.add(new AbstractColumn<>(createStringResource("ProcessedObjectsPanel.executionState")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<SimulationResultType>>> item, String id, IModel<SelectableBean<SimulationResultType>> model) {
                Component label = PageSimulationResult.createTaskStateLabel(id, () -> model.getObject().getValue(), null, getPageBase());
                item.add(label);
            }
        });

        return columns;
    }

    private void listProcessedObjectsPerformed(SimulationResultType object) {
        PageParameters params = new PageParameters();
        params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, object.getOid());

        getPageBase().navigateToNext(PageSimulationResultObjects.class, params);
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultType>, String> createCustomExportableColumn(
            IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {

        ItemPath path = WebComponentUtil.getPath(customColumn);
        if (SimulationResultType.F_START_TIMESTAMP.equivalent(path)) {
            return createStartTimestampColumn(displayModel);
        }

        return super.createCustomExportableColumn(displayModel, customColumn, expression);
    }

    private IColumn<SelectableBean<SimulationResultType>, String> createStartTimestampColumn(IModel<String> displayModel) {
        return new LambdaColumn<>(displayModel, row -> {
            XMLGregorianCalendar start = row.getValue().getStartTimestamp();
            return start != null ? WebComponentUtil.getLongDateTimeFormattedValue(start, getPageBase()) : null;
        });
    }
}
