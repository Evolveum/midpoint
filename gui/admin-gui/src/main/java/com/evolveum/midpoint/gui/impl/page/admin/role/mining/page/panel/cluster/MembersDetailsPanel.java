/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

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
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class MembersDetailsPanel extends BasePanel<String> {

    RoleAnalysisProcessModeType processModeType;
    Set<String> objectsOid;

    public MembersDetailsPanel(String id, IModel<String> messageModel, Set<String> objectsOid,
            RoleAnalysisProcessModeType processModeType) {
        super(id, messageModel);
        this.processModeType = processModeType;
        this.objectsOid = objectsOid;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Class<?> roleTypeClass;
        if (processModeType.equals(RoleAnalysisProcessModeType.ROLE)) {
            roleTypeClass = RoleType.class;
        } else {
            roleTypeClass = UserType.class;
        }

        List<PrismObject<FocusType>> objects = new ArrayList<>();
        Task task = getPageBase().createSimpleTask("getFocusTypeObject");
        OperationResult result = new OperationResult("getFocusTypeObject");
        for (String userOid : objectsOid) {
            objects.add(getPageBase().getRoleAnalysisService()
                    .getFocusTypeObject(userOid, task, result));
        }

        SelectableBeanObjectDataProvider<FocusType> selectableBeanObjectDataProvider = new SelectableBeanObjectDataProvider<>(
                this, Set.of()) {

            @Override
            protected List searchObjects(Class type, ObjectQuery query, Collection collection, Task task, OperationResult result) {
                Integer offset = query.getPaging().getOffset();
                Integer maxSize = query.getPaging().getMaxSize();

                return objects.subList(offset, offset + maxSize).stream().map(element -> element.asObjectable()).toList();
            }

            @Override
            protected Integer countObjects(Class<FocusType> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result) throws CommonException {
                return objects.size();
            }
        };

        MainObjectListPanel<FocusType> table = new MainObjectListPanel<>("table", FocusType.class, null) {

            @Override
            protected IColumn<SelectableBean<FocusType>, String> createCheckboxColumn() {
                return null;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected Class<FocusType> getDefaultType() {
                return (Class<FocusType>) roleTypeClass;
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
            protected ISelectableDataProvider<SelectableBean<FocusType>> createProvider() {
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
}
