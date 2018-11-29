/*
 * Copyright (c) 2018 Evolveum
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

/**
 * @author skublik
 */

package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.EditableColumn;
import com.evolveum.midpoint.gui.impl.component.form.TriStateFormGroup;
import com.evolveum.midpoint.gui.impl.model.PropertyWrapperFromContainerWrapperModel;
import com.evolveum.midpoint.gui.impl.model.RealValueFromSingleValuePropertyWrapperModel;
import com.evolveum.midpoint.gui.impl.model.RealValueOfSingleValuePropertyFromSingleValueContainerWrapperModel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.EditableLinkColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailServerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailTransportSecurityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;


/**
 * @author skublik
 */
public class NotificationConfigTabPanel extends BasePanel<ContainerWrapper<NotificationConfigurationType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(NotificationConfigTabPanel.class);

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
	
	MailConfigurationType mailConfigType;
	List<MailServerConfiguration> mailServers;

	public NotificationConfigTabPanel(String id, IModel<ContainerWrapper<NotificationConfigurationType>> model) {
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
    	getPageBase().getSessionStorage().getNotificationConfigurationTabMailServerTableStorage().setPaging(ObjectPaging.createPaging(0, (int) getPageBase().getItemsPerPage(UserProfileStorage.TableId.NOTIFICATION_TAB_MAIL_SERVER_TABLE)));
    }

	protected void initLayout() {
		
		PropertyWrapperFromContainerWrapperModel<MailConfigurationType, NotificationConfigurationType> mailConfig = 
				new PropertyWrapperFromContainerWrapperModel<MailConfigurationType, NotificationConfigurationType>(getModel().getObject(), NotificationConfigurationType.F_MAIL);
		
		add(createHeader(ID_MAIL_CONFIG_HEADER, mailConfig.getObject().getItemDefinition().getTypeName().getLocalPart() + ".details"));
		
		Form form = new Form<>("form");
		
		
		
		mailConfigType = new RealValueFromSingleValuePropertyWrapperModel<MailConfigurationType>(mailConfig).getObject();
		
		if(mailConfigType == null) {
			mailConfigType = new MailConfigurationType();
			((PrismPropertyValue<MailConfigurationType>)mailConfig.getObject().getValues().get(0).getValue()).setValue(mailConfigType);
    	}
		
		add(new TextFormGroup(ID_DEFAULT_FROM, new PropertyModel<String>(mailConfigType, "defaultFrom"), createStringResource(mailConfig.getObject().getItemDefinition().getTypeName().getLocalPart() + ".defaultFrom"), "", getInputCssClass(), false, true));
		
		add(new TextFormGroup(ID_REDIRECT_TO_FILE, new PropertyModel<String>(mailConfigType, "redirectToFile"), createStringResource(mailConfig.getObject().getItemDefinition().getTypeName().getLocalPart() + ".redirectToFile"), "", getInputCssClass(), false, true));
		
		add(new TextFormGroup(ID_LOG_TO_FILE, new PropertyModel<String>(mailConfigType, "logToFile"), createStringResource(mailConfig.getObject().getItemDefinition().getTypeName().getLocalPart() + ".logToFile"), "", getInputCssClass(), false, true));
		
		add(new TriStateFormGroup(ID_DEBUG, new PropertyModel<Boolean>(mailConfigType, "debug"), createStringResource(mailConfig.getObject().getItemDefinition().getTypeName().getLocalPart() + ".debug"), "", getInputCssClass(), false, true));
		
		add(createHeader(ID_MAIL_SERVER_CONFIG_HEADER, MailServerConfigurationType.COMPLEX_TYPE.getLocalPart() + ".details"));
		
        add(initServersTable());
        
        add(createHeader(ID_FILE_CONFIG_HEADER, FileConfigurationType.COMPLEX_TYPE.getLocalPart() + ".details"));
        
        PropertyWrapperFromContainerWrapperModel<FileConfigurationType, NotificationConfigurationType> fileConfig = 
        		new PropertyWrapperFromContainerWrapperModel<FileConfigurationType, NotificationConfigurationType>(getModel().getObject(), NotificationConfigurationType.F_FILE);
        
        WebMarkupContainer files = new WebMarkupContainer(ID_FILE_CONFIG);
        files.setOutputMarkupId(true);
        add(files);
        
        ListView<ValueWrapper<FileConfigurationType>> values = new ListView<ValueWrapper<FileConfigurationType>>("values",
                new PropertyModel<>(fileConfig, "values")) {
            	private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(final ListItem<ValueWrapper<FileConfigurationType>> item) {
                	
                	FileConfigurationType fileConfigType = item.getModelObject().getValue().getRealValue();
                	
                	item.add(createHeader(ID_VALUE_HEADER, fileConfigType == null || fileConfigType.getName() == null || fileConfigType.getName().isEmpty() ? (FileConfigurationType.COMPLEX_TYPE.getLocalPart() + ".details") : fileConfigType.getName()));
                	
                	AjaxLink removeButton = new AjaxLink(ID_REMOVE_BUTTON) {
                    	private static final long serialVersionUID = 1L;

                    	@Override
                    	public void onClick(AjaxRequestTarget target) {
                    		((PrismPropertyValue<FileConfigurationType>)item.getModelObject().getValue()).setValue(null);
                    		item.getParent().remove(item.getId());
                    		target.add(files);
                    	}
                    };
                    item.add(removeButton);
                	
                    TextFormGroup name = new TextFormGroup(ID_FILE_NAME, fileConfigType != null ? new PropertyModel<String>(fileConfigType, "name") : Model.of(""), createStringResource(fileConfigType == null ? "" : (fileConfigType.COMPLEX_TYPE.getLocalPart() + ".name")), "", getInputCssClass(), false, true);
                    name.getField().add(new OnChangeAjaxBehavior() {
						
						private static final long serialVersionUID = 1L;

						@Override
						protected void onUpdate(AjaxRequestTarget target) {
							((FileConfigurationType)item.getModelObject().getValue().getRealValue()).setName(name.getModelObject());
						}
					});
                	item.add(name);
                	
                	TextFormGroup file = new TextFormGroup(ID_FILE_PATH, fileConfigType != null ? new PropertyModel<String>(fileConfigType, "file") : Model.of(""), createStringResource(fileConfigType == null ? "" : (fileConfigType.COMPLEX_TYPE.getLocalPart() + ".file")), "", getInputCssClass(), false, true);
                	file.getField().add(new OnChangeAjaxBehavior() {
						
						private static final long serialVersionUID = 1L;

						@Override
						protected void onUpdate(AjaxRequestTarget target) {
							((FileConfigurationType)item.getModelObject().getValue().getRealValue()).setFile(file.getModelObject());
						}
					});
                	item.add(file);
                
                	item.add(new VisibleEnableBehaviour() {
                		
                		@Override
                		public boolean isVisible() {
                			return fileConfigType != null;
                		}
                	});
                }
            };
            values.add(new AttributeModifier("class", "col-md-6"));
            values.setReuseItems(true);
            files.add(values);
            
            AjaxLink addButton = new AjaxLink(ID_ADD_BUTTON) {
            	private static final long serialVersionUID = 1L;

            	@Override
            	public void onClick(AjaxRequestTarget target) {
            		ValueWrapper<FileConfigurationType> newValue = fileConfig.getObject().createAddedValue();
            		((PrismPropertyValue<FileConfigurationType>)newValue.getValue()).setValue(new FileConfigurationType());
            		fileConfig.getObject().getValues().add(newValue);
            		target.add(files);
            	}
            };
            add(addButton);
        
	}
	
	private BoxedTablePanel<MailServerConfiguration> initServersTable() {
		
		mailServers = getListOfMailServerConfiguration(mailConfigType.getServer());
		PageStorage pageStorage = getPageBase().getSessionStorage().getNotificationConfigurationTabMailServerTableStorage();
		ISortableDataProvider<MailServerConfiguration, String> provider = new ListDataProvider<MailServerConfiguration>(this,
                new ListModel<MailServerConfiguration>(mailServers) {

					private static final long serialVersionUID = 1L;
					
					@Override
					public void setObject(List<MailServerConfiguration> object) {
						super.setObject(object);
						mailConfigType.getServer().clear();
						for(MailServerConfiguration value : object) {
							mailConfigType.server(value.getValue());
						}
						
					}
			
		}) {

					private static final long serialVersionUID = 1L;
			
					@Override
					protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
						pageStorage.setPaging(paging);
					}
			
		};
		
		UserProfileStorage userProfile = getPageBase().getSessionStorage().getUserProfile();
		UserProfileStorage.TableId tableId = UserProfileStorage.TableId.NOTIFICATION_TAB_MAIL_SERVER_TABLE;
		BoxedTablePanel<MailServerConfiguration> table = new BoxedTablePanel<MailServerConfiguration>(ID_MAIL_SERVERS_TABLE, provider, initMailServersColumns(), tableId, userProfile.getPagingSize(tableId)) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public int getItemsPerPage() {
				return getPageBase().getSessionStorage().getUserProfile().getTables()
						.get(getTableId());
			}
			
			@Override
			protected WebMarkupContainer createButtonToolbar(String id) {
				AjaxIconButton newObjectIcon = new AjaxIconButton(id, new Model<>("fa fa-plus"),
						createStringResource("MainObjectListPanel.newObject")) {

					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						newItemPerformed(target);
					}
				};
				newObjectIcon.add(AttributeModifier.append("class", Model.of("btn btn-success btn-sm")));
				return newObjectIcon;
			}
		};
        table.setOutputMarkupId(true);
        table.setShowPaging(true);
        return table;
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
	
	private void newItemPerformed(AjaxRequestTarget target) {
		MailServerConfigurationType newServerType = new MailServerConfigurationType();
		mailConfigType.server(newServerType);
		MailServerConfiguration newServer = new MailServerConfiguration(newServerType);
		mailServers.add(newServer);
        mailServerEditPerformed(target, Model.of(newServer), null);
	}
	
	private List<IColumn<MailServerConfiguration, String>> initMailServersColumns() {
        List<IColumn<MailServerConfiguration, String>> columns = new ArrayList<>();
        columns.add(new CheckBoxHeaderColumn<MailServerConfiguration>());
        
        columns.add(new IconColumn<MailServerConfiguration>(Model.of("")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(IModel<MailServerConfiguration> rowModel) {
				return Model.of(WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE));
			}

		});
        
        columns.add(new EditableLinkColumn<MailServerConfiguration>(createStringResource("MailServerConfigurationType.host")){
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
        
        columns.add(new EditableColumn<MailServerConfiguration, String>(createStringResource("MailServerConfigurationType.port")){
        	
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
        
        columns.add(new EditableColumn<MailServerConfiguration, String>(createStringResource("MailServerConfigurationType.username")){
        	
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
        
        columns.add(new EditableColumn<MailServerConfiguration, String>(createStringResource("MailServerConfigurationType.password")){
        	
			private static final long serialVersionUID = 1L;

			@Override
        	protected Component createStaticPanel(String componentId, IModel<MailServerConfiguration> rowModel) {
        		return new PasswordPanel(componentId, Model.of(rowModel.getObject().getValue().getPassword()), true, false);
        	}
        	
        	@Override
        	protected Component createInputPanel(String componentId, IModel<MailServerConfiguration> rowModel) {
        		MailServerConfigurationType mailServer = rowModel.getObject().getValue();
        		return new PasswordPanel(componentId, new PropertyModel<>(mailServer, "password"), false, true);
        	}
        });
        
        columns.add(new EditableColumn<MailServerConfiguration, String>(createStringResource("MailServerConfigurationType.transportSecurity")){
        	
			private static final long serialVersionUID = 1L;

			@Override
        	protected Component createStaticPanel(String componentId, IModel<MailServerConfiguration> rowModel) {
        		return new Label(componentId, Model.of(WebComponentUtil.createLocalizedModelForEnum(rowModel.getObject().getValue().getTransportSecurity(), null)));//rowModel.getObject().getClass().getName() + "." + rowModel.getObject().getValue().getTransportSecurity().value()));
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
		menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.unassign")) {
			private static final long serialVersionUID = 1L;

			@Override
            public String getButtonIconCssClass() {
				return GuiStyleConstants.CLASS_DELETE_MENU_ITEM;
			}

			@Override
            public InlineMenuItemAction initAction() {
				return createDeleteColumnAction();
			}
		});

		menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
			private static final long serialVersionUID = 1L;

			@Override
            public String getButtonIconCssClass() {
				return GuiStyleConstants.CLASS_EDIT_MENU_ITEM;
			}

			@Override
            public InlineMenuItemAction initAction() {
				return createEditColumnAction();
			}
		});
		return menuItems;
	}
	
	private ColumnMenuAction<MailServerConfiguration> createDeleteColumnAction() {
		return new ColumnMenuAction<MailServerConfiguration>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				if (getRowModel() == null) {
					deleteItemPerformed(target, getSelectedItems());
				} else {
					List<MailServerConfiguration> toDelete = new ArrayList<>();
					toDelete.add(getRowModel().getObject());
					deleteItemPerformed(target, toDelete);
				}
			}
		};
	}
	
	private void deleteItemPerformed(AjaxRequestTarget target, List<MailServerConfiguration> toDelete) {
		if (toDelete == null){
			return;
		}
		
		
		RealValueOfSingleValuePropertyFromSingleValueContainerWrapperModel<MailConfigurationType, NotificationConfigurationType> mailConfigType = 
				new RealValueOfSingleValuePropertyFromSingleValueContainerWrapperModel<>(getModel(), NotificationConfigurationType.F_MAIL);
		List<MailServerConfigurationType> servers = mailConfigType.getObject().getServer();
		
		toDelete.forEach(value -> {
			servers.remove(value.getValue());
		});
		target.add(this.addOrReplace(initServersTable()));
		reloadSavePreviewButtons(target);
	}
	
	private void reloadSavePreviewButtons(AjaxRequestTarget target){
		FocusMainPanel mainPanel = findParent(FocusMainPanel.class);
		if (mainPanel != null) {
			mainPanel.reloadSavePreviewButtons(target);
		}
	}
	
	private ColumnMenuAction<MailServerConfiguration> createEditColumnAction() {
		return new ColumnMenuAction<MailServerConfiguration>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				mailServerEditPerformed(target, getRowModel(), getSelectedItems());
			}
		};
	}
	
	private List<MailServerConfiguration> getSelectedItems() {
		BoxedTablePanel<MailServerConfiguration> itemsTable = getMailServersTable();
		ListDataProvider<MailServerConfiguration> itemsProvider = (ListDataProvider<MailServerConfiguration>) itemsTable.getDataTable()
				.getDataProvider();
		return itemsProvider.getAvailableData().stream().filter(a -> a.isSelected()).collect(Collectors.toList());
	}
	
	private void mailServerEditPerformed(AjaxRequestTarget target, IModel<MailServerConfiguration> rowModel,
			List<MailServerConfiguration> listItems) {
		
		if(rowModel != null) {
			MailServerConfiguration server = rowModel.getObject();
			server.setEditing(true);
			server.setSelected(true);
    	} else {
    		for(MailServerConfiguration server : listItems) {
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
        return"col-xs-10";
    }
	
	private List<MailServerConfiguration> getListOfMailServerConfiguration(List<MailServerConfigurationType> mailServersType){
		List<MailServerConfiguration> list = new ArrayList<MailServerConfiguration>();
			for(MailServerConfigurationType value : mailServersType) {
				list.add(new MailServerConfiguration(value));
			}
		return list;
	}
	
	public class MailServerConfiguration extends Selectable<MailServerConfigurationType> implements Editable{

		private static final long serialVersionUID = 1L;
		
		private boolean editable;
		private MailServerConfigurationType mailServer;
		
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
