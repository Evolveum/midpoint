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
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.resources.content.PageAccount;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;

/**
 * @author semancik
 */
public class FocusProjectionsTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {
	private static final long serialVersionUID = 1L;

	private static final String ID_SHADOW_LIST = "shadowList";
	private static final String ID_SHADOWS = "shadows";
	private static final String ID_SHADOW_HEADER = "shadowHeader";
	private static final String ID_SHADOW = "shadow";
	private static final String ID_SHADOW_MENU = "shadowMenu";
	private static final String ID_SHADOW_CHECK_ALL = "shadowCheckAll";

	private static final String MODAL_ID_RESOURCE = "resourcePopup";

	private static final String DOT_CLASS = FocusProjectionsTabPanel.class.getName() + ".";
	private static final String OPERATION_ADD_ACCOUNT = DOT_CLASS + "addShadow";

	private static final Trace LOGGER = TraceManager.getTrace(FocusProjectionsTabPanel.class);

	private LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel;

	public FocusProjectionsTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusModel,
			LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel, PageBase page) {
		super(id, mainForm, focusModel, page);
		Validate.notNull(projectionModel, "Null projection model");
		this.projectionModel = projectionModel;
		initLayout(page);
	}

	private void initLayout(final PageBase page) {

		final WebMarkupContainer shadows = new WebMarkupContainer(ID_SHADOWS);
		shadows.setOutputMarkupId(true);
		add(shadows);

		InlineMenu accountMenu = new InlineMenu(ID_SHADOW_MENU, new Model((Serializable) createShadowMenu()));
        accountMenu.setVisible(!getObjectWrapper().isReadonly());
		shadows.add(accountMenu);

		final ListView<FocusSubwrapperDto<ShadowType>> projectionList = new ListView<FocusSubwrapperDto<ShadowType>>(
				ID_SHADOW_LIST, projectionModel) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<FocusSubwrapperDto<ShadowType>> item) {
				PackageResourceReference packageRef;
				final FocusSubwrapperDto<ShadowType> dto = item.getModelObject();
				final PropertyModel<ObjectWrapper<F>> objectWrapperModel = new PropertyModel<ObjectWrapper<F>>(
						item.getModel(), "object");

				final Panel shadowPanel;

				if (dto.isLoadedOK()) {
					packageRef = new PackageResourceReference(ImgResources.class, ImgResources.HDD_PRISM);

					shadowPanel = new PrismPanel<F>(ID_SHADOW,
							new ContainerWrapperListFromObjectWrapperModel<>(objectWrapperModel, null), packageRef,
							getMainForm(), null, getPageBase());
				} else {
					shadowPanel = new SimpleErrorPanel<ShadowType>(ID_SHADOW, item.getModel()) {
						private static final long serialVersionUID = 1L;

						@Override
						public void onShowMorePerformed(AjaxRequestTarget target) {
							OperationResult fetchResult = dto.getResult();
							if (fetchResult != null) {
								showResult(fetchResult);
								target.add(page.getFeedbackPanel());
							}
						}
					};
				}

				shadowPanel.setOutputMarkupId(true);

				shadowPanel.add(new VisibleEnableBehaviour() {
					private static final long serialVersionUID = 1L;

					@Override
					public boolean isVisible() {
						FocusSubwrapperDto<ShadowType> shadowWrapperDto = item.getModelObject();
						ObjectWrapper<ShadowType> shadowWrapper = shadowWrapperDto.getObject();
						return !shadowWrapper.isMinimalized();
					}

				});

				item.add(shadowPanel);

				CheckTableHeader<F> shadowHeader = new CheckTableHeader<F>(ID_SHADOW_HEADER,
						objectWrapperModel) {
					private static final long serialVersionUID = 1L;

					@Override
					protected void onClickPerformed(AjaxRequestTarget target) {
						super.onClickPerformed(target);
						onExpandCollapse(target, item.getModel());
						target.add(shadows);
					}
				};
                if (UserDtoStatus.DELETE.equals(dto.getStatus())) {
                    shadowHeader.add(new AttributeModifier("class", "box-header with-border delete"));
                }
				item.add(shadowHeader);
			}
		};

		AjaxCheckBox accountCheckAll = new AjaxCheckBox(ID_SHADOW_CHECK_ALL, new Model()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				for (FocusSubwrapperDto<ShadowType> dto : projectionList.getModelObject()) {
					if (dto.isLoadedOK()) {
						ObjectWrapper<ShadowType> accModel = dto.getObject();
						accModel.setSelected(getModelObject());
					}
				}

				target.add(shadows);
			}
		};
		shadows.add(accountCheckAll);

		shadows.add(projectionList);
	}

	private void onExpandCollapse(AjaxRequestTarget target, IModel<FocusSubwrapperDto<ShadowType>> dtoModel) {
		FocusSubwrapperDto<ShadowType> shadowWrapperDto = dtoModel.getObject();
		ObjectWrapper<ShadowType> shadowWrapper = shadowWrapperDto.getObject();
		if (shadowWrapper.isMinimalized()) {
			return;
		}
		if (WebModelServiceUtils.isNoFetch(shadowWrapper.getLoadOptions())) {
			((PageAdminFocus) getPage()).loadFullShadow(shadowWrapperDto);
		}
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
					error(getString("pageAdminFocus.message.couldntCreateAccountNoSchema",
							resource.getName()));
					continue;
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
				ObjectWrapper<ShadowType> wrapper = ObjectWrapperUtil.createObjectWrapper(
						WebComponentUtil.getOrigStringFromPoly(resource.getName()), null,
						shadow.asPrismObject(), ContainerStatus.ADDING, task, getPageBase());
				if (wrapper.getResult() != null
						&& !WebComponentUtil.isSuccessOrHandledError(wrapper.getResult())) {
					showResult(wrapper.getResult(), false);
				}

//				wrapper.setShowEmpty(true);
				wrapper.setMinimalized(true);
				projectionModel.getObject().add(new FocusSubwrapperDto(wrapper, UserDtoStatus.ADD));
			} catch (Exception ex) {
				error(getString("pageAdminFocus.message.couldntCreateAccount", resource.getName(),
						ex.getMessage()));
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create account", ex);
			}
		}
		target.add(get(ID_SHADOWS));
	}

	private List<InlineMenuItem> createShadowMenu() {
		List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();

		PrismObjectDefinition def = getObjectWrapper().getObject().getDefinition();
		PrismReferenceDefinition ref = def.findReferenceDefinition(UserType.F_LINK_REF);
		InlineMenuItem item;
		if (ref.canRead() && ref.canAdd()) {
			item = new InlineMenuItem(createStringResource("pageAdminFocus.button.addShadow"),
					new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							List<QName> supportedTypes = new ArrayList<>(1);
							supportedTypes.add(ResourceType.COMPLEX_TYPE);
							PageBase pageBase = FocusProjectionsTabPanel.this.getPageBase();
							ObjectBrowserPanel<ResourceType> resourceSelectionPanel = new ObjectBrowserPanel<ResourceType>(
									pageBase.getMainPopupBodyId(), ResourceType.class, supportedTypes, true,
									pageBase) {

								@Override
								protected void addPerformed(AjaxRequestTarget target, QName type,
										List<ResourceType> selected) {
									// TODO Auto-generated method stub
									FocusProjectionsTabPanel.this.addSelectedAccountPerformed(target,
											selected);
								}
							};
							resourceSelectionPanel.setOutputMarkupId(true);
							pageBase.showMainPopup(resourceSelectionPanel,
									target);
						}
					});
			items.add(item);
			items.add(new InlineMenuItem());
		}
		PrismPropertyDefinition prop = def
				.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		if (prop.canRead() && prop.canModify()) {
			item = new InlineMenuItem(createStringResource("pageAdminFocus.button.enable"),
					new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							updateShadowActivation(target, getSelectedProjections(projectionModel), true);
						}
					});
			items.add(item);
			item = new InlineMenuItem(createStringResource("pageAdminFocus.button.disable"),
					new InlineMenuItemAction() {

						/**
						 *
						 */
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							updateShadowActivation(target, getSelectedProjections(projectionModel), false);
						}
					});
			items.add(item);
		}
		if (ref.canRead() && ref.canAdd()) {
			item = new InlineMenuItem(createStringResource("pageAdminFocus.button.unlink"),
					new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							unlinkProjectionPerformed(target, projectionModel,
									getSelectedProjections(projectionModel), ID_SHADOWS);
						}
					});
			items.add(item);
		}
		prop = def.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
		if (prop.canRead() && prop.canModify()) {
			item = new InlineMenuItem(createStringResource("pageAdminFocus.button.unlock"),
					new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							unlockShadowPerformed(target, projectionModel,
									getSelectedProjections(projectionModel));
						}
					});
			items.add(item);
		}
		prop = def.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
		if (prop.canRead() && prop.canModify()) {
			items.add(new InlineMenuItem());
			item = new InlineMenuItem(createStringResource("pageAdminFocus.button.delete"),
					new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							deleteProjectionPerformed(target, projectionModel);
						}
					});
			items.add(item);
		}

		return items;
	}

	private List<FocusSubwrapperDto<ShadowType>> getSelectedProjections(
			IModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel) {
		List<FocusSubwrapperDto<ShadowType>> selected = new ArrayList<>();

		List<FocusSubwrapperDto<ShadowType>> all = projectionModel.getObject();
		for (FocusSubwrapperDto<ShadowType> shadow : all) {
			if (shadow.isLoadedOK() && shadow.getObject().isSelected()) {
				selected.add(shadow);
			}
		}

		return selected;
	}

	private void deleteProjectionPerformed(AjaxRequestTarget target,
			IModel<List<FocusSubwrapperDto<ShadowType>>> model) {
		if (!isAnyProjectionSelected(target, model)) {
			return;
		}

		showModalWindow(getDeleteProjectionPopupContent(),
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
			List<FocusSubwrapperDto<ShadowType>> accounts, boolean enabled) {
		if (!isAnyProjectionSelected(target, projectionModel)) {
			return;
		}

		for (FocusSubwrapperDto<ShadowType> account : accounts) {
			if (!account.isLoadedOK()) {
				continue;
			}

			ObjectWrapper<ShadowType> wrapper = account.getObject();
			ContainerWrapper<ActivationType> activation = wrapper
					.findContainerWrapper(new ItemPath(ShadowType.F_ACTIVATION));
			if (activation == null) {
				warn(getString("pageAdminFocus.message.noActivationFound", wrapper.getDisplayName()));
				continue;
			}

			PropertyWrapper enabledProperty = (PropertyWrapper) activation.getValues().iterator().next()
					.findPropertyWrapper(ActivationType.F_ADMINISTRATIVE_STATUS);
			if (enabledProperty == null || enabledProperty.getValues().size() != 1) {
				warn(getString("pageAdminFocus.message.noEnabledPropertyFound", wrapper.getDisplayName()));
				continue;
			}
			ValueWrapper value = (ValueWrapper) enabledProperty.getValues().get(0);
			ActivationStatusType status = enabled ? ActivationStatusType.ENABLED
					: ActivationStatusType.DISABLED;
			((PrismPropertyValue) value.getValue()).setValue(status);

			wrapper.setSelected(false);
		}

		target.add(getFeedbackPanel(), get(createComponentPath(ID_SHADOWS)));
	}

	private void unlockShadowPerformed(AjaxRequestTarget target,
			IModel<List<FocusSubwrapperDto<ShadowType>>> model,
			List<FocusSubwrapperDto<ShadowType>> selected) {
		if (!isAnyProjectionSelected(target, model)) {
			return;
		}

		for (FocusSubwrapperDto<ShadowType> account : selected) {
			if (!account.isLoadedOK()) {
				continue;
			}
			ObjectWrapper<ShadowType> wrapper = account.getObject();
			wrapper.setSelected(false);

			ContainerWrapper<ActivationType> activation = wrapper.findContainerWrapper(new ItemPath(ShadowType.F_ACTIVATION));
			if (activation == null) {
				warn(getString("pageAdminFocus.message.noActivationFound", wrapper.getDisplayName()));
				continue;
			}

			PropertyWrapper lockedProperty = (PropertyWrapper) activation.getValues().iterator().next().findPropertyWrapper(ActivationType.F_LOCKOUT_STATUS);
			if (lockedProperty == null || lockedProperty.getValues().size() != 1) {
				warn(getString("pageAdminFocus.message.noLockoutStatusPropertyFound", wrapper.getDisplayName()));
				continue;
			}
			ValueWrapper value = (ValueWrapper) lockedProperty.getValues().get(0);
			((PrismPropertyValue) value.getValue()).setValue(LockoutStatusType.NORMAL);
			info(getString("pageAdminFocus.message.unlocked", wrapper.getDisplayName()));			// TODO only for really unlocked accounts
		}
		target.add(getFeedbackPanel(), get(createComponentPath(ID_SHADOWS)));
	}

	private void unlinkProjectionPerformed(AjaxRequestTarget target,
			IModel<List<FocusSubwrapperDto<ShadowType>>> model, List<FocusSubwrapperDto<ShadowType>> selected,
			String componentPath) {
		if (!isAnyProjectionSelected(target, model)) {
			return;
		}

		for (FocusSubwrapperDto projection : selected) {
			if (UserDtoStatus.ADD.equals(projection.getStatus())) {
				continue;
			}
			projection.setStatus(UserDtoStatus.UNLINK);
		}
		target.add(get(createComponentPath(componentPath)));
	}

	private Popupable getDeleteProjectionPopupContent() {
		ConfirmationPanel dialog = new ConfirmationPanel(getPageBase().getMainPopupBodyId(),
				new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return createStringResource("pageAdminFocus.message.deleteAccountConfirm",
								getSelectedProjections(projectionModel).size()).getString();
					}
				}) {
			private static final long serialVersionUID = 1L;

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				ModalWindow modalWindow = findParent(ModalWindow.class);
				if (modalWindow != null) {
					modalWindow.close(target);
					deleteAccountConfirmedPerformed(target, getSelectedProjections(projectionModel));
				}
			}
		};
		return dialog;
	}

	private void deleteAccountConfirmedPerformed(AjaxRequestTarget target,
			List<FocusSubwrapperDto<ShadowType>> selected) {
		List<FocusSubwrapperDto<ShadowType>> accounts = projectionModel.getObject();
		for (FocusSubwrapperDto<ShadowType> account : selected) {
			if (UserDtoStatus.ADD.equals(account.getStatus())) {
				accounts.remove(account);
			} else {
				account.setStatus(UserDtoStatus.DELETE);
			}
		}
		target.add(get(createComponentPath(ID_SHADOWS)));
	}

}
