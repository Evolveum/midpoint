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

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.visualizer.SceneItemValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * TODO make this parametric (along with SceneItemValue)
 * @author mederly
 */
public class SceneItemValuePanel extends BasePanel<SceneItemValue> {

	private static final String ID_ICON = "icon";
	private static final String ID_LABEL = "label";
	private static final String ID_LINK = "link";
	private static final String ID_ADDITIONAL_TEXT = "additionalText";

	public SceneItemValuePanel(String id, IModel<SceneItemValue> model) {
		super(id, model);
		initLayout();
	}

	private void initLayout() {

		final VisibleEnableBehaviour visibleIfReference = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				SceneItemValue object = getModelObject();
				return hasValidReferenceValue(object);
			}
		};
		final VisibleEnableBehaviour visibleIfNotReference = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				SceneItemValue object = getModelObject();
				return !hasValidReferenceValue(object);
			}
		};

		final ImagePanel icon = new ImagePanel(ID_ICON, new IconModel(), new TitleModel());
		icon.add(visibleIfReference);
		add(icon);

		final Label label = new Label(ID_LABEL, new LabelModel());
		label.add(visibleIfNotReference);
		add(label);

		final LinkPanel link = new LinkPanel(ID_LINK, new LabelModel()) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				if (!(getModelObject().getSourceValue() instanceof PrismReferenceValue)) {
					return;
				}
				PrismReferenceValue refValue = (PrismReferenceValue) getModelObject().getSourceValue();
				ObjectReferenceType ort = new ObjectReferenceType();
				ort.setupReferenceValue(refValue);
				WebComponentUtil.dispatchToObjectDetailsPage(ort, getPageBase(), false);

			}
		};
		link.add(visibleIfReference);
		add(link);

		final Label additionalText = new Label(ID_ADDITIONAL_TEXT, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return getModelObject() != null ? getModelObject().getAdditionalText() : null;
			}
		});
		add(additionalText);
	}

	private boolean hasValidReferenceValue(SceneItemValue object) {
		return object != null && object.getSourceValue() instanceof PrismReferenceValue
				&& ((PrismReferenceValue) object.getSourceValue()).getTargetType() != null;
	}

	private ObjectTypeGuiDescriptor getObjectTypeDescriptor() {
		SceneItemValue value = getModelObject();
		if (value.getSourceValue() instanceof PrismReferenceValue) {
			QName targetType = ((PrismReferenceValue) value.getSourceValue()).getTargetType();
			return ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(targetType));
		} else {
			return null;
		}
	}

	private class IconModel extends AbstractReadOnlyModel<String> {
		@Override
		public String getObject() {
			ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor();
			return guiDescriptor != null ? guiDescriptor.getBlackIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
		}
	}

	private class TitleModel extends AbstractReadOnlyModel<String> {
		@Override
		public String getObject() {
			ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor();
			return guiDescriptor != null ? createStringResource(guiDescriptor.getLocalizationKey()).getObject() : null;
		}
	}

	private class LabelModel extends AbstractReadOnlyModel<String> {
		@Override
		public String getObject() {
			return getModelObject() != null ? getModelObject().getText() : null;
		}
	}
}
