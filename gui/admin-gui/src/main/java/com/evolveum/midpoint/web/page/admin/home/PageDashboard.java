/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home;

import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.box.InfoBoxData;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.request.component.IRequestablePage;

/**
 * @author lazyman
 */

public abstract class PageDashboard extends PageAdminHome {

    private static final long serialVersionUID = 1L;

    private final LoadableDetachableModel<PrismObject<? extends FocusType>> principalModel;

    public PageDashboard() {
        principalModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismObject<? extends FocusType> load() {
                return loadFocusSelf();
            }
        };
        setTimeZone();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getLastBreadcrumb();
        bc.setIcon(new Model(GuiStyleConstants.CLASS_DASHBOARD_ICON));
    }

    protected abstract void initLayout();

    protected <O extends ObjectType> void customizationObjectInfoBoxType(InfoBoxData infoBoxType, Class<O> type,
            ObjectQuery totalQuery, ObjectQuery activeQuery, String bgColor, String icon, String keyPrefix, Integer totalCount,
            Integer activeCount, OperationResult result, Task task) {
    }

    protected <O extends ObjectType> IModel<InfoBoxData> getObjectInfoBoxTypeModel(Class<O> type, ObjectQuery totalQuery,
            ObjectQuery activeQuery, String bgColor, String icon, String keyPrefix,
            OperationResult result, Task task, Class<? extends IRequestablePage> linkPage) {

        InfoBoxData data = new InfoBoxData(bgColor, icon, getString(keyPrefix + ".label"));
        data.setLink(linkPage);

        Integer totalCount = null;
        Integer activeCount = null;
        try {
            totalCount = getModelService().countObjects(type, totalQuery, null, task, result);
            if (totalCount == null) {
                totalCount = 0;
            }

            activeCount = getModelService().countObjects(type, activeQuery, null, task, result);
            if (activeCount == null) {
                activeCount = 0;
            }

            data.setNumber(activeCount + " " + getString(keyPrefix + ".number"));

            int progress = 0;
            if (totalCount != 0) {
                progress = activeCount * 100 / totalCount;
            }
            data.setProgress(progress);

            data.setDescription(totalCount + " " + getString(keyPrefix + ".total"));

        } catch (Exception e) {
            data.setNumber("ERROR: " + e.getMessage());
        }

        customizationObjectInfoBoxType(data, type, totalQuery, activeQuery, bgColor, icon, keyPrefix, totalCount, activeCount, result, task);

        return Model.of(data);
    }

    protected <F extends FocusType> IModel<InfoBoxData> getFocusInfoBoxType(Class<F> type, String bgColor,
            String icon, String keyPrefix, OperationResult result, Task task, Class<? extends IRequestablePage> linkPage) {
        InfoBoxData data = new InfoBoxData(bgColor, icon, getString(keyPrefix + ".label"));
        data.setLink(linkPage);

        Integer allCount;
        try {
            allCount = getModelService().countObjects(type, null, null, task, result);
            if (allCount == null) {
                allCount = 0;
            }

            ObjectQuery queryDisabled = getPrismContext().queryFor(type)
                    .item(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS).eq(ActivationStatusType.DISABLED)
                    .build();
            Integer disabledCount = getModelService().countObjects(type, queryDisabled, null, task, result);
            if (disabledCount == null) {
                disabledCount = 0;
            }

            ObjectQuery queryArchived = getPrismContext().queryFor(type)
                    .item(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS).eq(ActivationStatusType.ARCHIVED)
                    .build();
            Integer archivedCount = getModelService().countObjects(type, queryArchived, null, task, result);
            if (archivedCount == null) {
                archivedCount = 0;
            }

            int activeCount = allCount - disabledCount - archivedCount;
            int totalCount = allCount - archivedCount;

            data.setNumber(activeCount + " " + getString(keyPrefix + ".number"));

            int progress = 0;
            if (totalCount != 0) {
                progress = activeCount * 100 / totalCount;
            }
            data.setProgress(progress);

            StringBuilder descSb = new StringBuilder();
            descSb.append(totalCount).append(" ").append(getString(keyPrefix + ".total"));
            if (archivedCount != 0) {
                descSb.append(" ( + ").append(archivedCount).append(" ").append(getString(keyPrefix + ".archived")).append(")");
            }
            data.setDescription(descSb.toString());

        } catch (Exception e) {
            data.setNumber("ERROR: " + e.getMessage());
        }

        customizationFocusInfoBoxType(data, type, bgColor, icon, keyPrefix, result, task);

        return Model.of(data);
    }

    protected <F extends FocusType> void customizationFocusInfoBoxType(InfoBoxData infoBoxType, Class<F> type, String bgColor,
            String icon, String keyPrefix, OperationResult result, Task task) {
    }

    @Override
    protected void onDetach() {
        principalModel.detach();
        super.onDetach();
    }
}
