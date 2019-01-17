/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class MultifunctionalButton<S extends Serializable> extends BasePanel<S> {

    private static String ID_MAIN_BUTTON = "mainButton";
    private static String ID_BUTTON = "additionalButton";

    public MultifunctionalButton(String id){
        super(id);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        List<S> additionalButtons =  getAdditionalButtonsObjects();

        CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setBasicIcon(getDefaultButtonStyle(), IconCssStyle.IN_ROW_STYLE, "")
                .appendLayerIcon(GuiStyleConstants.CLASS_PLUS_CIRCLE, IconCssStyle.BOTTOM_RIGHT_STYLE, "green");

        AjaxCompositedIconButton mainButton = new AjaxCompositedIconButton(ID_MAIN_BUTTON, builder.build(),
                createStringResource("MainObjectListPanel.newObject")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (!additionalButtonsExist()){
                    buttonClickPerformed(target, null);
                }
            }
        };
        add(mainButton);

        RepeatingView buttonsPanel = new RepeatingView(ID_BUTTON);
        buttonsPanel.add(new VisibleBehaviour(() -> additionalButtonsExist()));
        add(buttonsPanel);

        if (additionalButtons != null){
            additionalButtons.forEach(additionalButtonObject -> {
                AjaxIconButton newObjectIcon = new AjaxIconButton(buttonsPanel.newChildId(), new Model<>(GuiStyleConstants.CLASS_ASSIGN),
                        createStringResource("MainObjectListPanel.newObject")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        buttonClickPerformed(target, additionalButtonObject);
                    }

                    @Override
                    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                        super.onComponentTagBody(markupStream, openTag);
                    }
                };
                newObjectIcon.add(AttributeAppender.append("class", "btn btn-success btn-sm"));
                buttonsPanel.add(newObjectIcon);
            });
        }
    }

    protected String getDefaultButtonStyle(){
        return "";
    }

    protected void buttonClickPerformed(AjaxRequestTarget target, S buttonObject){

    }

    private boolean additionalButtonsExist(){
        List additionalButtons =  getAdditionalButtonsObjects();
        return additionalButtons != null && additionalButtons.size() > 0;
    }

    protected List<S> getAdditionalButtonsObjects(){
        return new ArrayList<>();
    }


}
