/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.component;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.*;

/**
 * Created by honchar
 */
public class PolyStringEditorPanel extends InputPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_LOCALIZED_VALUE_CONTAINER = "localizedValueContainer";
    private static final String ID_LOCALIZED_VALUE_LABEL = "localizedValueLabel";
    private static final String ID_LOCALIZED_VALUE_PANEL = "localizedValue";
    private static final String ID_FULL_DATA_CONTAINER = "fullDataContainer";
    private static final String ID_ORIGIN_VALUE_CONTAINER = "originValueContainer";
    private static final String ID_ORIG_VALUE_LABEL = "originValueLabel";
    private static final String ID_ORIG_VALUE = "origValue";
    private static final String ID_ORIG_VALUE_WITH_BUTTON = "origValueWithButton";
    private static final String ID_KEY_VALUE = "keyValue";
    private static final String ID_LANGUAGES_REPEATER = "languagesRepeater";
    private static final String ID_LANGUAGE_NAME = "languageName";
    private static final String ID_TRANSLATION = "translation";
    private static final String ID_SHOW_HIDE_LANGUAGES_ORIG = "showHideLanguagesOrig";
    private static final String ID_SHOW_HIDE_LANGUAGES_LOCALIZED = "showHideLanguagesLocalized";
    private static final String ID_LOCALIZED_VALUE_WITH_BUTTON = "localizedValueWithButton";
    private static final String ID_LANGUAGE_EDITOR = "languageEditor";
    private static final String ID_LANGUAGES_LIST = "languagesList";
    private static final String ID_VALUE_TO_ADD = "valueToAdd";
    private static final String ID_ADD_LANGUAGE_VALUE_BUTTON = "addLanguageValue";
    private static final String ID_REMOVE_LANGUAGE_BUTTON = "removeLanguageButton";

    private boolean showFullData = false;
    private final StringBuilder currentlySelectedLang = new StringBuilder();
    IModel<PolyString> model;
    private LookupTableType predefinedValuesLookupTable;
    private boolean hasValueEnumerationRef;

    public PolyStringEditorPanel(String id, IModel<PolyString> model, LookupTableType predefinedValuesLookupTable, boolean hasValueEnumerationRef){
        super(id);
        this.model = model;
        this.predefinedValuesLookupTable = predefinedValuesLookupTable;
        this.hasValueEnumerationRef = hasValueEnumerationRef;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        setOutputMarkupId(true);

        String localizedValue = getLocalizedPolyStringValue();

        WebMarkupContainer localizedValueContainer = new WebMarkupContainer(ID_LOCALIZED_VALUE_CONTAINER);
        localizedValueContainer.setOutputMarkupId(true);
        localizedValueContainer.add(getInputFieldClassAppenderForContainer());
        localizedValueContainer.add(new VisibleBehaviour(() -> showFullData || StringUtils.isNotEmpty(localizedValue)));
        add(localizedValueContainer);

        Label localizedValueLabel = new Label(ID_LOCALIZED_VALUE_LABEL, ((PageBase)getPage()).createStringResource("PolyStringEditorPanel.localizedValue"));
        localizedValueLabel.setOutputMarkupId(true);
        localizedValueLabel.add(new VisibleBehaviour(() -> showFullData));
        localizedValueContainer.add(localizedValueLabel);

        WebMarkupContainer localizedValueWithButton = new WebMarkupContainer(ID_LOCALIZED_VALUE_WITH_BUTTON);
        localizedValueWithButton.setOutputMarkupId(true);
        localizedValueWithButton.add(getInputFieldClassAppender());
        localizedValueContainer.add(localizedValueWithButton);

        TextPanel<String> localizedValuePanel = new TextPanel<String>(ID_LOCALIZED_VALUE_PANEL, Model.of(localizedValue));
        localizedValuePanel.setOutputMarkupId(true);
        localizedValuePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        localizedValuePanel.add(new EnableBehaviour(() -> false));
        localizedValueWithButton.add(localizedValuePanel);

        AjaxButton showHideLanguagesLocalizedButton = new AjaxButton(ID_SHOW_HIDE_LANGUAGES_LOCALIZED) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showHideLanguagesPerformed(target);
            }
        };
        showHideLanguagesLocalizedButton.setOutputMarkupId(true);
//        showHideLanguagesLocalizedButton.add(AttributeAppender.append("style", "cursor: pointer;"));
        localizedValueWithButton.add(showHideLanguagesLocalizedButton);

        WebMarkupContainer originValueContainer = new WebMarkupContainer(ID_ORIGIN_VALUE_CONTAINER);
        originValueContainer.setOutputMarkupId(true);
        originValueContainer.add(getInputFieldClassAppenderForContainer());
        originValueContainer.add(new VisibleBehaviour(() -> showFullData || StringUtils.isEmpty(localizedValue)));
        add(originValueContainer);

        Label origValueLabel = new Label(ID_ORIG_VALUE_LABEL, ((PageBase) getPage()).createStringResource("PolyStringEditorPanel.origValue"));
        origValueLabel.setOutputMarkupId(true);
        origValueLabel.add(new VisibleBehaviour(() -> showFullData));
        originValueContainer.add(origValueLabel);

        WebMarkupContainer origValueWithButton = new WebMarkupContainer(ID_ORIG_VALUE_WITH_BUTTON);
        origValueWithButton.add(getInputFieldClassAppender());
        origValueWithButton.setOutputMarkupId(true);
        originValueContainer.add(origValueWithButton);

        InputPanel origValuePanel;
        IModel<String> origValueModel = new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getModelObject() != null ? getModelObject().getOrig() : null;
            }

            @Override
            public void setObject(String object) {
                PolyString oldModelObject = getModelObject();
                if (oldModelObject != null && (oldModelObject.getTranslation() != null || MapUtils.isNotEmpty(oldModelObject.getLang()))) {
                    getModel().setObject(new PolyString(object, oldModelObject.getNorm(), oldModelObject.getTranslation(), oldModelObject.getLang()));
                } else if (StringUtils.isNotBlank(object)) {
                    getModel().setObject(new PolyString(object));
                } else {
                    getModel().setObject(null);
                }
            }

            @Override
            public void detach() {

            }
        };
        if (predefinedValuesLookupTable == null) {
            origValuePanel = new TextPanel<String>(ID_ORIG_VALUE, origValueModel, String.class, false);
        } else {
            origValuePanel = new AutoCompleteTextPanel<String>(ID_ORIG_VALUE, origValueModel, String.class,
                    hasValueEnumerationRef, predefinedValuesLookupTable) {

                private static final long serialVersionUID = 1L;

                @Override
                public Iterator<String> getIterator(String input) {
                    return getPredefinedValuesIterator(input).iterator();
                }
            };
        }
        origValuePanel.setOutputMarkupId(true);
        origValuePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        origValueWithButton.add(origValuePanel);

        WebMarkupContainer fullDataContainer = new WebMarkupContainer(ID_FULL_DATA_CONTAINER);
        fullDataContainer.setOutputMarkupId(true);
        fullDataContainer.add(new VisibleBehaviour(() -> showFullData));
        add(fullDataContainer);

        TextPanel<String> keyValue = new TextPanel<String>(ID_KEY_VALUE, new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getModelObject() != null && getModelObject().getTranslation() != null ? getModelObject().getTranslation().getKey() : null;
            }

            @Override
            public void setObject(String object) {
                if (getModelObject() == null){
                    getModel().setObject(new PolyString(""));
                }
                if (getModelObject().getTranslation() == null){
                    getModelObject().setTranslation(new PolyStringTranslationType());
                }
                getModelObject().getTranslation().setKey(object);
            }

            @Override
            public void detach() {

            }
        });
        keyValue.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        keyValue.setOutputMarkupId(true);
        fullDataContainer.add(keyValue);

        IModel<String> langChoiceModel = Model.of();
        WebMarkupContainer languageEditorContainer = new WebMarkupContainer(ID_LANGUAGE_EDITOR);
        languageEditorContainer.setOutputMarkupId(true);
        languageEditorContainer.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(getLanguageChoicesModel().getObject())));
        fullDataContainer.add(languageEditorContainer);

        final DropDownChoicePanel<String> languageChoicePanel = new DropDownChoicePanel<String>(ID_LANGUAGES_LIST, langChoiceModel,
                getLanguageChoicesModel(), true);
        languageChoicePanel.setOutputMarkupId(true);
        languageChoicePanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        languageChoicePanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                removeLanguageValue(currentlySelectedLang.toString());
                clearSelectedLanguageValue();
                currentlySelectedLang.append(getLanguagesChoicePanel().getBaseFormComponent().getModelObject());
            }
        });
        languageEditorContainer.add(languageChoicePanel);

        final TextPanel<String> newLanguageValue = new TextPanel<String>(ID_VALUE_TO_ADD, Model.of());
        newLanguageValue.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateLanguageValue(languageChoicePanel.getBaseFormComponent().getModelObject(), newLanguageValue.getBaseFormComponent().getValue());
            }
        });
        languageChoicePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateLanguageValue(languageChoicePanel.getBaseFormComponent().getModelObject(), newLanguageValue.getBaseFormComponent().getValue());
            }
        });
        newLanguageValue.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                updateLanguageValue(languageChoicePanel.getBaseFormComponent().getModelObject(), newLanguageValue.getBaseFormComponent().getValue());
            }
        });
        newLanguageValue.setOutputMarkupId(true);
        languageEditorContainer.add(newLanguageValue);

        AjaxLink<Void> addLanguageButton = new AjaxLink<Void>(ID_ADD_LANGUAGE_VALUE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                updateLanguageValue(languageChoicePanel.getModel().getObject(),
                        newLanguageValue.getBaseFormComponent().getModelObject());
                languageChoicePanel.getModel().setObject(null);
                newLanguageValue.getBaseFormComponent().getModel().setObject(null);
                clearSelectedLanguageValue();
                target.add(PolyStringEditorPanel.this);
            }
        };
        addLanguageButton.add(AttributeAppender.append("style", "cursor: pointer;"));
        addLanguageButton.setOutputMarkupId(true);
        languageEditorContainer.add(addLanguageButton);

        Map<String, String> languagesMap = getModelObject() != null && getModelObject().getLang() != null ? getModelObject().getLang() : new HashMap<>();
        ListView<String> languagesContainer =
                new ListView<String>(ID_LANGUAGES_REPEATER, getLanguagesListModel()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<String> listItem) {

                        TextPanel<String> languageName = new TextPanel<String>(ID_LANGUAGE_NAME, listItem.getModel());
                        languageName.add(new EnableBehaviour(() -> false));
                        languageName.setOutputMarkupId(true);
                        listItem.add(languageName);

                        TextPanel<String> translation = new TextPanel<String>(ID_TRANSLATION, Model.of(getLanguageValueByKey(listItem.getModelObject())));
                        translation.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour(){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                updateLanguageValue(languageName.getBaseFormComponent().getValue(), translation.getBaseFormComponent().getValue());
                            }
                        });
                        translation.setOutputMarkupId(true);
                        translation.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                                updateLanguageValue(languageName.getBaseFormComponent().getValue(), translation.getBaseFormComponent().getValue());
                            }
                        });
                        listItem.add(translation);

                        AjaxLink<Void> removeButton = new AjaxLink<Void>(ID_REMOVE_LANGUAGE_BUTTON) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                removeLanguageValue(listItem.getModelObject());
                                target.add(PolyStringEditorPanel.this);
                            }
                        };
                        removeButton.setOutputMarkupId(true);
                        listItem.add(removeButton);
            }
        };
        languagesContainer.setOutputMarkupId(true);
        fullDataContainer.add(languagesContainer);

        AjaxButton showHideLanguagesButton = new AjaxButton(ID_SHOW_HIDE_LANGUAGES_ORIG) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showHideLanguagesPerformed(target);
            }
        };
        showHideLanguagesButton.setOutputMarkupId(true);
//        showHideLanguagesButton.add(AttributeAppender.append("style", "cursor: pointer;"));
        origValueWithButton.add(showHideLanguagesButton);

    }

    private String getLocalizedPolyStringValue(){
        return ((PageBase) getPage()).getLocalizationService().translate(getModelObject(), WebComponentUtil.getCurrentLocale(), false);
    }

    private IModel<List<String>> getLanguageChoicesModel(){
        return new IModel<List<String>>() {
            @Override
            public List<String> getObject() {
                List<String> allLanguagesList =  new ArrayList<>();
                String currentlySelectedLang = getLanguagesChoicePanel().getBaseFormComponent().getModel().getObject();
                MidPointApplication.AVAILABLE_LOCALES.forEach(locale -> {
                    String localeValue = locale.getLocale().getLanguage();
                    if (!isPolyStringLangNotNull()) {
                        allLanguagesList.add(localeValue);
                    } else if (!languageExists(localeValue) || localeValue.equals(currentlySelectedLang)){
                        allLanguagesList.add(locale.getLocale().getLanguage());
                    }
                });
                return allLanguagesList;
            }
        };
    }

    private IModel<List<String>> getLanguagesListModel(){
        return new IModel<List<String>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<String> getObject() {
                Map<String, String> languagesMap = isPolyStringLangNotNull() ? getModelObject().getLang() : new HashMap<>();
                List<String> languagesList = new ArrayList<>(languagesMap.keySet());
                String valueToExclude = getLanguagesChoicePanel().getBaseFormComponent().getModel().getObject();
                if (StringUtils.isNotEmpty(valueToExclude)) {
                    languagesList.remove(valueToExclude);
                }
                Collections.sort(languagesList);
                return languagesList;
            }
        };
    }

    private void addNewLanguagePerformed(AjaxRequestTarget target){
        if (getModelObject() == null){
            Map<String, String> languagesMap = new HashMap<>();
            languagesMap.put("", "");
            getModel().setObject(new PolyString(null, null, null, languagesMap));
        } else {
            if (getModelObject().getLang() == null) {
                getModelObject().setLang(new HashMap<String, String>());
            }
            getModelObject().getLang().put("", "");
        }
        target.add(PolyStringEditorPanel.this);
    }

    private void showHideLanguagesPerformed(AjaxRequestTarget target){
        showFullData = !showFullData;
        target.add(PolyStringEditorPanel.this);
    }

    private AttributeAppender getInputFieldClassAppenderForContainer(){
        return AttributeModifier.append("class", new LoadableModel<String>() {
            @Override
            protected String load() {
                return showFullData ? "prism-property" : "";
            }
        });
    }

    private AttributeAppender getInputFieldClassAppender(){
        return AttributeModifier.append("class", new LoadableModel<String>() {
            @Override
            protected String load() {
                return showFullData ? "col-lg-9 col-md-9 col-sm-9" : "col-lg-12 col-md-12 col-sm-12";
            }
        });
    }

    private InputPanel getOrigValuePanel(){
        return (InputPanel) get(ID_ORIGIN_VALUE_CONTAINER).get(ID_ORIG_VALUE_WITH_BUTTON).get(ID_ORIG_VALUE);
    }

    //todo refactor with PolyStringWrapper
    private void updateLanguageValue(String language, String value){
        if (StringUtils.isEmpty(language)){
            return;
        }
        if (getModelObject() == null){
            Map<String, String> languagesMap = new HashMap<>();
            languagesMap.put("", "");
            getModel().setObject(new PolyString(null, null, null, languagesMap));
            return;
        }
        if (getModelObject().getLang() == null){
            getModelObject().setLang(new HashMap<>());
        }
        if (getModelObject().getLang().containsKey(language)){
            getModelObject().getLang().replace(language, value);
        } else {
            getModelObject().getLang().put(language, value);
        }
    }

    private void removeLanguageValue(String language){
        if (!isPolyStringLangNotNull()){
            return;
        }
        getModelObject().getLang().remove(language);
    }

    private boolean isPolyStringLangNotNull(){
        return getModelObject() != null && getModelObject().getLang() != null;
    }

    private boolean languageExists(String value){
        return isPolyStringLangNotNull() && getModelObject().getLang().containsKey(value);
    }

    private String getLanguageValueByKey(String key){
        if (!isPolyStringLangNotNull()){
            return null;
        }
        return getModelObject().getLang().get(key);
    }

    private DropDownChoicePanel<String> getLanguagesChoicePanel(){
        return (DropDownChoicePanel<String>)get(ID_FULL_DATA_CONTAINER).get(ID_LANGUAGE_EDITOR).get(ID_LANGUAGES_LIST);
    }

    private void clearSelectedLanguageValue(){
        currentlySelectedLang.delete(0, currentlySelectedLang.length());
    }

    @Override
    public FormComponent getValidatableComponent() {
        return getOrigValuePanel().getBaseFormComponent();
    }

    @Override
    public FormComponent<PolyString> getBaseFormComponent() {
        return getOrigValuePanel().getBaseFormComponent();
    }

    private IModel<PolyString> getModel(){
        return model;
    }

    private PolyString getModelObject(){
        return model == null ? null : model.getObject();
    }

    private List<String> getPredefinedValuesIterator(String input) {
        return WebComponentUtil.prepareAutoCompleteList(predefinedValuesLookupTable, input,
                ((PageBase)getPage()).getLocalizationService());
    }
}
