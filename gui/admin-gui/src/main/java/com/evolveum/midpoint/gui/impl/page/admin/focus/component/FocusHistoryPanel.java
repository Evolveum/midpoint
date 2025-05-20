/*
 * Copyright (C) 2021-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.focus.component;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.policy.PagePolicyHistory;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.application.PanelTypeConstants;

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
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.org.PageOrgHistory;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRoleHistory;
import com.evolveum.midpoint.gui.impl.page.admin.service.PageServiceHistory;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUserHistory;
import com.evolveum.midpoint.prism.query.ObjectQuery;
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
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.web.session.AuditLogStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar.
 */
@PanelType(name = PanelTypeConstants.FOCUS_HISTORY_PANEL)
@PanelInstance(identifier = "history", applicableForType = FocusType.class, applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageAdminFocus.objectHistory", icon = GuiStyleConstants.CLASS_ICON_HISTORY, order = 60))
public class FocusHistoryPanel<F extends FocusType> extends AbstractObjectMainPanel<F, FocusDetailsModels<F>> {

    @Serial private static final long serialVersionUID = 1L;

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
        AuditLogViewerPanel panel = new AuditLogViewerPanel(ID_MAIN_PANEL, getPanelConfiguration()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<IColumn<SelectableBean<AuditEventRecordType>, String>> collectColumns() {
                List<IColumn<SelectableBean<AuditEventRecordType>, String>> columns = super.collectColumns();

                if (isPreview()) {
                    return columns;
                }

                IColumn<SelectableBean<AuditEventRecordType>, String> column = createViewButtonsColumns();
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

    private IColumn<SelectableBean<AuditEventRecordType>, String> createViewButtonsColumns() {
        return new AbstractColumn<>(new Model<>()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AuditEventRecordType>>> cellItem, String componentId,
                    IModel<SelectableBean<AuditEventRecordType>> rowModel) {

                cellItem.add(new MultiButtonPanel<>(componentId, rowModel, 2) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected Component createButton(int index, String componentId, IModel<SelectableBean<AuditEventRecordType>> model) {
                        AjaxIconButton btn = null;
                        switch (index) {
                            case 0 -> {
                                btn = buildDefaultButton(componentId, new Model<>("fa fa-circle-o"),
                                        createStringResource("ObjectHistoryTabPanel.viewHistoricalObjectDataTitle"),
                                        new Model<>("btn btn-sm " + DoubleButtonColumn.ButtonColorClass.INFO),
                                        target ->
                                                currentStateButtonClicked(getObjectWrapper().getOid(),
                                                        unwrapModel(model).getEventIdentifier(),
                                                        getObjectWrapper().getCompileTimeClass(),
                                                        WebComponentUtil.getLocalizedDate(unwrapModel(model).getTimestamp(),
                                                                DateLabelComponent.SHORT_NOTIME_STYLE)));
                                btn.setVisibilityAllowed(isHistoryPageAuthorized());
                            }
                            case 1 -> {
                                btn = buildDefaultButton(componentId, new Model<>(GuiStyleConstants.CLASS_FILE_TEXT),
                                        createStringResource("ObjectHistoryTabPanel.viewHistoricalObjectXmlTitle"),
                                        new Model<>("btn btn-sm " + DoubleButtonColumn.ButtonColorClass.SUCCESS),
                                        target ->
                                                viewObjectXmlButtonClicked(getObjectWrapper().getOid(),
                                                        unwrapModel(model).getEventIdentifier(),
                                                        getObjectWrapper().getCompileTimeClass(),
                                                        WebComponentUtil.getLocalizedDate(unwrapModel(model).getTimestamp(),
                                                                DateLabelComponent.SHORT_NOTIME_STYLE)));
                                btn.setVisibilityAllowed(SecurityUtils.isPageAuthorized(PageXmlDataReview.class));
                            }
                        }

                        return btn;
                    }
                });
            }
        };
    }

    protected void currentStateButtonClicked(String oid, String eventIdentifier, Class<F> type, String date) {
        //TODO fix sessionStorage
        getPageBase().getSessionStorage().setObjectDetailsStorage("details" + type.getSimpleName(), null);

        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OID_PARAMETER_LABEL, oid);
        pageParameters.add(EID_PARAMETER_LABEL, eventIdentifier);
        pageParameters.add(DATE_PARAMETER_LABEL, date);

        if (UserType.class.equals(type)) {
            getPageBase().navigateToNext(PageUserHistory.class, pageParameters);
        } else if (RoleType.class.equals(type)) {
            getPageBase().navigateToNext(PageRoleHistory.class, pageParameters);
        } else if (OrgType.class.equals(type)) {
            getPageBase().navigateToNext(PageOrgHistory.class, pageParameters);
        } else if (ServiceType.class.equals(type)) {
            getPageBase().navigateToNext(PageServiceHistory.class, pageParameters);
        } else if (PolicyType.class.equals(type)) {
            getPageBase().navigateToNext(PagePolicyHistory.class, pageParameters);
        }
    }

    private void viewObjectXmlButtonClicked(String oid, String eventIdentifier, Class<F> type, String date) {
        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OID_PARAMETER_LABEL, oid);
        pageParameters.add(EID_PARAMETER_LABEL, eventIdentifier);
        pageParameters.add(DATE_PARAMETER_LABEL, date);
        pageParameters.add(CLASS_TYPE_PARAMETER_LABEL, type.getName());

        getPageBase().navigateToNext(PageXmlDataReview.class, pageParameters);
    }

    protected boolean isHistoryPageAuthorized() {
        Class<? extends PageBase> pageHistoryDetailsPage = DetailsPageUtil.getPageHistoryDetailsPage(getPage().getPageClass());
        return SecurityUtils.isPageAuthorized(pageHistoryDetailsPage);
    }

    protected AuditEventRecordType unwrapModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        if (rowModel == null || rowModel.getObject() == null) {
            return null;
        }
        return rowModel.getObject().getValue();
    }
}
