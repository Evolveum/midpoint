/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemDefinitionImpl;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.util.FormTypeUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DynamicFieldGroupPanel<O extends ObjectType> extends BasePanel<ObjectWrapper<O>> {

	private static final long serialVersionUID = 1L;

	private static final transient Trace LOGGER = TraceManager.getTrace(DynamicFieldGroupPanel.class);

	private static final String ID_PROPERTY = "property";
	private static final String ID_HEADER = "header";

	private List<AbstractFormItemType> formItems;

	public DynamicFieldGroupPanel(String id, String groupName, IModel<ObjectWrapper<O>> objectWrapper, List<AbstractFormItemType> formItems, Form<?> mainForm, PageBase parentPage) {
		super(id, objectWrapper);
		setParent(parentPage);
		this.formItems = formItems;
		initLayout(groupName, formItems, mainForm);
	}

	public DynamicFieldGroupPanel(String id, IModel<ObjectWrapper<O>> objectWrapper, @NotNull FormDefinitionType formDefinition, Form<?> mainForm, PageBase parentPage) {
		super(id, objectWrapper);
		setParent(parentPage);
		this.formItems = FormTypeUtil.getFormItems(formDefinition.getFormItems());
		initLayout(getGroupName(formDefinition), formItems, mainForm);
	}

	private String getGroupName(@NotNull FormDefinitionType formDefinition) {
		if (formDefinition.getDisplay() != null) {
			return formDefinition.getDisplay().getLabel();
		} else {
			return "Basic";
		}
	}

	private void initLayout(String groupName, List<AbstractFormItemType> formItems, Form<?> mainForm) {

		Label header = new Label(ID_HEADER, groupName);
		add(header);

		RepeatingView itemView = new RepeatingView(ID_PROPERTY);
		add(itemView);

		int i = 0;
		for (AbstractFormItemType formItem : formItems) {

			if (formItem instanceof FormFieldGroupType) {
				DynamicFieldGroupPanel<O> dynamicFieldGroupPanel = new DynamicFieldGroupPanel<>(itemView.newChildId(), formItem.getName(), getModel(), FormTypeUtil.getFormItems(((FormFieldGroupType) formItem).getFormItems()), mainForm, getPageBase());
				dynamicFieldGroupPanel.setOutputMarkupId(true);
				itemView.add(dynamicFieldGroupPanel);
				continue;
			}

			ItemWrapper<?,?,?> itemWrapper = findAndTailorItemWrapper(formItem, getObjectWrapper());

			if (itemWrapper instanceof ContainerWrapper) {
				//noinspection unchecked
				ContainerWrapper<Containerable> containerWrapper = (ContainerWrapper<Containerable>) itemWrapper;
				PrismContainerPanel<?> containerPanel = new PrismContainerPanel<>(itemView.newChildId(),
						Model.of(containerWrapper), true, mainForm, w -> ItemVisibility.AUTO, getPageBase());
				containerPanel.setOutputMarkupId(true);
				itemView.add(containerPanel);
			} else {
				PrismPropertyPanel<?> propertyPanel = new PrismPropertyPanel<>(itemView.newChildId(),
						Model.of(itemWrapper), mainForm, null, getPageBase());
				propertyPanel.setOutputMarkupId(true);
				propertyPanel.add(AttributeModifier.append("class", ((i % 2) == 0) ? "" : "stripe"));
				itemView.add(propertyPanel);
			}
			i++;
		}
	}

	private RepeatingView getRepeatingPropertyView() {
		return (RepeatingView) get(ID_PROPERTY);
	}

	@NotNull
	private ItemWrapper<?,?,?> findAndTailorItemWrapper(AbstractFormItemType formField, ObjectWrapper<O> objectWrapper) {
		ItemWrapper<?, ?, ?> itemWrapper = findItemWrapper(formField, objectWrapper);
		applyFormDefinition(itemWrapper, formField);
		return itemWrapper;
	}

	@NotNull
	private ItemWrapper<?, ?, ?> findItemWrapper(AbstractFormItemType formField, ObjectWrapper<O> objectWrapper) {
		ItemPath path = GuiImplUtil.getItemPath(formField);
		if (path == null) {
			getSession().error("Bad form item definition. It has to contain reference to the real attribute");
			LOGGER.error("Bad form item definition. It has to contain reference to the real attribute");
			throw new RestartResponseException(getPageBase());
		}

		ItemWrapper<?,?,?> itemWrapper;
		ItemDefinition<?> itemDef = objectWrapper.getObject().getDefinition().findItemDefinition(path);
		if (itemDef instanceof PrismContainerDefinition) {
			itemWrapper = objectWrapper.findContainerWrapper(path);
		} else {
			itemWrapper = objectWrapper.findPropertyWrapper(path);
		}
		if (itemWrapper == null) {
			getSession().error("Bad form item definition. No attribute with path: " + path + " was found");
			LOGGER.error("Bad form item definition. No attribute with path: " + path + " was found");
			throw new RestartResponseException(getPageBase());
		}
		return itemWrapper;
	}

	private void applyFormDefinition(ItemWrapper itemWrapper, AbstractFormItemType formField) {

		FormItemDisplayType displayType = formField.getDisplay();

		if (displayType == null) {
			return;
		}

		ItemDefinitionImpl itemDef = (ItemDefinitionImpl) itemWrapper.getItemDefinition();
		if (StringUtils.isNotEmpty(displayType.getLabel())) {
			itemDef.setDisplayName(displayType.getLabel());
		}
		if (StringUtils.isNotEmpty(displayType.getHelp())) {
			itemDef.setHelp(displayType.getHelp());
		}
		if (StringUtils.isNotEmpty(displayType.getMaxOccurs())) {
			itemDef.setMaxOccurs(XsdTypeMapper.multiplicityToInteger(displayType.getMaxOccurs()));
		}
		if (StringUtils.isNotEmpty(displayType.getMinOccurs())) {
			itemDef.setMinOccurs(XsdTypeMapper.multiplicityToInteger(displayType.getMinOccurs()));
		}
	}

	public ObjectWrapper<O> getObjectWrapper() {
		return getModelObject();
	}

	public List<AbstractFormItemType> getFormItems() {
		return formItems;
	}

	/**
	 * Checks embedded properties if they are the minOccurs check.
	 * Experimental implementation. Please do not rely on it too much.
	 */
	public boolean checkRequiredFields(PageBase pageBase) {
		Holder<Boolean> rvHolder = new Holder<>(true);
		getRepeatingPropertyView().visitChildren((component, iVisit) -> {
			if (component instanceof PrismPropertyPanel) {
				IModel<?> model = component.getDefaultModel();
				if (model != null && model.getObject() instanceof ItemWrapper) {
					ItemWrapper<?, ?, ?> itemWrapper = (ItemWrapper<?, ?, ?>) model.getObject();
					if (!itemWrapper.checkRequired(pageBase)) {
						rvHolder.setValue(false);
					}
				}
			}
		});
		return rvHolder.getValue();
	}
}
