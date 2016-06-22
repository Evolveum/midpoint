/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.self.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.dialog.HelpInfoPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;
import com.evolveum.midpoint.web.page.self.PageSelfCredentials;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPropagationUserControlType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordChangeSecurityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;

/**
 * @author Kate Honchar
 */
public class ChangePasswordPanel extends BasePanel<MyPasswordsDto> {
	private static final long serialVersionUID = 1L;

	private static final String ID_PASSWORD_PANEL = "passwordPanel";
    private static final String ID_OLD_PASSWORD_FIELD = "oldPassword";
    private static final String ID_PASSWORD_LABEL = "passwordLabel";
    private static final String ID_OLD_PASSWORD_LABEL = "oldPasswordLabel";
    public static final String ID_ACCOUNTS_TABLE = "accounts";
    private static final String ID_PASSWORD_CREATE_INFO_LABEL = "createLabel";
    public static final String ID_LAST_CHANGE_LABEL = "lastChange";
    public static final String ID_ACCOUNTS_CONTAINER = "accountsContainer";
    private static final String ID_BUTTON_HELP = "help";
    private static final String DOT_CLASS = ChangePasswordPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUSer";
    public static final String SELECTED_ACCOUNT_ICON_CSS = "fa fa-check-square-o";
    public static final String DESELECTED_ACCOUNT_ICON_CSS = "fa fa-square-o";
    public static final String PROPAGATED_ACCOUNT_ICON_CSS = "fa fa-sign-out";
    private static final int HELP_MODAL_WIDTH = 400;
    private static final int HELP_MODAL_HEIGH = 600;
    private static final Trace LOGGER = TraceManager.getTrace(ChangePasswordPanel.class);

    private LoadableModel<MyPasswordsDto> model;
    private boolean midpointAccountSelected = true;

    public ChangePasswordPanel(String id) {
        super(id);
        initLayout();
    }

    public ChangePasswordPanel(String id, LoadableModel<MyPasswordsDto> model, MyPasswordsDto myPasswordsDto, PageBase pageBase) {
        super(id, model);
        setParent(pageBase);
        initLayout();
    }

    private void initLayout() {
        model = (LoadableModel<MyPasswordsDto>) getModel();

        Label oldPasswordLabel = new Label(ID_OLD_PASSWORD_LABEL, createStringResource("PageSelfCredentials.oldPasswordLabel"));
        add(oldPasswordLabel);

        Label passwordLabel = new Label(ID_PASSWORD_LABEL, createStringResource("PageSelfCredentials.passwordLabel1"));
        add(passwordLabel);
        
      

        MetadataType metadata = model.getObject().getMetadata();
        
        
        passwordMetaInfo(metadata.getCreatorRef(),ID_PASSWORD_CREATE_INFO_LABEL, metadata.getCreateTimestamp(), "PageSelfCredentials.createInfoLabel");
        passwordMetaInfo(metadata.getModifierRef(),ID_LAST_CHANGE_LABEL, metadata.getModifyTimestamp(), "PageSelfCredentials.lastChangeLabel");
        
       
        		
   

        PasswordTextField oldPasswordField =
                new PasswordTextField(ID_OLD_PASSWORD_FIELD, new PropertyModel<String>(model, MyPasswordsDto.F_OLD_PASSWORD));
        oldPasswordField.setRequired(false);
        oldPasswordField.setResetPassword(false);
        add(oldPasswordField);

        if (model.getObject().getPasswordChangeSecurity() != null &&
                model.getObject().getPasswordChangeSecurity().equals(PasswordChangeSecurityType.NONE)){
            oldPasswordField.setVisible(false);
            oldPasswordLabel.setVisible(false);
        }

        PasswordPanel passwordPanel = new PasswordPanel(ID_PASSWORD_PANEL, new PropertyModel<ProtectedStringType>(model, MyPasswordsDto.F_PASSWORD));
        passwordPanel.getBaseFormComponent().add(new AttributeModifier("autofocus", ""));
        add(passwordPanel);

        WebMarkupContainer accountContainer = new WebMarkupContainer(ID_ACCOUNTS_CONTAINER);

        List<IColumn<PasswordAccountDto, String>> columns = initColumns();
        ListDataProvider<PasswordAccountDto> provider = new ListDataProvider<PasswordAccountDto>(this,
                new PropertyModel<List<PasswordAccountDto>>(model, MyPasswordsDto.F_ACCOUNTS));
        TablePanel accounts = new TablePanel(ID_ACCOUNTS_TABLE, provider, columns);
        accounts.setItemsPerPage(30);
        accounts.setShowPaging(false);
        if (model.getObject().getPropagation() != null && model.getObject().getPropagation()
                .equals(CredentialsPropagationUserControlType.MAPPING)){
            accountContainer.setVisible(false);
        }
        accountContainer.add(accounts);

        AjaxLink help = new AjaxLink(ID_BUTTON_HELP) {
        	private static final long serialVersionUID = 1L;
        	
            @Override
            public void onClick(AjaxRequestTarget target) {
                showHelpPerformed(target);
            }
        };
        accountContainer.add(help);

        add(accountContainer);
    }
    
    public void passwordMetaInfo(ObjectReferenceType user,String wicketid, XMLGregorianCalendar timestamp, String messageKey){

        String cmUser = user == null ? "--" : WebComponentUtil.getOrigStringFromPoly(user.getTargetName());
        Label lastChangeLabel = new Label(wicketid,
        							createStringResource(messageKey,
        							user == null ? "--" : timestamp,
        							cmUser));
        add(lastChangeLabel);

    }



	private List<IColumn<PasswordAccountDto, String>> initColumns() {
        List<IColumn<PasswordAccountDto, String>> columns = new ArrayList<IColumn<PasswordAccountDto, String>>();

        IColumn column = new IconColumn<PasswordAccountDto>(new Model<String>()) {
        	private static final long serialVersionUID = 1L;
        	
            @Override
            protected IModel<String> createIconModel(final IModel<PasswordAccountDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {
                	private static final long serialVersionUID = 1L;
                	
                    @Override
                    public String getObject() {
                        PasswordAccountDto item = rowModel.getObject();
                        if (item.getCssClass() == null || item.getCssClass().trim().equals("")) {
                            if (item.isMidpoint()) {
                                item.setCssClass(SELECTED_ACCOUNT_ICON_CSS);
                            } else if (item.isPasswordOutbound()) {
                                item.setCssClass(PROPAGATED_ACCOUNT_ICON_CSS);
                            } else {
                                item.setCssClass(DESELECTED_ACCOUNT_ICON_CSS);
                            }
                        }
                        return item.getCssClass();
                    }
                };
            }

            @Override
            public void populateItem(Item<ICellPopulator<PasswordAccountDto>> item, String componentId,
                                     final IModel<PasswordAccountDto> rowModel) {
                super.populateItem(item, componentId, rowModel);
                final ImagePanel imagePanel = (ImagePanel) item.get(0);

                final PasswordAccountDto passwordAccountDto = rowModel.getObject();

                imagePanel.add(new AjaxEventBehavior("click") {
                	private static final long serialVersionUID = 1L;
                	
                                   protected void onEvent(final AjaxRequestTarget target) {
                                       if (!passwordAccountDto.isMidpoint()) {
                                           if (passwordAccountDto.getCssClass().equals(PROPAGATED_ACCOUNT_ICON_CSS)) {
                                               passwordAccountDto.setCssClass(SELECTED_ACCOUNT_ICON_CSS);
                                           } else if (passwordAccountDto.getCssClass().equals(SELECTED_ACCOUNT_ICON_CSS)
                                                   && passwordAccountDto.isPasswordOutbound() &&
                                                   midpointAccountSelected) {
                                               passwordAccountDto.setCssClass(PROPAGATED_ACCOUNT_ICON_CSS);
                                           }  else if (passwordAccountDto.getCssClass().equals(SELECTED_ACCOUNT_ICON_CSS)) {
                                               passwordAccountDto.setCssClass(DESELECTED_ACCOUNT_ICON_CSS);
                                           } else if (passwordAccountDto.getCssClass().equals(DESELECTED_ACCOUNT_ICON_CSS)) {
                                               passwordAccountDto.setCssClass(SELECTED_ACCOUNT_ICON_CSS);
                                           }
                                           target.add(imagePanel);
                                       } else {
                                           midpointAccountSelected = !midpointAccountSelected;
                                           if (passwordAccountDto.getCssClass().equals(SELECTED_ACCOUNT_ICON_CSS)) {
                                               passwordAccountDto.setCssClass(DESELECTED_ACCOUNT_ICON_CSS);
                                               updatePropagatedAccountIconsCssClass(DESELECTED_ACCOUNT_ICON_CSS);
                                               target.add(imagePanel.findParent(SelectableDataTable.class));
                                           } else if (passwordAccountDto.getCssClass().equals(DESELECTED_ACCOUNT_ICON_CSS)) {
                                               passwordAccountDto.setCssClass(SELECTED_ACCOUNT_ICON_CSS);
                                               updatePropagatedAccountIconsCssClass(PROPAGATED_ACCOUNT_ICON_CSS);
                                               target.add(imagePanel.findParent(SelectableDataTable.class));
                                           }
                                       }
                                   }
                               }
                );
            }
        };
        columns.add(column);

        columns.add(new AbstractColumn<PasswordAccountDto, String>(createStringResource("ChangePasswordPanel.name")) {
        	private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PasswordAccountDto>> item, String componentId,
                                     final IModel<PasswordAccountDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {
                	private static final long serialVersionUID = 1L;

                    @Override
                    public Object getObject() {
                        PasswordAccountDto dto = rowModel.getObject();
                        return dto.getDisplayName();
                    }
                }));
            }
        });

        column = new PropertyColumn(createStringResource("ChangePasswordPanel.resourceName"),
                PasswordAccountDto.F_RESOURCE_NAME);
        columns.add(column);

        CheckBoxColumn enabled = new CheckBoxColumn(createStringResource("ChangePasswordPanel.enabled"),
                PasswordAccountDto.F_ENABLED);
        enabled.setEnabled(false);
        columns.add(enabled);

        return columns;
    }

    private void updatePropagatedAccountIconsCssClass(String cssClassName) {
        MyPasswordsDto dto = model.getObject();
        for (PasswordAccountDto passwordAccountDto : dto.getAccounts()) {
            if (passwordAccountDto.isPasswordOutbound()) {
                passwordAccountDto.setCssClass(cssClassName);
            }
        }
    }

    private void showHelpPerformed(AjaxRequestTarget target){
        getPageBase().showMainPopup(new HelpInfoPanel(getPageBase().getMainPopupBodyId(), "ChangePasswordPanel.helpInfo") {
            @Override
            protected void closePerformed(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);

            }
        }, target);
    }
}
