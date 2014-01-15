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
package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import javax.swing.text.html.ObjectView;

/**
 *  @author shood
 * */
public class PageReport extends PageAdminReports{

    private static final String DOT_CLASS = PageReport.class.getName() + ".";
    private static final String OPERATION_LOAD_REPORT = DOT_CLASS + "loadReport";

    private static final String ID_EDIT_PANEL = "editPanel";

    IModel<ObjectViewDto> model;

    public PageReport(SimplePanel panel){
        model = new LoadableModel<ObjectViewDto>(false) {
            @Override
            protected ObjectViewDto load() {
                return loadReport();
            }
        };

        initLayout(panel);
    }

    @Override
    protected IModel<String> createPageTitleModel(){
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                return new StringResourceModel("pageReport.title", PageReport.this, null, null).getString();
            }
        };
    }

    private ObjectViewDto loadReport(){
        //TODO - load report from repository
        return new ObjectViewDto();
    }

    private void initLayout(SimplePanel panel){
        WebMarkupContainer container = new WebMarkupContainer(ID_EDIT_PANEL);
        container.setOutputMarkupId(true);
        add(container);
        container.add(panel);
    }
}
