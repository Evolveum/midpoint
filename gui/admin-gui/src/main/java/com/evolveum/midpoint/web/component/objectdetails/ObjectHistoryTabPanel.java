/*
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.objectdetails;

import static java.util.Arrays.asList;

import java.util.List;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.MultiButtonPanel2;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.reports.component.AuditLogViewerPanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;
import com.evolveum.midpoint.web.page.admin.users.PageUserHistory;
import com.evolveum.midpoint.web.page.admin.users.PageXmlDataReview;
import com.evolveum.midpoint.web.session.AuditLogStorage;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Created by honchar.
 */
public class ObjectHistoryTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {

    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_PANEL = "mainPanel";
    private static final Trace LOGGER = TraceManager.getTrace(ObjectHistoryTabPanel.class);
    private static final String DOT_CLASS = ObjectHistoryTabPanel.class.getName() + ".";
    private static final String OPERATION_RESTRUCT_OBJECT = DOT_CLASS + "restructObject";

    public ObjectHistoryTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusWrapperModel,
                                 PageAdminObjectDetails<F> parentPage) {
        super(id, mainForm, focusWrapperModel, parentPage);
        parentPage.getSessionStorage().setUserHistoryAuditLog(new AuditLogStorage());
        initLayout(focusWrapperModel, parentPage);
    }

    private void initLayout(final LoadableModel<ObjectWrapper<F>> focusWrapperModel, final PageAdminObjectDetails<F> page) {
        AuditSearchDto auditSearchDto = createAuditSearchDto(focusWrapperModel.getObject().getObject().asObjectable());
        AuditLogViewerPanel panel = new AuditLogViewerPanel(ID_MAIN_PANEL, Model.of(auditSearchDto), true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<IColumn<AuditEventRecordType, String>> initColumns() {
                List<IColumn<AuditEventRecordType, String>> columns = super.initColumns();

                IColumn<AuditEventRecordType, String> column
                        = new AbstractColumn<AuditEventRecordType, String>(new Model<>()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void populateItem(Item<ICellPopulator<AuditEventRecordType>> cellItem, String componentId,
                                             IModel<AuditEventRecordType> rowModel) {

                        cellItem.add(new MultiButtonPanel2<AuditEventRecordType>(componentId, rowModel, 2) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            protected AjaxIconButton createButton(int index, String componentId, IModel<AuditEventRecordType> model) {
                                AjaxIconButton btn = null;
                                switch (index) {
                                    case 0:
                                        btn = buildDefaultButton(componentId, new Model<>("fa fa-circle-o"),
                                                createStringResource("ObjectHistoryTabPanel.viewHistoricalObjectDataTitle"),
                                                new Model<>("btn btn-sm " + DoubleButtonColumn.BUTTON_COLOR_CLASS.INFO),
                                                target ->
                                                        currentStateButtonClicked(target, focusWrapperModel.getObject().getOid(),
                                                                model.getObject().getEventIdentifier(),
                                                                WebComponentUtil.getLocalizedDate(model.getObject().getTimestamp(), DateLabelComponent.SHORT_NOTIME_STYLE),
                                                                page.getCompileTimeClass()));
                                        break;
                                    case 1:
                                        btn = buildDefaultButton(componentId, new Model<>(GuiStyleConstants.CLASS_FILE_TEXT),
                                                createStringResource("ObjectHistoryTabPanel.viewHistoricalObjectXmlTitle"),
                                                new Model<>("btn btn-sm " + DoubleButtonColumn.BUTTON_COLOR_CLASS.SUCCESS),
                                                target ->
                                                        viewObjectXmlButtonClicked(focusWrapperModel.getObject().getOid(),
                                                                model.getObject().getEventIdentifier(),
                                                                page.getCompileTimeClass(),
                                                                WebComponentUtil.getLocalizedDate(model.getObject().getTimestamp(), DateLabelComponent.SHORT_NOTIME_STYLE)));
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
            protected void updateAuditSearchStorage(AuditSearchDto searchDto) {
                getPageBase().getSessionStorage().getUserHistoryAuditLog().setSearchDto(searchDto);
                getPageBase().getSessionStorage().getUserHistoryAuditLog().setPageNumber(0);


            }

            @Override
            protected void resetAuditSearchStorage() {
                getPageBase().getSessionStorage().getUserHistoryAuditLog().setSearchDto(createAuditSearchDto(focusWrapperModel.getObject().getObject().asObjectable()));

            }

            @Override
            protected void updateCurrentPage(long current) {
                getPageBase().getSessionStorage().getUserHistoryAuditLog().setPageNumber(current);

            }

            @Override
            protected long getCurrentPage() {
                return getPageBase().getSessionStorage().getUserHistoryAuditLog().getPageNumber();
            }

        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    private AuditSearchDto createAuditSearchDto(F focus) {
        AuditSearchDto searchDto = new AuditSearchDto();
        ObjectReferenceType ort = ObjectTypeUtil.createObjectRef(focus);
        searchDto.setTargetNames(asList(ort));
        searchDto.setEventStage(AuditEventStageType.EXECUTION);
        return searchDto;
    }

    private void currentStateButtonClicked(AjaxRequestTarget target, String oid, String eventIdentifier,
                                           String date, Class type) {
        //TODO cases for PageRoleHistory, PageOrgHistory if needed...
        getPageBase().navigateToNext(new PageUserHistory((PrismObject<UserType>) getReconstructedObject(oid, eventIdentifier, type), date));
    }

    private PrismObject<F> getReconstructedObject(String oid, String eventIdentifier,
                                                  Class type) {
        OperationResult result = new OperationResult(OPERATION_RESTRUCT_OBJECT);
        try {
            Task task = getPageBase().createSimpleTask(OPERATION_RESTRUCT_OBJECT);
            PrismObject<F> object = WebModelServiceUtils.reconstructObject(type, oid, eventIdentifier, task, result);
            return object;
        } catch (Exception ex) {
            result.recordFatalError("Couldn't restruct object.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't restruct object", ex);
        }
        return null;
    }

    private void viewObjectXmlButtonClicked(String oid, String eventIdentifier, Class type, String date) {
        PrismObject<F> object = getReconstructedObject(oid, eventIdentifier, type);
        String name = WebComponentUtil.getName(object);

        getPageBase().navigateToNext(new PageXmlDataReview(getPageBase().createStringResource("PageXmlDataReview.aceEditorPanelTitle", name, date),
                new IModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        PrismContext context = getPageBase().getPrismContext();
                        String xml = "";
                        try {
                            xml = context.serializerFor(PrismContext.LANG_XML).serialize(object);
                        } catch (Exception ex) {
                            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't serialize object", ex);
                        }
                        return xml;
                    }

                    @Override
                    public void setObject(String s) {

                    }

                    @Override
                    public void detach() {

                    }
                }));
    }

}
