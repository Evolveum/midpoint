/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgeListPanel;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.DisplayForLifecycleState;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.util.SelectableBean;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.util.List;
import java.util.Optional;

public class LifecycleStateBadgeColumn<C extends Containerable> extends AbstractColumn<SelectableBean<C>, String> {

    private final IModel<? extends PrismContainerDefinition<C>> mainModel;
    private final PageBase pageBase;

    public LifecycleStateBadgeColumn(IModel<? extends PrismContainerDefinition<C>> mainModel, PageBase pageBase) {
        super(null);
        this.mainModel = mainModel;
        this.pageBase = pageBase;
    }

    @Override
    public Component getHeader(String componentId) {
        LifecycleStateColumn column = new LifecycleStateColumn<>(mainModel, pageBase);
        return column.getHeader(componentId);
    }

    @Override
    public void populateItem(Item<ICellPopulator<SelectableBean<C>>> item, String componentId, IModel<SelectableBean<C>> iModel) {
        if (iModel.getObject() == null) {
            addEmptyComponent(item, componentId);
            return;
        }
        if (iModel.getObject().getValue() == null) {
            addEmptyComponent(item, componentId);
            return;
        }
        C containerValue = iModel.getObject().getValue();
        PrismProperty<String> lifecycle = containerValue.asPrismContainerValue().findProperty(ObjectType.F_LIFECYCLE_STATE);

        String value;
        if (lifecycle == null || StringUtils.isEmpty(lifecycle.getRealValue())) {
            value = SchemaConstants.LIFECYCLE_ACTIVE;
        } else {
            value = lifecycle.getRealValue();
        }

        LookupTableType lookupTable =
                WebComponentUtil.loadLookupTable(SystemObjectsType.LOOKUP_LIFECYCLE_STATES.value(), pageBase);
        if (lookupTable == null) {
            addEmptyComponent(item, componentId);
            return;
        }

        String finalValue = value;
        Optional<LookupTableRowType> rowOp =
                lookupTable.getRow().stream().filter(row -> finalValue.equals(row.getKey())).findFirst();
        if (rowOp.isEmpty()) {
            addEmptyComponent(item, componentId);
            return;
        }

        LookupTableRowType rowBean = rowOp.get();
        DisplayForLifecycleState state = DisplayForLifecycleState.valueOfOrDefault(rowBean.getKey());
        item.add(
                new BadgeListPanel(
                        componentId,
                        () -> List.of(new Badge(
                                state.getCssClass() + " alert px-1 py-0 m-0",
                                LocalizationUtil.translatePolyString(rowBean.getLabel())))));

        item.add(AttributeAppender.remove("class"));
    }

    private void addEmptyComponent(Item<ICellPopulator<SelectableBean<C>>> item, String componentId) {
        item.add(new Label(componentId));
    }
}
