/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.*;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismPropertyPanel;
import com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;

import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@PageDescriptor(urls = {
        @Url(mountUrl = "/archetypeSelection", matchUrlForSecurity = "/archetypeSelection")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.ARCHETYPE_SELECTION)
public class PageLoginNameRecovery extends PageAuthenticationBase {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PageLoginNameRecovery.class);
    private static final String DOT_CLASS = PageLoginNameRecovery.class.getName() + ".";
    protected static final String OPERATION_LOAD_USER_OBJECT_WRAPPER = DOT_CLASS + "loadUserObjectWrapper";
    protected static final String OPERATION_LOAD_ARCHETYPE_OBJECTS = DOT_CLASS + "loadArchetypeObjects";
    protected static final String OPERATION_LOAD_OBJECT_TEMPLATE = DOT_CLASS + "loadObjectTemplate";
    protected static final String OPERATION_LOAD_SYSTEM_CONFIGURATION = DOT_CLASS + "loadSystemConfiguration";
    protected static final String OPERATION_CREATE_ITEM_WRAPPER = DOT_CLASS + "createItemWrapper";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BACK_BUTTON = "back";
    private static final String ID_ARCHETYPE_SELECTION_PANEL = "archetypeSelectionPanel";
    private static final String ID_ARCHETYPES_PANEL = "archetypes";
    private static final String ID_ARCHETYPE_PANEL = "archetype";
    private static final String ID_ITEMS_PANEL = "itemsPanel";
    private static final String ID_ITEM_PANEL = "itemPanel";

    private LoadableDetachableModel<ArchetypeSelectionModuleType> archetypeSelectionModuleModel;
    private LoadableDetachableModel<List<ItemsCorrelatorType>> correlatorsModel;
    private LoadableDetachableModel<List<ItemPath>> itemPathListModel;

    private PrismObjectWrapper<UserType> objectWrapper;
    private boolean archetypeSelected = false;

    public PageLoginNameRecovery() {
        super();
    }

    @Override
    protected ObjectQuery createStaticFormQuery() {
        String username = "";
        return getPrismContext().queryFor(UserType.class).item(UserType.F_NAME)
                .eqPoly(username).matchingNorm().build();
    }

    @Override
    protected DynamicFormPanel<UserType> getDynamicForm() {
        return null;
    }

    @Override
    protected void initModels() {
        archetypeSelectionModuleModel = new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected ArchetypeSelectionModuleType load() {
                return loadArchetypeSelectionModule();
            }
        };

        itemPathListModel = new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1L;
            @Override
            protected List<ItemPath> load() {
                return getCurrentCorrelationItemPathList();
            }
        };

        initUserObjectWrapper();
    }

    private void initUserObjectWrapper() {
        var task = createAnonymousTask(OPERATION_LOAD_USER_OBJECT_WRAPPER);
        var result = new OperationResult(OPERATION_LOAD_USER_OBJECT_WRAPPER);
        var user = new UserType().asPrismObject();
        PrismObjectWrapperFactory<UserType> factory = findObjectWrapperFactory(user.getDefinition());

        WrapperContext context = new WrapperContext(task, result);
        try {
            objectWrapper = factory.createObjectWrapper(user, ItemStatus.NOT_CHANGED, context);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't create object wrapper", e);
        }
    }

    private ArchetypeSelectionModuleType getModuleByIdentifier(String moduleIdentifier) {
        if (StringUtils.isEmpty(moduleIdentifier)) {
            return null;
        }
        //TODO user model?
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(null);
        if (securityPolicy == null || securityPolicy.getAuthentication() == null) {
            getSession().error(getString("Security policy not found"));
            throw new RestartResponseException(PageError.class);
        }
        return securityPolicy.getAuthentication().getModules().getArchetypeSelection()
                .stream()
                .filter(m -> moduleIdentifier.equals(m.getIdentifier()) || moduleIdentifier.equals(m.getName()))
                .findFirst()
                .orElse(null);
    }

    private ArchetypeSelectionModuleType loadArchetypeSelectionModule() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication)) {
            getSession().error(getString("No midPoint authentication is found"));
            throw new RestartResponseException(PageError.class);
        }
        MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
        ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
        if (moduleAuthentication == null
                && !AuthenticationModuleNameConstants.ARCHETYPE_SELECTION.equals(moduleAuthentication.getModuleTypeName())) {
            getSession().error(getString("No authentication module is found"));
            throw new RestartResponseException(PageError.class);
        }
        if (StringUtils.isEmpty(moduleAuthentication.getModuleIdentifier())) {
            getSession().error(getString("No module identifier is defined"));
            throw new RestartResponseException(PageError.class);
        }
        ArchetypeSelectionModuleType module = getModuleByIdentifier(moduleAuthentication.getModuleIdentifier());
        if (module == null) {
            getSession().error(getString("No module with identifier \"" + moduleAuthentication.getModuleIdentifier() + "\" is found"));
            throw new RestartResponseException(PageError.class);
        }
        return module;
//        List<ModuleItemConfigurationType> itemConfigs = module.getItem();
//        return itemConfigs.stream()
//                .map(config -> config.getPath())
//                .collect(Collectors.toList());
//
//
//        Task task = createAnonymousTask(OPERATION_LOAD_ARCHETYPE_BASED_MODULE);
//        OperationResult parentResult = new OperationResult(OPERATION_LOAD_ARCHETYPE_BASED_MODULE);
//        try {
//            var securityPolicy = getModelInteractionService().getSecurityPolicy((PrismObject<? extends FocusType>) null,
//                    task, parentResult);
//            return SecurityUtils.getLoginRecoveryAuthModule(securityPolicy);
//        } catch (CommonException e) {
//            LOGGER.warn("Cannot load authentication module for login recovery: " + e.getMessage(), e);
//        }
//        return null;
    }

    @Override
    protected void initCustomLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_MAIN_FORM);
        form.setOutputMarkupId(true);
        add(form);

        initArchetypeSelectionPanel(form);
        initItemsPanel(form);

        AjaxButton backButton = new AjaxButton(ID_BACK_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed();
            }
        };
        backButton.setOutputMarkupId(true);
        add(backButton);
    }

    private void initArchetypeSelectionPanel(MidpointForm<?> form) {
        WebMarkupContainer archetypeSelectionPanel = new WebMarkupContainer(ID_ARCHETYPE_SELECTION_PANEL);
        archetypeSelectionPanel.setOutputMarkupId(true);
        archetypeSelectionPanel.add(new VisibleBehaviour(() -> !archetypeSelected));
        form.add(archetypeSelectionPanel);

        ListView<Tile<ArchetypeType>> archetypeListPanel = new ListView<>(ID_ARCHETYPES_PANEL, loadTilesModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Tile<ArchetypeType>> item) {
                item.add(createTilePanel(item.getModel()));
            }
        };
        archetypeSelectionPanel.add(archetypeListPanel);
    }

    private LoadableModel<List<Tile<ArchetypeType>>> loadTilesModel() {
        return new LoadableModel<>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<Tile<ArchetypeType>> load() {
                List<Tile<ArchetypeType>> tiles = new ArrayList<>();
                var archetypeSelectionType = archetypeSelectionModuleModel.getObject().getArchetypeSelection();
                if (archetypeSelectionType == null) {
                    return tiles;
                }
                List<ObjectReferenceType> archetypeRefs = archetypeSelectionType.getArchetypeRef();
                List<ArchetypeType> archetypes = resolveArchetypeObjects(archetypeRefs);
                archetypes.forEach(archetype -> {
                    tiles.add(createTile(archetype));
                });
                return tiles;
            }
        };
    }

    private List<ArchetypeType> resolveArchetypeObjects(List<ObjectReferenceType> archetypeRefs) {
        return runPrivileged((Producer<List<ArchetypeType>>) () -> {
            var loadArchetypesTask = createAnonymousTask(OPERATION_LOAD_ARCHETYPE_OBJECTS);
            return WebComponentUtil.loadReferencedObjectList(archetypeRefs,
                    OPERATION_LOAD_ARCHETYPE_OBJECTS, loadArchetypesTask, PageLoginNameRecovery.this);
        });
    }

    private Tile<ArchetypeType> createTile(ArchetypeType archetype) {
        var archetypeDisplayType = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(archetype,
                PageLoginNameRecovery.this);
        var iconCssClass = GuiDisplayTypeUtil.getIconCssClass(archetypeDisplayType);
        var label = LocalizationUtil.translatePolyString(GuiDisplayTypeUtil.getLabel(archetypeDisplayType));
        var help = GuiDisplayTypeUtil.getHelp(archetypeDisplayType);
        Tile<ArchetypeType> tile = new Tile<>(iconCssClass, label);
        tile.setDescription(help);
        tile.setValue(archetype);
        return tile;
    }

    private Component createTilePanel(IModel<Tile<ArchetypeType>> tileModel) {
        return new TilePanel<>(ID_ARCHETYPE_PANEL, tileModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target) {
                archetypeSelected = true;
                initCorrelationItemsModel(tileModel.getObject());
                target.add(PageLoginNameRecovery.this);
            }
        };
    }

    private ObjectTemplateType loadObjectTemplateForArchetype(ArchetypeType archetype) {
        return runPrivileged((Producer<ObjectTemplateType>) () -> {
            var archetypePolicy = archetype.getArchetypePolicy();
            if (archetypePolicy == null) {
                return null;
            }
            var objectTemplateRef = archetypePolicy.getObjectTemplateRef();
            var loadObjectTemplateTask = createAnonymousTask(OPERATION_LOAD_OBJECT_TEMPLATE);
            var result = new OperationResult(OPERATION_LOAD_OBJECT_TEMPLATE);
            PrismObject<ObjectTemplateType> objectTemplate = WebModelServiceUtils.resolveReferenceNoFetch(objectTemplateRef,
                    PageLoginNameRecovery.this, loadObjectTemplateTask, result);
            return objectTemplate == null ? null : objectTemplate.asObjectable();
        });
    }

    private void initCorrelationItemsModel(Tile<ArchetypeType> tile) {
        correlatorsModel = new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<ItemsCorrelatorType> load() {
                var archetype = tile.getValue();
                var objectTemplate = loadObjectTemplateForArchetype(archetype);
                if (objectTemplate == null) {
                    //todo show warning?
                    return Collections.emptyList();
                }
                return getCorrelators(objectTemplate);
            }
        };
    }

    private List<ItemsCorrelatorType> getCorrelators(ObjectTemplateType objectTemplate) {
        var correlatorConfiguration = determineCorrelatorConfiguration(objectTemplate);
        if (correlatorConfiguration == null) {
            return Collections.emptyList();
        }

        return ((CompositeCorrelatorType) correlatorConfiguration.getConfigurationBean())
                .getItems()
                .stream()
                .filter(c -> c instanceof ItemsCorrelatorType)
                .collect(Collectors.toList());
    }

    private CorrelatorConfiguration determineCorrelatorConfiguration(ObjectTemplateType objectTemplate) {
        OperationResult result = new OperationResult(OPERATION_LOAD_SYSTEM_CONFIGURATION);
        try {
            var systemConfiguration = getModelInteractionService().getSystemConfiguration(result);
            return getCorrelationService().determineCorrelatorConfiguration(objectTemplate, systemConfiguration);
        } catch (SchemaException| ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't determine correlation configuration.", e);
        }
        return null;
    }

    private List<ItemPath> getCurrentCorrelationItemPathList() {
        var index = 0; //todo this should be the index of the currently processing correlator
        List<ItemPath> pathList = new ArrayList<>();
        if (correlatorsModel == null || correlatorsModel.getObject() == null) {
            return pathList;
        }
        if (CollectionUtils.isNotEmpty(correlatorsModel.getObject())) {
            var correlator = correlatorsModel.getObject().get(index);
            correlator.getItem().forEach(item -> {
                ItemPathType pathBean = item.getRef();
                if (pathBean != null) {
                    pathList.add(pathBean.getItemPath());
                }
            });
        }
        return pathList;
    }

    private void initItemsPanel(MidpointForm<?> form) {
        ListView<ItemPath> itemsPanel = new ListView<ItemPath>(ID_ITEMS_PANEL, itemPathListModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ItemPath> listItem) {
                IModel<ItemWrapper<?, ?>> model = () -> createItemWrapper(listItem.getModelObject());
                Panel panel = WebPrismUtil.createVerticalPropertyPanel(ID_ITEM_PANEL, model, null);
                if (panel instanceof VerticalFormPrismPropertyPanel<?>) {
                    ((VerticalFormPrismPropertyPanel<?>) panel).setRequiredTagVisibleInHeaderPanel(true);
                }
                listItem.add(panel);
            }
        };
        itemsPanel.add(new VisibleBehaviour(() -> archetypeSelected));
        form.add(itemsPanel);
    }

    private ItemWrapper<?, ?> createItemWrapper(ItemPath itemPath) {
        return runPrivileged((Producer<? extends ItemWrapper<?,?>>) () -> {
            try {
                var task = createAnonymousTask(OPERATION_CREATE_ITEM_WRAPPER);
                var result = new OperationResult(OPERATION_CREATE_ITEM_WRAPPER);
                var def = objectWrapper.findItemDefinition(itemPath);
                return createItemWrapper(def.instantiate(), ItemStatus.ADDED, new WrapperContext(task, result));
            } catch (SchemaException ex) {
                LoggingUtils.logException(LOGGER, "Couldn't create item wrapper for item " + itemPath, ex);
            }
            return null;
        });
    }

}
