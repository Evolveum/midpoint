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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 */
public class SimpleRoleSelector<F extends FocusType, R extends AbstractRoleType> extends BasePanel<List<AssignmentEditorDto>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SimpleRoleSelector.class);

    private static final String ID_LIST = "list";
    private static final String ID_ITEM = "item";
    private static final String ID_BUTTON_RESET = "buttonReset";

    List<PrismObject<R>> availableRoles;

    public SimpleRoleSelector(String id, IModel<List<AssignmentEditorDto>> assignmentModel, List<PrismObject<R>> availableRoles) {
        super(id, assignmentModel);
        this.availableRoles = availableRoles;
        initLayout();
    }

    public List<AssignmentType> getAssignmentTypeList() {
        return null;
    }

    public String getExcludeOid() {
        return null;
    }

    protected IModel<List<AssignmentEditorDto>> getAssignmentModel() {
        return getModel();
    }

    private void initLayout() {
        setOutputMarkupId(true);
        ListView<PrismObject<R>> list = new ListView<PrismObject<R>>(ID_LIST, availableRoles) {
            @Override
            protected void populateItem(ListItem<PrismObject<R>> item) {
                item.add(createRoleLink(ID_ITEM, item.getModel()));
            }
        };
        list.setOutputMarkupId(true);
        add(list);

        AjaxLink<String> buttonReset = new AjaxLink<String>(ID_BUTTON_RESET) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                reset();
                target.add(SimpleRoleSelector.this);
            }
        };
        buttonReset.setBody(createStringResource("SimpleRoleSelector.reset"));
        add(buttonReset);
    }


    private Component createRoleLink(String id, IModel<PrismObject<R>> model) {
        AjaxLink<PrismObject<R>> button = new AjaxLink<PrismObject<R>>(id, model) {

            @Override
            public IModel<?> getBody() {
                return new Model<String>(getModel().getObject().asObjectable().getName().getOrig());
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                LOGGER.trace("{} CLICK: {}", this, getModel().getObject());
                toggleRole(getModel().getObject());
                target.add(this);
            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                PrismObject<R> role = getModel().getObject();
                if (isSelected(role)) {
                    tag.put("class", "list-group-item active");
                } else {
                    tag.put("class", "list-group-item");
                }
                String description = role.asObjectable().getDescription();
                if (description != null) {
                    tag.put("title", description);
                }
            }
        };
        button.setOutputMarkupId(true);
        return button;
    }


    private boolean isSelected(PrismObject<R> role) {
        for (AssignmentEditorDto dto: getAssignmentModel().getObject()) {
            if (willProcessAssignment(dto)) {
                if (dto.getTargetRef() != null && role.getOid().equals(dto.getTargetRef().getOid())) {
                    if (dto.getStatus() != UserDtoStatus.DELETE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void toggleRole(PrismObject<R> role) {
        Iterator<AssignmentEditorDto> iterator = getAssignmentModel().getObject().iterator();
        while (iterator.hasNext()) {
            AssignmentEditorDto dto = iterator.next();
            if (willProcessAssignment(dto)) {
                if (dto.getTargetRef() != null && role.getOid().equals(dto.getTargetRef().getOid())) {
                    if (dto.getStatus() == UserDtoStatus.ADD) {
                        iterator.remove();
                    } else {
                        dto.setStatus(UserDtoStatus.DELETE);
                    }
                    return;
                }
            }
        }

        AssignmentEditorDto dto = createAddAssignmentDto(role, getPageBase());
        getAssignmentModel().getObject().add(dto);
    }

    protected AssignmentEditorDto createAddAssignmentDto(PrismObject<R> role, PageBase pageBase) {
        AssignmentEditorDto dto = AssignmentEditorDto.createDtoAddFromSelectedObject(role.asObjectable(), getPageBase());
        dto.setMinimized(true);
        return dto;
    }

    private void reset() {
        Iterator<AssignmentEditorDto> iterator = getAssignmentModel().getObject().iterator();
        while (iterator.hasNext()) {
            AssignmentEditorDto dto = iterator.next();
            if (isManagedRole(dto) && willProcessAssignment(dto)) {
                if (dto.getStatus() == UserDtoStatus.ADD) {
                    iterator.remove();
                } else if (dto.getStatus() == UserDtoStatus.DELETE) {
                    dto.setStatus(UserDtoStatus.MODIFY);
                }
            }
        }
    }

    protected boolean willProcessAssignment(AssignmentEditorDto dto) {
        return true;
    }

    protected boolean isManagedRole(AssignmentEditorDto dto) {
        if (dto.getTargetRef() == null || dto.getTargetRef().getOid() == null) {
            return false;
        }
        for (PrismObject<R> availableRole: availableRoles) {
            if (availableRole.getOid().equals(dto.getTargetRef().getOid())) {
                return true;
            }
        }
        return false;
    }

}
