/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web.component.objectdetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.parser.XmlTag.TagType;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.ObjectWrapperOld;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentPanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.CheckTableHeader;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.PrismPanel;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.SimpleErrorPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 */
public class FocusProjectionsTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {
	private static final long serialVersionUID = 1L;

	private static final String ID_SHADOW_LIST = "shadowList";
	private static final String ID_SHADOW_TABLE = "shadowTable";
	private static final String ID_SHADOWS = "shadows";
	private static final String ID_SHADOWS_CONTAINER = "shadowContainer";
	private static final String ID_SHADOW_HEADER = "shadowHeader";
	private static final String ID_SHADOW = "shadow";
	private static final String ID_SHADOW_MENU = "shadowMenu";
	private static final String ID_SHADOW_CHECK_ALL = "shadowCheckAll";
	protected static final String ID_SPECIFIC_CONTAINERS_FRAGMENT = "specificContainersFragment";

	private static final String DOT_CLASS = FocusProjectionsTabPanel.class.getName() + ".";
	private static final String OPERATION_ADD_ACCOUNT = DOT_CLASS + "addShadow";

	private static final Trace LOGGER = TraceManager.getTrace(FocusProjectionsTabPanel.class);

	private LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel;

	public FocusProjectionsTabPanel(String id, Form mainForm, LoadableModel<PrismObjectWrapper<F>> focusModel,
			LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel) {
		super(id, mainForm, focusModel);
		Validate.notNull(projectionModel, "Null projection model");
		this.projectionModel = projectionModel;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}

	private void initLayout() {

//		final WebMarkupContainer shadows = new WebMarkupContainer(ID_SHADOWS);
//		shadows.setOutputMarkupId(true);
//		add(shadows);
//
//		InlineMenu accountMenu = new InlineMenu(ID_SHADOW_MENU, new Model((Serializable) createShadowMenu()));
//        accountMenu.setVisible(!getObjectWrapper().isReadOnly());
//		shadows.add(accountMenu);
		
		Map<PrismContainerValueWrapper<ShadowType>, FocusSubwrapperDto<ShadowType>> dtoByContainerValue = new HashMap<PrismContainerValueWrapper<ShadowType>, FocusSubwrapperDto<ShadowType>>();
		for (FocusSubwrapperDto<ShadowType> projection : projectionModel.getObject()) {
			dtoByContainerValue.put(projection.getObject().getValue(), projection);
		}
		
		TableId tableIdLoggers = UserProfileStorage.TableId.LOGGING_TAB_LOGGER_TABLE;
    	PageStorage pageStorageLoggers = getPageBase().getSessionStorage().getLoggingConfigurationTabLoggerTableStorage();
    	
    	
    	MultivalueContainerListPanelWithDetailsPanel<ShadowType, F> multivalueContainerListPanel =
				new MultivalueContainerListPanelWithDetailsPanel<ShadowType, F>(ID_SHADOW_TABLE, Model.of(projectionModel.getObject().get(0).getObject()),
    			tableIdLoggers, pageStorageLoggers) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected List<PrismContainerValueWrapper<ShadowType>> postSearch(
					List<PrismContainerValueWrapper<ShadowType>> itemss) {
				List<PrismContainerValueWrapper<ShadowType>> items = new ArrayList<PrismContainerValueWrapper<ShadowType>>();
				for (FocusSubwrapperDto<ShadowType> projection : projectionModel.getObject()) {
					items.add(projection.getObject().getValue());
				}
				return items;
			}
			
			@Override
			protected void newItemPerformed(AjaxRequestTarget target) {
				List<QName> supportedTypes = new ArrayList<>(1);
				supportedTypes.add(ResourceType.COMPLEX_TYPE);
				PageBase pageBase = FocusProjectionsTabPanel.this.getPageBase();
				ObjectBrowserPanel<ResourceType> resourceSelectionPanel = new ObjectBrowserPanel<ResourceType>(
						pageBase.getMainPopupBodyId(), ResourceType.class, supportedTypes, true,
						pageBase) {

					private static final long serialVersionUID = 1L;

					@Override
					protected void addPerformed(AjaxRequestTarget target, QName type,
												List<ResourceType> selected) {
						FocusProjectionsTabPanel.this.addSelectedAccountPerformed(target,
								selected);
					}
				};
				resourceSelectionPanel.setOutputMarkupId(true);
				pageBase.showMainPopup(resourceSelectionPanel,
						target);
			}
			
			@Override
			protected void initPaging() {
//				initLoggerPaging();
			}
			
			@Override
			protected boolean enableActionNewObject() {
				PrismObjectDefinition<F> def = getObjectWrapper().getObject().getDefinition();
				PrismReferenceDefinition ref = def.findReferenceDefinition(UserType.F_LINK_REF);
				return (ref.canRead() && ref.canAdd());
			}
			
			@Override
			protected ObjectQuery createQuery() {
			   return null;
			}
			
			@Override
			protected List<IColumn<PrismContainerValueWrapper<ShadowType>, String>> createColumns() {
				return initBasicColumns();
			}

			@Override
			protected void itemPerformedForDefaultAction(AjaxRequestTarget target,
					IModel<PrismContainerValueWrapper<ShadowType>> rowModel,
					List<PrismContainerValueWrapper<ShadowType>> listItems) {
				super.itemPerformedForDefaultAction(target, rowModel, listItems);
			}

			@Override
			protected List<SearchItemDefinition> initSearchableItems(
					PrismContainerDefinition<ShadowType> containerDef) {
				List<SearchItemDefinition> defs = new ArrayList<>();
				
				return defs;
			}

			@Override
			protected MultivalueContainerDetailsPanel<ShadowType> getMultivalueContainerDetailsPanel(
					ListItem<PrismContainerValueWrapper<ShadowType>> item) {
				return FocusProjectionsTabPanel.this.getMultivalueContainerDetailsPanel(item);
			}
		};
		add(multivalueContainerListPanel);
		setOutputMarkupId(true);

//		final ListView<FocusSubwrapperDto<ShadowType>> projectionList = new ListView<FocusSubwrapperDto<ShadowType>>(
//				ID_SHADOW_LIST, projectionModel) {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			protected void populateItem(final ListItem<FocusSubwrapperDto<ShadowType>> item) {
//				PackageResourceReference packageRef;
//				final FocusSubwrapperDto<ShadowType> dto = item.getModelObject();
//				final PropertyModel<PrismObjectWrapper<ShadowType>> objectWrapperModel = new PropertyModel<>(
//                    item.getModel(), "object");
//				final PropertyModel<ObjectWrapperOld<ShadowType>> objectWrapperModelOld = new PropertyModel<>(
//	                    item.getModel(), "objectOld");
//
////				Panel shadowPanel;
//
//				if (dto.isLoadedOK()) {
//					packageRef = new PackageResourceReference(ImgResources.class, ImgResources.HDD_PRISM);
//
//					//TODO shadowPanel
//					
//					
//					final ListView<PrismContainerWrapper<ShadowType>> shadowPanel = new ListView<PrismContainerWrapper<ShadowType>>(
//							ID_SHADOW, new ContainerWrapperListFromObjectWrapperModel(objectWrapperModel, 
//									WebComponentUtil.getShadowItemsToShow())) {
//						private static final long serialVersionUID = 1L;
//
//						@Override
//						protected void populateItem(ListItem<PrismContainerWrapper<ShadowType>> item) {
//							try {
//					    		Panel shadowContainerPanel = getPageBase().initItemPanel(ID_SHADOWS_CONTAINER, item.getModelObject().getTypeName(), item.getModel(), 
//					    				itemWrapper -> checkShadowContainerVisibilityVisibility(itemWrapper, objectWrapperModel));
//					    		item.add(shadowContainerPanel);
//							} catch (SchemaException e) {
//								LOGGER.error("Cannot create panel for logging: {}", e.getMessage(), e);
//								getSession().error("Cannot create panle for logging");
//							}
//						}
//
//					};
//					item.add(shadowPanel);
////					shadowPanel = new TextPanel(ID_SHADOW, Model.of("shadows here"));
////					shadowPanel = new PrismPanel<F>(ID_SHADOW,
////							new ContainerWrapperListFromObjectWrapperModel(objectWrapperModel, 
////									WebComponentUtil.getShadowItemsToShow()), packageRef,
////							getMainForm(), itemWrapper -> WebComponentUtil.checkShadowActivationAndPasswordVisibility(
////									itemWrapper, WebComponentUtil.adopt(objectWrapperModel, getPageBase().getPrismContext())), getPageBase());
////				} else {
////					shadowPanel = new SimpleErrorPanel<ShadowType>(ID_SHADOW, item.getModel()) {
////						private static final long serialVersionUID = 1L;
////
////						@Override
////						public void onShowMorePerformed(AjaxRequestTarget target) {
////							OperationResult fetchResult = dto.getResult();
////							if (fetchResult != null) {
////								showResult(fetchResult);
////								target.add(getFeedbackPanel());
////							}
////						}
////					};
//				}
//
////				shadowPanel.setOutputMarkupId(true);
////
////				shadowPanel.add(new VisibleEnableBehaviour() {
////					private static final long serialVersionUID = 1L;
////
////					@Override
////					public boolean isVisible() {
////						return true;//!objectWrapperModelOld.getObject().isMinimalized();
////					}
////
////				});
////
////				item.add(shadowPanel);
////				CheckTableHeader<ShadowType> shadowHeader = new CheckTableHeader<ShadowType>(ID_SHADOW_HEADER,
////						objectWrapperModelOld) {
////					private static final long serialVersionUID = 1L;
////
////					@Override
////					protected void onClickPerformed(AjaxRequestTarget target) {
////						super.onClickPerformed(target);
////						onExpandCollapse(target, item.getModel());
////						target.add(shadows);
////					}
////				};
////                if (UserDtoStatus.DELETE.equals(dto.getStatus())) {
////                    shadowHeader.add(new AttributeModifier("class", "box-header with-border delete"));
////                } 
//				Panel shadowHeader = new TextPanel(ID_SHADOW_HEADER, Model.of("shadows header here"));
//				item.add(shadowHeader);
//			}
//		};
//
//		AjaxCheckBox accountCheckAll = new AjaxCheckBox(ID_SHADOW_CHECK_ALL, new Model()) {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			protected void onUpdate(AjaxRequestTarget target) {
//				for (FocusSubwrapperDto<ShadowType> dto : projectionList.getModelObject()) {
//					if (dto.isLoadedOK()) {
//						ObjectWrapperOld<ShadowType> accModel = dto.getObjectOld();
//						accModel.setSelected(getModelObject());
//					}
//				}
//
//				target.add(shadows);
//			}
//		};
//		shadows.add(accountCheckAll);
//
//		shadows.add(projectionList);
	}
	
	private MultivalueContainerDetailsPanel<ShadowType> getMultivalueContainerDetailsPanel(
			ListItem<PrismContainerValueWrapper<ShadowType>> item) {
    	MultivalueContainerDetailsPanel<ShadowType> detailsPanel = new  MultivalueContainerDetailsPanel<ShadowType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected DisplayNamePanel<ShadowType> createDisplayNamePanel(String displayNamePanelId) {
				ItemRealValueModel<ShadowType> displayNameModel = 
						new ItemRealValueModel<ShadowType>(item.getModel());
				return new DisplayNamePanel<ShadowType>(displayNamePanelId, displayNameModel);
			}
			
			@Override
			protected void addBasicContainerValuePanel(String idPanel) {
				add(new WebMarkupContainer(idPanel));
			}
			
			@Override
			protected WebMarkupContainer getSpecificContainers(String contentAreaId) {
				Fragment specificContainers = new Fragment(contentAreaId, ID_SPECIFIC_CONTAINERS_FRAGMENT, FocusProjectionsTabPanel.this);
				
				List<? extends ItemWrapper<?, ?, ?, ?>> items = getModel().getObject().getItems();
				
				List<PrismContainerWrapper<ShadowType>> containers = new ArrayList<PrismContainerWrapper<ShadowType>>();
				
				for(ItemWrapper<?, ?, ?, ?> item : items) {
					if(QNameUtil.match(item.getName(), ShadowType.F_ATTRIBUTES) ||
							QNameUtil.match(item.getName(), ShadowType.F_ACTIVATION) ||
							QNameUtil.match(item.getName(), ShadowType.F_ASSOCIATION)) {
						containers.add((PrismContainerWrapper<ShadowType>)item);
					}
					if(QNameUtil.match(item.getName(), ShadowType.F_CREDENTIALS)) {
						try {
							containers.add(((PrismContainerWrapper<ShadowType>)item).findContainer(CredentialsType.F_PASSWORD));
						} catch (SchemaException e) {
							e.printStackTrace();
						}
					}
				}
				
				IModel<PrismContainerValueWrapper<ShadowType>> model = getModel();
				
				final ListView<PrismContainerWrapper<ShadowType>> shadowPanel = new ListView<PrismContainerWrapper<ShadowType>>(
						ID_SHADOW, Model.of((Collection)containers)) {
					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(ListItem<PrismContainerWrapper<ShadowType>> item) {
						try {
				    		Panel shadowContainerPanel = getPageBase().initItemPanel(ID_SHADOWS_CONTAINER, item.getModelObject().getTypeName(), item.getModel(), 
				    				itemWrapper -> checkShadowContainerVisibility(itemWrapper, model));
				    		item.add(shadowContainerPanel);
						} catch (SchemaException e) {
							LOGGER.error("Cannot create panel for logging: {}", e.getMessage(), e);
							getSession().error("Cannot create panle for logging");
						}
					}

				};
				specificContainers.add(shadowPanel);
				return specificContainers;
			}
		};
		return detailsPanel;
	}
	
	private List<IColumn<PrismContainerValueWrapper<ShadowType>, String>> initBasicColumns() {
		List<IColumn<PrismContainerValueWrapper<ShadowType>, String>> columns = new ArrayList<>();
		columns.add(new CheckBoxHeaderColumn<>());
		columns.add(new IconColumn<PrismContainerValueWrapper<ShadowType>>(Model.of("")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<ShadowType>> rowModel) {
				return WebComponentUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE));

			}

		});
		
		columns.add(new PrismPropertyColumn<ShadowType, String>(Model.of(projectionModel.getObject().get(0).getObject()), ShadowType.F_NAME, ColumnType.LINK){
			private static final long serialVersionUID = 1L;

			@Override
			protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ShadowType>> rowModel) {
				getMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
			}
		});
		columns.add(new PrismPropertyColumn<ShadowType, String>(Model.of(projectionModel.getObject().get(0).getObject()), ShadowType.F_OBJECT_CLASS, ColumnType.STRING));
//		columns.add(new PrismPropertyColumn<ShadowType, String>(Model.of(projectionModel.getObject().get(0).getObject()), ShadowType.F_RESOURCE_REF, ColumnType.STRING));
		columns.add(new PrismPropertyColumn<ShadowType, String>(Model.of(projectionModel.getObject().get(0).getObject()), ShadowType.F_KIND, ColumnType.STRING));
		columns.add(new PrismPropertyColumn<ShadowType, String>(Model.of(projectionModel.getObject().get(0).getObject()), ShadowType.F_INTENT, ColumnType.STRING));
		
//		List<InlineMenuItem> menuActionsList = getMultivalueContainerListPanel().getDefaultMenuActions();
		columns.add(new InlineMenuButtonColumn<>(createShadowMenu(), getPageBase()));
		
		return columns;
	}
	
	private MultivalueContainerListPanelWithDetailsPanel<ShadowType, F> getMultivalueContainerListPanel(){
		return ((MultivalueContainerListPanelWithDetailsPanel<ShadowType, F>)get(ID_SHADOW_TABLE));
	}
	
	private ItemVisibility checkShadowContainerVisibility(ItemWrapper itemWrapper, IModel<PrismContainerValueWrapper<ShadowType>> model) {
		
		if(itemWrapper.getPath().equivalent(ItemPath.create(ShadowType.F_ASSOCIATION))) {
			if(!((PrismContainerWrapper)itemWrapper).isEmpty() ) {
				return ItemVisibility.AUTO;
			} else {
				return ItemVisibility.HIDDEN;
			}
		}
		
		return WebComponentUtil.checkShadowActivationAndPasswordVisibility(itemWrapper, model);
	}
	
	private void onExpandCollapse(AjaxRequestTarget target, IModel<FocusSubwrapperDto<ShadowType>> dtoModel) {
		FocusSubwrapperDto<ShadowType> shadowWrapperDto = dtoModel.getObject();
		PrismObjectWrapper<ShadowType> shadowWrapper = shadowWrapperDto.getObject();
		if (!shadowWrapper.isExpanded()) {
			return;
		}
//		if (WebModelServiceUtils.isNoFetch(shadowWrapper.getLoadOptions())) {
//			((PageAdminFocus) getPage()).loadFullShadow(shadowWrapperDto);
//		}
	}

	private void addSelectedAccountPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
		getPageBase().hideMainPopup(target);

		if (newResources.isEmpty()) {
			warn(getString("pageUser.message.noResourceSelected"));
			return;
		}

		for (ResourceType resource : newResources) {
			try {
				ShadowType shadow = new ShadowType();
				shadow.setResource(resource);

				RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(
						resource.asPrismObject(), LayerType.PRESENTATION, getPrismContext());
				if (refinedSchema == null) {
					Task task = getPageBase().createSimpleTask(FocusPersonasTabPanel.class.getSimpleName() + ".loadResource");
					OperationResult result = task.getResult();
					PrismObject<ResourceType> loadedResource = WebModelServiceUtils.loadObject(ResourceType.class, resource.getOid(), getPageBase(), task, result);
					result.recomputeStatus();
					
					refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(
							loadedResource, LayerType.PRESENTATION, getPrismContext());
					
					if (refinedSchema == null) {
						error(getString("pageAdminFocus.message.couldntCreateAccountNoSchema",
								resource.getName()));
						continue;
					}
					
//					resource = loadedResource.asObjectable();
					shadow.setResource(loadedResource.asObjectable());
					
				}
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Refined schema for {}\n{}", resource, refinedSchema.debugDump());
				}

				RefinedObjectClassDefinition accountDefinition = refinedSchema
						.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
				if (accountDefinition == null) {
					error(getString("pageAdminFocus.message.couldntCreateAccountNoAccountSchema",
							resource.getName()));
					continue;
				}

				QName objectClass = accountDefinition.getObjectClassDefinition().getTypeName();
				shadow.setObjectClass(objectClass);

				getPrismContext().adopt(shadow);

				Task task = getPageBase().createSimpleTask(OPERATION_ADD_ACCOUNT);
				PrismObjectWrapperFactory<ShadowType> factory = getPageBase().getRegistry().getObjectWrapperFactory(shadow.asPrismObject().getDefinition());
				WrapperContext context = new WrapperContext(task, task.getResult());
				PrismObjectWrapper<ShadowType> wrappernew = factory.createObjectWrapper(shadow.asPrismObject(), ItemStatus.ADDED, context);
//				ObjectWrapperOld<ShadowType> wrapper = ObjectWrapperUtil.createObjectWrapper(
//						WebComponentUtil.getOrigStringFromPoly(resource.getName()), null,
//						shadow.asPrismObject(), ContainerStatus.ADDING, task, getPageBase());
				if (task.getResult() != null
						&& !WebComponentUtil.isSuccessOrHandledError(task.getResult())) {
					showResult(task.getResult(), false);
				}

//				wrapper.setShowEmpty(true);
//				wrapper.setMinimalized(true);
				projectionModel.getObject().add(new FocusSubwrapperDto(wrappernew, UserDtoStatus.ADD));
			} catch (Exception ex) {
				error(getString("pageAdminFocus.message.couldntCreateAccount", resource.getName(),
						ex.getMessage()));
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create account", ex);
			}
		}
		target.add(get(ID_SHADOW_TABLE));
	}

	private List<InlineMenuItem> createShadowMenu() {
		List<InlineMenuItem> items = new ArrayList<>();

		PrismObjectDefinition<F> def = getObjectWrapper().getObject().getDefinition();
		PrismReferenceDefinition ref = def.findReferenceDefinition(UserType.F_LINK_REF);
		ButtonInlineMenuItem item;
//		if (ref.canRead() && ref.canAdd()) {
//			item = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.addShadow")) {
//				private static final long serialVersionUID = 1L;
//
//				@Override
//				public InlineMenuItemAction initAction() {
//					return new InlineMenuItemAction() {
//						private static final long serialVersionUID = 1L;
//
//						@Override
//						public void onClick(AjaxRequestTarget target) {
//							List<QName> supportedTypes = new ArrayList<>(1);
//							supportedTypes.add(ResourceType.COMPLEX_TYPE);
//							PageBase pageBase = FocusProjectionsTabPanel.this.getPageBase();
//							ObjectBrowserPanel<ResourceType> resourceSelectionPanel = new ObjectBrowserPanel<ResourceType>(
//									pageBase.getMainPopupBodyId(), ResourceType.class, supportedTypes, true,
//									pageBase) {
//
//								private static final long serialVersionUID = 1L;
//
//								@Override
//								protected void addPerformed(AjaxRequestTarget target, QName type,
//															List<ResourceType> selected) {
//									FocusProjectionsTabPanel.this.addSelectedAccountPerformed(target,
//											selected);
//								}
//							};
//							resourceSelectionPanel.setOutputMarkupId(true);
//							pageBase.showMainPopup(resourceSelectionPanel,
//									target);
//						}
//					};
//				}
//
//				@Override
//				public String getButtonIconCssClass() {
//					// TODO Auto-generated method stub
//					return null;
//				}
//			};
//			items.add(item);
////			items.add(new InlineMenuItem());
//		}
		PrismPropertyDefinition<ActivationStatusType> administrativeStatus = def
				.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		if (administrativeStatus.canRead() && administrativeStatus.canModify()) {
			item = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.enable")) {
				private static final long serialVersionUID = 1L;

				@Override
				public InlineMenuItemAction initAction() {
					return new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							updateShadowActivation(target, getMultivalueContainerListPanel().getSelectedItems(), true);
						}
					};
				}
				
				@Override
				public String getButtonIconCssClass() {
					return GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON;
				}
			};
			items.add(item);
			item = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.disable")) {
				private static final long serialVersionUID = 1L;

				@Override
				public InlineMenuItemAction initAction() {
					return new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							updateShadowActivation(target, getMultivalueContainerListPanel().getSelectedItems(), false);
						}
					};
				}
				
				@Override
				public String getButtonIconCssClass() {
					return GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON;
				}
			};
			items.add(item);
		}
		if (ref.canRead() && ref.canAdd()) {
			item = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.unlink")) {
				private static final long serialVersionUID = 1L;

				@Override
				public InlineMenuItemAction initAction() {
					return new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							unlinkProjectionPerformed(target,
									getMultivalueContainerListPanel().getSelectedItems(), ID_SHADOWS);
						}
					};
				}
				
				@Override
				public String getButtonIconCssClass() {
					return GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON;
				}
			};
			items.add(item);
		}
		PrismPropertyDefinition<LockoutStatusType> locakoutStatus = def.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
		if (locakoutStatus.canRead() && locakoutStatus.canModify()) {
			item = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.unlock")) {
				private static final long serialVersionUID = 1L;

				@Override
				public InlineMenuItemAction initAction() {
					return new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							unlockShadowPerformed(target,
									getMultivalueContainerListPanel().getSelectedItems());
						}
					};
				}
				
				@Override
				public String getButtonIconCssClass() {
					return GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON;
				}
			};
			items.add(item);
		}
		if (administrativeStatus.canRead() && administrativeStatus.canModify()) {
//			items.add(new InlineMenuItem());
			item = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
				private static final long serialVersionUID = 1L;

				@Override
				public InlineMenuItemAction initAction() {
					return new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							deleteProjectionPerformed(target, getMultivalueContainerListPanel().getSelectedItems());
						}
					};
				}

				@Override
				public String getButtonIconCssClass() {
					return GuiStyleConstants.CLASS_DELETE_MENU_ITEM;
				}
			};
			items.add(item);
		}
		item = new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getButtonIconCssClass() {
				return GuiStyleConstants.CLASS_EDIT_MENU_ITEM;
			}

			@Override
			public InlineMenuItemAction initAction() {
				return getMultivalueContainerListPanel().createEditColumnAction();
			}
		};
		items.add(item);
		return items;
	}

	private List<FocusSubwrapperDto<ShadowType>> getSelectedProjections(
			IModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel) {
		List<FocusSubwrapperDto<ShadowType>> selected = new ArrayList<>();

		List<FocusSubwrapperDto<ShadowType>> all = projectionModel.getObject();
//		for (FocusSubwrapperDto<ShadowType> shadow : all) {
//			if (shadow.isLoadedOK() && shadow.getObject().isSelected()) {
//				selected.add(shadow);
//			}
//		}

		return selected;
	}

	private void deleteProjectionPerformed(AjaxRequestTarget target,
			List<PrismContainerValueWrapper<ShadowType>> selected) {
//		if (!isAnyProjectionSelected(target, model)) {
//			return;
//		}

		showModalWindow(getDeleteProjectionPopupContent(selected),
				target);
	}

	private boolean isAnyProjectionSelected(AjaxRequestTarget target,
			IModel<List<FocusSubwrapperDto<ShadowType>>> model) {
		List<FocusSubwrapperDto<ShadowType>> selected = getSelectedProjections(model);
		if (selected.isEmpty()) {
			warn(getString("pageAdminFocus.message.noAccountSelected"));
			target.add(getFeedbackPanel());
			return false;
		}

		return true;
	}

	private void updateShadowActivation(AjaxRequestTarget target,
			List<PrismContainerValueWrapper<ShadowType>> accounts, boolean enabled) {
		if (!isAnyProjectionSelected(target, projectionModel)) {
			return;
		}

		for (PrismContainerValueWrapper<ShadowType> account : accounts) {
//			if (!account.isLoadedOK()) {
//				continue;
//			}
			try {
//				ObjectWrapperOld<ShadowType> wrapper = account.getObjectOld();
//				PrismObjectWrapper<ShadowType> wrapper = account.getObject();
				PrismContainerWrapper<ActivationType> activation = account
						.findContainer(ShadowType.F_ACTIVATION);
				if (activation == null) {
					warn(getString("pageAdminFocus.message.noActivationFound", account.getDisplayName()));
					continue;
				}

				PrismPropertyWrapper enabledProperty = (PrismPropertyWrapper) activation.getValues().iterator().next()
						.findProperty(ActivationType.F_ADMINISTRATIVE_STATUS);
				if (enabledProperty == null || enabledProperty.getValues().size() != 1) {
					warn(getString("pageAdminFocus.message.noEnabledPropertyFound", account.getDisplayName()));
					continue;
				}
				PrismValueWrapper value = (PrismValueWrapper) enabledProperty.getValues().get(0);
				ActivationStatusType status = enabled ? ActivationStatusType.ENABLED
						: ActivationStatusType.DISABLED;
				value.setRealValue(status);
			} catch (SchemaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

//			wrapper.setSelected(false);
		}

		target.add(getFeedbackPanel(), get(createComponentPath(ID_SHADOWS)));
	}

	private void unlockShadowPerformed(AjaxRequestTarget target,
			List<PrismContainerValueWrapper<ShadowType>> selected) {
//		if (!isAnyProjectionSelected(target, model)) {
//			return;
//		}

		for (PrismContainerValueWrapper<ShadowType> account : selected) {
//			if (!account.isLoadedOK()) {
//				continue;
//			}
			try {
//				ObjectWrapperOld<ShadowType> wrapper = account.getObjectOld();
//				PrismObjectWrapper<ShadowType> wrapper = account.getObject();
//				wrapper.setSelected(false);

				PrismContainerWrapper<ActivationType> activation = account.findContainer(ShadowType.F_ACTIVATION);
				if (activation == null) {
					warn(getString("pageAdminFocus.message.noActivationFound", account.getDisplayName()));
					continue;
				}

				PrismPropertyWrapper lockedProperty = (PrismPropertyWrapper) activation.getValues().iterator().next().findProperty(ActivationType.F_LOCKOUT_STATUS);
				if (lockedProperty == null || lockedProperty.getValues().size() != 1) {
					warn(getString("pageAdminFocus.message.noLockoutStatusPropertyFound", account.getDisplayName()));
					continue;
				}
				PrismValueWrapper value = (PrismValueWrapper) lockedProperty.getValues().get(0);
				value.setRealValue(LockoutStatusType.NORMAL);
				info(getString("pageAdminFocus.message.unlocked", account.getDisplayName()));	
			} catch (SchemaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}// TODO only for really unlocked accounts
		}
		target.add(getFeedbackPanel(), get(createComponentPath(ID_SHADOWS)));
	}

	private void unlinkProjectionPerformed(AjaxRequestTarget target,
			List<PrismContainerValueWrapper<ShadowType>> selected,
			String componentPath) {
//		if (!isAnyProjectionSelected(target, model)) {
//			return;
//		}

		for (PrismContainerValueWrapper projection : selected) {
			if (UserDtoStatus.ADD.equals(projection.getStatus())) {
				continue;
			}
//			projection.setStatus(UserDtoStatus.UNLINK);
			projection.setStatus(ValueStatus.DELETED);
		}
		target.add(get(createComponentPath(componentPath)));
	}

	private Popupable getDeleteProjectionPopupContent(List<PrismContainerValueWrapper<ShadowType>> selected) {
		ConfirmationPanel dialog = new ConfirmationPanel(getPageBase().getMainPopupBodyId(),
				new IModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return createStringResource("pageAdminFocus.message.deleteAccountConfirm",
								selected.size()).getString();
					}
				}) {
			private static final long serialVersionUID = 1L;

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				deleteAccountConfirmedPerformed(target, selected);
			}
		};
		return dialog;
	}

	private void deleteAccountConfirmedPerformed(AjaxRequestTarget target,
			List<PrismContainerValueWrapper<ShadowType>> selected) {
		List<FocusSubwrapperDto<ShadowType>> accounts = projectionModel.getObject();
		for (PrismContainerValueWrapper<ShadowType> account : selected) {
			if (ValueStatus.ADDED.equals(account.getStatus())) {
				accounts.remove(getFocusSubwrapperDtoByShadowName(account.getRealValue().getName()));
			} else {
				account.setStatus(ValueStatus.DELETED);
			}
		}
		target.add(getMultivalueContainerListPanel());
	}
	
	private FocusSubwrapperDto<ShadowType> getFocusSubwrapperDtoByShadowName(PolyStringType name){
		for(FocusSubwrapperDto<ShadowType> projection : projectionModel.getObject()) {
			if(projection != null && projection.getObject() != null && 
					!projection.getObject().isEmpty() && projection.getObject().getValue() != null &&
					projection.getObject().getValue().getRealValue() != null &&
					projection.getObject().getValue().getRealValue().getName().equals(name)) {
				return projection;
			}
		}
		return null;
	}

}
