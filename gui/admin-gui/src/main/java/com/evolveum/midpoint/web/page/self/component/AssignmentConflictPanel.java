package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.context.EvaluatedConstruction;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.NoFocusNameSchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by honchar.
 */
public class AssignmentConflictPanel extends BasePanel {
    private static final String ID_STATUS_ICON = "statusIcon";
    private static final String ID_EXISTING_ASSIGNMENT = "existingAssignment";
    private static final String ID_CONFLICT_MESSAGE = "conflictMessage";
    private static final String ID_ADDED_ASSIGNMENT = "addedAssignment";
    private static final String ID_UNSELECT_BUTTON = "unselectButton";
    private static final String ID_REMOVE_BUTTON = "removeButton";


    private PageBase pageBase;
    PrismObject<UserType> user;

    public AssignmentConflictPanel(String id, PrismObject<UserType> user, PageBase pageBase){
        super(id);
        this.pageBase = pageBase;
        this.user = user;
        initLayout();
    }

    private void initLayout(){
//        getAssignmentConflicts();

    }


}
