/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shood
 * @author lazyman
 *
 */
public class ReportConfigurationPanel extends BasePanel<ReportDto> {

    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXPORT_TYPE = "exportType";
    private static final String ID_VIRTUALIZER = "virtualizer";
    private static final String ID_VIRTUALIZER_KICKON = "virtualizerKickOn";
    private static final String ID_MAXPAGES = "maxPages";
    private static final String ID_PROPERTIES = "properties";
    private static final String ID_USE_HIBERNATE_SESSION = "useHibernateSession";
    private static final String ID_ORIENTATION = "orientation";
    private static final String ID_TIMEOUT = "timeout";

    private static final String ID_SEARCH_ON_RESOURCE = "searchOnResource";
    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";

    public ReportConfigurationPanel(String id, IModel<ReportDto> model) {
        super(id, model);
		initLayout();
    }

    protected void initLayout() {
        TextFormGroup name = new TextFormGroup(ID_NAME, new PropertyModel<String>(getModel(), ID_NAME),
                createStringResource("ObjectType.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        add(name);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION,
                new PropertyModel<String>(getModel(), ID_DESCRIPTION),
                createStringResource("ObjectType.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        add(description);

        IModel choices = WebComponentUtil.createReadonlyModelFromEnum(ExportType.class, e -> e != ExportType.JXL);
        IChoiceRenderer renderer = new EnumChoiceRenderer();
        DropDownFormGroup exportType = new DropDownFormGroup(ID_EXPORT_TYPE, new PropertyModel<ExportType>(getModel(), ReportDto.F_EXPORT_TYPE), choices, renderer,
                createStringResource("ReportType.export"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        add(exportType);

        TextFormGroup virtualizerKickOn = null;
        DropDownFormGroup virtualizer = new DropDownFormGroup(ID_VIRTUALIZER, new PropertyModel<String>(getModel(), ReportDto.F_VIRTUALIZER),
                createVirtualizerListModel(), new ChoiceRenderer<String>(),
                createStringResource("ReportType.virtualizer"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        //virtualizer.add(new VirtualizerAjaxFormUpdatingBehaviour(virtualizerKickOn));
        add(virtualizer);

        virtualizerKickOn = new TextFormGroup(ID_VIRTUALIZER_KICKON, new PropertyModel<String>(getModel(), ReportDto.F_VIRTUALIZER_KICKON),
                createStringResource("ReportType.virtualizerKickOn"), ID_LABEL_SIZE, "col-md-4", false);
        add(virtualizerKickOn);

        TextFormGroup maxPages = new TextFormGroup(ID_MAXPAGES, new PropertyModel<String>(getModel(), ReportDto.F_MAXPAGES),
                createStringResource("ReportType.maxPages"), ID_LABEL_SIZE, "col-md-4", false);
        add(maxPages);

        TextFormGroup timeout = new TextFormGroup(ID_TIMEOUT, new PropertyModel<String>(getModel(), ReportDto.F_TIMEOUT),
                createStringResource("ReportType.timeout"), ID_LABEL_SIZE, "col-md-4", false);
        add(timeout);
    }

    private IModel<List<String>> createVirtualizerListModel() {
        final List<String> virtualizerList = new ArrayList();

        virtualizerList.add("JRFileVirtualizer");
        virtualizerList.add("JRSwapFileVirtualizer");
        virtualizerList.add("JRGzipVirtualizer");

        return new AbstractReadOnlyModel<List<String>>() {

            @Override
            public List<String> getObject() {
                return virtualizerList;
            }
        };
    }

    /*
    private static class VirtualizerAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

        Component virtualizerKickOn;

        public VirtualizerAjaxFormUpdatingBehaviour(Component virtualizerKickOn) {
            super("change");
            this.virtualizerKickOn = virtualizerKickOn;
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            if (virtualizerKickOn != null) {
                virtualizerKickOn.setVisible(!virtualizerKickOn.isVisible()); // just demo
                target.add(virtualizerKickOn);
            }

        }
    }
    */
}
