package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.dto.AssignmentConflictDto;
import com.evolveum.midpoint.web.page.self.dto.ConflictDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar.
 */
public class AssignmentConflictPanel extends BasePanel<ConflictDto> {
    private static final String ID_STATUS_ICON = "statusIcon";
    private static final String ID_EXISTING_ASSIGNMENT = "existingAssignment";
    private static final String ID_ADDED_ASSIGNMENT = "addedAssignment";
    private static final String ID_UNSELECT_BUTTON = "unselectButton";
    private static final String ID_REMOVE_BUTTON = "removeButton";
    private static final String ID_PANEL_CONTAINER = "panelContainer";

    private static final String STATUS_FIXED = GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_SUCCESS_COLORED + " fa-lg";
    private static final String STATUS_WARNING = GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_WARNING_COLORED + " fa-lg";
    private static final String STATUS_ERROR = GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_FATAL_ERROR_COLORED + " fa-lg";

    public AssignmentConflictPanel(String id, IModel<ConflictDto> model) {
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
                return getModelObject() != null ?
                        (getModelObject().isResolved() ? STATUS_FIXED :
                                (getModelObject().isWarning() ? STATUS_WARNING : STATUS_ERROR)) : STATUS_ERROR;
            }
        }));
        container.add(statusIconLabel);

        Label existingAssignment = new Label(ID_EXISTING_ASSIGNMENT,
                new AbstractReadOnlyModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        if (getModelObject() != null) {
                            String name = getModelObject().getAssignment1().getAssignmentTargetObject().asObjectable().getName() != null ?
                                    getModelObject().getAssignment1().getAssignmentTargetObject().asObjectable().getName().getOrig() :
                                    getModelObject().getAssignment1().getAssignmentTargetObject().getOid();
                            return name + " " +
                                    getMessageLabel(getModelObject().getAssignment1().isOldAssignment());
                        }
                        return "";
                    }
                });
        existingAssignment.add(new AttributeAppender("style", new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getModelObject() != null && getModelObject().getAssignment1().isResolved() ? "text-decoration: line-through;" : "";
            }
        }));
        container.add(existingAssignment);

        Label addedAssignment = new Label(ID_ADDED_ASSIGNMENT,
                new AbstractReadOnlyModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        if (getModelObject() != null) {
                            String name = getModelObject().getAssignment2().getAssignmentTargetObject().asObjectable().getName() != null ?
                                    getModelObject().getAssignment2().getAssignmentTargetObject().asObjectable().getName().getOrig() :
                                    getModelObject().getAssignment2().getAssignmentTargetObject().getOid();
                            return name + " " + getMessageLabel(getModelObject().getAssignment2().isOldAssignment());

                        }
                        return "";
                    }
                });
        addedAssignment.add(new AttributeAppender("style", new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getModelObject() != null && getModelObject().getAssignment2().isResolved() ? "text-decoration: line-through;" : "";
            }
        }));
        container.add(addedAssignment);

        AbstractReadOnlyModel<String> removeButtonTitleModel = new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getModelObject().getAssignment1().isResolved() ?
                        createStringResource("AssignmentConflictPanel.undoAction").getString() :
                        createStringResource("AssignmentConflictPanel.removeButton").getString();
            }
        };
        AjaxSubmitButton removeButton = new AjaxSubmitButton(ID_REMOVE_BUTTON, removeButtonTitleModel) {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                AssignmentConflictPanel.this.removeAssignmentPerformed(getModelObject().getAssignment1(), target);
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
                return !getModelObject().getAssignment2().isResolved();
            }
        });
        container.add(removeButton);

        AbstractReadOnlyModel<String> unselectButtonTitleModel = new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getModelObject().getAssignment2().isResolved() ?
                        createStringResource("AssignmentConflictPanel.undoAction").getString() :
                        createStringResource("AssignmentConflictPanel.removeButton").getString();
            }
        };
        AjaxSubmitButton unselectButton = new AjaxSubmitButton(ID_UNSELECT_BUTTON,
                unselectButtonTitleModel) {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                AssignmentConflictPanel.this.removeAssignmentPerformed(getModelObject().getAssignment2(), target);
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
                return !getModelObject().getAssignment1().isResolved();
            }
        });
        container.add(unselectButton);
    }

    private void removeAssignmentPerformed(AssignmentConflictDto dto, AjaxRequestTarget target) {
        dto.setResolved(!dto.isResolved());
        target.add(get(ID_PANEL_CONTAINER));
    }

    private String getMessageLabel(boolean isOldAssignment){
        return isOldAssignment ? createStringResource("AssignmentConflictPanel.existingAssignmentLabelMessage").getString() :
                createStringResource("AssignmentConflictPanel.addedAssignmentLabelMessage").getString();
    }
}
