/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.tile.SingleSelectContainerTileTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PageableListView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Iterator;
import java.util.List;

public class SmartObjectTypeSuggestionTable<O extends PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> extends SingleSelectContainerTileTablePanel<ResourceObjectTypeDefinitionType> {

    protected static final String ID_TILES_RADIO_FRAGMENT = "tilesRadioFragment";
    protected static final String ID_TILES_RADIO_FORM = "tileForm";
    protected static final String ID_TILES_RADIO = "radioGroup";

    static final String ID_TILE_VIEW = "tileView";
    static final String ID_TILES_CONTAINER = "tilesContainer";

    private static final int MAX_TILE_COUNT = 6;

    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> selectedTileModel;
    String resourceOid;

    public SmartObjectTypeSuggestionTable(
            @NotNull String id,
            @NotNull UserProfileStorage.TableId tableId,
            @NotNull IModel<List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> model,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> selectedModel,
            @NotNull String resourceOid) {
        super(id, tableId, model);
        this.resourceOid = resourceOid;
        this.selectedTileModel = selectedModel;
        setDefaultPagingSize(tableId);
    }

    @Override
    protected WebMarkupContainer createTilesContainer(
            String idTilesContainer,
            ISortableDataProvider<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String> provider,
            UserProfileStorage.TableId tableId) {
        Fragment tilesFragment = new Fragment(idTilesContainer, ID_TILES_RADIO_FRAGMENT, this);
        tilesFragment.add(AttributeModifier.replace("class", getTileContainerCssClass()));

        initializeSelectedTile(provider);

        PageableListView<TemplateTile<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>,
                PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> tiles = createTilesPanel(ID_TILES, provider);
        tiles.setOutputMarkupId(true);
        tiles.setReuseItems(false);

        RadioGroup<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> radioGroup = new RadioGroup<>(
                ID_TILES_RADIO, getSelectedTileModel());
        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                onRadioTileSelected(getSelectedTileModel(), ajaxRequestTarget);
                ajaxRequestTarget.add(SmartObjectTypeSuggestionTable.this);
            }
        });
        radioGroup.add(tiles);

        Form<Void> form = new Form<>(ID_TILES_RADIO_FORM);

        form.setOutputMarkupId(true);
        form.add(radioGroup);

        tilesFragment.add(form);
        return tilesFragment;
    }

    private void initializeSelectedTile(
            @NotNull ISortableDataProvider<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String> provider) {
        if (selectedTileModel.getObject() != null) {
            return;
        }
        IModel<O> def = getDefaultSelectedTileModel();
        if (def != null && def.getObject() != null) {
            selectedTileModel.setObject(def.getObject());
        } else {
            Iterator<? extends PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> it = provider.iterator(0, 1);
            if (it.hasNext()) {
                selectedTileModel.setObject(it.next());
            }
        }
    }

    @Override
    protected Class<? extends Containerable> getType() {
        return ResourceObjectTypeDefinitionType.class;
    }

    @Override
    protected TemplateTile<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> createTileObject(
            PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> object) {
        return new SmartObjectTypeSuggestionTileModel<>(object, resourceOid);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Component createTile(String id, IModel<TemplateTile<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> model) {
        return new SmartObjectTypeSuggestionPanel(id, model, selectedTileModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void performOnDelete(AjaxRequestTarget target) {
                super.performOnDelete(target);
                refresh(target);
            }
        };
    }

    @Override
    protected MultivalueContainerListDataProvider<ResourceObjectTypeDefinitionType> createProvider() {
        return super.createProvider();
    }

    protected void createToolbarButtons(RepeatingView repeatingView) {
    }

    @Override
    protected WebMarkupContainer createTilesButtonToolbar(String id) {
        RepeatingView repView = new RepeatingView(id);
        createToolbarButtons(repView);
        return repView;
    }

    @Override
    protected Component createHeader(String id) {
        WebMarkupContainer header = new WebMarkupContainer(id);
        header.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        header.add(AttributeModifier.remove("class"));
        return header;
    }

    private IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getSelectedTileModel() {
        return selectedTileModel;
    }

    protected @Nullable IModel<O> getDefaultSelectedTileModel() {
        return null;
    }

    protected void onRadioTileSelected(
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> selectedTileModel,
            AjaxRequestTarget target) {

    }

    @SuppressWarnings("unchecked")
    @Override
    protected PageableListView<ResourceObjectTypeDefinitionType, PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getTiles() {
        WebMarkupContainer container = (WebMarkupContainer) get(ID_TILE_VIEW).get(ID_TILES_CONTAINER);
        return (PageableListView<ResourceObjectTypeDefinitionType, PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>) container
                .get(ID_TILES_RADIO_FORM).get(ID_TILES_RADIO).get(ID_TILES);
    }

    protected void setDefaultPagingSize(UserProfileStorage.@NotNull TableId tableId) {
        MidPointAuthWebSession session = getSession();
        UserProfileStorage userProfile = session.getSessionStorage().getUserProfile();
        userProfile.setPagingSize(
                tableId,
                getMaxTileCount());
    }

    @Override
    protected String getAdditionalBoxCssClasses() {
        return " m-0";
    }

    @Override
    protected String getTilesFooterCssClasses() {
        return "pt-1";
    }

    @Override
    protected String getTileCssStyle() {
        return "";
    }

    @Override
    protected String getTileCssClasses() {
        return "col-12 p-2";
    }

    @Override
    protected String getTileContainerCssClass() {
        return "justify-content-left pt-2 ";
    }

    @Override
    protected String getTilesContainerAdditionalClass() {
        return "";
    }

    @Override
    protected boolean showFooter() {
        return getTilesModel().getObject().size() > getMaxTileCount();
    }

    protected int getMaxTileCount() {
        return SmartObjectTypeSuggestionTable.MAX_TILE_COUNT;
    }

    @Override
    public void refresh(AjaxRequestTarget target) {
        super.refresh(target);
    }
}
