/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;


/**
 *  @author shood
 * */
public class SimpleErrorPanel<O extends ObjectType> extends SimplePanel<FocusSubwrapperDto<O>>{
	private static final long serialVersionUID = 1L;

	//    private static final String ID_CHECK = "check";
    private static final String ID_ICON = "icon";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_LINK = "link";
    private static final String ID_SHOW_MORE = "showMore";

    public SimpleErrorPanel(String id, IModel<FocusSubwrapperDto<O>> model){
        super(id, model);

        add(AttributeModifier.append("class", "check-table-header"));
    }

    @Override
    protected void initLayout(){

        Label icon = new Label(ID_ICON);
        add(icon);

        Label description = new Label(ID_DESCRIPTION, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getDescription();
            }
        });
        add(description);

        AjaxLink link = new AjaxLink(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onShowMorePerformed(target);
            }
        };
        add(link);

        Label showMore = new Label(ID_SHOW_MORE, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createStringResource("simpleErrorPanel.label.showMore").getString();
            }
        });
        link.add(showMore);
    }

    private String getDescription(){
        return createStringResource("simpleErrorPanel.message.error", getModel().getObject().getDescription()).getString();
    }

    public void onShowMorePerformed(AjaxRequestTarget target){}


}
