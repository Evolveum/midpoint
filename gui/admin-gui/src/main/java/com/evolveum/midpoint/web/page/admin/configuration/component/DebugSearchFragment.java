/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration.component;

import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.ChoiceableChoiceRenderer;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugSearchDto;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class DebugSearchFragment extends Fragment {

    private static final String ID_SEARCH = "search";
    private static final String ID_ZIP_CHECK = "zipCheck";
    private static final String ID_SHOW_ALL_ITEMS_CHECK = "showAllItemsCheck";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_OID_FILTER = "oidFilter";
    private static final String ID_SEARCH_BY_OID_BUTTON = "searchByOidButton";
    private static final String ID_RESOURCE = "resource";
    private static final String ID_OBJECT_CLASS = "objectClass";
    private static final String ID_CHOICE_CONTAINER = "choiceContainer";
    private static final String ID_CHOICE = "choice";

    public DebugSearchFragment(String id, String markupId, MarkupContainer markupProvider,
            IModel<DebugSearchDto> model, IModel<List<ObjectViewDto<ResourceType>>> resourcesModel,
            IModel<List<QName>> objectClassListModel, IModel<Boolean> showAllItemsModel) {
        super(id, markupId, markupProvider, model);

        initLayout(resourcesModel, objectClassListModel, showAllItemsModel);
    }

    private PageDebugList getPageBase() {
        Page page = getPage();
        if (page instanceof PageDebugList) {
            return (PageDebugList) page;
        }
        throw new IllegalStateException("Unexpected parent page, " + page);
    }

    private IModel<DebugSearchDto> getModel() {
        //noinspection unchecked
        return (IModel<DebugSearchDto>) getDefaultModel();
    }

    private void initLayout(IModel<List<ObjectViewDto<ResourceType>>> resourcesModel, IModel<List<QName>> objectClassListModel,
            IModel<Boolean> showAllItemsModel) {

        createSearchByOid();
        createSearchForm(resourcesModel, objectClassListModel);

        AjaxCheckBox zipCheck = new AjaxCheckBox(ID_ZIP_CHECK, new Model<>(false)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        add(zipCheck);

        AjaxCheckBox showAllItemsCheck = new AjaxCheckBox(ID_SHOW_ALL_ITEMS_CHECK, showAllItemsModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        add(showAllItemsCheck);

    }

    private void createSearchByOid() {
        TextField<String> oidFilterField = new TextField<>(ID_OID_FILTER, new PropertyModel<>(getModel(), DebugSearchDto.F_OID_FILTER));
        oidFilterField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        oidFilterField.setOutputMarkupId(true);
        oidFilterField.setOutputMarkupPlaceholderTag(true);
        add(oidFilterField);

        AjaxSubmitButton searchByOidButton = new AjaxSubmitButton(ID_SEARCH_BY_OID_BUTTON) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                searchPerformed(target, true);
            }
        };
        searchByOidButton.setOutputMarkupId(true);
        add(searchByOidButton);
    }

    private void createSearchForm(IModel<List<ObjectViewDto<ResourceType>>> resourcesModel, IModel<List<QName>> objectClassListModel) {
        final Form<?> searchForm = new MidpointForm<>(ID_SEARCH_FORM);
        add(searchForm);
        searchForm.setOutputMarkupId(true);
        searchForm.add(createTypePanel());
        searchForm.add(createResourcePanel(resourcesModel));
        searchForm.add(createObjectClassPanel(objectClassListModel));
        searchForm.add(createSearchPanel());
    }

    private WebMarkupContainer createTypePanel() {
        EnumChoiceRenderer<ObjectTypes> renderer = new EnumChoiceRenderer<ObjectTypes>() {

            protected String resourceKey(ObjectTypes object) {
                ObjectTypeGuiDescriptor descriptor = ObjectTypeGuiDescriptor.getDescriptor(object);
                if (descriptor == null) {
                    return ObjectTypeGuiDescriptor.ERROR_LOCALIZATION_KEY;
                }

                return descriptor.getLocalizationKey();
            }
        };

        WebMarkupContainer choiceContainer = new WebMarkupContainer(ID_CHOICE_CONTAINER);
        choiceContainer.setOutputMarkupId(true);

        DropDownChoicePanel<ObjectTypes> choice = new DropDownChoicePanel<>(ID_CHOICE,
                new PropertyModel<>(getModel(), DebugSearchDto.F_TYPE), createChoiceModel(), renderer);
        choiceContainer.add(choice);
        choice.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                DebugSearchDto searchDto = DebugSearchFragment.this.getModel().getObject();
                searchDto.setType(choice.getModel().getObject());
                searchDto.setSearch(null);
                target.add(getSearchPanel());
                searchPerformed(target);
            }
        });

        return choiceContainer;
    }

    private WebMarkupContainer createResourcePanel(IModel<List<ObjectViewDto<ResourceType>>> resourcesModel) {
        DropDownChoicePanel<ObjectViewDto<ResourceType>> resource = new DropDownChoicePanel<>(ID_RESOURCE,
                new PropertyModel<>(getModel(), DebugSearchDto.F_RESOURCE), resourcesModel,
                createResourceRenderer(), true);
        resource.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // nothing to do, it's here just to update model
            }
        });
        resource.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                DebugSearchDto searchDto = getModel().getObject();
                searchDto.setObjectClass(null);
                searchPerformed(target);
            }
        });
        resource.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                DebugSearchDto dto = getModel().getObject();
                return ObjectTypes.SHADOW.equals(dto.getType());
            }
        });
        return resource;
    }

    private WebMarkupContainer createObjectClassPanel(IModel<List<QName>> objectClassListModel) {
        DropDownChoicePanel<QName> objectClass = new DropDownChoicePanel<>(ID_OBJECT_CLASS,
                new PropertyModel<>(getModel(), DebugSearchDto.F_OBJECT_CLASS), objectClassListModel,
                new QNameIChoiceRenderer(""), true);
        objectClass.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // nothing to do, it's here just to update model
            }
        });
        objectClass.getBaseFormComponent().add(AttributeAppender.append("title",
                PageBase.createStringResourceStatic(objectClass, "pageDebugList.objectClass")));
        objectClass.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                searchPerformed(target);
            }
        });
        objectClass.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                DebugSearchDto dto = getModel().getObject();
                return ObjectTypes.SHADOW.equals(dto.getType());
            }

            @Override
            public boolean isEnabled() {
                DebugSearchDto dto = getModel().getObject();
                return dto.getResource() != null && StringUtils.isNotEmpty(dto.getResource().getOid());
            }
        });
        return objectClass;
    }

    private WebMarkupContainer createSearchPanel() {
        SearchPanel searchPanel = new SearchPanel(ID_SEARCH,
                new PropertyModel<>(getModel(), DebugSearchDto.F_SEARCH)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
                DebugSearchFragment.this.searchPerformed(target);
            }
        };
        searchPanel.setOutputMarkupId(true);
        return searchPanel;
    }

    public AjaxCheckBox getZipCheck() {
        return (AjaxCheckBox) get(ID_ZIP_CHECK);
    }

    private IModel<List<ObjectTypes>> createChoiceModel() {
        return new LoadableModel<List<ObjectTypes>>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<ObjectTypes> load() {
                List<ObjectTypes> choices = WebComponentUtil.createObjectTypesList();
                choices.remove(ObjectTypes.OBJECT);
                return choices;
            }
        };
    }

    private IChoiceRenderer<ObjectViewDto<ResourceType>> createResourceRenderer() {
        return new ChoiceableChoiceRenderer<ObjectViewDto<ResourceType>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(ObjectViewDto<ResourceType> object) {
                if (object == null) {
                    return getString("pageDebugList.resource");
                }
                return object.getName();
            }

        };
    }

    private void searchPerformed(AjaxRequestTarget target) {
        searchPerformed(target, false);
    }

    protected void searchPerformed(AjaxRequestTarget target, boolean oidFilter) {
    }

    private SearchPanel getSearchPanel() {
        return (SearchPanel) get(ID_SEARCH_FORM).get(ID_SEARCH);
    }
}
