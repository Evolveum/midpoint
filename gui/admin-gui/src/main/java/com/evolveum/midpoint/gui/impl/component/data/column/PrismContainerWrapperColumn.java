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
package com.evolveum.midpoint.gui.impl.component.data.column;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;

/**
 * @author katka
 *
 */
public class PrismContainerWrapperColumn<C extends Containerable> extends AbstractItemWrapperColumn<C, PrismContainerValueWrapper<C>>{

	private static final transient Trace LOGGER = TraceManager.getTrace(PrismContainerWrapperColumn.class);
	
	public PrismContainerWrapperColumn(IModel<PrismContainerWrapper<C>> rowModel, ItemPath itemName) {
		super(rowModel, itemName, ColumnType.STRING);
	}

	private static final long serialVersionUID = 1L;
	
	private static final String ID_HEADER = "header";

	@Override
	public IModel<?> getDataModel(IModel<PrismContainerValueWrapper<C>> rowModel) {
		return PrismContainerWrapperModel.fromContainerValueWrapper(rowModel, itemName);
	}

	@Override
	protected Component createHeader(String componentId, IModel<PrismContainerWrapper<C>> mainModel) {
		return new PrismContainerHeaderPanel<>(componentId, PrismContainerWrapperModel.fromContainerWrapper(mainModel, itemName));
	}

	@Override
	protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
		return new PrismContainerWrapperColumnPanel<C>(componentId, (IModel<PrismContainerWrapper<C>>) rowModel, getColumnType());
	}
	
}
