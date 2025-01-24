/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.Collection;
import java.util.List;
import java.util.Set;

//TODO temporary solution replace with proper implementation
public class RoleAnalysisObjectDetailsTablePopupPanel extends BasePanel<String> implements Popupable {

    private final List<PrismObject<ObjectType>> elements;

    public RoleAnalysisObjectDetailsTablePopupPanel(String id, IModel<String> messageModel, List<PrismObject<ObjectType>> members) {
        super(id, messageModel);
        this.elements = members;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        SelectableBeanObjectDataProvider<ObjectType> selectableBeanObjectDataProvider = new SelectableBeanObjectDataProvider<>(
                this, Set.of()) {

            @Override
            protected List searchObjects(Class type, ObjectQuery query, Collection collection, Task task, OperationResult result) {
                Integer offset = query.getPaging().getOffset();
                Integer maxSize = query.getPaging().getMaxSize();

                return elements.subList(offset, offset + maxSize).stream().map(element -> element.asObjectable()).toList();
            }

            @Override
            protected Integer countObjects(Class<ObjectType> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result) {
                return elements.size();
            }
        };

        MainObjectListPanel<ObjectType> table = new MainObjectListPanel<>("table", ObjectType.class, null) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected boolean showTableAsCard() {
                return RoleAnalysisObjectDetailsTablePopupPanel.this.showTableAsCard();
            }

            @Override
            protected List<IColumn<SelectableBean<ObjectType>, String>> createDefaultColumns() {
                return super.createDefaultColumns();
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return List.of();
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ObjectType>> createProvider() {
                return selectableBeanObjectDataProvider;
            }

            @Override
            protected boolean isDuplicationSupported() {
                return false;
            }
        };

        table.setOutputMarkupId(true);
        add(table);

    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return null;
    }

    protected boolean showTableAsCard() {
        return true;
    }
}
