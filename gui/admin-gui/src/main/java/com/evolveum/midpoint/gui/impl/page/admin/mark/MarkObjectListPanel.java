/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.mark;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MarkObjectListPanel extends MainObjectListPanel<MarkType> {

    private static final long serialVersionUID = 1L;

    public MarkObjectListPanel(String id) {
        super(id, MarkType.class, null);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_MARKS_TABLE;
    }

    @Override
    protected IColumn<SelectableBean<MarkType>, String> createCustomExportableColumn(
            IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {

        ItemPath path = WebComponentUtil.getPath(customColumn);
        if (ItemPath.create(MarkType.F_EVENT_MARK, EventMarkInformationType.F_DOMAIN).equivalent(path)) {
            return createDomainColumn(displayModel);
        } else if (ItemPath.create(MarkType.F_EVENT_MARK, EventMarkInformationType.F_ENABLED_BY_DEFAULT).equivalent(path)) {
            return createEnabledByDefaultColumn(displayModel);
        }

        return super.createCustomExportableColumn(displayModel, customColumn, expression);
    }

    private IColumn<SelectableBean<MarkType>, String> createEnabledByDefaultColumn(IModel<String> displayModel) {
        return new IconColumn<>(displayModel) {

            @Override
            protected DisplayType getIconDisplayType(IModel<SelectableBean<MarkType>> rowModel) {
                MarkType mark = rowModel.getObject().getValue();
                if (mark == null) {
                    return null;
                }

                EventMarkInformationType info = mark.getEventMark();
                if (info == null || info.isEnabledByDefault() == null) {
                    return null;
                }

                String cssClass = info.isEnabledByDefault() ? "fa-regular fa-circle-check text-success" : "fa-regular fa-circle-xmark text-danger";

                return new DisplayType()
                        .beginIcon()
                        .cssClass(cssClass)
                        .end();
            }
        };
    }

    private IColumn<SelectableBean<MarkType>, String> createDomainColumn(IModel<String> displayModel) {
        return new LambdaColumn<>(displayModel, selectableBean -> {

            MarkType mark = selectableBean.getValue();
            EventMarkInformationType info = mark.getEventMark();
            if (info == null || info.getDomain() == null) {
                return null;
            }

            EventMarkDomainType domain = info.getDomain();
            if (domain.getSimulation() != null) {
                return getString("EventMarkDomainType.simulation");
            }

            return null;
        });
    }
}
