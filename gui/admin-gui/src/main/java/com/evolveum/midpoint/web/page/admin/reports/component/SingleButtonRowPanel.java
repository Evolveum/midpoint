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
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 *  @author shood
 *
 *  TODO - add option to change button's css class
 */
public class SingleButtonRowPanel<T> extends SimplePanel<T> {

    private static final String ID_BUTTON = "button";

    private String buttonName = null;

    public SingleButtonRowPanel(String id, IModel<T> model){
        super(id, model);
        initializeLayout();
    }

    public SingleButtonRowPanel(String id, IModel<T> model, String buttonName){
        super(id, model);
        this.buttonName = buttonName;
        initializeLayout();
    }

    private void initializeLayout(){
        AjaxButton button = new AjaxButton(ID_BUTTON, createButtonStringResource()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                executePerformed(target, SingleButtonRowPanel.this.getModel());
            }
        };
        add(button);
    }

    public void executePerformed(AjaxRequestTarget target, IModel<T> model){}

    private StringResourceModel createButtonStringResource(){
        if(buttonName == null){
            return createStringResource("SingleButtonRowPanel.button.default");
        }else {
            return createStringResource(buttonName);
        }
    }


}
