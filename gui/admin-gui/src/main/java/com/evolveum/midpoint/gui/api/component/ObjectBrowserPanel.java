/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.SearchConfigurationWrapper;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.gui.impl.component.search.AbstractSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ObjectBrowserPanel<O extends ObjectType> extends BasePanel<O> implements Popupable {

    private static final long serialVersionUID = 1L;
    private static final String ID_TYPE = "type";
    private static final String ID_TYPE_PANEL = "typePanel";
    private static final String ID_TABLE = "table";
    private static final String ID_WARNING_MESSAGE = "warningMessage";

    private static final String ID_BUTTON_ADD = "addButton";
    private static final String ID_BUTTON_CANCEL = "cancelButton";

    private IModel<ObjectTypes> typeModel;

    private PageBase parentPage;
    private ObjectFilter queryFilter;
    private List<O> selectedObjectsList = new ArrayList<>();
    private Class<? extends O> defaultType;
    private List<QName> supportedTypes = new ArrayList<>();
    boolean multiselect;

    /**
     * @param defaultType specifies type of the object that will be selected by default
     */
    public ObjectBrowserPanel(String id, final Class<? extends O> defaultType, List<QName> supportedTypes, boolean multiselect,
                              PageBase parentPage) {
        this(id, defaultType, supportedTypes, multiselect, parentPage, null);
    }

    /**
     * @param defaultType specifies type of the object that will be selected by default
     */
    public ObjectBrowserPanel(String id, final Class<? extends O> defaultType, List<QName> supportedTypes, boolean multiselect,
                              PageBase parentPage, ObjectFilter queryFilter) {
        this(id, defaultType, supportedTypes, multiselect, parentPage, queryFilter, new ArrayList<>());
    }

    public ObjectBrowserPanel(String id, final Class<? extends O> defaultType, List<QName> supportedTypes, boolean multiselect,
                              PageBase parentPage, ObjectFilter queryFilter, List<O> selectedData) {
        super(id);
        this.parentPage = parentPage;
        this.queryFilter = queryFilter;
        this.selectedObjectsList = selectedData;
        if (defaultType == null) {
            if (supportedTypes == null || supportedTypes.isEmpty()) {
                this.defaultType = (Class<? extends O>) ObjectType.class;
            }
            this.defaultType =
                    (Class<? extends O>) WebComponentUtil.qnameToClass(parentPage.getPrismContext(),
                            supportedTypes.iterator().next());
        } else {
            this.defaultType = defaultType;
        }
        typeModel = new LoadableModel<>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectTypes load() {
                if (defaultType == null) {
                    return null;
                }
                return ObjectTypes.getObjectType(defaultType);
            }

            @Override
            public void setObject(ObjectTypes object) {
                if (object == null) {
                    object = ObjectTypes.getObjectType(ObjectBrowserPanel.this.defaultType);
                }
                super.setObject(object);
            }
        };
        this.supportedTypes = supportedTypes;
        this.multiselect = multiselect;
    }

    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MessagePanel warningMessage = new MessagePanel(ID_WARNING_MESSAGE, MessagePanel.MessagePanelType.WARN, getWarningMessageModel());
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> getWarningMessageModel() != null));
        add(warningMessage);

        List<ObjectTypes> supported = new ArrayList<>();
        for (QName qname : supportedTypes) {
            supported.add(ObjectTypes.getObjectTypeFromTypeQName(qname));
        }

        WebMarkupContainer typePanel = new WebMarkupContainer(ID_TYPE_PANEL);
        typePanel.setOutputMarkupId(true);
        typePanel.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return supportedTypes.size() != 1;
            }
        });
        add(typePanel);

        DropDownChoice<ObjectTypes> typeSelect = new DropDownChoice<>(ID_TYPE, typeModel,
            new ListModel<>(supported), new EnumChoiceRenderer<>(this));
        typeSelect.add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                ObjectListPanel<O> listPanel = (ObjectListPanel<O>) get(ID_TABLE);

                listPanel = createObjectListPanel(typeModel.getObject(), multiselect);
                addOrReplace(listPanel);
                target.add(listPanel);
            }
        });
        typePanel.add(typeSelect);

        ObjectTypes objType = defaultType != null ? ObjectTypes.getObjectType(defaultType) : null;
        ObjectListPanel<O> listPanel = createObjectListPanel(objType, multiselect);
        add(listPanel);

        AjaxButton addButton = new AjaxButton(ID_BUTTON_ADD,
                createStringResource("userBrowserDialog.button.addButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<O> selected = ((PopupObjectListPanel) getParent().get(ID_TABLE)).getSelectedRealObjects();
                ObjectTypes type = ObjectBrowserPanel.this.typeModel.getObject();
                QName qname = type != null ? type.getTypeQName() : null;
                ObjectBrowserPanel.this.addPerformed(target, qname, selected);
            }
        };

        addButton.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return multiselect;
            }
        });

        add(addButton);

        AjaxButton cancelButton = new AjaxButton(ID_BUTTON_CANCEL,
                createStringResource("Button.cancel")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                getPageBase().hideMainPopup(ajaxRequestTarget);
            }
        };
        add(cancelButton);
    }

    protected void onClick(AjaxRequestTarget target, O focus) {
        parentPage.hideMainPopup(target);
    }

    protected void onSelectPerformed(AjaxRequestTarget target, O focus) {
        parentPage.hideMainPopup(target);
    }

    private ObjectListPanel<O> createObjectListPanel(ObjectTypes type, final boolean multiselect) {
        Class<O> typeClass = type.getClassDefinition();

        PopupObjectListPanel<O> listPanel = new PopupObjectListPanel<>(ID_TABLE, typeClass, getOptions(),
                multiselect) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, O object) {
                ObjectBrowserPanel.this.onSelectPerformed(target, object);
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                ObjectQuery query = null;
                if (queryFilter != null) {
                    query = parentPage.getPrismContext().queryFactory().createQuery(queryFilter);
                }
                return query;
            }

            @Override
            protected List<O> getPreselectedObjectList() {
                return selectedObjectsList;
            }

            @Override
            public List<O> getSelectedRealObjects() {
                return getPreselectedObjectList();
            }

            @Override
            protected Search createSearch(Class<O> type) {
                String collectionName = isCollectionViewPanelForCompiledView() ?
                        WebComponentUtil.getCollectionNameParameterValue(getPageBase()).toString() : null;
                return SearchFactory.createSearch(createSearchConfigWrapper(type, collectionName), getPageBase());
//                Search search = super.createSearch(type);
//                getSpecialSearchItemWrappers()
//                        .forEach(function -> search.addSpecialItem(function.apply(search)));
//                return search;
            }
        };
        listPanel.setOutputMarkupId(true);
        return listPanel;
    }

    private SearchConfigurationWrapper<O> createSearchConfigWrapper(Class<O> type, String collectionViewName) {
        SearchConfigurationWrapper searchConfigWrapper = SearchFactory.createDefaultSearchBoxConfigurationWrapper(type, getPageBase());
        searchConfigWrapper.setCollectionViewName(collectionViewName);
        searchConfigWrapper.getItemsList().addAll(new ArrayList(getSpecialSearchItemWrappers()));
        return searchConfigWrapper;
    }

    protected Set<SerializableSupplier<AbstractSearchItemWrapper>> getSpecialSearchItemWrappers() {
        return Collections.emptySet();
    }

    protected void addPerformed(AjaxRequestTarget target, QName type, List<O> selected) {
        parentPage.hideMainPopup(target);
    }

    private Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        if (ObjectTypes.SHADOW.getTypeQName().equals(typeModel.getObject() != null ? typeModel.getObject().getTypeQName() : null)) {
            return getSchemaService().getOperationOptionsBuilder().noFetch().build();
        }
        return null;
    }

    protected IModel<String> getWarningMessageModel(){
        return null;
    }

    @Override
    public int getWidth() {
        return 900;
    }

    @Override
    public int getHeight() {
        return 700;
    }

    @Override
    public String getWidthUnit(){
        return "px";
    }

    @Override
    public String getHeightUnit(){
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return parentPage.createStringResource("ObjectBrowserPanel.chooseObject");
    }
}
