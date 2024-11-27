/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login.module;

import static com.evolveum.midpoint.gui.api.GuiStyleConstants.CLASS_TEST_CONNECTION_MENU_ITEM;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.ArchetypeSelectionModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;

@PageDescriptor(urls = {
        @Url(mountUrl = "/archetypeSelection", matchUrlForSecurity = "/archetypeSelection")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.ARCHETYPE_SELECTION)
public class PageArchetypeSelection extends PageAbstractAuthenticationModule<ArchetypeSelectionModuleAuthentication> {

    @Serial private static final long serialVersionUID = 1L;
    private static final String DOT_CLASS = PageArchetypeSelection.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageArchetypeSelection.class);
    protected static final String OPERATION_LOAD_ARCHETYPE_OBJECTS = DOT_CLASS + "loadArchetypeObjects";
    private static final String ID_ARCHETYPE_SELECTION_PANEL = "archetypeSelectionPanel";
    private static final String ID_ARCHETYPES_PANEL = "archetypes";
    private static final String ID_ARCHETYPE_PANEL = "archetype";
    private static final String ID_ARCHETYPE_OID = "archetypeOid";
    private static final String ID_ALLOW_UNDEFINED_ARCHETYPE = "allowUndefinedArchetype";

    private final IModel<String> archetypeOidModel = Model.of();

    private LoadableModel<ArchetypeSelectionModuleAuthentication> archetypeSelectionModuleModel;
    private LoadableModel<List<Tile<ArchetypeType>>> tilesModel;

    public PageArchetypeSelection() {
        super();
        initModels();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    protected void initModels() {
        archetypeSelectionModuleModel = new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected ArchetypeSelectionModuleAuthentication load() {
                return getAuthenticationModuleConfiguration();
            }
        };

        tilesModel = new LoadableModel<>(false) {
            @Override
            protected List<Tile<ArchetypeType>> load() {
                return loadTilesList();
            }
        };
    }

    @Override
    protected void initModuleLayout(MidpointForm form) {
        HiddenField<String> archetypeOidField = new HiddenField<>(ID_ARCHETYPE_OID, archetypeOidModel);
        archetypeOidField.setOutputMarkupId(true);
        form.add(archetypeOidField);

        HiddenField<Boolean> undefinedArchetypeAllowed = new HiddenField<>(ID_ALLOW_UNDEFINED_ARCHETYPE, Model.of(isUndefinedArchetypeAllowed()));
        undefinedArchetypeAllowed.setOutputMarkupId(true);
        form.add(undefinedArchetypeAllowed);

        initArchetypeSelectionPanel(form);
    }

    @Override
    protected IModel<String> getDefaultLoginPanelTitleModel() {
        return createStringResource("PageArchetypeSelection.form.title");
    }

    @Override
    protected IModel<String> getDefaultLoginPanelDescriptionModel() {
        return createStringResource("PageArchetypeSelection.form.description");
    }

    private boolean isUndefinedArchetypeAllowed() {
        var securityPolicy = resolveSecurityPolicy(null);
        var identityRecoveryModule = SecurityUtils.getArchetypeSelectionAuthModule(securityPolicy);
        if (identityRecoveryModule == null) {
            return false;
        }
        return Boolean.TRUE.equals(identityRecoveryModule.isAllowUndefinedArchetype());
    }

    private void initArchetypeSelectionPanel(MidpointForm<?> form) {
        WebMarkupContainer archetypeSelectionPanel = new WebMarkupContainer(ID_ARCHETYPE_SELECTION_PANEL);
        archetypeSelectionPanel.setOutputMarkupId(true);
        form.add(archetypeSelectionPanel);

        ListView<Tile<ArchetypeType>> archetypeListPanel = new ListView<>(ID_ARCHETYPES_PANEL, tilesModel) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Tile<ArchetypeType>> item) {
                item.add(createTilePanel(item.getModel()));
            }
        };
        archetypeSelectionPanel.add(archetypeListPanel);
    }

    private List<Tile<ArchetypeType>> loadTilesList() {
        List<Tile<ArchetypeType>> tiles = new ArrayList<>();
        ArchetypeSelectionModuleAuthentication moduleAuthentication = archetypeSelectionModuleModel.getObject();
        var archetypeSelectionType = moduleAuthentication.getArchetypeSelection();
        if (archetypeSelectionType == null) {
            return tiles;
        }
        List<ObjectReferenceType> archetypeRefs = archetypeSelectionType.getArchetypeRef();
        List<ArchetypeType> archetypes = resolveArchetypeObjects(archetypeRefs);
        tiles = archetypes.stream()
                .map(archetype -> createTile(archetype))
                .collect(Collectors.toList());
        if (moduleAuthentication.isAllowUndefined()) {
            var undefinedArchetypeTile = createUndefinedArchetypeTile();
            tiles.add(undefinedArchetypeTile);
        }
        return tiles;
    }

    private List<ArchetypeType> resolveArchetypeObjects(List<ObjectReferenceType> archetypeRefs) {
        return runPrivileged((Producer<List<ArchetypeType>>) () -> {
            var loadArchetypesTask = createAnonymousTask(OPERATION_LOAD_ARCHETYPE_OBJECTS);
            return WebComponentUtil.loadReferencedObjectList(archetypeRefs,
                    OPERATION_LOAD_ARCHETYPE_OBJECTS, loadArchetypesTask, PageArchetypeSelection.this);
        });
    }

    private Tile<ArchetypeType> createUndefinedArchetypeTile() {
        var archetype = new ArchetypeType();

        var archetypePolicy = new ArchetypePolicyType();

        var undefinedArchetypeLabel = new PolyStringType("Undefined");
        undefinedArchetypeLabel.setTranslation(new PolyStringTranslationType().key("PageArchetypeSelection.undefinedArchetype"));

        var undefinedArchetypeHelp = new PolyStringType("Undefined");
        undefinedArchetypeHelp.setTranslation(new PolyStringTranslationType().key("PageArchetypeSelection.undefinedArchetypeHelp"));

        var archetypeDisplay = new DisplayType()
                .label(undefinedArchetypeLabel)
                .icon(new IconType().cssClass(CLASS_TEST_CONNECTION_MENU_ITEM))
                .help(undefinedArchetypeHelp);

        Tile<ArchetypeType> tile = createTile(archetype, archetypeDisplay);
        tile.setSelected(true);
        return tile;
    }


    private Tile<ArchetypeType> createTile(ArchetypeType archetype) {
        var archetypeDisplayType = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(archetype,
                PageArchetypeSelection.this);
        return createTile(archetype, archetypeDisplayType);
    }

    private Tile<ArchetypeType> createTile(ArchetypeType archetype, DisplayType archetypeDisplayType) {
        var iconCssClass = GuiDisplayTypeUtil.getIconCssClass(archetypeDisplayType);
        var label = GuiDisplayTypeUtil.getTranslatedLabel(archetypeDisplayType);
        var help = GuiDisplayTypeUtil.getHelp(archetypeDisplayType);
        Tile<ArchetypeType> tile = new Tile<>(iconCssClass, label);
        tile.setDescription(help);
        tile.setValue(archetype);
        return tile;
    }

    private Component createTilePanel(IModel<Tile<ArchetypeType>> tileModel) {
        var tilePanel = new TilePanel<>(ID_ARCHETYPE_PANEL, tileModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target) {
                archetypeSelected(tileModel, target);
                target.add(getArchetypesContainer());
            }
        };
        tilePanel.add(AttributeAppender.append(
                "aria-checked", () -> tileModel.getObject().isSelected() ? "true" : "false"));
        tilePanel.setHorizontal(false);
        return tilePanel;
    }

    private void archetypeSelected(IModel<Tile<ArchetypeType>> tileModel, AjaxRequestTarget target) {
        archetypeOidModel.setObject(getArchetypeOid(tileModel));

        Tile<ArchetypeType> tile = tileModel.getObject();
        boolean tileState = tile.isSelected();

        tilesModel.getObject().forEach(t -> t.setSelected(false));
        tile.setSelected(!tileState);

        target.add(getArchetypeOidField());
    }

    private String getArchetypeOid(IModel<Tile<ArchetypeType>> tileModel) {
        return tileModel.getObject().getValue().getOid();
    }


    private WebMarkupContainer getArchetypesContainer() {
        return (WebMarkupContainer) getForm().get(ID_ARCHETYPE_SELECTION_PANEL);
    }

    private Component getArchetypeOidField() {
        return getForm().get(ID_ARCHETYPE_OID);
    }

    protected String getArchetypeOid() {
        return archetypeOidModel.getObject();
    }
}
