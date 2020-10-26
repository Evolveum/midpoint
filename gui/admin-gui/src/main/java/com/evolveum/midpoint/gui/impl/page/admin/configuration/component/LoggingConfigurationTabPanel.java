/*
 * Copyright (C) 2018-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.session.SessionStorage;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
public class LoggingConfigurationTabPanel extends BasePanel<PrismContainerWrapper<LoggingConfigurationType>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(LoggingConfigurationTabPanel.class);

    private static final String ID_LOGGING = "logging";
    private static final String ID_APPENDERS = "appenders";
    private static final String ID_LOGGERS = "loggers";
    private static final String ID_AUDITING = "audit";
    private static final String ID_NEW_ITEM_BUTTON = "newItemButton";
    private static final String ID_BUTTON_TOOLBAR_FRAGMENT = "buttonToolbarFragment";
    private static final String ID_APPENDERS_CHOICE = "appendersChoice";
    private static final String ID_CHOICE_APPENDER_TYPE_FORM = "choiceAppenderTypeForm";

    public LoggingConfigurationTabPanel(String id, IModel<PrismContainerWrapper<LoggingConfigurationType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        try {
            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder();
            builder.visibilityHandler(itemWrapper -> getLoggingVisibility(itemWrapper.getPath()));
            Panel loggingPanel = getPageBase().initItemPanel(ID_LOGGING, LoggingConfigurationType.COMPLEX_TYPE, getModel(), builder.build());
            add(loggingPanel);
        } catch (SchemaException e) {
            LOGGER.error("Cannot create panel for logging: {}", e.getMessage(), e);
            getSession().error("Cannot create panle for logging");
        }

        PrismContainerWrapperModel<LoggingConfigurationType, ClassLoggerConfigurationType> loggerModel = PrismContainerWrapperModel.fromContainerWrapper(getModel(), LoggingConfigurationType.F_CLASS_LOGGER);

        MultivalueContainerListPanel<ClassLoggerConfigurationType> loggersMultivalueContainerListPanel =
                new MultivalueContainerListPanel<ClassLoggerConfigurationType>(ID_LOGGERS, loggerModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> postSearch(
                    List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> items) {
                return items;
            }

            @Override
            protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation) {
                PrismContainerValue<ClassLoggerConfigurationType> newLogger = loggerModel.getObject().getItem().createNewValue();
                PrismContainerValueWrapper<ClassLoggerConfigurationType> newLoggerWrapper = getLoggersMultivalueContainerListPanel().createNewItemContainerValueWrapper(newLogger, loggerModel.getObject(), target);
                loggerEditPerformed(target, Model.of(newLoggerWrapper), null);
            }

            @Override
            protected void initPaging() {
//                initLoggerPaging();
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return true;
            }

            @Override
            protected ObjectQuery createQuery() {
                return null;
            }

            @Override
            protected String getStorageKey() {
                return SessionStorage.KEY_LOGGING_TAB_LOGGER_TABLE;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.LOGGING_TAB_LOGGER_TABLE;
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<ClassLoggerConfigurationType>, String>> createColumns() {
                return initLoggersBasicColumns(loggerModel);
            }

                    @Override
            protected void editItemPerformed(AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<ClassLoggerConfigurationType>> rowModel,
                    List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> listItems) {
                loggerEditPerformed(target, rowModel, listItems);
            }

            @Override
            protected List<SearchItemDefinition> initSearchableItems(
                    PrismContainerDefinition<ClassLoggerConfigurationType> containerDef) {
                List<SearchItemDefinition> defs = new ArrayList<>();

                return defs;
            }
        };
        add(loggersMultivalueContainerListPanel);

        PrismContainerWrapperModel<LoggingConfigurationType, AppenderConfigurationType> appenderModel = PrismContainerWrapperModel.fromContainerWrapper(getModel(), LoggingConfigurationType.F_APPENDER);

        MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType> appendersMultivalueContainerListPanel =
                new MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType>(ID_APPENDERS, appenderModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<PrismContainerValueWrapper<AppenderConfigurationType>> postSearch(
                    List<PrismContainerValueWrapper<AppenderConfigurationType>> items) {
                return items;
            }

            @Override
            protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation) {
                newAppendersClickPerformed(target);
            }

            @Override
            protected void initPaging() {
//                initAppenderPaging();
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return true;
            }

            @Override
            protected ObjectQuery createQuery() {
                return null;
            }

            @Override
            protected String getStorageKey() {
                return SessionStorage.KEY_LOGGING_TAB_APPENDER_TABLE;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.LOGGING_TAB_APPENDER_TABLE;
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<AppenderConfigurationType>, String>> createColumns() {
                return initAppendersBasicColumns(appenderModel);
            }

                    @Override
            protected MultivalueContainerDetailsPanel<AppenderConfigurationType> getMultivalueContainerDetailsPanel(
                    ListItem<PrismContainerValueWrapper<AppenderConfigurationType>> item) {
                return LoggingConfigurationTabPanel.this.getAppendersMultivalueContainerDetailsPanel(item);
            }

            @Override
            protected List<SearchItemDefinition> initSearchableItems(
                    PrismContainerDefinition<AppenderConfigurationType> containerDef) {
                List<SearchItemDefinition> defs = new ArrayList<>();

                return defs;
            }

            @Override
            protected WebMarkupContainer initButtonToolbar(String contentAreaId) {
                Fragment searchContainer = new Fragment(contentAreaId, ID_BUTTON_TOOLBAR_FRAGMENT, LoggingConfigurationTabPanel.this);

                MidpointForm appenderTypeForm = new MidpointForm(ID_CHOICE_APPENDER_TYPE_FORM);
                searchContainer.add(appenderTypeForm);

                AjaxSubmitButton newObjectIcon = new AjaxSubmitButton(ID_NEW_ITEM_BUTTON, new Model<>("<i class=\"fa fa-plus\"></i>")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        newItemPerformed(target, null);
                    }
                };

                newObjectIcon.add(new VisibleEnableBehaviour() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        return isCreateNewObjectVisible();
                    }

                    @Override
                    public boolean isEnabled() {
                        return isNewObjectButtonEnabled();
                    }
                });
                newObjectIcon.add(AttributeModifier.append("class", createStyleClassModelForNewObjectIcon()));
                appenderTypeForm.add(newObjectIcon);
                List<QName> appendersChoicesList = new ArrayList<>();
                appendersChoicesList.add(FileAppenderConfigurationType.COMPLEX_TYPE);
                appendersChoicesList.add(SyslogAppenderConfigurationType.COMPLEX_TYPE);
                DropDownChoicePanel<QName> appenderChoice = new DropDownChoicePanel<>(
                        ID_APPENDERS_CHOICE,
                        new Model<>(FileAppenderConfigurationType.COMPLEX_TYPE),
                        Model.ofList(appendersChoicesList),
                        new QNameIChoiceRenderer("LoggingConfigurationTabPanel." + ID_APPENDERS_CHOICE));
                appenderChoice.setOutputMarkupId(true);
                appenderTypeForm.addOrReplace(appenderChoice);
                return searchContainer;
            }
        };
        add(appendersMultivalueContainerListPanel);

        IModel<PrismContainerWrapper<LoggingAuditingConfigurationType>> auditModel = PrismContainerWrapperModel.fromContainerWrapper(getModel(), LoggingConfigurationType.F_AUDITING);
        try {
            Panel auditPanel = getPageBase().initItemPanel(ID_AUDITING, LoggingAuditingConfigurationType.COMPLEX_TYPE, auditModel, new ItemPanelSettingsBuilder().build());
            add(auditPanel);
        } catch (SchemaException e) {
            LOGGER.error("Cannot create panel for auditing: {}", e.getMessage(), e);
            getSession().error("Cannot create panel for auditing.");
        }
        setOutputMarkupId(true);
    }

    private ItemVisibility getLoggingVisibility(ItemPath pathToCheck) {
        if (ItemPath.create(SystemConfigurationType.F_LOGGING).equivalent(pathToCheck)) {
            return ItemVisibility.AUTO;
        }

        if (ItemPath.create(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_ROOT_LOGGER_APPENDER).equivalent(pathToCheck)) {
            return ItemVisibility.AUTO;
        }

        if (ItemPath.create(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_ROOT_LOGGER_LEVEL).equivalent(pathToCheck)) {
            return ItemVisibility.AUTO;
        }

        if (ItemPath.create(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_DEBUG).equivalent(pathToCheck)) {
            return ItemVisibility.AUTO;
        }

        return ItemVisibility.HIDDEN;
    }

    private List<IColumn<PrismContainerValueWrapper<ClassLoggerConfigurationType>, String>> initLoggersBasicColumns(IModel<PrismContainerWrapper<ClassLoggerConfigurationType>> loggersModel) {
        List<IColumn<PrismContainerValueWrapper<ClassLoggerConfigurationType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());
        columns.add(new IconColumn<PrismContainerValueWrapper<ClassLoggerConfigurationType>>(Model.of("")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<ClassLoggerConfigurationType>> rowModel) {
                return WebComponentUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE));

            }

        });

        columns.add(new PrismPropertyWrapperColumn(loggersModel, ClassLoggerConfigurationType.F_PACKAGE, ColumnType.VALUE, getPageBase()) {

            @Override
            public String getCssClass() {
                return " col-md-5 ";
            }
        });
        columns.add(new PrismPropertyWrapperColumn<>(loggersModel, ClassLoggerConfigurationType.F_LEVEL, ColumnType.VALUE, getPageBase()));
        columns.add(new PrismPropertyWrapperColumn<>(loggersModel, ClassLoggerConfigurationType.F_APPENDER, ColumnType.VALUE, getPageBase()));

        List<InlineMenuItem> menuActionsList = getLoggersMultivalueContainerListPanel().getDefaultMenuActions();
        columns.add(new InlineMenuButtonColumn(menuActionsList, getPageBase()) {

            @Override
            public String getCssClass() {
                return " col-md-1 ";
            }
        });

        return columns;
    }

    private void loggerEditPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ClassLoggerConfigurationType>> rowModel,
            List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> listItems) {
        if (rowModel != null) {
            PrismContainerValueWrapper<ClassLoggerConfigurationType> logger = rowModel.getObject();
            logger.setSelected(true);
        } else {
            for (PrismContainerValueWrapper<ClassLoggerConfigurationType> logger : listItems) {
                logger.setSelected(true);
            }
        }
        target.add(getLoggersMultivalueContainerListPanel());
    }

    protected void newAppendersClickPerformed(AjaxRequestTarget target) {
        MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType> appenders
                = (MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType>) get(ID_APPENDERS);
        DropDownChoicePanel<QName> appendersChoice = (DropDownChoicePanel<QName>) getAppendersMultivalueContainerListPanel().getListPanel().getTable().getFooterButtonToolbar().get(createComponentPath(ID_CHOICE_APPENDER_TYPE_FORM, ID_APPENDERS_CHOICE));
        PrismContainerValue<AppenderConfigurationType> newObjectPolicy = null;
        if (QNameUtil.match(appendersChoice.getModel().getObject(), FileAppenderConfigurationType.COMPLEX_TYPE)) {
            newObjectPolicy = new FileAppenderConfigurationType().asPrismContainerValue();
        } else {
            newObjectPolicy = new SyslogAppenderConfigurationType().asPrismContainerValue();
        }
        newObjectPolicy.setParent(appenders.getModelObject().getItem());
        newObjectPolicy.setPrismContext(getPageBase().getPrismContext());

        PrismContainerValueWrapper<AppenderConfigurationType> newAppenderContainerWrapper = getAppendersMultivalueContainerListPanel().createNewItemContainerValueWrapper(newObjectPolicy, appenders.getModelObject(), target);
        getAppendersMultivalueContainerListPanel().itemDetailsPerformed(target, Collections.singletonList(newAppenderContainerWrapper));
    }

    private MultivalueContainerDetailsPanel<AppenderConfigurationType> getAppendersMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<AppenderConfigurationType>> item) {
        MultivalueContainerDetailsPanel<AppenderConfigurationType> detailsPanel = new MultivalueContainerDetailsPanel<AppenderConfigurationType>(
                MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayNamePanel<AppenderConfigurationType> createDisplayNamePanel(String displayNamePanelId) {
                IModel<AppenderConfigurationType> displayNameModel = new IModel<AppenderConfigurationType>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public AppenderConfigurationType getObject() {
                        return item.getModelObject().getRealValue();
                    }
                };
                return new DisplayNamePanel<>(displayNamePanelId, displayNameModel);
            }
        };
        return detailsPanel;
    }

    private boolean isFileAppender(IModel<AppenderConfigurationType> appender) {
        if (appender == null || appender.getObject() == null) {
            return false;
        }
        return (appender.getObject() instanceof FileAppenderConfigurationType);
    }

    private boolean isSyslogAppender(IModel<AppenderConfigurationType> appender) {
        if (appender == null || appender.getObject() == null) {
            return false;
        }
        return (appender.getObject() instanceof SyslogAppenderConfigurationType);
    }

    private String getInputCssClass() {
        return "col-xs-10";
    }

    private Label createHeader(String id, String displayName) {
        if (StringUtils.isEmpty(displayName)) {
            displayName = "displayName.not.set";
        }
        StringResourceModel headerLabelModel = createStringResource(displayName);
        Label header = new Label(id, headerLabelModel);
        header.add(AttributeAppender.prepend("class", "prism-title pull-left"));
        return header;
    }

    private MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType> getAppendersMultivalueContainerListPanel() {
        return ((MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType>) get(ID_APPENDERS));
    }

    private MultivalueContainerListPanel<ClassLoggerConfigurationType> getLoggersMultivalueContainerListPanel() {
        return ((MultivalueContainerListPanel<ClassLoggerConfigurationType>) get(ID_LOGGERS));
    }

//    private void initAppenderPaging() {
//        getPageBase().getSessionStorage().getLoggingConfigurationTabAppenderTableStorage().setPaging(
//                getPrismContext().queryFactory().createPaging(0, (int) ((PageBase) getPage()).getItemsPerPage(UserProfileStorage.TableId.LOGGING_TAB_APPENDER_TABLE)));
//    }
//
//    private void initLoggerPaging() {
//        getPageBase().getSessionStorage().getLoggingConfigurationTabLoggerTableStorage().setPaging(
//                getPrismContext().queryFactory().createPaging(0, (int) ((PageBase) getPage()).getItemsPerPage(UserProfileStorage.TableId.LOGGING_TAB_APPENDER_TABLE)));
//    }

    private List<IColumn<PrismContainerValueWrapper<AppenderConfigurationType>, String>> initAppendersBasicColumns(IModel<PrismContainerWrapper<AppenderConfigurationType>> appenderModel) {
        List<IColumn<PrismContainerValueWrapper<AppenderConfigurationType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());
        columns.add(new IconColumn<PrismContainerValueWrapper<AppenderConfigurationType>>(Model.of("")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AppenderConfigurationType>> rowModel) {
                return WebComponentUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE));
            }
        });

        columns.add(new PrismPropertyWrapperColumn<AppenderConfigurationType, String>(appenderModel, AppenderConfigurationType.F_NAME, ColumnType.LINK, getPageBase()) {

            @Override
            protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AppenderConfigurationType>> rowModel) {
                getAppendersMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
            }

        });

        columns.add(new PrismPropertyWrapperColumn<AppenderConfigurationType, String>(appenderModel, AppenderConfigurationType.F_PATTERN, ColumnType.VALUE, getPageBase()) {
            @Override
            public String getCssClass() {
                return " col-md-5 ";
            }
        });

        columns.add(new AbstractColumn<PrismContainerValueWrapper<AppenderConfigurationType>, String>(createStringResource("LoggingConfigurationTabPanel.appender.typeColumn")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AppenderConfigurationType>>> item, String componentId,
                    final IModel<PrismContainerValueWrapper<AppenderConfigurationType>> rowModel) {
                ItemRealValueModel<AppenderConfigurationType> appender =
                        new ItemRealValueModel<>(rowModel);
                String type = "";
                if (appender != null && appender.getObject() instanceof FileAppenderConfigurationType) {
                    type = "File appender";
                } else if (appender != null && appender.getObject() instanceof SyslogAppenderConfigurationType) {
                    type = "Syslog appender";
                }
                item.add(new Label(componentId, Model.of(type)));
            }
        });

        List<InlineMenuItem> menuActionsList = getAppendersMultivalueContainerListPanel().getDefaultMenuActions();
        columns.add(new InlineMenuButtonColumn(menuActionsList, getPageBase()) {
            @Override
            public String getCssClass() {
                return " col-md-1 ";
            }
        });

        return columns;
    }
}
