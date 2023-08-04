/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.CollectionPanelType;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.CompiledShadowCollectionView;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.ResourceObjectClassChoiceRenderer;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.shadows.ShadowTablePanel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PanelType(name = "resourceUncategorized")
@PanelInstance(identifier = "resourceUncategorized", applicableForOperation = OperationTypeType.MODIFY, applicableForType = ResourceType.class,
        display = @PanelDisplay(label = "PageResource.tab.content.others", icon = GuiStyleConstants.CLASS_SHADOW_ICON_UNKNOWN, order = 80))
public class ResourceUncategorizedPanel extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> {

    private static final String ID_OBJECT_TYPE = "objectType";
    private static final String ID_TABLE = "table";
    private static final String ID_TITLE = "title";


    public ResourceUncategorizedPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        createPanelTitle();
        createObjectTypeChoice();
        createShadowTable();
    }

    private void createPanelTitle() {
        Label title = new Label(ID_TITLE, createStringResource("ResourceUncategorizedPanel.select.objectClass.title"));
        title.add(getTitleVisibleBehaviour());
        title.setOutputMarkupId(true);
        add(title);
    }

    protected VisibleEnableBehaviour getTitleVisibleBehaviour() {
        return VisibleBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    private void createObjectTypeChoice() {
        var objectTypes = new DropDownChoicePanel<>(ID_OBJECT_TYPE,
                Model.of(getDefaultObjectClass()),
                () -> getObjectDetailsModels().getResourceObjectClassesDefinitions(),
                new ResourceObjectClassChoiceRenderer(), false);
        objectTypes.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(getShadowTable());
            }
        });
        objectTypes.setOutputMarkupId(true);
        add(objectTypes);
    }

    protected QName getDefaultObjectClass() {
        return getObjectDetailsModels().getDefaultObjectClass();
    }

    private void createShadowTable() {
        ShadowTablePanel shadowTablePanel = new ShadowTablePanel(ID_TABLE, getPanelConfiguration()) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_RESOURCE_OBJECT_CLASS_PANEL;
            }

            @Override
            public PageStorage getPageStorage() {
                return getPageBase().getSessionStorage().getResourceContentStorage(null);
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ShadowType>> createProvider() {
                SelectableBeanObjectDataProvider<ShadowType> provider = createSelectableBeanObjectDataProvider(() -> getResourceContentQuery(), null,
                        createSearchOptions());
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(null);
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                return provider;
//                return ResourceUncategorizedPanel.this.createProvider(getSearchModel(), (CompiledShadowCollectionView) getObjectCollectionView());
            }

            @Override
            protected SearchContext createAdditionalSearchContext() {
                SearchContext searchContext = new SearchContext();
                searchContext.setPanelType(CollectionPanelType.RESOURCE_SHADOW);
                searchContext.setResourceObjectDefinition(getObjectDetailsModels().findResourceObjectClassDefinition(getSelectedObjectClass()));
                return searchContext;
            }


            @Override
            public CompiledObjectCollectionView getObjectCollectionView() {
                CompiledShadowCollectionView compiledView = findContainerPanelConfig();
                if (compiledView != null) {
                    return compiledView;
                }
                return super.getObjectCollectionView();
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return new ArrayList<>(super.createToolbarButtonsList(buttonId));

            }

            @Override
            protected boolean isShadowDetailsEnabled(IModel<SelectableBean<ShadowType>> rowModel) {
                return ResourceUncategorizedPanel.this.isShadowDetailsEnabled();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                if (isEnabledInlineMenu()) {
                    return super.createInlineMenu();
                }
                return List.of();
            }
        };
        shadowTablePanel.setOutputMarkupId(true);
        add(shadowTablePanel);
    }

    protected boolean isEnabledInlineMenu() {
        return true;
    }

    protected boolean isShadowDetailsEnabled() {
        return true;
    }

    private QName getSelectedObjectClass() {
        DropDownChoicePanel<QName> objectTypeSelector = getObjectTypeSelector();
        if (objectTypeSelector != null && objectTypeSelector.getModel() != null) {
            return objectTypeSelector.getModel().getObject();
        }
        return null;
    }


    private DropDownChoicePanel<QName> getObjectTypeSelector() {
        return (DropDownChoicePanel<QName>) get(ID_OBJECT_TYPE);
    }

    public ShadowTablePanel getShadowTable() {
        return (ShadowTablePanel) get(ID_TABLE);
    }

    private ObjectQuery getResourceContentQuery() {
        return ObjectQueryUtil.createResourceAndObjectClassQuery(getObjectWrapper().getOid(), getSelectedObjectClass());
    }

    private Collection<SelectorOptions<GetOperationOptions>> createSearchOptions() {
        GetOperationOptionsBuilder builder = getPageBase().getOperationOptionsBuilder()
                .item(ShadowType.F_ASSOCIATION).dontRetrieve();
        return builder.build();
    }

}
