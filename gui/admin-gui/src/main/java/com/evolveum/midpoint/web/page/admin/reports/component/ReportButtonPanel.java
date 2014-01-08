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

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 *  @author shood
 * */
public class ReportButtonPanel<T> extends SimplePanel<T>{

    private static final String ID_BUTTON_RUN = "run";
    private static final String ID_BUTTON_CONFIGURE = "configure";

    public ReportButtonPanel(String id, IModel<T> model){
        super(id, model);
    }

    @Override
    protected void initLayout(){
        AjaxButton run = new AjaxButton(ID_BUTTON_RUN, createStringResource("ReportButtonPanel.button.run")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                runPerformed(target, ReportButtonPanel.this.getModel());
            }
        };

        AjaxButton configure = new AjaxButton(ID_BUTTON_CONFIGURE, createStringResource("ReportButtonPanel.button.configure")){

            @Override
            public void onClick(AjaxRequestTarget target){
                configurePerformed(target, ReportButtonPanel.this.getModel());
            }
        };

        add(run);
        add(configure);
    }

    public void runPerformed(AjaxRequestTarget target, IModel<T> model){}

    public void configurePerformed(AjaxRequestTarget target, IModel<T> model){}

}
