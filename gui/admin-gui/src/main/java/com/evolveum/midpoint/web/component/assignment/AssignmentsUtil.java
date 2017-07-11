package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.TypedAssignablePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * Created by honchar.
 */
public class AssignmentsUtil {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentsUtil.class);

    public static IModel<String> createActivationTitleModel(IModel<AssignmentEditorDto> model, String defaultTitle, BasePanel basePanel) {
        return new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                AssignmentEditorDto dto = model.getObject();
                ActivationType activation = dto.getActivation();
                if (activation == null) {
                    return defaultTitle;
                }

                ActivationStatusType status = activation.getAdministrativeStatus();
                String strEnabled = basePanel.createStringResource(status, "lower", "ActivationStatusType.null")
                        .getString();

                if (activation.getValidFrom() != null && activation.getValidTo() != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledFromTo", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()),
                            MiscUtil.asDate(activation.getValidTo()));
                } else if (activation.getValidFrom() != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledFrom", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()));
                } else if (activation.getValidTo() != null) {
                    return basePanel.getString("AssignmentEditorPanel.enabledTo", strEnabled,
                            MiscUtil.asDate(activation.getValidTo()));
                }

                return defaultTitle;
            }
        };
    }

    public static AssignmentEditorDto createAssignmentFromSelectedObjects(ObjectType object, RelationTypes relation, PageBase pageBase){
            try {

                if (object instanceof ResourceType) {
                    AssignmentEditorDto dto = addSelectedResourceAssignPerformed((ResourceType) object, pageBase);
                    return dto;
                }
                if (object instanceof UserType) {
                    AssignmentEditorDto dto = AssignmentEditorDto.createDtoAddFromSelectedObject(object,
                            SchemaConstants.ORG_DEPUTY, pageBase);
                    dto.getTargetRef().setRelation(relation.getRelation());
                    return dto;
                } else {
                    AssignmentEditorDto dto = AssignmentEditorDto.createDtoAddFromSelectedObject(object, pageBase);
                    dto.getTargetRef().setRelation(relation.getRelation());
                    return dto;
                }
            } catch (Exception e) {
                pageBase.error(pageBase.getString("AssignmentTablePanel.message.couldntAssignObject", object.getName(),
                        e.getMessage()));
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't assign object", e);
            }
            return null;

    }

    public static AssignmentEditorDto addSelectedResourceAssignPerformed(ResourceType resource, PageBase pageBase) {
        AssignmentType assignment = new AssignmentType();
        ConstructionType construction = new ConstructionType();
        assignment.setConstruction(construction);

        try {
            pageBase.getPrismContext().adopt(assignment, UserType.class,
                    new ItemPath(UserType.F_ASSIGNMENT));
        } catch (SchemaException e) {
            pageBase.error(pageBase.getString("Could not create assignment", resource.getName(), e.getMessage()));
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create assignment", e);
            return null;
        }

        construction.setResource(resource);

        AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, pageBase);

        dto.setMinimized(true);
        dto.setShowEmpty(true);
        return dto;
    }

}
