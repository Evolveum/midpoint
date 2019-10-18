/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;


/**
 *  @author shood
 * */
public class SimpleErrorPanel<O extends ObjectType> extends BasePanel<FocusSubwrapperDto<O>>{
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
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout(){

        Label icon = new Label(ID_ICON);
        add(icon);

        Label description = new Label(ID_DESCRIPTION, new IModel<String>() {

            @Override
            public String getObject() {
                return getDescription();
            }
        });
        add(description);

        AjaxLink<Void> link = new AjaxLink<Void>(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onShowMorePerformed(target);
            }
        };
        add(link);


        Label showMore = new Label(ID_SHOW_MORE, new IModel<String>() {

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
