/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.ResourceObjectTypeWizardPreviewPanel.ResourceObjectTypePreviewTileType;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.schema.util.ShadowUtil;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.model.ContainerValueWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteQNamePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.ResourceContentRepositoryPanel;
import com.evolveum.midpoint.web.page.admin.resources.ResourceContentResourcePanel;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.ResourceContentSearchDto;
import com.evolveum.midpoint.web.session.ResourceContentStorage;
import com.evolveum.midpoint.web.session.SessionStorage;

/**
 * @author katkav
 * @author semancik
 */
public class ResourceContentPanel extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ResourceContentPanel.class);

    enum Operation {
        REMOVE, MODIFY;
    }

    private static final String DOT_CLASS = ResourceContentPanel.class.getName() + ".";

    private static final String ID_TOP_TABLE_BUTTONS = "topButtons";
    private static final String ID_INTENT = "intent";
    private static final String ID_REAL_OBJECT_CLASS = "realObjectClass";
    private static final String ID_OBJECT_CLASS = "objectClass";
    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_RESOURCE_CHOICE_CONTAINER_SEARCH = "resourceChoiceContainer";
    private static final String ID_REPO_SEARCH = "repositorySearch";
    private static final String ID_RESOURCE_SEARCH = "resourceSearch";

    private static final String ID_TABLE = "table";

    private ShadowKindType kind;

    private boolean useObjectClass;
    private boolean isRepoSearch;

    private IModel<ResourceContentSearchDto> resourceContentSearch;

    public ResourceContentPanel(String id, final ShadowKindType kind,
            final ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        this(id, kind, model, config, true);
    }

    public ResourceContentPanel(String id, final ShadowKindType kind,
            final ResourceDetailsModel model, ContainerPanelConfigurationType config,
            boolean isRepoSearch) {
        super(id, model, config);

        this.kind = kind;
        this.resourceContentSearch = createContentSearchModel(kind);
        this.isRepoSearch = isRepoSearch;
        //TODO config
    }

    private IModel<ResourceContentSearchDto> createContentSearchModel(final ShadowKindType kind) {
        return new LoadableModel<>(true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected ResourceContentSearchDto load() {
                isRepoSearch = !getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT).getResourceSearch();
                return getContentStorage(kind, isRepoSearch ? SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT :
                        SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT).getContentSearch();

            }

        };

    }

    private void updateResourceContentSearch() {
        ResourceContentSearchDto searchDto = resourceContentSearch.getObject();
        getContentStorage(kind, isRepoSearch ? SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT :
                SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT).setContentSearch(searchDto);
    }

    private ResourceContentStorage getContentStorage(ShadowKindType kind, String searchMode) {
        return getObjectDetailsModels().getPageResource().getSessionStorage().getResourceContentStorage(kind, searchMode);
    }

    protected void initLayout() {
        setOutputMarkupId(true);

        RepeatingView topButtons = new RepeatingView(ID_TOP_TABLE_BUTTONS);
        topButtons.setOutputMarkupId(true);
        topButtons.add(new VisibleBehaviour(() -> isTopTableButtonsVisible()));
        add(topButtons);

        initSychronizationButton(topButtons);
        initAttributeMappingButton (topButtons);

        final Form mainForm = new MidpointForm(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        mainForm.addOrReplace(initTable(getObjectWrapperModel()));
        add(mainForm);

        AutoCompleteTextPanel<String> intent = new AutoCompleteTextPanel<>(ID_INTENT,
                new PropertyModel<>(resourceContentSearch, "intent"), String.class, false) {
            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<String> getIterator(String input) {
                ResourceSchema refinedSchema;
                try {
                    refinedSchema = ResourceSchemaFactory.getCompleteSchema(getObjectWrapper().getObject());
                    if (refinedSchema != null) {
                        return refinedSchema.getIntentsForKind(getKind()).iterator();
                    } else {
                        return Collections.emptyIterator();
                    }
                } catch (SchemaException | ConfigurationException e) {
                    return Collections.emptyIterator();
                }
            }

        };
        intent.getBaseFormComponent().add(WebComponentUtil.preventSubmitOnEnterKeyDownBehavior());
        intent.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(get(ID_REAL_OBJECT_CLASS));
                updateResourceContentSearch();
                mainForm.addOrReplace(initTable(getObjectWrapperModel()));
                target.add(mainForm);

            }
        });
        intent.setOutputMarkupId(true);
        intent.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isIntentAndObjectClassPanelVisible() && !isUseObjectClass();
            }
        });
        add(intent);

        Label realObjectClassLabel = new Label(ID_REAL_OBJECT_CLASS, new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                ResourceObjectDefinition ocDef;
                try {
                    ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(getObjectWrapper().getObject());
                    if (refinedSchema == null) {
                        return "NO SCHEMA DEFINED";
                    }
                    String intent = getIntent();
                    if (ShadowUtil.isKnown(intent)) {
                        ocDef = refinedSchema.findObjectDefinition(getKind(), intent);
                    } else {
                        ocDef = refinedSchema.findDefaultDefinitionForKind(getKind());
                    }
                    if (ocDef != null) {
                        return ocDef.getObjectClassName().getLocalPart();
                    }
                } catch (SchemaException | ConfigurationException e) {
                    // TODO?
                }

                return "NOT FOUND";
            }
        });
        realObjectClassLabel.setOutputMarkupId(true);
        add(realObjectClassLabel);

        AutoCompleteQNamePanel objectClassPanel = new AutoCompleteQNamePanel(ID_OBJECT_CLASS,
                new PropertyModel<>(resourceContentSearch, "objectClass")) {
            private static final long serialVersionUID = 1L;

            @Override
            public Collection<QName> loadChoices() {
                return createObjectClassChoices(getObjectWrapperModel());
            }

            @Override
            protected void onChange(AjaxRequestTarget target) {
                LOGGER.trace("Object class panel update: {}", isUseObjectClass());
                updateResourceContentSearch();
                mainForm.addOrReplace(initTable(getObjectWrapperModel()));
                target.add(mainForm);
            }

        };

        objectClassPanel.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isIntentAndObjectClassPanelVisible() && isUseObjectClass();
            }
        });
        add(objectClassPanel);

        WebMarkupContainer resourceChoiceContainer = new WebMarkupContainer(ID_RESOURCE_CHOICE_CONTAINER_SEARCH);
        resourceChoiceContainer.setOutputMarkupId(true);
        resourceChoiceContainer.add(new VisibleBehaviour( () -> isSourceChoiceVisible()));
        add(resourceChoiceContainer);

        AjaxLink<Boolean> repoSearch = new AjaxLink<Boolean>(ID_REPO_SEARCH,
                new PropertyModel<>(resourceContentSearch, "resourceSearch")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                isRepoSearch = true;
                getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT).setResourceSearch(Boolean.FALSE);
                getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT).setResourceSearch(Boolean.FALSE);

                resourceContentSearch.getObject().setResourceSearch(Boolean.FALSE);
                updateResourceContentSearch();
                mainForm.addOrReplace(initRepoContent(ResourceContentPanel.this.getObjectWrapperModel()));
                target.add(getParent().addOrReplace(mainForm));
                target.add(this);
                target.add(getParent().get(getPageBase().createComponentPath(ID_RESOURCE_CHOICE_CONTAINER_SEARCH, ID_RESOURCE_SEARCH))
                        .add(AttributeModifier.replace("class", "btn btn-sm btn-default")));
            }

            @Override
            protected void onBeforeRender() {
                super.onBeforeRender();
                if (!getModelObject().booleanValue()) {add(AttributeModifier.replace("class", "btn btn-sm btn-default active"));}
            }
        };
        resourceChoiceContainer.add(repoSearch);

        AjaxLink<Boolean> resourceSearch = new AjaxLink<Boolean>(ID_RESOURCE_SEARCH,
                new PropertyModel<>(resourceContentSearch, "resourceSearch")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                isRepoSearch = false;
                getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT).setResourceSearch(Boolean.TRUE);
                getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT).setResourceSearch(Boolean.TRUE);
                updateResourceContentSearch();
                resourceContentSearch.getObject().setResourceSearch(Boolean.TRUE);
                mainForm.addOrReplace(initResourceContent(ResourceContentPanel.this.getObjectWrapperModel()));
                target.add(getParent().addOrReplace(mainForm));
                target.add(this.add(AttributeModifier.append("class", " active")));
                target.add(getParent().get(getPageBase().createComponentPath(ID_RESOURCE_CHOICE_CONTAINER_SEARCH, ID_REPO_SEARCH))
                        .add(AttributeModifier.replace("class", "btn btn-sm btn-default")));
            }

            @Override
            protected void onBeforeRender() {
                super.onBeforeRender();
                getModelObject().booleanValue();
                if (getModelObject().booleanValue()) {add(AttributeModifier.replace("class", "btn btn-sm btn-default active"));}
            }
        };
        resourceChoiceContainer.add(resourceSearch);

    }

    protected boolean isSourceChoiceVisible() {
        return true;
    }

    private void initAttributeMappingButton(RepeatingView topButtons) {
        AjaxIconButton attrMappingButton = new AjaxIconButton(
                topButtons.newChildId(),
                Model.of(ResourceObjectTypePreviewTileType.ATTRIBUTE_MAPPING.getIcon()),
                getPageBase().createStringResource(ResourceObjectTypePreviewTileType.ATTRIBUTE_MAPPING)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel =
                        getResourceObjectTypeValue(target);
                if (valueModel != null) {
                    getObjectDetailsModels().getPageResource().showAttributeMappingWizard(
                            target,
                            valueModel);
                }
            }
        };
        attrMappingButton.showTitleAsLabel(true);
        attrMappingButton.add(AttributeAppender.append("class", "btn btn-primary btn p-3 flex-basis-0 flex-fill"));
        topButtons.add(attrMappingButton);
    }

    private void initSychronizationButton(RepeatingView topButtons) {
        AjaxIconButton synchConfButton = new AjaxIconButton(
                topButtons.newChildId(),
                Model.of(ResourceObjectTypePreviewTileType.SYNCHRONIZATION_CONFIG.getIcon()),
                getPageBase().createStringResource(ResourceObjectTypePreviewTileType.SYNCHRONIZATION_CONFIG)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel =
                        getResourceObjectTypeValue(target);
                if (valueModel != null) {
                    getObjectDetailsModels().getPageResource().showSynchronizationWizard(
                            target,
                            valueModel);
                }
            }
        };
        synchConfButton.showTitleAsLabel(true);
        synchConfButton.add(AttributeAppender.append("class", "btn btn-primary btn p-3 flex-fill flex-basis-0 mr-3"));
        topButtons.add(synchConfButton);
    }

    private IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getResourceObjectTypeValue(
            AjaxRequestTarget target) {

        if ((!isUseObjectClass() && resourceContentSearch.getObject().getKind() == null)
                || (isUseObjectClass() && resourceContentSearch.getObject().getObjectClass() == null)) {
            getPageBase().warn("Couldn't recognize resource object type");
            LOGGER.debug("Couldn't recognize resource object type");
            target.add(getPageBase().getFeedbackPanel());
            return null;
        }
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> foundValue = null;
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> defaultValue = null;
        try {
            PrismContainerWrapper<ResourceObjectTypeDefinitionType> container = getObjectWrapperModel().getObject().findContainer(
                    ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
            for (PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> value : container.getValues()) {
                if (!isUseObjectClass()
                        && (resourceContentSearch.getObject().getKind().equals(value.getRealValue().getKind()))
                        || kind.equals(value.getRealValue().getKind())) {
                    if (resourceContentSearch.getObject().getIntent() != null
                            && resourceContentSearch.getObject().getIntent().equals(value.getRealValue().getKind())) {
                        foundValue = value;
                    }
                    if (Boolean.TRUE.equals(value.getRealValue().isDefaultForKind())){
                        defaultValue = value;
                    }
                }
                if (isUseObjectClass()
                        && resourceContentSearch.getObject().getObjectClass().equals(value.getRealValue().getObjectClass())
                        && Boolean.TRUE.equals(value.getRealValue().isDefaultForObjectClass())) {
                    foundValue = value;
                }
                if (Boolean.TRUE.equals(value.getRealValue().isDefault() && defaultValue == null)) {
                    defaultValue = value;
                }
            }
        } catch (SchemaException e) {
            //ignore issue log error below
        }
        if (foundValue == null) {
            if (defaultValue == null) {
                getPageBase().warn("Couldn't recognize resource object type");
                LOGGER.debug("Couldn't recognize resource object type");
                target.add(getPageBase().getFeedbackPanel());
                return null;
            }
            foundValue = defaultValue;
        }
        return new ContainerValueWrapperFromObjectWrapperModel<>(getObjectWrapperModel(), foundValue.getPath());
    }

    protected boolean isTopTableButtonsVisible() {
        return true;
    }

    private List<QName> createObjectClassChoices(IModel<PrismObjectWrapper<ResourceType>> model) {
        ResourceSchema refinedSchema;
        try {
            refinedSchema = ResourceSchemaFactory.getCompleteSchema(model.getObject().getObject());
        } catch (SchemaException | ConfigurationException e) {
            warn("Could not determine defined object classes for resource");
            return new ArrayList<>();
        }
        return refinedSchema != null ?
                new ArrayList<>(refinedSchema.getObjectClassNames()) : List.of();
    }

    private com.evolveum.midpoint.web.page.admin.resources.ResourceContentPanel initTable(IModel<PrismObjectWrapper<ResourceType>> model) {
        if (isResourceSearch()) {
            return initResourceContent(getObjectWrapperModel());
        } else {
            return initRepoContent(getObjectWrapperModel());
        }
    }

    private ResourceContentResourcePanel initResourceContent(IModel<PrismObjectWrapper<ResourceType>> model) {
        String searchMode = isRepoSearch ? SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT :
                SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT;
        ResourceContentResourcePanel resourceContent = new ResourceContentResourcePanel(ID_TABLE, loadResourceModel(),
                getObjectClass(), getKind(), getIntent(), searchMode, getPanelConfiguration()) {
            @Override
            protected ResourceSchema getRefinedSchema() throws SchemaException, ConfigurationException {
                try {
                    return super.getRefinedSchema();
                } catch (SchemaException | ConfigurationException e) {
                    return getObjectDetailsModels().getRefinedSchema();
                }
            }

            @Override
            protected boolean isTaskButtonsContainerVisible() {
                return ResourceContentPanel.this.isTaskButtonsContainerVisible();
            }
        };
        resourceContent.setOutputMarkupId(true);
        return resourceContent;

    }

    protected boolean isTaskButtonsContainerVisible() {
        return true;
    }

    private ResourceContentRepositoryPanel initRepoContent(IModel<PrismObjectWrapper<ResourceType>> model) {
        String searchMode = isRepoSearch ? SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT :
                SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT;
        ResourceContentRepositoryPanel repositoryContent = new ResourceContentRepositoryPanel(ID_TABLE, loadResourceModel(),
                getObjectClass(), getKind(), getIntent(), searchMode, getPanelConfiguration()) {
            @Override
            protected ResourceSchema getRefinedSchema() throws SchemaException, ConfigurationException {
                try {
                    return super.getRefinedSchema();
                } catch (SchemaException | ConfigurationException e) {
                    return getObjectDetailsModels().getRefinedSchema();
                }
            }

            @Override
            protected boolean isTaskButtonsContainerVisible() {
                return ResourceContentPanel.this.isTaskButtonsContainerVisible();
            }
        };
        repositoryContent.setOutputMarkupId(true);
        return repositoryContent;
    }

    private LoadableModel<PrismObject<ResourceType>> loadResourceModel() {
        return new LoadableModel<>(false) {

            @Override
            protected PrismObject<ResourceType> load() {
                return getObjectWrapper().getObject();
            }
        };
    }

    private ShadowKindType getKind() {
        return resourceContentSearch.getObject().getKind();
    }

    protected String getIntent() {
        return resourceContentSearch.getObject().getIntent();
    }

    protected QName getObjectClass() {
        return resourceContentSearch.getObject().getObjectClass();
    }

    private boolean isResourceSearch() {
        Boolean isResourceSearch = resourceContentSearch.getObject().isResourceSearch();
        if (isResourceSearch == null) {
            return false;
        }
        return resourceContentSearch.getObject().isResourceSearch();
    }

    private boolean isUseObjectClass() {
        return resourceContentSearch.getObject().isUseObjectClass();
    }

    protected boolean isIntentAndObjectClassPanelVisible() {
        return true;
    }

}
