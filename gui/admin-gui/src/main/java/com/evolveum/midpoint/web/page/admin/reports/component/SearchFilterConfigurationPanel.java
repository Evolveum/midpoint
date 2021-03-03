/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.SearchFilterTypeModel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.BasicSearchFilterModel;
import com.evolveum.midpoint.web.component.search.SearchPropertiesConfigPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;

/**
 * @author honchar
 */
public class SearchFilterConfigurationPanel<O extends ObjectType> extends BasePanel<SearchFilterType> {

    private enum FiledType {
        XML, DNS
    }

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SearchFilterConfigurationPanel.class);

    private static final String ID_ACE_EDITOR_XML_CONTAINER = "aceEditorContainer";
    private static final String ID_ACE_EDITOR_XML_FIELD = "aceEditorField";
    private static final String ID_TEXT_DNS_CONTAINER = "textFieldContainer";
    private static final String ID_TEXT_DNS_FIELD = "textField";
    private static final String ID_CONFIGURE_BUTTON = "configureButton";
    private static final String ID_FILED_TYPE_BUTTON = "fieldTypeButton";

    private LoadableModel<Class<O>> filterTypeModel;
    private PrismContainerValueWrapper<ObjectCollectionType> containerWrapper;
    private final FiledType fieldType;

    public SearchFilterConfigurationPanel(String id, IModel<SearchFilterType> model, PrismContainerValueWrapper<ObjectCollectionType> containerWrapper){
        super(id, model);
        this.containerWrapper = containerWrapper;
        if (model.getObject() != null && StringUtils.isNotBlank(model.getObject().getText())) {
            fieldType = FiledType.DNS;
        } else {
            fieldType = FiledType.XML;
        }
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initFilterTypeModel();
        initLayout();
    }

    private void initFilterTypeModel(){
        filterTypeModel = new LoadableModel<Class<O>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Class<O> load() {
                QName filterType = null;
                if (containerWrapper != null) {
                    ObjectCollectionType collectionObj = containerWrapper.getRealValue();
                    filterType = collectionObj.getType() != null ? collectionObj.getType() : ObjectType.COMPLEX_TYPE;
                }
                return (Class<O>) WebComponentUtil.qnameToClass(SearchFilterConfigurationPanel.this.getPageBase().getPrismContext(),
                        filterType == null ? ObjectType.COMPLEX_TYPE : filterType);
            }
        };
    }

    private void initLayout() {
        WebMarkupContainer aceEditorContainer = new WebMarkupContainer(ID_ACE_EDITOR_XML_CONTAINER);
        aceEditorContainer.setOutputMarkupId(true);
        aceEditorContainer.add(new VisibleBehaviour(() -> FiledType.XML.equals(fieldType)));
        aceEditorContainer.add(AttributeAppender.append("class", (IModel<?>) () -> {
            if (containerWrapper != null) {
                return "col-md-11";
            }
            return "col-md-12";
        }));
        add(aceEditorContainer);

        AceEditorPanel aceEditorField = new AceEditorPanel(ID_ACE_EDITOR_XML_FIELD, null, new SearchFilterTypeModel(getModel(), getPageBase()), 10);
        aceEditorField.setOutputMarkupId(true);
        aceEditorContainer.add(aceEditorField);

        WebMarkupContainer textFieldContainer = new WebMarkupContainer(ID_TEXT_DNS_CONTAINER);
        textFieldContainer.setOutputMarkupId(true);
        textFieldContainer.add(new VisibleBehaviour(() -> FiledType.DNS.equals(fieldType)));
        add(textFieldContainer);

        TextPanel textPanel = new TextPanel(ID_TEXT_DNS_FIELD, new PropertyModel(getModel(), "text"));
        textPanel.setOutputMarkupId(true);
        textFieldContainer.add(textPanel);

        AjaxButton searchConfigurationButton = new AjaxButton(ID_CONFIGURE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                searchConfigurationPerformed(target);
            }
        };
        searchConfigurationButton.setOutputMarkupId(true);
        searchConfigurationButton.add(new VisibleBehaviour(() -> FiledType.XML.equals(fieldType) && containerWrapper != null));
        add(searchConfigurationButton);

//        IModel<String> labelModel = (IModel) () -> {
//            if (FiledType.DNS.equals(fieldType)) {
//                return FiledType.XML.toString();
//            }
//            return FiledType.DNS.toString();
//            };
//
//        AjaxButton filedTypeButton = new AjaxButton(ID_FILED_TYPE_BUTTON, labelModel) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                if (FiledType.DNS.equals(fieldType)) {
//                    fieldType = FiledType.XML;
//                } else {
//                    fieldType = FiledType.DNS;
//                }
//                target.add(getAceEditorContainer());
//                target.add(getTextFieldContainer());
//                target.add(this);
//            }
//        };
//        filedTypeButton.setOutputMarkupId(true);
//        add(filedTypeButton);

    }

    private void searchConfigurationPerformed(AjaxRequestTarget target) {
        filterTypeModel.reset();
        SearchPropertiesConfigPanel<O> configPanel = new SearchPropertiesConfigPanel<O>(getPageBase().getMainPopupBodyId(),
                new BasicSearchFilterModel<O>(getModel(), filterTypeModel.getObject(), getPageBase()), filterTypeModel){
            private static final long serialVersionUID = 1L;

            @Override
            protected void filterConfiguredPerformed(ObjectFilter configuredFilter, AjaxRequestTarget target){
                getPageBase().hideMainPopup(target);

                try {
                    if (configuredFilter == null) {
                        return;
                    }
                    SearchFilterConfigurationPanel.this.getModel().setObject(SearchFilterConfigurationPanel.this.getPageBase().getPrismContext().getQueryConverter().createSearchFilterType(configuredFilter));
                    target.add(getAceEditorContainer());
                    target.add(getTextFieldContainer());
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize filter", e);
                }
            }
        };
        getPageBase().showMainPopup(configPanel, target);
    }

    private Component getAceEditorContainer(){
        return get(ID_ACE_EDITOR_XML_FIELD);
    }

    private Component getTextFieldContainer(){
        return get(ID_TEXT_DNS_CONTAINER);
    }
}
