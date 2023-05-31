/*
 * Copyright (C) 2018-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.password.PasswordPropertyPanel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.EditableColumn;
import com.evolveum.midpoint.gui.impl.component.form.TriStateFormGroup;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.DeprecatedPropertyWrapperModel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
public class NotificationConfigTabPanel extends BasePanel<PrismContainerWrapper<NotificationConfigurationType>> {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = NotificationConfigTabPanel.class.getName() + ".";
    private static final String OPERATION_CREATE_NEW_VALUE = DOT_CLASS + "createNewValue";

    private static final String ID_MAIL_CONFIG_HEADER = "mailConfigurationHeader";
    private static final String ID_DEFAULT_FROM = "defaultFrom";
    private static final String ID_REDIRECT_TO_FILE = "redirectToFile";
    private static final String ID_LOG_TO_FILE = "logToFile";
    private static final String ID_DEBUG = "debug";
    private static final String ID_MAIL_SERVERS_TABLE = "mailServersTable";
    private static final String ID_MAIL_SERVER_CONFIG_HEADER = "mailServerConfigurationHeader";
    private static final String ID_FILE_CONFIG_HEADER = "fileConfigurationHeader";
    private static final String ID_FILE_CONFIG = "fileConfiguration";
    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_VALUE_HEADER = "valueHeader";
    private static final String ID_FILE_NAME = "fileName";
    private static final String ID_FILE_PATH = "filePath";
    private static final String ID_REMOVE_BUTTON = "removeButton";

    public NotificationConfigTabPanel(String id, IModel<PrismContainerWrapper<NotificationConfigurationType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initPaging();
        initLayout();

        setOutputMarkupId(true);
    }

    private void initPaging() {
        getPageBase().getSessionStorage().getNotificationConfigurationTabMailServerTableStorage().setPaging(
                getPrismContext().queryFactory().createPaging(0, (int) getPageBase().getItemsPerPage(UserProfileStorage.TableId.NOTIFICATION_TAB_MAIL_SERVER_TABLE)));
    }

    protected void initLayout() {


        add(createHeader(ID_MAIL_SERVER_CONFIG_HEADER, MailServerConfigurationType.COMPLEX_TYPE.getLocalPart() + ".details"));

        WebMarkupContainer files = new WebMarkupContainer(ID_FILE_CONFIG);
        files.setOutputMarkupId(true);
        add(files);
    }


    @Deprecated
    private Label createHeader(String id, String displayName) {
        if (StringUtils.isEmpty(displayName)) {
            displayName = "displayName.not.set";
        }
        StringResourceModel headerLabelModel = createStringResource(displayName);
        Label header = new Label(id, headerLabelModel);
        header.add(AttributeAppender.prepend("class", "prism-title"));
        return header;
    }


    private List<IColumn<MailServerConfiguration, String>> initMailServersColumns() {
        List<IColumn<MailServerConfiguration, String>> columns = new ArrayList<>();
        columns.add(new CheckBoxHeaderColumn<>());

        columns.add(new IconColumn<>(Model.of("")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<MailServerConfiguration> rowModel) {
                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE));
            }

        });

        columns.add(new EditableAjaxLinkColumn<>(createStringResource("MailServerConfigurationType.host")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<MailServerConfiguration> rowModel) {
                return Model.of(rowModel.getObject().getValue().getHost());
            }

            @Override
            protected Component createInputPanel(String componentId, IModel<MailServerConfiguration> rowModel) {
                MailServerConfigurationType mailServer = rowModel.getObject().getValue();
                InputPanel input = new TextPanel<>(componentId, new PropertyModel<>(mailServer, "host"));
                input.add(AttributeAppender.prepend("class", getInputCssClass()));
                return input;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<MailServerConfiguration> rowModel) {
                mailServerEditPerformed(target, rowModel, null);
            }
        });

        columns.add(new EditableColumn<>(createStringResource("MailServerConfigurationType.port")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Component createStaticPanel(String componentId, IModel<MailServerConfiguration> rowModel) {
                return new Label(componentId, Model.of(rowModel.getObject().getValue().getPort()));
            }

            @Override
            protected Component createInputPanel(String componentId, IModel<MailServerConfiguration> rowModel) {
                MailServerConfigurationType mailServer = rowModel.getObject().getValue();
                InputPanel input = new TextPanel<>(componentId, new PropertyModel<>(mailServer, "port"));
                input.add(AttributeAppender.prepend("class", getInputCssClass()));
                return input;
            }
        });

        columns.add(new EditableColumn<>(createStringResource("MailServerConfigurationType.username")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Component createStaticPanel(String componentId, IModel<MailServerConfiguration> rowModel) {
                return new Label(componentId, Model.of(rowModel.getObject().getValue().getUsername()));
            }

            @Override
            protected Component createInputPanel(String componentId, IModel<MailServerConfiguration> rowModel) {
                MailServerConfigurationType mailServer = rowModel.getObject().getValue();
                InputPanel input = new TextPanel<>(componentId, new PropertyModel<>(mailServer, "username"));
                input.add(AttributeAppender.prepend("class", getInputCssClass()));
                return input;
            }
        });

        columns.add(new EditableColumn<>(createStringResource("MailServerConfigurationType.password")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Component createStaticPanel(String componentId, IModel<MailServerConfiguration> rowModel) {
                return new PasswordPropertyPanel(componentId, Model.of(rowModel.getObject().getValue().getPassword()), true, false);
            }

            @Override
            protected Component createInputPanel(String componentId, IModel<MailServerConfiguration> rowModel) {
                MailServerConfigurationType mailServer = rowModel.getObject().getValue();
                return new PasswordPropertyPanel(componentId, new PropertyModel<>(mailServer, "password"), false, true);
            }
        });

        columns.add(new EditableColumn<>(createStringResource("MailServerConfigurationType.transportSecurity")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Component createStaticPanel(String componentId, IModel<MailServerConfiguration> rowModel) {
                IModel<String> retModel = WebComponentUtil.createLocalizedModelForEnum(rowModel.getObject().getValue().getTransportSecurity(), null);
                return new Label(componentId, retModel != null && retModel.getObject() != null ? retModel.getObject() : "");
            }

            @Override
            protected Component createInputPanel(String componentId, IModel<MailServerConfiguration> rowModel) {
                MailServerConfigurationType mailServer = rowModel.getObject().getValue();
                InputPanel input = WebComponentUtil.createEnumPanel(MailTransportSecurityType.class, componentId, new PropertyModel<>(mailServer, "transportSecurity"), NotificationConfigTabPanel.this);
                input.add(AttributeAppender.prepend("class", getInputCssClass()));
                return input;
            }
        });

        List<InlineMenuItem> menuActionsList = getMenuActions();
        columns.add(new InlineMenuButtonColumn<>(menuActionsList, getPageBase()));

        return columns;
    }

    private List<InlineMenuItem> getMenuActions() {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        return menuItems;
    }

    private void mailServerEditPerformed(AjaxRequestTarget target, IModel<MailServerConfiguration> rowModel,
            List<MailServerConfiguration> listItems) {

        if (rowModel != null) {
            MailServerConfiguration server = rowModel.getObject();
            server.setEditing(true);
            server.setSelected(true);
        } else {
            for (MailServerConfiguration server : listItems) {
                server.setSelected(true);
                server.setEditing(true);
            }
        }
        target.add(getMailServersTable());
    }

    private BoxedTablePanel<MailServerConfiguration> getMailServersTable() {
        return (BoxedTablePanel<MailServerConfiguration>) get(ID_MAIL_SERVERS_TABLE);
    }

    private String getInputCssClass() {
        return "col-10";
    }

    public class MailServerConfiguration extends Selectable<MailServerConfigurationType> implements Editable {

        private static final long serialVersionUID = 1L;

        private final MailServerConfigurationType mailServer;

        private boolean editable;

        public MailServerConfiguration(MailServerConfigurationType mailServer) {
            this.mailServer = mailServer;
        }

        @Override
        public boolean isEditing() {
            return editable;
        }

        @Override
        public void setEditing(boolean editing) {
            editable = editing;
        }

        @Override
        public MailServerConfigurationType getValue() {
            return mailServer;
        }

    }
}
