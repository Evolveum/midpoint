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
package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.page.admin.users.component.*;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.resource.PackageResourceReference;

import javax.xml.namespace.QName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author semancik
 */
public class FocusProjectionsTabPanel<F extends FocusType> extends AbstractObjectTabPanel {
	private static final long serialVersionUID = 1L;
	
	private static final String ID_SHADOW_LIST = "shadowList";
	private static final String ID_SHADOWS = "shadows";
	private static final String ID_SHADOW = "shadow";
	private static final String ID_SHADOW_MENU = "shadowMenu";
	private static final String ID_SHADOW_CHECK_ALL = "shadowCheckAll";
	
	private static final String MODAL_ID_RESOURCE = "resourcePopup";
	private static final String MODAL_ID_CONFIRM_DELETE_SHADOW = "confirmDeleteShadowPopup";
	
	private static final Trace LOGGER = TraceManager.getTrace(FocusProjectionsTabPanel.class);
	
	private LoadableModel<List<FocusProjectionDto>> projectionModel;

	public FocusProjectionsTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusModel, 
			LoadableModel<List<FocusProjectionDto>> projectionModel, PageBase page) {
		super(id, mainForm, focusModel, page);
		Validate.notNull(projectionModel, "Null projection model");
		this.projectionModel = projectionModel;
		initLayout();
	}
	
	private void initLayout() {

		final WebMarkupContainer shadows = new WebMarkupContainer(ID_SHADOWS);
		shadows.setOutputMarkupId(true);
		add(shadows);
		
		InlineMenu accountMenu = new InlineMenu(ID_SHADOW_MENU, new Model((Serializable) createShadowMenu()));
		shadows.add(accountMenu);

		final ListView<FocusProjectionDto> accountList = new ListView<FocusProjectionDto>(ID_SHADOW_LIST,
				projectionModel) {

			@Override
			protected void populateItem(final ListItem<FocusProjectionDto> item) {
				PackageResourceReference packageRef;
				final FocusProjectionDto dto = item.getModelObject();

				Panel panel;

				if (dto.isLoadedOK()) {
					packageRef = new PackageResourceReference(ImgResources.class, ImgResources.HDD_PRISM);

					panel = new PrismObjectPanel<F>(ID_SHADOW,
							new PropertyModel<ObjectWrapper<F>>(item.getModel(), "object"), packageRef,
							getMainForm(), getPageBase()) {

						@Override
						protected Component createHeader(String id, IModel<ObjectWrapper<F>> model) {
							return new CheckTableHeader(id, (IModel) model) {

//								@Override
//								protected List<InlineMenuItem> createMenuItems() {
//									return createDefaultMenuItems(getModel());
//								}
							};
						}
					};
				} else {
					panel = new SimpleErrorPanel(ID_SHADOW, item.getModel()) {

						@Override
						public void onShowMorePerformed(AjaxRequestTarget target) {
							OperationResult fetchResult = dto.getResult();
							if (fetchResult != null) {
								showResult(fetchResult);
								target.add(getPageBase().getFeedbackPanel());
							}
						}
					};
				}

				panel.setOutputMarkupId(true);
				item.add(panel);
			}
		};

		AjaxCheckBox accountCheckAll = new AjaxCheckBox(ID_SHADOW_CHECK_ALL, new Model()) {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				for (FocusProjectionDto dto : accountList.getModelObject()) {
					if (dto.isLoadedOK()) {
						ObjectWrapper accModel = dto.getObject();
						accModel.setSelected(getModelObject());
					}
				}

				target.add(shadows);
			}
		};
		shadows.add(accountCheckAll);

		shadows.add(accountList);

		initResourceModal();
	}

	private void initResourceModal() {
		ModalWindow window = new ModalWindow(MODAL_ID_RESOURCE);

		final SimpleUserResourceProvider provider = new SimpleUserResourceProvider(this, projectionModel) {
			@Override
			protected void handlePartialError(OperationResult result) {
				showResult(result);
			}
		};

		ResourcesSelectionPanel.Context context = new ResourcesSelectionPanel.Context(this) {
			@Override
			public FocusProjectionsTabPanel getRealParent() {
				return WebComponentUtil.theSameForPage(FocusProjectionsTabPanel.this, getCallingPageReference());
			}

			@Override
			public SimpleUserResourceProvider getProvider() {
				return provider;
			}

			@Override
			public void addPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
				getRealParent().addSelectedAccountPerformed(target, newResources);
			}
		};
		ResourcesSelectionPage.prepareDialog(window, context, this, "pageAdminFocus.title.selectResource", ID_SHADOWS);

		add(window);
	}

	private void addSelectedAccountPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
		ModalWindow window = (ModalWindow) get(MODAL_ID_RESOURCE);
		window.close(target);

		if (newResources.isEmpty()) {
			warn(getString("pageUser.message.noResourceSelected"));
			return;
		}

		for (ResourceType resource : newResources) {
			try {
				ShadowType shadow = new ShadowType();
				shadow.setResource(resource);

				RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(
						resource.asPrismObject(), LayerType.PRESENTATION, getPrismContext());
				if (refinedSchema == null) {
					error(getString("pageAdminFocus.message.couldntCreateAccountNoSchema",
							resource.getName()));
					continue;
				}
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Refined schema for {}\n{}", resource, refinedSchema.debugDump());
				}

				RefinedObjectClassDefinition accountDefinition = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
				if (accountDefinition == null) {
					error(getString("pageAdminFocus.message.couldntCreateAccountNoAccountSchema",
							resource.getName()));
					continue;
				}

				QName objectClass = accountDefinition.getObjectClassDefinition().getTypeName();
				shadow.setObjectClass(objectClass);

				getPrismContext().adopt(shadow);

				ObjectWrapper wrapper = ObjectWrapperUtil.createObjectWrapper(
						WebComponentUtil.getOrigStringFromPoly(resource.getName()), null, shadow.asPrismObject(),
						ContainerStatus.ADDING, getPageBase());
				if (wrapper.getResult() != null
						&& !WebComponentUtil.isSuccessOrHandledError(wrapper.getResult())) {
					showResult(wrapper.getResult(), false);
				}

				wrapper.setShowEmpty(true);
				wrapper.setMinimalized(false);
				projectionModel.getObject().add(new FocusProjectionDto(wrapper, UserDtoStatus.ADD));
			} catch (Exception ex) {
				error(getString("pageAdminFocus.message.couldntCreateAccount", resource.getName(),
						ex.getMessage()));
				LoggingUtils.logException(LOGGER, "Couldn't create account", ex);
			}
		}
	}
	
	private List<InlineMenuItem> createShadowMenu() {
		List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();

        PrismObjectDefinition def = getObjectWrapper().getObject().getDefinition();
        PrismReferenceDefinition ref = def.findReferenceDefinition(UserType.F_LINK_REF);
        InlineMenuItem item ;
        if (ref.canRead() && ref.canAdd()){
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.addShadow"),
                    new InlineMenuItemAction() {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            showModalWindow(MODAL_ID_RESOURCE, target);
                        }
                    });
            items.add(item);
            items.add(new InlineMenuItem());
        }
        PrismPropertyDefinition prop = def.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        if (prop.canRead() && prop.canModify()) {
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.enable"),
                    new InlineMenuItemAction() {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            updateShadowActivation(target, getSelectedProjections(projectionModel), true);
                        }
                    });
            items.add(item);
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.disable"),
                    new InlineMenuItemAction() {

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

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            unlinkProjectionPerformed(target, projectionModel, getSelectedProjections(projectionModel), ID_SHADOWS);
                        }
                    });
            items.add(item);
        }
        prop = def.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
        if (prop.canRead() && prop.canModify()) {
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.unlock"),
                    new InlineMenuItemAction() {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            unlockShadowPerformed(target, projectionModel, getSelectedProjections(projectionModel));
                        }
                    });
            items.add(item);
        }
        prop = def.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        if (prop.canRead() && prop.canModify()) {
            items.add(new InlineMenuItem());
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.delete"),
                    new InlineMenuItemAction() {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            deleteProjectionPerformed(target, projectionModel);
                        }
                    });
            items.add(item);
        }

		return items;
	}
	
	private List<FocusProjectionDto> getSelectedProjections(IModel<List<FocusProjectionDto>> model) {
		List<FocusProjectionDto> selected = new ArrayList<FocusProjectionDto>();

		List<FocusProjectionDto> all = model.getObject();
		for (FocusProjectionDto shadow : all) {
			if (shadow.isLoadedOK() && shadow.getObject().isSelected()) {
				selected.add(shadow);
			}
		}

		return selected;
	}
	
	private void deleteProjectionPerformed(AjaxRequestTarget target, IModel<List<FocusProjectionDto>> model) {
		if (!isAnyProjectionSelected(target, model)) {
			return;
		}

		showModalWindow(getDeleteProjectionPopupContent(),
                createStringResource("pageAdminFocus.title.confirmDelete"), target);
	}
	
	private boolean isAnyProjectionSelected(AjaxRequestTarget target,
			IModel<List<FocusProjectionDto>> model) {
		List<FocusProjectionDto> selected = getSelectedProjections(model);
		if (selected.isEmpty()) {
			warn(getString("pageAdminFocus.message.noAccountSelected"));
			target.add(getFeedbackPanel());
			return false;
		}

		return true;
	}

	private void updateShadowActivation(AjaxRequestTarget target, List<FocusProjectionDto> accounts,
			boolean enabled) {
		if (!isAnyProjectionSelected(target, projectionModel)) {
			return;
		}

		for (FocusProjectionDto account : accounts) {
			if (!account.isLoadedOK()) {
				continue;
			}

			ObjectWrapper wrapper = account.getObject();
			ContainerWrapper activation = wrapper.findContainerWrapper(new ItemPath(ShadowType.F_ACTIVATION));
			if (activation == null) {
				warn(getString("pageAdminFocus.message.noActivationFound", wrapper.getDisplayName()));
				continue;
			}

			PropertyWrapper enabledProperty = (PropertyWrapper) activation
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
	
	private void unlockShadowPerformed(AjaxRequestTarget target, IModel<List<FocusProjectionDto>> model,
			List<FocusProjectionDto> selected) {
		if (!isAnyProjectionSelected(target, model)) {
			return;
		}

		for (FocusProjectionDto account : selected) {
			// TODO: implement unlock
		}
	}
	
	private void unlinkProjectionPerformed(AjaxRequestTarget target, IModel<List<FocusProjectionDto>> model,
			List<FocusProjectionDto> selected, String componentPath) {
		if (!isAnyProjectionSelected(target, model)) {
			return;
		}

		for (FocusProjectionDto projection : selected) {
			if (UserDtoStatus.ADD.equals(projection.getStatus())) {
				continue;
			}
			projection.setStatus(UserDtoStatus.UNLINK);
		}
		target.add(get(createComponentPath(componentPath)));
	}

    private Component getDeleteProjectionPopupContent(){
        ConfirmationPanel dialog = new ConfirmationPanel(getPageBase().getMainPopupBodyId(),
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return createStringResource("pageAdminFocus.message.deleteAccountConfirm",
                                getSelectedProjections(projectionModel).size()).getString();
                    }
                }) {

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
                                                 List<FocusProjectionDto> selected) {
        List<FocusProjectionDto> accounts = projectionModel.getObject();
        for (FocusProjectionDto account : selected) {
            if (UserDtoStatus.ADD.equals(account.getStatus())) {
                accounts.remove(account);
            } else {
                account.setStatus(UserDtoStatus.DELETE);
            }
        }
        target.add(get(createComponentPath(ID_SHADOWS)));
    }

}
