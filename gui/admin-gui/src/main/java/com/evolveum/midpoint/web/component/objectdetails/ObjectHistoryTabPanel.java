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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.MultiButtonColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.reports.component.AuditLogViewerPanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class ObjectHistoryTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {
    private static final String ID_MAIN_PANEL = "mainPanel";

    public ObjectHistoryTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusWrapperModel,
                                     PageBase page) {
        super(id, mainForm, focusWrapperModel, page);
        initLayout(focusWrapperModel, page);
    }

    private void initLayout(final LoadableModel<ObjectWrapper<F>> focusWrapperModel, PageBase page) {
        AuditSearchDto searchDto = new AuditSearchDto();
        ObjectReferenceType ort = new ObjectReferenceType();
        ort.setOid(focusWrapperModel.getObject().getOid());
        searchDto.setTargetName(ort);

        searchDto.setEventStage(AuditEventStageType.EXECUTION);

        AuditLogViewerPanel panel = new AuditLogViewerPanel(ID_MAIN_PANEL, page, searchDto){
            @Override
            protected boolean isTargetNameVisible(){
                return false;
            }

            @Override
            protected boolean isEventStageVisible(){
                return false;
            }

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
                    public String getButtonColorCssClass(int id) {
                        return colors[id].toString();
                    }

                   @Override
                    protected String getButtonCssClass(int id) {
                       StringBuilder sb = new StringBuilder();
                       sb.append(DoubleButtonColumn.BUTTON_BASE_CLASS).append(" ");
                       sb.append(getButtonColorCssClass(id)).append(" ");
                       switch (id) {
                           case 0:  sb.append("fa fa-circle-o"); break;
                           case 1:  sb.append("fa fa-exchange"); break;
                       }
                       return sb.toString();
                    }

                    @Override
                    public void clickPerformed(int id, AjaxRequestTarget target, IModel<AuditEventRecordType> model) {
                        switch (id) {
                            case 0:  currentStateButtonClicked(target, focusWrapperModel);break;
                            case 1:  break;
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

    private void currentStateButtonClicked(AjaxRequestTarget target, LoadableModel<ObjectWrapper<F>> focusWrapperModel){
        setResponsePage(new PageUser(((ObjectWrapper<UserType>)focusWrapperModel.getObject()).getObject()));
    }


}
