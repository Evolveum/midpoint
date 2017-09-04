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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 */
public class SimpleParametricRoleSelector<F extends FocusType, R extends AbstractRoleType> extends SimpleRoleSelector<F,R> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SimpleParametricRoleSelector.class);

    private static final String ID_LABEL_ROLE = "labelRole";
    private static final String ID_LABEL_PARAM = "labelParam";
    private static final String ID_LIST_PARAM = "listParam";
    private static final String ID_ITEM_PARAM = "itemParam";
    private static final String ID_ADD_INPUT = "addInput";
    private static final String ID_ADD_LINK = "addLink";
    private static final String ID_DELETE_LINK = "deleteLink";

    private String labelParam = null;
    private String labelRole = null;
    private ItemPath parameterPath;
    private ListView<String> paramList;
    final private IModel<List<String>> paramListModel;
    private String selectedParam = null;

    public SimpleParametricRoleSelector(String id, IModel<List<AssignmentDto>> assignmentModel, List<PrismObject<R>> availableRoles, ItemPath parameterPath) {
        super(id, assignmentModel, availableRoles);
        this.parameterPath = parameterPath;
        paramListModel = initParamListModel(assignmentModel);
        initLayout();
    }

    public String getLabelParam() {
        return labelParam;
    }

    public void setLabelParam(String labelParam) {
        this.labelParam = labelParam;
    }

    public String getLabelRole() {
        return labelRole;
    }

    public void setLabelRole(String labelRole) {
        this.labelRole = labelRole;
    }

    private IModel<List<String>> initParamListModel(final IModel<List<AssignmentDto>> assignmentModel) {
        return new IModel<List<String>>() {

            private List<String> list = null;

            @Override
            public void detach() {
            }

            @Override
            public List<String> getObject() {
                if (list == null) {
                    list = initParamList(assignmentModel.getObject());
                }
                return list;
            }

            @Override
            public void setObject(List<String> list) {
                this.list = list;
            }
        };
    }

    private List<String> initParamList(List<AssignmentDto> assignmentDtos) {
        List<String> params = new ArrayList<>();
        for (AssignmentDto assignmentDto: assignmentDtos) {
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

    private String getParamValue(AssignmentDto assignmentDto) {
        PrismContainerValue newValue = assignmentDto.getAssignment().asPrismContainerValue();

        if (newValue != null) {
            PrismProperty<String> paramProp =  newValue.findProperty(parameterPath);
            if (paramProp != null) {
                return paramProp.getRealValue();
            }
        }
//        PrismContainerValue oldValue = assignmentDto.getOldValue();
//        if (oldValue != null) {
//            PrismProperty<String> paramProp =  oldValue.findProperty(parameterPath);
//            if (paramProp != null) {
//                return paramProp.getRealValue();
//            }
//        }
        return null;
    }

    private void initLayout() {

        IModel<String> labelParamModel = new IModel<String>() {
            @Override
            public void detach() {
            }

            @Override
            public String getObject() {
                return getLabelParam();
            }

            @Override
            public void setObject(String object) {
            }
        };
        add(new Label(ID_LABEL_PARAM, labelParamModel) {
            @Override
            protected void onConfigure() {
                setVisible(getLabelParam() != null);
                super.onConfigure();
            }
        });

        IModel<String> labelRoleModel = new IModel<String>() {
            @Override
            public void detach() {
            }

            @Override
            public String getObject() {
                return getLabelRole();
            }

            @Override
            public void setObject(String object) {
            }
        };
        add(new Label(ID_LABEL_ROLE, labelRoleModel) {
            @Override
            protected void onConfigure() {
                setVisible(getLabelRole() != null);
                super.onConfigure();
            }
        });

        paramList = new ListView<String>(ID_LIST_PARAM, paramListModel) {
            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(createParamLink(ID_ITEM_PARAM, item.getModel()));
            }

        };
        paramList.setOutputMarkupId(true);
        add(paramList);

        final Model<String> addInputModel = new Model<String>();
        TextField<String> addInput = new TextField<>(ID_ADD_INPUT, addInputModel);
        addInput.setOutputMarkupId(true);
        addInput.add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // nothing to do, Ajax behavior is there only to get data to model
            }
        });
        add(addInput);

        AjaxLink<String> addLink = new AjaxLink<String>(ID_ADD_LINK) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String newParam = addInputModel.getObject();
                LOGGER.debug("ADD cliked, input field value: {}", newParam);
                if (!StringUtils.isBlank(newParam)) {
                    addParam(newParam);

                }
                addInputModel.setObject(null);
                target.add(SimpleParametricRoleSelector.this);
            }
        };
        add(addLink);

        AjaxLink<String> deleteLink = new AjaxLink<String>(ID_DELETE_LINK) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                LOGGER.debug("DELETE cliked, selected param: {}", selectedParam);
                deleteParam(selectedParam);
                target.add(SimpleParametricRoleSelector.this);
            }
        };
        add(deleteLink);
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

    private void addParam(String newParam) {
        List<String> params = paramListModel.getObject();
        if (!params.contains(newParam)) {
            params.add(newParam);
        }
    }

    private void deleteParam(String paramToDelete) {
        paramListModel.getObject().remove(paramToDelete);
        // make sure that all the assignments with the parameter parameter are also removed from assignement model
        Iterator<AssignmentDto> iterator = getAssignmentModel().getObject().iterator();
        while (iterator.hasNext()) {
        	AssignmentDto dto = iterator.next();
            if (isManagedRole(dto) && paramToDelete.equals(getParamValue(dto))) {
                if (dto.getStatus() == UserDtoStatus.ADD) {
                    iterator.remove();
                } else {
                    dto.setStatus(UserDtoStatus.DELETE);
                }
            }
        }
    }

    @Override
    protected AssignmentDto createAddAssignmentDto(PrismObject<R> role, PageBase pageBase) {
    	AssignmentDto dto = super.createAddAssignmentDto(role, pageBase);
        PrismContainerValue<AssignmentType> newValue;
        try {
            newValue = dto.getAssignment().asPrismContainerValue();
            PrismProperty<String> prop = newValue.findOrCreateProperty(parameterPath);
            prop.setRealValue(selectedParam);
        } catch (SchemaException e) {
            throw new SystemException(e.getMessage(), e);
        }

        return dto;
    }

    @Override
    protected boolean willProcessAssignment(AssignmentDto dto) {
        if (!super.willProcessAssignment(dto)) {
            return false;
        }
        if (selectedParam == null) {
            return false;
        }
        return selectedParam.equals(getParamValue(dto));
    }



}
