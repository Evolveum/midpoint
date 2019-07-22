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
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
public abstract class MultifunctionalButton<S extends Serializable> extends BasePanel<S> {

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

        DisplayType defaultObjectButtonDisplayType = validateDisplayType(getDefaultObjectButtonDisplayType());
        DisplayType mainButtonDisplayType = validateDisplayType(getMainButtonDisplayType());
        //we set default button icon class if no other is defined
        if (StringUtils.isEmpty(mainButtonDisplayType.getIcon().getCssClass())){
            mainButtonDisplayType.getIcon().setCssClass(defaultObjectButtonDisplayType.getIcon().getCssClass());
        }
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
                DisplayType additionalButtonDisplayType = validateDisplayType(getAdditionalButtonDisplayType(additionalButtonObject));
                //we set default button icon class if no other is defined
//                if (StringUtils.isEmpty(additionalButtonDisplayType.getIcon().getCssClass())){
//                    additionalButtonDisplayType.getIcon().setCssClass(defaultObjectButtonDisplayType.getIcon().getCssClass());
//                }

                CompositedIconBuilder additionalButtonBuilder = getAdditionalIconBuilder(additionalButtonObject, additionalButtonDisplayType);
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

            //we set main button icon class if no other is defined
            if (StringUtils.isEmpty(defaultObjectButtonDisplayType.getIcon().getCssClass())){
                defaultObjectButtonDisplayType.getIcon().setCssClass(mainButtonDisplayType.getIcon().getCssClass());
            }
//            CompositedIconBuilder defaultObjectButtonBuilder = new CompositedIconBuilder();
//            defaultObjectButtonBuilder.setBasicIcon(WebComponentUtil.getIconCssClass(defaultObjectButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
//                .appendColorHtmlValue(WebComponentUtil.getIconColor(defaultObjectButtonDisplayType))
//                    .appendLayerIcon(WebComponentUtil.createIconType(GuiStyleConstants.CLASS_PLUS_CIRCLE, "green"), IconCssStyle.BOTTOM_RIGHT_STYLE);

            AjaxCompositedIconButton defaultButton = new AjaxCompositedIconButton(buttonsPanel.newChildId(),
                    getAdditionalIconBuilder(null, defaultObjectButtonDisplayType).build(),
                    Model.of(WebComponentUtil.getDisplayTypeTitle(defaultObjectButtonDisplayType))){

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    buttonClickPerformed(target, null);
                }
            };
            defaultButton.add(AttributeAppender.append("class", DEFAULT_BUTTON_STYLE));
            buttonsPanel.add(defaultButton);
        }
    }

    protected abstract DisplayType getMainButtonDisplayType();

    protected abstract DisplayType getAdditionalButtonDisplayType(S buttonObject);

    /**
     * this method should return the display properties for the last button on the dropdown  panel with additional buttons.
     * The last button is supposed to produce a default action (an action with no additional objects to process)
     * @return
     */
    protected abstract DisplayType getDefaultObjectButtonDisplayType();

    protected CompositedIconBuilder getAdditionalIconBuilder(S additionalButtonObject, DisplayType additionalButtonDisplayType){
        CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setBasicIcon(WebComponentUtil.getIconCssClass(additionalButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                .appendColorHtmlValue(WebComponentUtil.getIconColor(additionalButtonDisplayType))
                .appendLayerIcon(WebComponentUtil.createIconType(GuiStyleConstants.CLASS_PLUS_CIRCLE, "green"), IconCssStyle.BOTTOM_RIGHT_STYLE);
        return builder;
    }

    private DisplayType validateDisplayType(DisplayType displayType){
        if (displayType == null){
            displayType = new DisplayType();
        }
        if (displayType.getIcon() == null){
            displayType.setIcon(new IconType());
        }
        if (displayType.getIcon().getCssClass() == null){
            displayType.getIcon().setCssClass("");
        }

        return displayType;
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
