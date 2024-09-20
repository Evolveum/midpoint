/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.page.admin.resources.SynchronizationTaskFlavor;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteQNamePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.web.page.admin.resources.ResourceContentRepositoryPanel;
import com.evolveum.midpoint.web.page.admin.resources.ResourceContentResourcePanel;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.ResourceContentSearchDto;
import com.evolveum.midpoint.web.page.admin.shadows.ShadowTablePanel;
import com.evolveum.midpoint.web.session.ResourceContentStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

    protected static final String OPERATION_RECLASSIFY_SHADOWS = DOT_CLASS + "reclassifyShadows";

    private static final String ID_TOP_TABLE_BUTTONS_CONTAINER = "topButtonsContainer";
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
                isRepoSearch = isRepoSearch();
                ResourceContentSearchDto contentSearch = getContentStorage(kind, isRepoSearch ? SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT :
                        SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT).getContentSearch();
                if (contentSearch.isUseObjectClass() && contentSearch.getObjectClass() == null) {
                    List<QName> choices = createObjectClassChoices(getObjectWrapperModel());
                    if (choices.size() == 1) {
                        contentSearch.setObjectClass(choices.iterator().next());
                    }
                }
                return contentSearch;
            }

        };

    }

    //TODO this should be fixed. It somehow doesn't make sense to look into the storage first
    // to decide which storage should be used?
    protected boolean isRepoSearch() {
        return !getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT).getResourceSearch();
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

        WebMarkupContainer topButtonsContainer = new WebMarkupContainer(ID_TOP_TABLE_BUTTONS_CONTAINER);
        topButtonsContainer.setOutputMarkupId(true);
        add(topButtonsContainer);

        RepeatingView topButtons = new RepeatingView(ID_TOP_TABLE_BUTTONS);
        topButtons.setOutputMarkupId(true);

        initBasicConfigButton(topButtons);
        initSychronizationButton(topButtons);
        initAttributeMappingButton(topButtons);
        initCorrelationButton(topButtons);
        initCapabilitiesButton(topButtons);
        initCredentialsButton(topButtons);
        initActivationsButton(topButtons);

        topButtonsContainer.add(topButtons);

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
                    refinedSchema = ResourceSchemaFactory.getCompleteSchema(getObjectWrapper().getObjectApplyDelta());
                    if (refinedSchema != null) {
                        return refinedSchema.getIntentsForKind(getKind()).iterator();
                    } else {
                        return Collections.emptyIterator();
                    }
                } catch (CommonException e) {
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
                target.add(ResourceContentPanel.this.get(ID_TOP_TABLE_BUTTONS_CONTAINER));

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
                target.add(ResourceContentPanel.this.get(ID_TOP_TABLE_BUTTONS_CONTAINER));
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
        resourceChoiceContainer.add(new VisibleBehaviour(() -> isSourceChoiceVisible()));
        add(resourceChoiceContainer);

        AjaxLink<Boolean> repoSearch = new AjaxLink<Boolean>(ID_REPO_SEARCH,
                new PropertyModel<>(resourceContentSearch, "resourceSearch")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                updateSearchButtons(true);
                replaceContentPanel(target, initRepoContent(ResourceContentPanel.this.getObjectWrapperModel()));
            }
        };

        repoSearch.add(AttributeAppender.replace("class", () -> "btn btn-sm btn-default" + (isRepoSearch ? " active" : "")));
        resourceChoiceContainer.add(repoSearch);

        AjaxLink<Boolean> resourceSearch = new AjaxLink<Boolean>(ID_RESOURCE_SEARCH,
                new PropertyModel<>(resourceContentSearch, "resourceSearch")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                updateSearchButtons(false);
                replaceContentPanel(target, initResourceContent(ResourceContentPanel.this.getObjectWrapperModel()));
            }

        };
        resourceSearch.add(AttributeAppender.replace("class", () -> "btn btn-sm btn-default" + (isRepoSearch ? "" : " active")));
        resourceChoiceContainer.add(resourceSearch);

    }

    private void updateSearchButtons(boolean repoSearch) {
        getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT).setResourceSearch(repoSearch);
        getContentStorage(kind, SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT).setResourceSearch(!repoSearch);

        resourceContentSearch.getObject().setResourceSearch(!repoSearch);
        this.isRepoSearch = repoSearch;
        updateResourceContentSearch();
    }

    private void replaceContentPanel(AjaxRequestTarget target, Panel newPanel) {
        Form mainForm = getMainForm();
        mainForm.addOrReplace(newPanel);
        target.add(mainForm);
        target.add(get(ID_RESOURCE_CHOICE_CONTAINER_SEARCH));
    }

    private Form getMainForm() {
        return (Form) get(ID_MAIN_FORM);
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
                if (valueModel != null && valueModel.getObject() != null) {
                    getObjectDetailsModels().getPageResource().showAttributeMappingWizard(
                            target,
                            valueModel.getObject().getPath());
                }
            }
        };
        attrMappingButton.setOutputMarkupId(true);
        attrMappingButton.showTitleAsLabel(true);
        attrMappingButton.add(new VisibleBehaviour(() -> isTopTableButtonsVisible()));
        topButtons.add(attrMappingButton);
    }

    private void initBasicConfigButton(RepeatingView topButtons) {
        AjaxIconButton synchConfButton = new AjaxIconButton(
                topButtons.newChildId(),
                Model.of(ResourceObjectTypePreviewTileType.BASIC.getIcon()),
                getPageBase().createStringResource(ResourceObjectTypePreviewTileType.BASIC)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel =
                        getResourceObjectTypeValue(target);
                if (valueModel != null && valueModel.getObject() != null) {
                    getObjectDetailsModels().getPageResource().showResourceObjectTypeBasicWizard(
                            target,
                            valueModel.getObject().getPath());
                }
            }
        };
        synchConfButton.setOutputMarkupId(true);
        synchConfButton.showTitleAsLabel(true);
        synchConfButton.add(new VisibleBehaviour(() -> isTopTableButtonsVisible()));
        topButtons.add(synchConfButton);
    }

    private void initSychronizationButton(RepeatingView topButtons) {
        AjaxIconButton synchConfButton = new AjaxIconButton(
                topButtons.newChildId(),
                Model.of(ResourceObjectTypePreviewTileType.SYNCHRONIZATION.getIcon()),
                getPageBase().createStringResource(ResourceObjectTypePreviewTileType.SYNCHRONIZATION)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel =
                        getResourceObjectTypeValue(target);
                if (valueModel != null && valueModel.getObject() != null) {
                    getObjectDetailsModels().getPageResource().showSynchronizationWizard(target, valueModel.getObject().getPath());
                }
            }
        };
        synchConfButton.setOutputMarkupId(true);
        synchConfButton.showTitleAsLabel(true);
        synchConfButton.add(new VisibleBehaviour(() -> isTopTableButtonsVisible()));
        topButtons.add(synchConfButton);
    }

    private void initCorrelationButton(RepeatingView topButtons) {
        AjaxIconButton correlationConfButton = new AjaxIconButton(
                topButtons.newChildId(),
                Model.of(ResourceObjectTypePreviewTileType.CORRELATION.getIcon()),
                getPageBase().createStringResource(ResourceObjectTypePreviewTileType.CORRELATION)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel =
                        getResourceObjectTypeValue(target);
                if (valueModel != null && valueModel.getObject() != null) {
                    getObjectDetailsModels().getPageResource().showCorrelationWizard(target, valueModel.getObject().getPath());
                }
            }
        };
        correlationConfButton.setOutputMarkupId(true);
        correlationConfButton.showTitleAsLabel(true);
        correlationConfButton.add(new VisibleBehaviour(() -> isTopTableButtonsVisible()));
        topButtons.add(correlationConfButton);
    }

    private void initCapabilitiesButton(RepeatingView topButtons) {
        AjaxIconButton capabilitiesConfButton = new AjaxIconButton(
                topButtons.newChildId(),
                Model.of(ResourceObjectTypePreviewTileType.CAPABILITIES.getIcon()),
                getPageBase().createStringResource(ResourceObjectTypePreviewTileType.CAPABILITIES)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel =
                        getResourceObjectTypeValue(target);
                if (valueModel != null && valueModel.getObject() != null) {
                    getObjectDetailsModels().getPageResource().showCapabilitiesWizard(
                            target,
                            valueModel.getObject().getPath().append(ResourceObjectTypeDefinitionType.F_CONFIGURED_CAPABILITIES));
                }
            }
        };
        capabilitiesConfButton.setOutputMarkupId(true);
        capabilitiesConfButton.showTitleAsLabel(true);
        capabilitiesConfButton.add(new VisibleBehaviour(() -> isTopTableButtonsVisible()));
        topButtons.add(capabilitiesConfButton);
    }

    private void initCredentialsButton(RepeatingView topButtons) {
        AjaxIconButton credentialsConfButton = new AjaxIconButton(
                topButtons.newChildId(),
                Model.of(ResourceObjectTypePreviewTileType.CREDENTIALS.getIcon()),
                getPageBase().createStringResource(ResourceObjectTypePreviewTileType.CREDENTIALS)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel =
                        getResourceObjectTypeValue(target);
                if (valueModel != null && valueModel.getObject() != null) {
                    getObjectDetailsModels().getPageResource().showCredentialsWizard(
                            target,
                            valueModel.getObject().getPath().append(ResourceObjectTypeDefinitionType.F_CREDENTIALS));
                }
            }
        };
        credentialsConfButton.setOutputMarkupId(true);
        credentialsConfButton.showTitleAsLabel(true);
        credentialsConfButton.add(new VisibleBehaviour(() -> isTopTableButtonsVisible()));
        topButtons.add(credentialsConfButton);
    }

    private void initActivationsButton(RepeatingView topButtons) {
        AjaxIconButton activationButton = new AjaxIconButton(
                topButtons.newChildId(),
                Model.of(ResourceObjectTypePreviewTileType.ACTIVATION.getIcon()),
                getPageBase().createStringResource(ResourceObjectTypePreviewTileType.ACTIVATION)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel =
                        getResourceObjectTypeValue(target);
                if (valueModel != null && valueModel.getObject() != null) {
                    getObjectDetailsModels().getPageResource().showActivationsWizard(
                            target,
                            valueModel.getObject().getPath().append(ResourceObjectTypeDefinitionType.F_ACTIVATION));
                }
            }
        };
        activationButton.setOutputMarkupId(true);
        activationButton.showTitleAsLabel(true);
        activationButton.add(new VisibleBehaviour(() -> isTopTableButtonsVisible()));
        topButtons.add(activationButton);
    }

    private IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getResourceObjectTypeValue(
            AjaxRequestTarget target) {

        if ((!isUseObjectClass() && resourceContentSearch.getObject().getKind() == null)
                || (isUseObjectClass() && resourceContentSearch.getObject().getObjectClass() == null)) {
            if (target != null) {
                getPageBase().warn("Couldn't recognize resource object type");
                LOGGER.debug("Couldn't recognize resource object type");
                target.add(getPageBase().getFeedbackPanel());
            }
            return null;
        }
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> foundValue = null;
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> defaultValue = null;
        try {
            PrismContainerWrapper<ResourceObjectTypeDefinitionType> container = getObjectWrapperModel().getObject().findContainer(
                    ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
            for (PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> value : container.getValues()) {
                ResourceObjectTypeDefinitionType objectTypeDefinition = value.getRealValue();
                if (objectTypeDefinition == null) {
                    continue;
                }
                ShadowKindType searchKind = resourceContentSearch.getObject().getKind();
                String searchIntent = resourceContentSearch.getObject().getIntent();
                if (!isUseObjectClass()
                        && (searchKind.equals(objectTypeDefinition.getKind()))
                        || (kind != null && kind.equals(objectTypeDefinition.getKind()))) {
                    if (searchIntent != null
                            && searchIntent.equals(objectTypeDefinition.getIntent())) {
                        foundValue = value;
                    }
                    if (Boolean.TRUE.equals(objectTypeDefinition.isDefaultForKind())) {
                        defaultValue = value;
                    }
                }
                if (isUseObjectClass()
                        && (resourceContentSearch.getObject().getObjectClass().equals(objectTypeDefinition.getObjectClass())
                        || (objectTypeDefinition.getDelineation() != null
                        && resourceContentSearch.getObject().getObjectClass().equals(objectTypeDefinition.getDelineation().getObjectClass())))
                        && Boolean.TRUE.equals(objectTypeDefinition.isDefaultForObjectClass())) {
                    foundValue = value;
                }
                if (BooleanUtils.isTrue(objectTypeDefinition.isDefault()) && defaultValue == null) {
                    defaultValue = value;
                }
            }
        } catch (SchemaException e) {
            //ignore issue log error below
        }
        if (foundValue == null) {
            if (defaultValue == null) {
                if (target != null) {
                    getPageBase().warn("Couldn't recognize resource object type");
                    LOGGER.debug("Couldn't recognize resource object type");
                    target.add(getPageBase().getFeedbackPanel());
                }
                return null;
            }
            foundValue = defaultValue;
        }
        return PrismContainerValueWrapperModel.fromContainerWrapper(getObjectWrapperModel(), foundValue.getPath());
    }

    protected boolean isTopTableButtonsVisible() {
        return getResourceObjectTypeValue(null) != null;
    }

    private List<QName> createObjectClassChoices(IModel<PrismObjectWrapper<ResourceType>> model) {
        ResourceSchema refinedSchema;
        try {
            refinedSchema = ResourceSchemaFactory.getCompleteSchema(model.getObject().getObjectApplyDelta());
        } catch (CommonException e) {
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
                getObjectClassFromSearch(), getKind(), getIntent(), searchMode, getPanelConfiguration()) {
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

            protected void customizeProvider(SelectableBeanObjectDataProvider<ShadowType> provider) {
                ResourceContentPanel.this.customizeProvider(provider);
            }

            protected Collection<? extends Component> createToolbarButtonsList(String buttonId) {
                return ResourceContentPanel.this.createToolbarButtonsList(buttonId);
            }

            @Override
            protected boolean isShadowDetailsEnabled(IModel<SelectableBean<ShadowType>> rowModel) {
                return ResourceContentPanel.this.isShadowDetailsEnabled(rowModel);
            }
        };
        resourceContent.setOutputMarkupId(true);
        return resourceContent;

    }

    protected boolean isShadowDetailsEnabled(IModel<SelectableBean<ShadowType>> rowModel) {
        return true;
    }

    protected void customizeProvider(SelectableBeanObjectDataProvider<ShadowType> provider) {
    }

    protected boolean isTaskButtonsContainerVisible() {
        return true;
    }

    private ResourceContentRepositoryPanel initRepoContent(IModel<PrismObjectWrapper<ResourceType>> model) {
        String searchMode = isRepoSearch ? SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT :
                SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT;
        ResourceContentRepositoryPanel repositoryContent = new ResourceContentRepositoryPanel(ID_TABLE, loadResourceModel(),
                getObjectClassFromSearch(), getKind(), getIntent(), searchMode, getPanelConfiguration()) {
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

            protected void customizeProvider(SelectableBeanObjectDataProvider<ShadowType> provider) {
                ResourceContentPanel.this.customizeProvider(provider);
            }

            protected Collection<? extends Component> createToolbarButtonsList(String buttonId) {
                return ResourceContentPanel.this.createToolbarButtonsList(buttonId);
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

    protected QName getObjectClassFromSearch() {
        return resourceContentSearch.getObject().getObjectClass();
    }

    protected QName getObjectClass() {
        QName objectClass = null;
        IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model = getResourceObjectTypeValue(null);
        if (model != null) {
            ResourceObjectTypeDefinitionType objectType = model.getObject().getRealValue();
            if (objectType.getDelineation() != null) {
                objectClass = objectType.getDelineation().getObjectClass();
            }
            if (objectClass == null) {
                objectClass = objectType.getObjectClass();
            }
        }
        if (objectClass == null) {
            objectClass = getObjectClassFromSearch();
        }

        if (objectClass == null) {
            ResourceSchema refinedSchema = null;
            try {
                refinedSchema = ResourceSchemaFactory.getCompleteSchema(getObjectWrapper().getObject());
            } catch (SchemaException | ConfigurationException e) {
                // ignore it
            }
            if (refinedSchema == null) {
                return null;
            }
            String intent = getIntent();
            ResourceObjectDefinition ocDef;
            if (ShadowUtil.isKnown(getIntent())) {
                ocDef = refinedSchema.findObjectDefinition(getKind(), intent);
            } else {
                ocDef = refinedSchema.findDefaultDefinitionForKind(getKind());
            }
            if (ocDef != null) {
                objectClass = ocDef.getObjectClassName();
            }
        }
        return objectClass;
    }

    protected boolean isResourceSearch() {
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

    public BoxedTablePanel getTable() {
        ShadowTablePanel table = getShadowTable();
        if (table == null) {
            return null;
        }
        return table.getTable();
    }

    protected ShadowTablePanel getShadowTable() {
        com.evolveum.midpoint.web.page.admin.resources.ResourceContentPanel panel =
                (com.evolveum.midpoint.web.page.admin.resources.ResourceContentPanel) get(getPageBase().createComponentPath(ID_MAIN_FORM, ID_TABLE));
        if (panel == null) {
            return null;
        }
        return panel.getTable();
    }

    private Collection<? extends Component> createToolbarButtonsList(String buttonId) {
        List<Component> buttonsList = new ArrayList<>();

        AjaxIconButton reclassify = new AjaxIconButton(buttonId, Model.of("fa fa-rotate-right"),
                createStringResource("ResourceCategorizedPanel.button.reclassify")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<String> confirmModel;

                confirmModel = getPageBase().createStringResource(
                        "ResourceCategorizedPanel.button.reclassify.confirmation.objectClass",
                        getObjectClass().getLocalPart());

                ConfirmationPanel confirmationPanel = new ConfirmationPanel(getPageBase().getMainPopupBodyId(), confirmModel) {
                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        createReclassifyTask(target);
                        getShadowTable().startRefreshing(target);
                        target.add(getShadowTable());
                    }
                };
                getPageBase().showMainPopup(confirmationPanel, target);
            }
        };
        reclassify.add(AttributeAppender.append("class", "btn btn-primary btn-sm mr-2"));
        reclassify.setOutputMarkupId(true);
        reclassify.showTitleAsLabel(true);
        reclassify.add(new VisibleBehaviour(() -> getObjectClass() != null && isReclassifyButtonVisible()));
        buttonsList.add(reclassify);

        return buttonsList;
    }

    protected boolean isReclassifyButtonVisible() {
        return true;
    }

    private void createReclassifyTask(AjaxRequestTarget target) throws RestartResponseException {
        getPageBase().taskAwareExecutor(target, OPERATION_RECLASSIFY_SHADOWS)
                .runVoid((task, result) -> {
                    ResourceType resource = getObjectWrapperObject().asObjectable();
                    ResourceTaskCreator.forResource(resource, getPageBase())
                            .ofFlavor(SynchronizationTaskFlavor.IMPORT)
                            .withCoordinates(
                                    getKind(), // FIXME not static
                                    getIntent(), // FIXME not static
                                    getObjectClass()) // FIXME not static
                            .withExecutionMode(ExecutionModeType.PREVIEW)
                            .withPredefinedConfiguration(PredefinedConfigurationType.DEVELOPMENT)
                            .withSubmissionOptions(
                                    ActivitySubmissionOptions.create()
                                            .withTaskTemplate(new TaskType()
                                                    .name("Reclassifying objects on " + resource.getName())
                                                    .cleanupAfterCompletion(XmlTypeConverter.createDuration("PT0S"))))
                            .submit(task, result);
                });
    }
}
