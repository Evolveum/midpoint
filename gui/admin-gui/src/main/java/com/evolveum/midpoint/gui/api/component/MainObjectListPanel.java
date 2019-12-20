/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.MultifunctionalButton;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.button.CsvDownloadButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageImportObject;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.time.Duration;

/**
 * @author katkav
 */
public abstract class MainObjectListPanel<O extends ObjectType, S extends Serializable> extends ObjectListPanel<O> {
    private static final long serialVersionUID = 1L;

    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_BUTTON_REPEATER = "buttonsRepeater";
    private static final String ID_BUTTON = "button";
    private static final Trace LOGGER = TraceManager.getTrace(MainObjectListPanel.class);

    private Boolean manualRefreshEnabled;

    public MainObjectListPanel(String id, Class<O> type, TableId tableId, Collection<SelectorOptions<GetOperationOptions>> options, PageBase parentPage) {
        super(id, type, tableId, options);


    }

    @Override
    protected IColumn<SelectableBean<O>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected IColumn<SelectableBean<O>, String> createNameColumn(IModel<String> columnNameModel, String itemPath) {
        if (StringUtils.isEmpty(itemPath)) {
            return new ObjectNameColumn<O>(columnNameModel == null ? createStringResource("ObjectType.name") : columnNameModel) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
                    O object = rowModel.getObject().getValue();
                    MainObjectListPanel.this.objectDetailsPerformed(target, object);
                }

                @Override
                public boolean isClickable(IModel<SelectableBean<O>> rowModel) {
                    return MainObjectListPanel.this.isClickable(rowModel);
                }
            };
        } else {
            return new ObjectNameColumn<O>(columnNameModel == null ? createStringResource("ObjectType.name") : columnNameModel,
                    itemPath) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
                    O object = rowModel.getObject().getValue();
                    MainObjectListPanel.this.objectDetailsPerformed(target, object);
                }

                @Override
                public boolean isClickable(IModel<SelectableBean<O>> rowModel) {
                    return MainObjectListPanel.this.isClickable(rowModel);
                }
            };
        }
    }

    protected boolean isClickable(IModel<SelectableBean<O>> rowModel) {
        return true;
    }

    protected abstract void objectDetailsPerformed(AjaxRequestTarget target, O object);

    protected void newObjectPerformed(AjaxRequestTarget target, S collectionView){}

    @Override
    protected WebMarkupContainer createTableButtonToolbar(String id) {
        return new ButtonBar(id, ID_BUTTON_BAR, this, createToolbarButtonsList(ID_BUTTON));
    }

    protected List<Component> createToolbarButtonsList(String buttonId){
        List<Component> buttonsList = new ArrayList<>();
        MultifunctionalButton<S> createNewObjectButton = new MultifunctionalButton<S>(buttonId){
            private static final long serialVersionUID = 1L;

            @Override
            protected List<S> getAdditionalButtonsObjects(){
                return getNewObjectInfluencesList();
            }

            @Override
            protected void buttonClickPerformed(AjaxRequestTarget target, S influencingObject){
                newObjectPerformed(target, influencingObject);
            }

            @Override
            protected DisplayType getMainButtonDisplayType(){
                return getNewObjectButtonStandardDisplayType();
            }

            @Override
            protected Map<IconCssStyle, IconType> getMainButtonLayerIcons(){
                return getNewObjectButtonLayerIconStyleMap();
            }

            @Override
            protected CompositedIconBuilder getAdditionalIconBuilder(S influencingObject, DisplayType additionalButtonDisplayType){
                CompositedIconBuilder builder = MainObjectListPanel.this.getNewObjectButtonAdditionalIconBuilder(influencingObject, additionalButtonDisplayType);
                if (builder == null){
                    return super.getAdditionalIconBuilder(influencingObject, additionalButtonDisplayType);
                } else {
                    return builder;
                }
            }

            @Override
            protected DisplayType getDefaultObjectButtonDisplayType(){
                return getNewObjectButtonSpecialDisplayType();
            }

            @Override
            protected DisplayType getAdditionalButtonDisplayType(S buttonObject){
                return getNewObjectButtonAdditionalDisplayType(buttonObject);
            }
        };
        createNewObjectButton.add(new VisibleBehaviour(() -> isCreateNewObjectEnabled()));
        createNewObjectButton.add(AttributeAppender.append("class", "btn-margin-right"));
        buttonsList.add(createNewObjectButton);

        AjaxIconButton importObject = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_UPLOAD),
                createStringResource("MainObjectListPanel.import")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ((PageBase) getPage()).navigateToNext(PageImportObject.class);
            }
        };
        importObject.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        importObject.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){

                boolean isVisible = false;
                try {
                    isVisible = ((PageBase) getPage()).isAuthorized(ModelAuthorizationAction.IMPORT_OBJECTS.getUrl())
                            && WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CONFIGURATION_ALL_URL,
                            AuthorizationConstants.AUTZ_UI_CONFIGURATION_IMPORT_URL);
                } catch (Exception ex){
                    LOGGER.error("Failed to check authorization for IMPORT action for " + getType().getSimpleName()
                            + " object, ", ex);
                }
                return isVisible;
            }
        });
        buttonsList.add(importObject);

        boolean canCountBeforeExporting = getType() == null || !ShadowType.class.isAssignableFrom(getType()) ||
                isRawOrNoFetchOption(getOptions());
        CsvDownloadButtonPanel exportDataLink = new CsvDownloadButtonPanel(buttonId, canCountBeforeExporting) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DataTable<?, ?> getDataTable() {
                return getTable().getDataTable();
            }

            @Override
            protected String getFilename() {
                return getType().getSimpleName() +
                        "_" + createStringResource("MainObjectListPanel.exportFileName").getString();
            }

        };
        exportDataLink.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_URI);
            }
        });

        AjaxIconButton refreshIcon = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                createStringResource("MainObjectListPanel.refresh")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearCache();
                refreshTable((Class<O>) getType(), target);

                target.add((Component) getTable());
            }
        };
        refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        buttonsList.add(refreshIcon);

        AjaxIconButton playPauseIcon = new AjaxIconButton(buttonId, getRefreshPausePlayButtonModel(),
                createStringResource("MainObjectListPanel.refresh.enabled", isRefreshEnabled())) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearCache();
                manualRefreshEnabled = !isRefreshEnabled();
                target.add(getTable());
            }
        };
        playPauseIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        buttonsList.add(playPauseIcon);

        buttonsList.add(exportDataLink);
        return buttonsList;
    }

    private IModel<String> getRefreshPausePlayButtonModel() {
        return () -> {
            if (isRefreshEnabled()) {
                return GuiStyleConstants.CLASS_PAUSE;
            }

            return GuiStyleConstants.CLASS_PLAY;
        };
    }

    private boolean isRawOrNoFetchOption(Collection<SelectorOptions<GetOperationOptions>> options){
        if (options == null){
            return false;
        }
        for (SelectorOptions<GetOperationOptions> option : options){
            if (Boolean.TRUE.equals(option.getOptions().getRaw()) ||
                    Boolean.TRUE.equals(option.getOptions().getNoFetch())){
                return true;
            }
        }
        return false;
    }

    protected boolean isCreateNewObjectEnabled(){
        return true;
    }

    protected List<S> getNewObjectInfluencesList(){
        return new ArrayList<>();
    }

    protected DisplayType getNewObjectButtonStandardDisplayType(){
        StringBuilder sb = new StringBuilder();
        sb.append(createStringResource("MainObjectListPanel.newObject").getString());
        sb.append(" ");
        sb.append(createStringResource("ObjectTypeLowercase." + getType().getSimpleName()).getString());
        return WebComponentUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green",
                sb.toString());
    }

    protected Map<IconCssStyle, IconType> getNewObjectButtonLayerIconStyleMap(){
        return null;
    }

    protected DisplayType getNewObjectButtonSpecialDisplayType(){
        String iconCssStyle = WebComponentUtil.createDefaultBlackIcon(WebComponentUtil.classToQName(getPageBase().getPrismContext(), getType()));

        StringBuilder sb = new StringBuilder();
        sb.append(createStringResource("MainObjectListPanel.newObject").getString());
        sb.append(" ");
        sb.append(createStringResource("ObjectTypeLowercase." + getType().getSimpleName()).getString());

        return WebComponentUtil.createDisplayType(iconCssStyle, "", sb.toString());
    }

    protected DisplayType getNewObjectButtonAdditionalDisplayType(S buttonObject){
        return null;
    }

    protected CompositedIconBuilder getNewObjectButtonAdditionalIconBuilder(S influencingObject, DisplayType additionalButtonDisplayType){
        return null;
    }

    private static class ButtonBar extends Fragment {

        private static final long serialVersionUID = 1L;

        public <O extends ObjectType, S extends Serializable> ButtonBar(String id, String markupId, MainObjectListPanel<O, S> markupProvider, List<Component> buttonsList) {
            super(id, markupId, markupProvider);

            initLayout(buttonsList);
        }

        private <O extends ObjectType> void initLayout(final List<Component> buttonsList) {
            ListView<Component> buttonsView = new ListView<Component>(ID_BUTTON_REPEATER, Model.ofList(buttonsList)) {
                @Override
                protected void populateItem(ListItem<Component> listItem) {
                    listItem.add(listItem.getModelObject());
                }
            };
            add(buttonsView);
        }
    }

    @Override
    protected boolean isRefreshEnabled() {
        if (getAutoRefreshInterval() == 0) {
            return manualRefreshEnabled == null ? false : manualRefreshEnabled.booleanValue();
        }

        if (manualRefreshEnabled == null) {
            return true;
        }

        return manualRefreshEnabled.booleanValue();
    }

    @Override
    protected int getAutoRefreshInterval() {
        if (!isCollectionViewPanel()) {
            return 0;
        }

        String collectionName = getCollectionNameParameterValue().toString();
        CompiledObjectCollectionView view = getPageBase().getCompiledUserProfile().findObjectViewByViewName(getType(), collectionName);

        Integer autoRefreshInterval = view.getRefreshInterval();
        if (autoRefreshInterval == null) {
            return 0;
        }

        return autoRefreshInterval.intValue();

    }

    private StringValue getCollectionNameParameterValue(){
        PageParameters parameters = getPageBase().getPageParameters();
        return parameters ==  null ? null : parameters.get(PageBase.PARAMETER_OBJECT_COLLECTION_NAME);
    }

    private boolean isCollectionViewPanel() {
        return getCollectionNameParameterValue() != null && getCollectionNameParameterValue().toString() != null;
    }

}
