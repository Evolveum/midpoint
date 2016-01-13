/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 */
public class SimpleParametricRoleSelector<F extends FocusType, R extends AbstractRoleType> extends SimpleRoleSelector<F,R> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(SimpleParametricRoleSelector.class);
	
	private static final String ID_LIST_PARAM = "listParam";
	private static final String ID_ITEM_PARAM = "itemParam";
	private static final String ID_BUTTON_ADD = "buttonAdd";
	private static final String ID_BUTTON_DELETE = "buttonDelete";
	
	private ItemPath parameterPath;
	private LoadableModel<List<String>> paramListModel;
	private String selectedParam = null;

	public SimpleParametricRoleSelector(String id, IModel<List<AssignmentEditorDto>> assignmentModel, List<PrismObject<R>> availableRoles, ItemPath parameterPath) {
		super(id, assignmentModel, availableRoles);
		this.parameterPath = parameterPath;
		paramListModel = initParamListModel(assignmentModel);
		initLayout();
	}

	private LoadableModel<List<String>> initParamListModel(final IModel<List<AssignmentEditorDto>> assignmentModel) {
		LoadableModel<List<String>> paramListModel = new LoadableModel<List<String>>() {
			@Override
			protected List<String> load() {
				return initParamList(assignmentModel.getObject());
			}
		};
		return paramListModel;
	}
	
	private List<String> initParamList(List<AssignmentEditorDto> assignmentDtos) {
		List<String> params = new ArrayList<>();
		for (AssignmentEditorDto assignmentDto: assignmentDtos) {
			String paramVal = getParamValue(assignmentDto);
			if (paramVal != null) {
				if (!params.contains(paramVal)) {
					params.add(paramVal);
				}
			}
		}
		Collections.sort(params);
		return params;
	}

	private String getParamValue(AssignmentEditorDto assignmentDto) {
		PrismContainerValue newValue;
		try {
			newValue = assignmentDto.getNewValue(getPageBase().getPrismContext());
		} catch (SchemaException e) {
			throw new SystemException(e.getMessage(),e);
		}
		if (newValue != null) {
			PrismProperty<String> paramProp =  newValue.findProperty(parameterPath);
			if (paramProp != null) {
				return paramProp.getRealValue();
			}
		}
		PrismContainerValue oldValue = assignmentDto.getOldValue();
		if (oldValue != null) {
			PrismProperty<String> paramProp =  oldValue.findProperty(parameterPath);
			if (paramProp != null) {
				return paramProp.getRealValue();
			}
		}
		return null;
	}
	 
	private void initLayout() {
		
		ListView<String> list = new ListView<String>(ID_LIST_PARAM, paramListModel) {
			@Override
			protected void populateItem(ListItem<String> item) {
				item.add(createParamLink(ID_ITEM_PARAM, item.getModel()));
			}
		};
		list.setOutputMarkupId(true);
		add(list);

//		AjaxLink<String> buttonReset = new AjaxLink<String>(ID_BUTTON_RESET) {
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				reset();
//				target.add(SimpleParametricRoleSelector.this);
//			}
//		};
//		buttonReset.setBody(createStringResource("SimpleRoleSelector.reset"));
//		add(buttonReset);
	}


	private Component createParamLink(String id, IModel<String> itemModel) {
		AjaxLink<String> button = new AjaxLink<String>(id, itemModel) {
			
			@Override
			public IModel<?> getBody() {
				return new Model<String>(getModel().getObject());
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				LOGGER.trace("{} CLICK param: {}", this, getModel().getObject());
				toggleParam(getModel().getObject());
				target.add(SimpleParametricRoleSelector.this);
			}
			
			@Override
		    protected void onComponentTag(ComponentTag tag) {
		        super.onComponentTag(tag);
		        String param = getModel().getObject();
		        if (param.equals(selectedParam)) {
		        	tag.put("class", "list-group-item active");
		        } else {
		        	tag.put("class", "list-group-item");
		        }
		    }
		};
		button.setOutputMarkupId(true);
		return button;
	}
		
	private void toggleParam(String param) {
		selectedParam = param;
	}

	@Override
	protected AssignmentEditorDto createAddAssignmentDto(PrismObject<R> role, PageBase pageBase) {
		AssignmentEditorDto dto = super.createAddAssignmentDto(role, pageBase);
		PrismContainerValue<AssignmentType> newValue;
		try {
			newValue = dto.getNewValue(getPageBase().getPrismContext());
			PrismProperty<String> prop = newValue.findOrCreateProperty(parameterPath);
			prop.setRealValue(selectedParam);
		} catch (SchemaException e) {
			throw new SystemException(e.getMessage(), e);
		}
		
		return dto;
	}

	@Override
	protected boolean willProcessAssignment(AssignmentEditorDto dto) {
		if (selectedParam == null) {
			return false;
		}
		return selectedParam.equals(getParamValue(dto));
	}
	
	
		
}
