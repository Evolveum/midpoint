/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.lostusername;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.page.login.AbstractPageLogin;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;

import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.ArrayList;
import java.util.List;

@PageDescriptor(urls = { @Url(mountUrl = "/loginRecovery", matchUrlForSecurity = "/loginRecovery") }, permitAll = true)
public class PageLoginNameRecovery extends AbstractPageLogin {

    private static final Trace LOGGER = TraceManager.getTrace(PageLoginNameRecovery.class);
    private static final String DOT_CLASS = PageLoginNameRecovery.class.getName() + ".";
    protected static final String OPERATION_LOAD_ARCHETYPE_BASED_MODULE = DOT_CLASS + "loadArchetypeBasedAuthModule";
    protected static final String OPERATION_LOAD_ARCHETYPE_OBJECTS = DOT_CLASS + "loadArchetypeObjects";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BACK_BUTTON = "back";
    private static final String ID_ARCHETYPE_SELECTION_PANEL = "archetypeSelectionPanel";
    private static final String ID_ARCHETYPES_PANEL = "archetypes";
    private static final String ID_ARCHETYPE_PANEL = "archetype";

    private LoadableDetachableModel<ArchetypeBasedModuleType> archetypeBasedAuthModuleModel;

    public PageLoginNameRecovery() {
        super();
    }

    @Override
    protected void initModels() {
        archetypeBasedAuthModuleModel = new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected ArchetypeBasedModuleType load() {
                return loadArchetypeBasedModule();
            }
        };
    }

    private ArchetypeBasedModuleType loadArchetypeBasedModule() {
        Task task = createAnonymousTask(OPERATION_LOAD_ARCHETYPE_BASED_MODULE);
        OperationResult parentResult = new OperationResult(OPERATION_LOAD_ARCHETYPE_BASED_MODULE);
        try {
            var securityPolicy = getModelInteractionService().getSecurityPolicy((PrismObject<? extends FocusType>) null,
                    task, parentResult);
            return SecurityUtils.getLoginRecoveryAuthModule(securityPolicy);
        } catch (CommonException e) {
            LOGGER.warn("Cannot load authentication module for login recovery: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    protected void initCustomLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_MAIN_FORM);
        form.setOutputMarkupId(true);
        add(form);

        initArchetypeSelectionPanel(form);

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
        form.add(archetypeSelectionPanel);

        ListView<Tile<ArchetypeType>> archetypeListPanel = new ListView<>(ID_ARCHETYPES_PANEL, loadTilesModel()) {

            @Override
            protected void populateItem(ListItem<Tile<ArchetypeType>> item) {
                item.add(createTilePanel(ID_ARCHETYPE_PANEL, item.getModel()));
            }
        };
        archetypeSelectionPanel.add(archetypeListPanel);
    }

    private LoadableModel<List<Tile<ArchetypeType>>> loadTilesModel() {
        return new LoadableModel<>(false) {

            @Override
            protected List<Tile<ArchetypeType>> load() {
                List<Tile<ArchetypeType>> tiles = new ArrayList<>();
                var archetypeSelectionType = archetypeBasedAuthModuleModel.getObject().getArchetypeSelection();
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

    private Component createTilePanel(String id, IModel<Tile<ArchetypeType>> tileModel) {
        return new TilePanel<>(id, tileModel) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                //todo get correlation rule through object template ref from archetype
            }
        };
    }

}
