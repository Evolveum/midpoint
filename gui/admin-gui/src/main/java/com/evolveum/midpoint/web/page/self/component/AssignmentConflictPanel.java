package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.dto.AssignmentConflictDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar.
 */
public class AssignmentConflictPanel extends BasePanel<AssignmentConflictDto> {
    private static final String ID_STATUS_ICON = "statusIcon";
    private static final String ID_EXISTING_ASSIGNMENT = "existingAssignment";
    private static final String ID_ADDED_ASSIGNMENT = "addedAssignment";
    private static final String ID_UNSELECT_BUTTON = "unselectButton";
    private static final String ID_REMOVE_BUTTON = "removeButton";
    private static final String ID_PANEL_CONTAINER = "panelContainer";

    private static final String STATUS_FIXED = GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_SUCCESS_COLORED + " fa-lg";
    private static final String STATUS_ERROR = GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_FATAL_ERROR_COLORED + " fa-lg";

    public AssignmentConflictPanel(String id, IModel<AssignmentConflictDto> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        Form container = new com.evolveum.midpoint.web.component.form.Form<>(ID_PANEL_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        Label statusIconLabel = new Label(ID_STATUS_ICON);
        statusIconLabel.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getModelObject() != null ? (getModelObject().isSolved() ? STATUS_FIXED : STATUS_ERROR) : STATUS_ERROR;
            }
        }));
        container.add(statusIconLabel);

        Label existingAssignment = new Label(ID_EXISTING_ASSIGNMENT,
                new AbstractReadOnlyModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        if (getModelObject() != null) {
                            String name = getModelObject().getExistingAssignmentTargetObj().asObjectable().getName() != null ?
                                    getModelObject().getExistingAssignmentTargetObj().asObjectable().getName().getOrig() :
                                    getModelObject().getExistingAssignmentTargetObj().getOid();
                            return name + " " + createStringResource("AssignmentConflictPanel.existingAssignmentLabelMessage").getString();
                        }
                        return "";
                    }
                });
        existingAssignment.add(new AttributeAppender("style", new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getModelObject() != null && getModelObject().isRemovedOld() ? "text-decoration: line-through;" : "";
            }
        }));
        container.add(existingAssignment);

        Label addedAssignment = new Label(ID_ADDED_ASSIGNMENT,
                new AbstractReadOnlyModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        if (getModelObject() != null) {
                            String name = getModelObject().getAddedAssignmentTargetObj().asObjectable().getName() != null ?
                                    getModelObject().getAddedAssignmentTargetObj().asObjectable().getName().getOrig() :
                                    getModelObject().getAddedAssignmentTargetObj().getOid();
                            return name + " " + createStringResource("AssignmentConflictPanel.addedAssignmentLabelMessage").getString();
                        }
                        return "";
                    }
                });
        addedAssignment.add(new AttributeAppender("style", new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getModelObject() != null && getModelObject().isUnassignedNew() ? "text-decoration: line-through;" : "";
            }
        }));
        container.add(addedAssignment);

        AbstractReadOnlyModel<String> removeButtonTitleModel = new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getModelObject().isRemovedOld() ?
                        createStringResource("AssignmentConflictPanel.undoAction").getString() :
                        createStringResource("AssignmentConflictPanel.removeButton").getString();
            }
        };
        AjaxSubmitButton removeButton = new AjaxSubmitButton(ID_REMOVE_BUTTON, removeButtonTitleModel) {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                AssignmentConflictPanel.this.removeAssignmentPerformed(target);
            }
        };
        removeButton.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return getModelObject() != null;
            }

            @Override
            public boolean isEnabled() {
                return !getModelObject().isUnassignedNew();
            }
        });
        container.add(removeButton);

        AbstractReadOnlyModel<String> unselectButtonTitleModel = new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getModelObject().isUnassignedNew() ?
                        createStringResource("AssignmentConflictPanel.undoAction").getString() :
                        createStringResource("AssignmentConflictPanel.unselectButton").getString();
            }
        };
        AjaxSubmitButton unselectButton = new AjaxSubmitButton(ID_UNSELECT_BUTTON,
                unselectButtonTitleModel) {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                AssignmentConflictPanel.this.unselectAssignmentPerformed(target);
            }
        };
        unselectButton.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return getModelObject() != null;
            }

            @Override
            public boolean isEnabled() {
                return !getModelObject().isRemovedOld();
            }
        });
        container.add(unselectButton);
    }

    private void unselectAssignmentPerformed(AjaxRequestTarget target) {
        getModelObject().setUnassignedNew(!getModelObject().isUnassignedNew());
        target.add(get(ID_PANEL_CONTAINER));
    }

    private void removeAssignmentPerformed(AjaxRequestTarget target) {
        getModelObject().setRemovedOld(!getModelObject().isRemovedOld());
        target.add(get(ID_PANEL_CONTAINER));
    }

}
