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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
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

    private static String DEFAULT_BUTTON_STYLE = "btn btn-default btn-sm buttons-panel-marging";

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

        DisplayType mainButtonDisplayType = getMainButtonDisplayType();
        CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setBasicIcon(WebComponentUtil.getIconCssClass(mainButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                .appendColorHtmlValue(WebComponentUtil.getIconColor(mainButtonDisplayType));

        AjaxCompositedIconButton mainButton = new AjaxCompositedIconButton(ID_MAIN_BUTTON, builder.build(),
                Model.of(WebComponentUtil.getDisplayTypeTitle(mainButtonDisplayType))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (!additionalButtonsExist()){
                    buttonClickPerformed(target, null);
                }
            }
        };
        mainButton.add(AttributeAppender.append(" data-toggle", additionalButtonsExist() ? "dropdown" : ""));
        add(mainButton);

        RepeatingView buttonsPanel = new RepeatingView(ID_BUTTON);
        buttonsPanel.add(new VisibleBehaviour(() -> additionalButtonsExist()));
        add(buttonsPanel);

        if (additionalButtonsExist()){
            additionalButtons.forEach(additionalButtonObject -> {
                DisplayType additionalButtonDisplayType = getAdditionalButtonDisplayType(additionalButtonObject);

                CompositedIconBuilder additionalButtonBuilder = new CompositedIconBuilder();
                additionalButtonBuilder.setBasicIcon(WebComponentUtil.getIconCssClass(additionalButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                        .appendColorHtmlValue(WebComponentUtil.getIconColor(additionalButtonDisplayType));
//                    .appendLayerIcon(GuiStyleConstants.CLASS_PLUS_CIRCLE, IconCssStyle.BOTTOM_RIGHT_STYLE, GuiStyleConstants.GREEN_COLOR);

                AjaxCompositedIconButton additionalButton = new AjaxCompositedIconButton(buttonsPanel.newChildId(), additionalButtonBuilder.build(),
                        Model.of(WebComponentUtil.getDisplayTypeTitle(additionalButtonDisplayType))) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        buttonClickPerformed(target, additionalButtonObject);
                    }
                };
                additionalButton.add(AttributeAppender.append("class", DEFAULT_BUTTON_STYLE));
                buttonsPanel.add(additionalButton);
            });

            DisplayType defaultObjectButtonDisplayType = getDefaultObjectButtonDisplayType();
            CompositedIconBuilder defaultObjectButtonBuilder = new CompositedIconBuilder();
            defaultObjectButtonBuilder.setBasicIcon(WebComponentUtil.getIconCssClass(defaultObjectButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                .appendColorHtmlValue(WebComponentUtil.getIconColor(defaultObjectButtonDisplayType));
            //TODO fix style for circle image
//                    .appendLayerIcon(GuiStyleConstants.CLASS_PLUS_CIRCLE, IconCssStyle.BOTTOM_RIGHT_STYLE, GuiStyleConstants.GREEN_COLOR);

            AjaxCompositedIconButton defaultButton = new AjaxCompositedIconButton(buttonsPanel.newChildId(),
                    defaultObjectButtonBuilder.build(),
                    Model.of(WebComponentUtil.getDisplayTypeTitle(defaultObjectButtonDisplayType))){

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (!additionalButtonsExist()){
                        buttonClickPerformed(target, null);
                    }
                }
            };
            defaultButton.add(AttributeAppender.append("class", DEFAULT_BUTTON_STYLE));
            buttonsPanel.add(defaultButton);
        }
    }

    protected DisplayType getMainButtonDisplayType(){
        return null;
    }

    protected DisplayType getAdditionalButtonDisplayType(S buttonObject){
        return null;
    }

    /**
     * this method should return the display properties for the last button on the dropdown  panel with additional buttons.
     * The last button is supposed to produce a default action (an action with no additional objects to process)
     * @return
     */
    protected DisplayType getDefaultObjectButtonDisplayType(){
        return null;
    }

    protected void buttonClickPerformed(AjaxRequestTarget target, S buttonObject){
    }

    private boolean additionalButtonsExist(){
        return !CollectionUtils.isEmpty(getAdditionalButtonsObjects());
    }

    protected List<S> getAdditionalButtonsObjects(){
        return new ArrayList<>();
    }


}
