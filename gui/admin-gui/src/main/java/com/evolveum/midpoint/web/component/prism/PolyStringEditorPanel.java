/*
 * Copyright (c) 2010-2019 Evolveum
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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringLangType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationArgumentType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by honchar
 */
public class PolyStringEditorPanel extends BasePanel<PolyStringType>{
    private static final long serialVersionUID = 1L;

    private static final String ID_DEFAULT_VALUE_PANEL = "defaultValuePanel";
    private static final String ID_FULL_DATA_CONTAINER = "fullDataContainer";
    private static final String ID_ORIG_VALUE = "origValue";
    private static final String ID_KEY_VALUE = "keyValue";
    private static final String ID_LANGUAGES_REPEATER = "languagesRepeater";
    private static final String ID_LANGUAGE_NAME = "languageName";
    private static final String ID_TRANSLATION = "translation";
    private static final String ID_SHOW_HIDE_LANGUAGES = "showHideLanguages";

    private boolean showFullData = false;

    public PolyStringEditorPanel(String id, IModel<PolyStringType> model){
        super(id, model);
    }

    @Override
    protected void onInitialize(){
        initLayout();
    }

    private void initLayout(){
        TextPanel<String> defaultValuePanel = new TextPanel<String>(ID_DEFAULT_VALUE_PANEL, Model.of(getDefaultPolyStringValue()));
        defaultValuePanel.setOutputMarkupId(true);
        defaultValuePanel.add(new VisibleBehaviour(() -> !showFullData));
        add(defaultValuePanel);

        WebMarkupContainer fullDataContainer = new WebMarkupContainer(ID_FULL_DATA_CONTAINER);
        fullDataContainer.setOutputMarkupId(true);
        fullDataContainer.add(new VisibleBehaviour(() -> showFullData));
        add(fullDataContainer);

        TextPanel<String> origValue = new TextPanel<String>(ID_ORIG_VALUE, Model.of(getDefaultPolyStringValue()));
        origValue.setOutputMarkupId(true);
        fullDataContainer.add(origValue);

        TextPanel<String> keyValue = new TextPanel<String>(ID_KEY_VALUE, Model.of(getKeyValue()));
        keyValue.setOutputMarkupId(true);
        fullDataContainer.add(keyValue);

        Map<String, String> languagesMap = getModelObject() != null && getModelObject().getLang() != null ? getModelObject().getLang().getLang() : new HashMap<>();
        List<String> languagesList = new ArrayList<>(languagesMap.keySet());
        ListView<String> languagesContainer =
                new ListView<String>(ID_LANGUAGES_REPEATER, Model.ofList(languagesList)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<String> listItem) {
                        Label languageName = new Label(ID_LANGUAGE_NAME, Model.of(listItem.getModelObject()));
                        languageName.setOutputMarkupId(true);
                        listItem.add(languageName);

                        TextPanel<String> translation = new TextPanel<String>(ID_TRANSLATION, Model.of(languagesMap.get(listItem.getModelObject())));
                        translation.setOutputMarkupId(true);
                        listItem.add(translation);

            }
        };
        languagesContainer.setOutputMarkupId(true);
        fullDataContainer.add(languagesContainer);


        AjaxButton showHideLanguagesButton = new AjaxButton(ID_SHOW_HIDE_LANGUAGES, showFullData ? createStringResource("PolyStringEditorPanel.hideLanguages") :
                createStringResource("PolyStringEditorPanel.showLanguages")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showFullData = !showFullData;
                target.add(PolyStringEditorPanel.this);
            }
        };
        showHideLanguagesButton.setOutputMarkupId(true);
        showHideLanguagesButton.add(AttributeAppender.append("style", "cursor: pointer;"));
        add(showHideLanguagesButton);

    }

    private String getDefaultPolyStringValue(){
        return WebComponentUtil.getDisplayPolyStringValue(getModelObject(), getPageBase());
    }

    private String getKeyValue(){
        return getModelObject() != null && getModelObject().getTranslation() != null ?
                getModelObject().getTranslation().getKey() : "";
    }

    private List<PolyStringTranslationArgumentType> getTranslationArgumentList(){
        if (getModel() == null || getModelObject() == null || getModelObject().getTranslation() == null ||
                getModelObject().getTranslation().getArgument() == null){
            return new ArrayList<>();
        }
        return getModelObject().getTranslation().getArgument();
    }

}
