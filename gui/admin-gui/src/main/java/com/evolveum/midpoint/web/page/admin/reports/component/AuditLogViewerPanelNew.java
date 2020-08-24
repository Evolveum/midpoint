/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.dispatchToObjectDetailsPage;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.web.component.search.PropertySearchItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;

import com.evolveum.midpoint.web.session.AuditLogStorage;
import com.evolveum.midpoint.web.session.PageStorage;

import com.evolveum.midpoint.web.session.SessionStorage;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.Channel;
import com.evolveum.midpoint.gui.impl.component.ContainerListPanel;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.reports.PageAuditLogDetails;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordProvider;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.apache.wicket.util.string.StringValue;

/**
 * Created by honchar
 */
public class AuditLogViewerPanelNew extends BasePanel {

    private static final long serialVersionUID = 1L;
    private static final String ID_AUDIT_LOG_VIEWER_TABLE = "auditLogViewerTable";

    public AuditLogViewerPanelNew(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ContainerListPanel auditLogViewerTable = new ContainerListPanel(ID_AUDIT_LOG_VIEWER_TABLE, AuditEventRecordType.class) {

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
                return AuditLogViewerPanelNew.this.addFilterToContentQuery(query, getPageStorage());
            }

            @Override
            protected IColumn createNameColumn(IModel columnNameModel, String itemPath, ExpressionType expression) {
                return AuditLogViewerPanelNew.this.createNameColumn();
            }

            @Override
            protected IColumn createIconColumn() {
                return null;
            }

            @Override
            protected Search createSearch() {
                return SearchFactory.createContainerSearch(getType(), AuditEventRecordType.F_TIMESTAMP, getPageBase());
            }

            @Override
            protected PageStorage getPageStorage(String storageKey){
                if (getAuditLogViewerStorage() == null){
                    return super.getPageStorage(storageKey);
                } else {
                    return getAuditLogViewerStorage();
                }
            }
        };
        auditLogViewerTable.setOutputMarkupId(true);
        add(auditLogViewerTable);
    }

    protected List<IColumn<PrismContainerValueWrapper<AuditEventRecordType>, String>> createColumns() {
        List<IColumn<PrismContainerValueWrapper<AuditEventRecordType>, String>> columns = new ArrayList<>();
        LinkColumn<PrismContainerValueWrapper<AuditEventRecordType>> initiatorRefColumn =
                new LinkColumn<PrismContainerValueWrapper<AuditEventRecordType>>(createStringResource("AuditEventRecordType.initiatorRef"),
                        AuditEventRecordProvider.INITIATOR_OID_PARAMETER, AuditEventRecordType.F_INITIATOR_REF.getLocalPart()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                        AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                        return Model.of(WebModelServiceUtils.resolveReferenceName(auditEventRecordType.getInitiatorRef(), getPageBase(), true));
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                        AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                        dispatchToObjectDetailsPage(auditEventRecordType.getInitiatorRef(), getPageBase(), false);
                    }
                };
        columns.add(initiatorRefColumn);

        if (!isObjectHistoryPanel()) {
            IColumn<PrismContainerValueWrapper<AuditEventRecordType>, String> eventStageColumn =
                    new PropertyColumn<PrismContainerValueWrapper<AuditEventRecordType>, String>(
                            createStringResource("PageAuditLogViewer.eventStageLabel"),
                            AuditEventRecordProvider.EVENT_STAGE_PARAMETER, "eventStage") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                            return WebComponentUtil.createLocalizedModelForEnum(unwrapModel(rowModel).getEventStage(),
                                    AuditLogViewerPanelNew.this);
                        }
                    };
            columns.add(eventStageColumn);
        }

        IColumn<PrismContainerValueWrapper<AuditEventRecordType>, String> eventTypeColumn =
                new PropertyColumn<PrismContainerValueWrapper<AuditEventRecordType>, String>(createStringResource("PageAuditLogViewer.eventTypeLabel"),
                        AuditEventRecordProvider.EVENT_TYPE_PARAMETER, "eventType") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                        return WebComponentUtil.createLocalizedModelForEnum(unwrapModel(rowModel).getEventType(), AuditLogViewerPanelNew.this);
                    }
                };
        columns.add(eventTypeColumn);

        if (!isObjectHistoryPanel()) {
            LinkColumn<PrismContainerValueWrapper<AuditEventRecordType>> targetRefColumn =
                    new LinkColumn<PrismContainerValueWrapper<AuditEventRecordType>>(createStringResource("AuditEventRecordType.targetRef"),
                            AuditEventRecordProvider.TARGET_OID_PARAMETER, AuditEventRecordType.F_TARGET_REF.getLocalPart()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                            AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                            return Model.of(WebModelServiceUtils.resolveReferenceName(auditEventRecordType.getTargetRef(), getPageBase(), true));
                        }

                        @Override
                        public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                            AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                            dispatchToObjectDetailsPage(auditEventRecordType.getTargetRef(), getPageBase(), false);
                        }
                    };
            columns.add(targetRefColumn);
        }

        if (!isObjectHistoryPanel()) {
            LinkColumn<PrismContainerValueWrapper<AuditEventRecordType>> targetOwnerRefColumn =
                    new LinkColumn<PrismContainerValueWrapper<AuditEventRecordType>>(createStringResource("AuditEventRecordType.targetOwnerRef"),
                            AuditEventRecordProvider.TARGET_OWNER_OID_PARAMETER, AuditEventRecordType.F_TARGET_OWNER_REF.getLocalPart()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                            AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                            return Model.of(WebModelServiceUtils.resolveReferenceName(auditEventRecordType.getTargetOwnerRef(), getPageBase(), true));
                        }

                        @Override
                        public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                            AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                            dispatchToObjectDetailsPage(auditEventRecordType.getTargetOwnerRef(), getPageBase(), false);
                        }
                    };
            columns.add(targetOwnerRefColumn);
        }
        IColumn<PrismContainerValueWrapper<AuditEventRecordType>, String> channelColumn =
                new PropertyColumn<PrismContainerValueWrapper<AuditEventRecordType>, String>(
                        createStringResource("AuditEventRecordType.channel"),
                        AuditEventRecordProvider.CHANNEL_PARAMETER, "channel") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AuditEventRecordType>>> item, String componentId,
                            IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                        AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                        String channel = auditEventRecordType.getChannel();
                        Channel channelValue = null;
                        for (Channel chan : Channel.values()) {
                            if (chan.getChannel().equals(channel)) {
                                channelValue = chan;
                                break;
                            } else if (SchemaConstants.CHANGE_CHANNEL_IMPORT_URI.equals(channel)) {
                                channelValue = Channel.IMPORT;
                            }
                        }
                        if (channelValue != null) {
                            item.add(new Label(componentId, WebComponentUtil.createLocalizedModelForEnum(channelValue, AuditLogViewerPanelNew.this)));
                        } else {
                            item.add(new Label(componentId, ""));
                        }
                        item.add(new AttributeModifier("style", new Model<>("width: 10%;")));
                    }
                };
        columns.add(channelColumn);

        IColumn<PrismContainerValueWrapper<AuditEventRecordType>, String> outcomeColumn =
                new PropertyColumn<PrismContainerValueWrapper<AuditEventRecordType>, String>(
                        createStringResource("PageAuditLogViewer.outcomeLabel"),
                        AuditEventRecordProvider.OUTCOME_PARAMETER, "outcome") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                        return WebComponentUtil.createLocalizedModelForEnum(unwrapModel(rowModel).getOutcome(), AuditLogViewerPanelNew.this);
                    }
                };
        columns.add(outcomeColumn);

        return columns;
    }

    private IColumn createNameColumn() {
        return new LinkColumn<PrismContainerValueWrapper<AuditEventRecordType>>(createStringResource("AuditEventRecordType.timestamp")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                AuditEventRecordType record = unwrapModel(rowModel);
                return Model.of(WebComponentUtil.formatDate(record.getTimestamp()));
            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                return unwrapModel(rowModel) != null;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
                AuditEventRecordType record = unwrapModel(rowModel);
                try {
                    AuditEventRecord.adopt(record, getPageBase().getPrismContext());
                } catch (SchemaException e) {
                    throw new SystemException("Couldn't adopt event record: " + e, e);
                }
                getPageBase().navigateToNext(new PageAuditLogDetails(record));
            }
        };
    }

    protected ObjectQuery addFilterToContentQuery(ObjectQuery query, PageStorage pageStorage){
        if (pageStorage != null && pageStorage.getSearch() == null) {
            Date todayDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
            XMLGregorianCalendar todayStartTimestamp = XmlTypeConverter.createXMLGregorianCalendar(todayDate);
            ObjectFilter todayTimestampFilter = getPageBase().getPrismContext().queryFor(AuditEventRecordType.class)
                    .item(AuditEventRecordType.F_TIMESTAMP)
                    .gt(todayStartTimestamp)
                    .buildFilter();
            if (query == null) {
                query = getPageBase().getPrismContext().queryFor(AuditEventRecordType.class).build();
            }
            query.addFilter(todayTimestampFilter);
        }
        return query;
    }

    protected AuditEventRecordType unwrapModel(IModel<PrismContainerValueWrapper<AuditEventRecordType>> rowModel) {
        if (rowModel == null || rowModel.getObject() == null) {
            return null;
        }
        return rowModel.getObject().getRealValue();
    }

    protected boolean isObjectHistoryPanel() {
        return false;
    }

    protected AuditLogStorage getAuditLogViewerStorage(){
        return null;
    }

}
