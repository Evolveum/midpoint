/**
 * Copyright (c) 2015 Evolveum
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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.ContainerableFromPrismObjectModel;
import com.evolveum.midpoint.web.model.ReadOnlyPrismObjectFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.ReadOnlyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;

import java.util.Collection;

/**
 * @author semancik
 *
 */
public abstract class FocusSummaryPanel<O extends ObjectType> extends AbstractSummaryPanel<O> {
	private static final long serialVersionUID = 1L;
	
	protected static final String ICON_CLASS_ACTIVATION_ACTIVE = "fa fa-check";
	protected static final String ICON_CLASS_ACTIVATION_INACTIVE = "fa fa-times";

	private IModel<ObjectWrapper<O>> wrapperModel;
	
	public FocusSummaryPanel(String id, final IModel<ObjectWrapper<O>> model) {
		super(id, new ContainerableFromPrismObjectModel<O>(new ReadOnlyPrismObjectFromObjectWrapperModel<O>(model)));

		this.wrapperModel = model;
		initLayoutCommon();				// calls getParentOrgModel that depends on wrapperModel

		SummaryTag<O> tagActivation = new SummaryTag<O>(ID_FIRST_SUMMARY_TAG, model) {
			@Override
			protected void initialize(ObjectWrapper<O> wrapper) {
				ActivationType activation = null;
				O object = wrapper.getObject().asObjectable();
				if (object instanceof FocusType) {
					activation = ((FocusType)object).getActivation();
				}
				if (activation == null) {
					setIconCssClass(ICON_CLASS_ACTIVATION_ACTIVE);
					setLabel("Active");
					setColor("green");
				} else if (activation.getEffectiveStatus() == ActivationStatusType.ENABLED) {
					setIconCssClass(ICON_CLASS_ACTIVATION_ACTIVE);
					setLabel("Active");
					setColor("green");
				} else {
					setIconCssClass(ICON_CLASS_ACTIVATION_INACTIVE);
					setLabel("Inactive");
					setColor("red");
				}
			}
		};
		tagActivation.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return isActivationVisible();
			}
		});
		addTag(tagActivation);
	}

	protected IModel<String> getParentOrgModel() {
		return new ReadOnlyWrapperModel<String,O>(wrapperModel) {
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
	protected IModel<AbstractResource> getPhotoModel() {
		return new AbstractReadOnlyModel<AbstractResource>() {
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
}
