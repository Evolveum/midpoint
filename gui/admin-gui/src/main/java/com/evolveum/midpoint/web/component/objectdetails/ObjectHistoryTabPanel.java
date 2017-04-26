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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.MultiButtonColumn;
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
        AuditSearchDto searchDto = new AuditSearchDto();
        ObjectReferenceType ort = new ObjectReferenceType();
        ort.setOid(focusWrapperModel.getObject().getOid());
        searchDto.setTargetNames(asList(ort));

        searchDto.setEventStage(AuditEventStageType.EXECUTION);

        Map<String, Boolean> visibilityMap = new HashMap<>();
        visibilityMap.put(AuditLogViewerPanel.TARGET_NAME_LABEL_VISIBILITY, false);
        visibilityMap.put(AuditLogViewerPanel.TARGET_NAME_FIELD_VISIBILITY, false);
        visibilityMap.put(AuditLogViewerPanel.TARGET_OWNER_LABEL_VISIBILITY, false);
        visibilityMap.put(AuditLogViewerPanel.TARGET_OWNER_FIELD_VISIBILITY, false);
        visibilityMap.put(AuditLogViewerPanel.EVENT_STAGE_LABEL_VISIBILITY, false);
        visibilityMap.put(AuditLogViewerPanel.EVENT_STAGE_FIELD_VISIBILITY, false);
        visibilityMap.put(AuditLogViewerPanel.EVENT_STAGE_COLUMN_VISIBILITY, false);
        visibilityMap.put(AuditLogViewerPanel.TARGET_COLUMN_VISIBILITY, false);
        visibilityMap.put(AuditLogViewerPanel.TARGET_OWNER_COLUMN_VISIBILITY, false);
        AuditLogViewerPanel panel = new AuditLogViewerPanel(ID_MAIN_PANEL, page, searchDto, visibilityMap) {
            @Override
            protected List<IColumn<AuditEventRecordType, String>> initColumns() {
                List<IColumn<AuditEventRecordType, String>> columns = super.initColumns();
                IColumn<AuditEventRecordType, String> column
                        = new MultiButtonColumn<AuditEventRecordType>(new Model(), 2) {

                    private final DoubleButtonColumn.BUTTON_COLOR_CLASS[] colors = {
                            DoubleButtonColumn.BUTTON_COLOR_CLASS.INFO,
                            DoubleButtonColumn.BUTTON_COLOR_CLASS.SUCCESS
                    };

                    @Override
                    public String getCaption(int id) {
                        return "";
                    }

                    @Override
                    public String getButtonTitle(int id) {
                        switch (id) {
                            case 0:
                                return page.createStringResource("ObjectHistoryTabPanel.viewHistoricalObjectDataTitle").getString();
                            case 1:
                                return page.createStringResource("ObjectHistoryTabPanel.viewHistoricalObjectXmlTitle").getString();
                        }
                        return "";
                    }

                    @Override
                    public String getButtonColorCssClass(int id) {
                        return colors[id].toString();
                    }

                    @Override
                    protected String getButtonCssClass(int id) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(DoubleButtonColumn.BUTTON_BASE_CLASS).append(" ");
                        sb.append(getButtonColorCssClass(id)).append(" ");
                        switch (id) {
                            case 0:
                                sb.append("fa fa-circle-o");
                                break;
                            case 1:
                                sb.append("fa fa-file-text-o");
                                break;
                        }
                        return sb.toString();
                    }

                    @Override
                    public void clickPerformed(int id, AjaxRequestTarget target, IModel<AuditEventRecordType> model) {
                        switch (id) {
                            case 0:
                                currentStateButtonClicked(target, focusWrapperModel.getObject().getOid(),
                                        model.getObject().getEventIdentifier(),
                                        WebComponentUtil.getLocalizedDate(model.getObject().getTimestamp(), DateLabelComponent.SHORT_NOTIME_STYLE),
                                        page.getCompileTimeClass());
                                break;
                            case 1:
                                viewObjectXmlButtonClicked(focusWrapperModel.getObject().getOid(),
                                        model.getObject().getEventIdentifier(),
                                        page.getCompileTimeClass(),
                                        WebComponentUtil.getLocalizedDate(model.getObject().getTimestamp(), DateLabelComponent.SHORT_NOTIME_STYLE));
                                break;
                        }
                    }

                };
                columns.add(column);

                return columns;
            }

        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    private void currentStateButtonClicked(AjaxRequestTarget target, String oid, String eventIdentifier,
                                           String date, Class type) {
        //TODO cases for PageRoleHistory, PageOrgHistory if needed...
        getPageBase().navigateToNext(new PageUserHistory((PrismObject<UserType>) getReconstructedObject(oid, eventIdentifier, type), date));
    }

    private PrismObject<F> getReconstructedObject(String oid, String eventIdentifier,
                                                  Class type){
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
    private void viewObjectXmlButtonClicked(String oid, String eventIdentifier, Class type, String date){
        PrismObject<F> object = getReconstructedObject(oid, eventIdentifier, type);
        String name = WebComponentUtil.getName(object);

        setResponsePage(new PageXmlDataReview(getPageBase().createStringResource("PageXmlDataReview.aceEditorPanelTitle", name, date),
                new IModel<String>() {
                    @Override
                    public String getObject() {
                        PrismContext context = getPageBase().getPrismContext();
                        String xml = "";
                        try{
                            xml = context.serializerFor(PrismContext.LANG_XML).serialize(object);
                        } catch (Exception ex){
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
