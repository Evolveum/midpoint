/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.ContainerListPanel;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class AuditLogViewerPanelNew extends BasePanel {

    private static final long serialVersionUID = 1L;
    private static final String ID_AUDIT_LOG_VIEWER_TABLE = "auditLogViewerTable";

    public AuditLogViewerPanelNew(String id){
        super(id);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        ContainerListPanel workItemsPanel = new ContainerListPanel(ID_AUDIT_LOG_VIEWER_TABLE, AuditEventRecordType.class) {

            @Override
            protected List<IColumn<PrismContainerValueWrapper<AuditEventRecordType>, String>> createDefaultColumns() {
                return AuditLogViewerPanelNew.this.createColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return null;
            }

            @Override
            protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
                return query;
            }

//            @Override
//            protected Collection<SelectorOptions<GetOperationOptions>> createOptions() {
//                return AuditLogViewerPanelNew.this.getPageBase().getOperationOptionsBuilder()
//                        .item(AbstractWorkItemType.F_ASSIGNEE_REF).resolve()
//                        .item(PrismConstants.T_PARENT, CaseType.F_OBJECT_REF).resolve()
//                        .item(PrismConstants.T_PARENT, CaseType.F_TARGET_REF).resolve()
//                        .build();
//            }
//
//            @Override
//            protected void setDefaultSorting(BaseSortableDataProvider provider){
//                provider.setSort(CaseWorkItemType.F_CREATE_TIMESTAMP.getLocalPart(), SortOrder.DESCENDING);
//            }

//            @Override
//            protected IColumn createCheckboxColumn() {
//                return CaseWorkItemsPanel.this.createCheckboxColumn();
//            }
//
//            @Override
//            protected IColumn createIconColumn() {
//                return CaseWorkItemsPanel.this.createIconColumn();
//            }
//
            @Override
            protected IColumn createNameColumn(IModel columnNameModel, String itemPath, ExpressionType expression) {
                return AuditLogViewerPanelNew.this.createNameColumn();
            }

        };
        workItemsPanel.setOutputMarkupId(true);
        add(workItemsPanel);
    }

    private List<IColumn<PrismContainerValueWrapper<AuditEventRecordType>, String>> createColumns(){
        List<IColumn<PrismContainerValueWrapper<AuditEventRecordType>, String>> columns = new ArrayList<>();
        return columns;
    }

    private IColumn createNameColumn(){
        return new LinkColumn<PrismContainerValueWrapper<AuditEventRecordType>>(createStringResource("PolicyRulesPanel.nameColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                return Model.of(rowModel.getObject().getRealValue().getEventIdentifier());
            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                if (rowModel.getObject() == null || rowModel.getObject().getRealValue() == null){
                    return false;
                }
                return true;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
            }
        };
    }

}
