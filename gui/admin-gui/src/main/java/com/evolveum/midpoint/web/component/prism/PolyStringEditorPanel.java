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
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import org.apache.commons.lang.StringUtils;
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

/**
 * Created by honchar
 */
public class PolyStringEditorPanel extends BasePanel<PolyString>{
    private static final long serialVersionUID = 1L;

    private static final String ID_DEFAULT_VALUE_PANEL = "defaultValuePanel";
    private static final String ID_FULL_DATA_CONTAINER = "fullDataContainer";
    private static final String ID_ORIG_VALUE = "origValue";
    private static final String ID_KEY_VALUE = "keyValue";
    private static final String ID_LANGUAGES_REPEATER = "languagesRepeater";
    private static final String ID_LANGUAGE_NAME = "languageName";
    private static final String ID_TRANSLATION = "translation";
    private static final String ID_SHOW_HIDE_LANGUAGES = "showHideLanguages";
    private static final String ID_ADD_LANGUAGE = "addLanguage";

    private boolean showFullData = false;

    public PolyStringEditorPanel(String id, IModel<PolyString> model){
        super(id, model);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        setOutputMarkupId(true);

        TextPanel<String> defaultValuePanel = new TextPanel<String>(ID_DEFAULT_VALUE_PANEL, Model.of(getDefaultPolyStringValue()));
        defaultValuePanel.setOutputMarkupId(true);
        defaultValuePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        defaultValuePanel.add(new VisibleBehaviour(() -> !showFullData));
        add(defaultValuePanel);

        WebMarkupContainer fullDataContainer = new WebMarkupContainer(ID_FULL_DATA_CONTAINER);
        fullDataContainer.setOutputMarkupId(true);
        fullDataContainer.add(new VisibleBehaviour(() -> showFullData));
        add(fullDataContainer);

        TextPanel<String> origValue = new TextPanel<String>(ID_ORIG_VALUE, Model.of(getDefaultPolyStringValue()));
        origValue.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        origValue.setOutputMarkupId(true);
        fullDataContainer.add(origValue);

        TextPanel<String> keyValue = new TextPanel<String>(ID_KEY_VALUE, Model.of(getKeyValue()));
        keyValue.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        keyValue.setOutputMarkupId(true);
        fullDataContainer.add(keyValue);

        Map<String, String> languagesMap = getModelObject() != null && getModelObject().getLang() != null ? getModelObject().getLang() : new HashMap<>();
        ListView<String> languagesContainer =
                new ListView<String>(ID_LANGUAGES_REPEATER, getLanguagesListModel()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<String> listItem) {
                        if (StringUtils.isEmpty(listItem.getModelObject())){
                            TextPanel<String> languageName = new TextPanel<String>(ID_LANGUAGE_NAME, Model.of(listItem.getModelObject()));
                            languageName.getBaseFormComponent().setOutputMarkupId(true);
                            listItem.add(languageName);
                        } else {
                            Label languageName = new Label(ID_LANGUAGE_NAME, Model.of(listItem.getModelObject()));
                            languageName.setOutputMarkupId(true);
                            listItem.add(languageName);
                        }

                        TextPanel<String> translation = new TextPanel<String>(ID_TRANSLATION, Model.of(languagesMap.get(listItem.getModelObject())));
                        translation.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                        translation.setOutputMarkupId(true);
                        listItem.add(translation);

            }
        };
        languagesContainer.setOutputMarkupId(true);
        fullDataContainer.add(languagesContainer);

        AjaxButton addLanguageButton = new AjaxButton(ID_ADD_LANGUAGE, createStringResource("PolyStringEditorPanel.addLanguage")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addNewLanguagePerformed(target);
            }
        };
        addLanguageButton.add(AttributeAppender.append("style", "cursor: pointer;"));
        addLanguageButton.setOutputMarkupId(true);
        fullDataContainer.add(addLanguageButton);

        AjaxButton showHideLanguagesButton = new AjaxButton(ID_SHOW_HIDE_LANGUAGES, new LoadableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return showFullData ? createStringResource("PolyStringEditorPanel.hideLanguages").getString() :
                        createStringResource("PolyStringEditorPanel.showLanguages").getString();
            }
        }) {

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

    private LoadableModel<List<String>> getLanguagesListModel(){
        return new LoadableModel<List<String>>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<String> load() {
                Map<String, String> languagesMap = getModelObject() != null && getModelObject().getLang() != null ? getModelObject().getLang() : new HashMap<>();
                return new ArrayList<>(languagesMap.keySet());
            }
        };
    }

    private void addNewLanguagePerformed(AjaxRequestTarget target){
        if (getModelObject().getLang() == null){
            getModelObject().setLang(new HashMap<String, String>());
        }
        getModelObject().getLang().put("", "");
        target.add(PolyStringEditorPanel.this);

    }
}
