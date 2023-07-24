/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.*;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;

import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.api.GuiStyleConstants.CLASS_TEST_CONNECTION_MENU_ITEM;

@PageDescriptor(urls = {
        @Url(mountUrl = "/archetypeSelection", matchUrlForSecurity = "/archetypeSelection")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.ARCHETYPE_SELECTION)
public class PageArchetypeSelection extends PageAuthenticationBase {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PageArchetypeSelection.class);
    private static final String DOT_CLASS = PageArchetypeSelection.class.getName() + ".";
    protected static final String OPERATION_LOAD_ARCHETYPE_OBJECTS = DOT_CLASS + "loadArchetypeObjects";
    public static final String UNDEFINED_OID = DOT_CLASS + "undefined";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BACK_BUTTON = "back";
    private static final String ID_ARCHETYPE_SELECTION_PANEL = "archetypeSelectionPanel";
    private static final String ID_CSRF_FIELD = "csrfField";
    private static final String ID_ARCHETYPES_PANEL = "archetypes";
    private static final String ID_ARCHETYPE_PANEL = "archetype";
    private static final String ID_ARCHETYPE_OID = "archetypeOid";
    private static final String ID_ALLOW_UNDEFINED_ARCHETYPE = "allowUndefinedArchetype";

    private IModel<String> archetypeOidModel = Model.of();
    private boolean allowUndefinedArchetype;

    private LoadableDetachableModel<ArchetypeSelectionModuleType> archetypeSelectionModuleModel;

    public PageArchetypeSelection() {
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
    protected String getModuleTypeName() {
        return AuthenticationModuleNameConstants.ARCHETYPE_SELECTION;
    }

    @Override
    protected void initModels() {
        archetypeSelectionModuleModel = new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected ArchetypeSelectionModuleType load() {
                var securityPolicy = resolveSecurityPolicy(null);

                return ConfigurationLoadUtil.loadArchetypeSelectionModuleForLoginRecovery(PageArchetypeSelection.this, securityPolicy);
            }
        };
        allowUndefinedArchetype = loadAllowUndefinedArchetypeConfig();
    }

    private boolean loadAllowUndefinedArchetypeConfig() {
        var securityPolicy = resolveSecurityPolicy(null);
        var archetypeSelectionModule = ConfigurationLoadUtil.loadArchetypeSelectionModuleForLoginRecovery(
                PageArchetypeSelection.this, securityPolicy);
        var allowUndefinedArchetype = Boolean.TRUE.equals(archetypeSelectionModule.isAllowUndefinedArchetype());
        return allowUndefinedArchetype;
    }

    @Override
    protected void initCustomLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_MAIN_FORM);
        form.add(AttributeModifier.replace("action", (IModel<String>) this::getUrlProcessingLogin));
        add(form);

        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);

        HiddenField<String> archetypeOidField = new HiddenField<>(ID_ARCHETYPE_OID, archetypeOidModel);
        archetypeOidField.setOutputMarkupId(true);
        form.add(archetypeOidField);

        HiddenField<Boolean> allowUndefinedArchetypeField =
                new HiddenField<>(ID_ALLOW_UNDEFINED_ARCHETYPE, Model.of(allowUndefinedArchetype));
        allowUndefinedArchetypeField.setOutputMarkupId(true);
        form.add(allowUndefinedArchetypeField);

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
                if (allowUndefinedArchetype) {
                    var undefinedArchetypeTile = createUndefinedArchetypeTile();
                    tiles.add(undefinedArchetypeTile);
                }
                return tiles;
            }
        };
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
        archetype.setOid(UNDEFINED_OID);

        var archetypePolicy = new ArchetypePolicyType();

        var undefinedArchetypeLabel = new PolyStringType("Undefined");
        undefinedArchetypeLabel.setTranslation(new PolyStringTranslationType().key("PageArchetypeSelection.undefinedArchetype"));

        var undefinedArchetypeHelp = new PolyStringType("Undefined");
        undefinedArchetypeHelp.setTranslation(new PolyStringTranslationType().key("PageArchetypeSelection.undefinedArchetypeHelp"));

        var archetypeDisplay = new DisplayType()
                .label(undefinedArchetypeLabel)
                .icon(new IconType().cssClass(CLASS_TEST_CONNECTION_MENU_ITEM))
                .help(undefinedArchetypeHelp);
        archetypePolicy.setDisplay(archetypeDisplay);
        archetype.setArchetypePolicy(archetypePolicy);

        return createTile(archetype);
    }


    private Tile<ArchetypeType> createTile(ArchetypeType archetype) {
        var archetypeDisplayType = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(archetype,
                PageArchetypeSelection.this);
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
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target) {
                updateArchetypeOidField(tileModel, target);
                target.add(getArchetypesContainer());
            }
        };
        tilePanel.add(AttributeModifier.append("class", getActiveClassModel(tileModel.getObject())));
        return tilePanel;
    }

    private void updateArchetypeOidField(IModel<Tile<ArchetypeType>> tileModel, AjaxRequestTarget target) {
        archetypeOidModel.setObject(getArchetypeOid(tileModel));
        target.add(getArchetypeOidField());
    }

    private String getArchetypeOid(IModel<Tile<ArchetypeType>> tileModel) {
        return tileModel.getObject().getValue().getOid();
    }

    private IModel<String> getActiveClassModel(Tile<ArchetypeType> tile) {
        var isArchetypeSelected = tile.getValue().getOid().equals(archetypeOidModel.getObject());
        return isArchetypeSelected ? Model.of("active") : Model.of();
    }
    private MidpointForm<?> getForm() {
        return (MidpointForm<?>) get(ID_MAIN_FORM);
    }

    private WebMarkupContainer getArchetypesContainer() {
        return (WebMarkupContainer) getForm().get(ID_ARCHETYPE_SELECTION_PANEL);
    }

    private Component getArchetypeOidField() {
        return getForm().get(ID_ARCHETYPE_OID);
    }

}
