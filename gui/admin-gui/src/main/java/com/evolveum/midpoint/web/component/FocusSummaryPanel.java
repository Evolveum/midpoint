/**
 * Copyright (c) 2015-2017 Evolveum
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
package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.ReadOnlyPrismObjectFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.ReadOnlyWrapperModel;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.component.OrgSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.component.ServiceSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author semancik
 *
 */
public abstract class FocusSummaryPanel<O extends ObjectType> extends ObjectSummaryPanel<O> {
	private static final long serialVersionUID = 1L;

	private static final String ID_ACTIVATION_TAG = "activationTag";

	private IModel<ObjectWrapper<O>> wrapperModel;

	public FocusSummaryPanel(String id, Class<O> type, final IModel<ObjectWrapper<O>> model, ModelServiceLocator serviceLocator) {
		super(id, type, new ReadOnlyPrismObjectFromObjectWrapperModel<O>(model), serviceLocator);

		this.wrapperModel = model;
		initLayoutCommon(serviceLocator);	// calls getParentOrgModel that depends on wrapperModel

		SummaryTag<O> tagActivation = new SummaryTag<O>(ID_ACTIVATION_TAG, model) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void initialize(ObjectWrapper<O> wrapper) {
				ActivationType activation = null;
				O object = wrapper.getObject().asObjectable();
				if (object instanceof FocusType) {
					activation = ((FocusType)object).getActivation();
				}
				if (activation == null) {
					setIconCssClass(GuiStyleConstants.CLASS_ICON_ACTIVATION_ACTIVE);
					setLabel(getString("ActivationStatusType.ENABLED"));

				} else if (activation.getEffectiveStatus() == ActivationStatusType.DISABLED) {
					setIconCssClass(GuiStyleConstants.CLASS_ICON_ACTIVATION_INACTIVE);
					setLabel(getString("ActivationStatusType.DISABLED"));
					setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_DISABLED);

				} else if (activation.getEffectiveStatus() == ActivationStatusType.ARCHIVED) {
					setIconCssClass(GuiStyleConstants.CLASS_ICON_ACTIVATION_INACTIVE);
					setLabel(getString("ActivationStatusType.ARCHIVED"));
					setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_ARCHIVED);

				} else {
					setIconCssClass(GuiStyleConstants.CLASS_ICON_ACTIVATION_ACTIVE);
					setLabel(getString("ActivationStatusType.ENABLED"));
				}
			}
		};
		tagActivation.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isActivationVisible();
			}
		});
		addTag(tagActivation);
	}

	@Override
	protected IModel<String> getDefaltParentOrgModel() {
		return new ReadOnlyWrapperModel<String,O>(wrapperModel) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				Collection<PrismObject<OrgType>> parentOrgs = getWrapper().getParentOrgs();
				if (parentOrgs.isEmpty()) {
					return "";
				}
				// Kinda hack now .. "functional" orgType always has preference
				// this whole thing should be driven by an expression later on
				for (PrismObject<OrgType> org: parentOrgs) {
					OrgType orgType = org.asObjectable();
					if (orgType.getOrgType().contains("functional")) {
						return PolyString.getOrig(orgType.getDisplayName());
					}
				}
				// Just use the first one as a fallback
				return PolyString.getOrig(parentOrgs.iterator().next().asObjectable().getDisplayName());
			}
		};
	}

	@Override
	protected void addAdditionalExpressionVariables(ExpressionVariables variables) {
		Collection<PrismObject<OrgType>> parentOrgs = wrapperModel.getObject().getParentOrgs();
		Collection<OrgType> parentOrgTypes = parentOrgs.stream().map(o -> o.asObjectable()).collect(Collectors.toList());
		variables.addVariableDefinition(ExpressionConstants.VAR_ORGS, parentOrgTypes);
	}

	@Override
	protected IModel<AbstractResource> getPhotoModel() {
		return new AbstractReadOnlyModel<AbstractResource>() {
			private static final long serialVersionUID = 1L;

			@Override
			public AbstractResource getObject() {
				byte[] jpegPhoto = null;
				O object = getModel().getObject();
				if (object instanceof FocusType) {
					jpegPhoto = ((FocusType) object).getJpegPhoto();
				}
				if (jpegPhoto == null) {
					return null;
				} else {
					return new ByteArrayResource("image/jpeg", jpegPhoto);
				}
			}
		};
	}

	protected boolean isActivationVisible() {
		return true;
	}

	public static void addSummaryPanel(MarkupContainer parentComponent, PrismObject<FocusType> focus, ObjectWrapper<FocusType> focusWrapper, String id, ModelServiceLocator serviceLocator) {
		if (focus.getCompileTimeClass().equals(UserType.class)) {
			parentComponent.add(new UserSummaryPanel(id,
                    new Model<ObjectWrapper<UserType>>((ObjectWrapper) focusWrapper), serviceLocator));
        } else if (focus.getCompileTimeClass().equals(RoleType.class)) {
        	parentComponent.add(new RoleSummaryPanel(id,
                    new Model<ObjectWrapper<RoleType>>((ObjectWrapper) focusWrapper), serviceLocator));
        } else if (focus.getCompileTimeClass().equals(OrgType.class)) {
        	parentComponent.add(new OrgSummaryPanel(id,
                    new Model<ObjectWrapper<OrgType>>((ObjectWrapper) focusWrapper), serviceLocator));
        } else if (focus.getCompileTimeClass().equals(ServiceType.class)) {
        	parentComponent.add(new ServiceSummaryPanel(id,
                    new Model<ObjectWrapper<ServiceType>>((ObjectWrapper) focusWrapper), serviceLocator));
        }
	}
}
