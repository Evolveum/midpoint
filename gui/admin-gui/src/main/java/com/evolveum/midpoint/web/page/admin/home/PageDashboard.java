/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.box.InfoBoxType;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;

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

    protected <O extends ObjectType> void customizationObjectInfoBoxType(InfoBoxType infoBoxType, Class<O> type,
            List<QName> items, Object eqObject, String bgColor, String icon, String keyPrefix, Integer totalCount,
            Integer activeCount, OperationResult result, Task task) {
    }

    protected <O extends ObjectType> Model<InfoBoxType> getObjectInfoBoxTypeModel(Class<O> type, List<QName> items,
            Object eqObject, String bgColor, String icon, String keyPrefix, OperationResult result, Task task) {

        InfoBoxType infoBoxType = new InfoBoxType(bgColor, icon, getString(keyPrefix + ".label"));
        Integer totalCount = null;
        Integer activeCount = null;
        try {
            totalCount = getModelService().countObjects(type, null, null, task, result);
            if (totalCount == null) {
                totalCount = 0;
            }
            QName[] queryItems = new QName[items.size()];
            ObjectQuery query = getPrismContext().queryFor(type)
                .item(items.toArray(queryItems)).eq(eqObject)
                .build();

            activeCount = getModelService().countObjects(type, query, null, task, result);
            if (activeCount == null) {
                activeCount = 0;
            }

            infoBoxType.setNumber(activeCount + " " + getString(keyPrefix + ".number"));

            int progress = 0;
            if (totalCount != 0) {
                progress = activeCount * 100 / totalCount;
            }
            infoBoxType.setProgress(progress);

            infoBoxType.setDescription(totalCount + " " + getString(keyPrefix + ".total"));

        } catch (Exception e) {
            infoBoxType.setNumber("ERROR: "+e.getMessage());
        }

        customizationObjectInfoBoxType(infoBoxType, type, items, eqObject, bgColor, icon,
                keyPrefix, totalCount, activeCount, result, task);

        return new Model<>(infoBoxType);
    }

    protected <F extends FocusType> Model<InfoBoxType> getFocusInfoBoxType(Class<F> type, String bgColor,
            String icon, String keyPrefix, OperationResult result, Task task) {
        InfoBoxType infoBoxType = new InfoBoxType(bgColor, icon, getString(keyPrefix + ".label"));
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

            infoBoxType.setNumber(activeCount + " " + getString(keyPrefix + ".number"));

            int progress = 0;
            if (totalCount != 0) {
                progress = activeCount * 100 / totalCount;
            }
            infoBoxType.setProgress(progress);

            StringBuilder descSb = new StringBuilder();
            descSb.append(totalCount).append(" ").append(getString(keyPrefix + ".total"));
            if (archivedCount != 0) {
                descSb.append(" ( + ").append(archivedCount).append(" ").append(getString(keyPrefix + ".archived")).append(")");
            }
            infoBoxType.setDescription(descSb.toString());

        } catch (Exception e) {
            infoBoxType.setNumber("ERROR: "+e.getMessage());
        }

        customizationFocusInfoBoxType(infoBoxType, type, bgColor, icon, keyPrefix, result, task);

        return new Model<>(infoBoxType);
    }

    protected <F extends FocusType> void customizationFocusInfoBoxType(InfoBoxType infoBoxType, Class<F> type, String bgColor,
            String icon, String keyPrefix, OperationResult result, Task task) {
    }


    @Override
    protected void onDetach() {
        principalModel.detach();
        super.onDetach();
    }
}
