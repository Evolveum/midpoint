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

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;

import java.util.List;

/**
 * Created by honchar.
 */
public class MultiStateHorizontalButton extends BasePanel {

    private static final String ID_BUTTONS_CONTAINER = "buttonsContainer";
    private static final String ID_BUTTON = "button";

    private int selectedIndex = 0;
    private PageBase pageBase;
    private List<String> propertyKeysList;  //contains property keys for button labels. if button doesn't have
                                            //label, should be just "". the size of this list defines the
                                            //count of the buttons panel

    public MultiStateHorizontalButton(String id, int selectedIndex, List<String> propertyKeysList, PageBase pageBase){
        super (id);
        this.selectedIndex = selectedIndex;
        this.propertyKeysList = propertyKeysList;
        this.pageBase = pageBase;
        initLayout();
    }

    private void initLayout(){
        WebMarkupContainer buttonsPanel = new WebMarkupContainer(ID_BUTTONS_CONTAINER);
        buttonsPanel.setOutputMarkupId(true);
        add(buttonsPanel);

        RepeatingView buttons = new RepeatingView(ID_BUTTON);
        buttons.setOutputMarkupId(true);
        buttonsPanel.add(buttons);

        for (String propertyKey : propertyKeysList){
            AjaxSubmitButton button = new AjaxSubmitButton(buttons.newChildId(), pageBase.createStringResource(propertyKey)) {
                @Override
                public void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                    MultiStateHorizontalButton.this.onStateChanged(propertyKeysList.indexOf(propertyKey), ajaxRequestTarget);
                }
                @Override
                public void onError(AjaxRequestTarget ajaxRequestTarget, Form form) {
                    MultiStateHorizontalButton.this.onStateChanged(propertyKeysList.indexOf(propertyKey), ajaxRequestTarget);
                }
            };
            button.add(getActiveButtonClassAppender(propertyKeysList.indexOf(propertyKey)));
            button.setOutputMarkupId(true);
            buttons.add(button);
        }

    }

   private AttributeAppender getActiveButtonClassAppender(final int index){
        return new AttributeAppender("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if(index == selectedIndex){
                    return " active";
                }
                return null;
            }
        });
    }

    protected void onStateChanged(int index, AjaxRequestTarget target){
        setSelectedIndex(index);
        target.add(getButtonsContainer());
    }

    protected WebMarkupContainer getButtonsContainer(){
        return (WebMarkupContainer) get(ID_BUTTONS_CONTAINER);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }
}
