/*
 * Copyright (C) 2021-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.focus.component;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.org.PageOrgHistory;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRoleHistory;
import com.evolveum.midpoint.gui.impl.page.admin.service.PageServiceHistory;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUserHistory;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.MultiButtonPanel;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.reports.component.AuditLogViewerPanel;
import com.evolveum.midpoint.web.page.admin.users.PageXmlDataReview;
import com.evolveum.midpoint.web.session.AuditLogStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar.
 */
@PanelType(name = "history")
@PanelInstance(identifier = "history", applicableForType = FocusType.class, applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageAdminFocus.objectHistory", icon = GuiStyleConstants.CLASS_ICON_HISTORY, order = 60))
public class FocusHistoryPanel<F extends FocusType> extends AbstractObjectMainPanel<F, FocusDetailsModels<F>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_PANEL = "mainPanel";
    private static final Trace LOGGER = TraceManager.getTrace(FocusHistoryPanel.class);
    private static final String DOT_CLASS = FocusHistoryPanel.class.getName() + ".";
    private static final String OPERATION_RESTRUCT_OBJECT = DOT_CLASS + "restructObject";

    private static final String OID_PARAMETER_LABEL = "oid";
    private static final String EID_PARAMETER_LABEL = "eventIdentifier";
    private static final String DATE_PARAMETER_LABEL = "date";
    private static final String CLASS_TYPE_PARAMETER_LABEL = "classType";

    public FocusHistoryPanel(String id, FocusDetailsModels<F> focusModel, ContainerPanelConfigurationType config) {
        super(id, focusModel, config);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        getPageBase().getSessionStorage().setObjectHistoryAuditLog(getObjectWrapper().getTypeName(), new AuditLogStorage());
    }

    protected void initLayout() {
        AuditLogViewerPanel panel = new AuditLogViewerPanel(ID_MAIN_PANEL, null, getPanelConfiguration()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<IColumn<SelectableBean<AuditEventRecordType>, String>> createColumns() {
                List<IColumn<SelectableBean<AuditEventRecordType>, String>> columns = super.createColumns();
                if (isDashboard()) {
                    return columns;
                }
                IColumn<SelectableBean<AuditEventRecordType>, String> column
                        = new AbstractColumn<>(new Model<>()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<AuditEventRecordType>>> cellItem, String componentId,
                            IModel<SelectableBean<AuditEventRecordType>> rowModel) {

                        cellItem.add(new MultiButtonPanel<>(componentId, rowModel, 2) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            protected Component createButton(int index, String componentId, IModel<SelectableBean<AuditEventRecordType>> model) {
                                AjaxIconButton btn = null;
                                switch (index) {
                                    case 0:
                                        btn = buildDefaultButton(componentId, new Model<>("fa fa-circle-o"),
                                                createStringResource("ObjectHistoryTabPanel.viewHistoricalObjectDataTitle"),
                                                new Model<>("btn btn-sm " + DoubleButtonColumn.ButtonColorClass.INFO),
                                                target ->
                                                        currentStateButtonClicked(getObjectWrapper().getOid(),
                                                                unwrapModel(model).getEventIdentifier(),
                                                                getObjectWrapper().getCompileTimeClass(),
                                                                WebComponentUtil.getLocalizedDate(unwrapModel(model).getTimestamp(), DateLabelComponent.SHORT_NOTIME_STYLE)));
                                        break;
                                    case 1:
                                        btn = buildDefaultButton(componentId, new Model<>(GuiStyleConstants.CLASS_FILE_TEXT),
                                                createStringResource("ObjectHistoryTabPanel.viewHistoricalObjectXmlTitle"),
                                                new Model<>("btn btn-sm " + DoubleButtonColumn.ButtonColorClass.SUCCESS),
                                                target ->
                                                        viewObjectXmlButtonClicked(getObjectWrapper().getOid(),
                                                                unwrapModel(model).getEventIdentifier(),
                                                                getObjectWrapper().getCompileTimeClass(),
                                                                WebComponentUtil.getLocalizedDate(unwrapModel(model).getTimestamp(), DateLabelComponent.SHORT_NOTIME_STYLE)));
                                        break;
                                }

                                return btn;
                            }
                        });
                    }
                };

                columns.add(column);

                return columns;
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getPageBase().getPrismContext().queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_TARGET_REF)
                        .ref(getObjectWrapper().getOid())
                        .and()
                        .item(AuditEventRecordType.F_EVENT_STAGE)
                        .eq(AuditEventStageType.EXECUTION)
                        .build();
            }

            @Override
            protected String getAuditStorageKey(String collectionNameValue) {
                if (StringUtils.isNotEmpty(collectionNameValue)) {
                    return SessionStorage.KEY_OBJECT_HISTORY_AUDIT_LOG + "." + collectionNameValue
                            + "." + getObjectWrapper().getTypeName().getLocalPart();
                }
                return SessionStorage.KEY_OBJECT_HISTORY_AUDIT_LOG + "." + getObjectWrapper().getTypeName().getLocalPart();
            }

            @Override
            protected boolean isObjectHistoryPanel() {
                return true;
            }

        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    protected void currentStateButtonClicked(String oid, String eventIdentifier, Class<F> type, String date) {
        PrismObject<F> object = getReconstructedObject(oid, eventIdentifier, type);

        //TODO fix sessionStorage
        getPageBase().getSessionStorage().setObjectDetailsStorage("details" + type.getSimpleName(), null);

        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OID_PARAMETER_LABEL, oid);
        pageParameters.add(EID_PARAMETER_LABEL, eventIdentifier);
        pageParameters.add(DATE_PARAMETER_LABEL, date);

        Class<F> objectClass = null;
        if (object != null) {
            objectClass = object.getCompileTimeClass();
        }
        if (UserType.class.equals(objectClass)) {
            getPageBase().navigateToNext(PageUserHistory.class, pageParameters);
        } else if (RoleType.class.equals(objectClass)) {
            getPageBase().navigateToNext(PageRoleHistory.class, pageParameters);
        } else if (OrgType.class.equals(objectClass)) {
            getPageBase().navigateToNext(PageOrgHistory.class, pageParameters);
        } else if (ServiceType.class.equals(objectClass)) {
            getPageBase().navigateToNext(PageServiceHistory.class, pageParameters);
        }
    }

    private PrismObject<F> getReconstructedObject(String oid, String eventIdentifier,
            Class<F> type) {
        OperationResult result = new OperationResult(OPERATION_RESTRUCT_OBJECT);
        try {
            Task task = getPageBase().createSimpleTask(OPERATION_RESTRUCT_OBJECT);
            return WebModelServiceUtils.reconstructObject(type, oid, eventIdentifier, task, result);
        } catch (Exception ex) {
            result.recordFatalError(getPageBase().createStringResource("ObjectHistoryTabPanel.message.getReconstructedObject.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't restruct object", ex);
        }
        return null;
    }

    private void viewObjectXmlButtonClicked(String oid, String eventIdentifier, Class<F> type, String date) {
        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OID_PARAMETER_LABEL, oid);
        pageParameters.add(EID_PARAMETER_LABEL, eventIdentifier);
        pageParameters.add(DATE_PARAMETER_LABEL, date);
        pageParameters.add(CLASS_TYPE_PARAMETER_LABEL, type.getName());

        getPageBase().navigateToNext(PageXmlDataReview.class, pageParameters);
    }
}
