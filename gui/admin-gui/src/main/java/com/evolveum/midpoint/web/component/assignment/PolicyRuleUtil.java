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
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;

import javax.xml.namespace.QName;
import java.time.Duration;
import java.util.List;

/**
 * Created by honchar.
 */
public class PolicyRuleUtil {
    private static final long serialVersionUID = 1L;

    public static String getPolicyConstraintsAsString(PolicyConstraintsType policyConstraints, PageBase pageBase){
        if (policyConstraints == null){
            return "";
        }
        StringBuilder policyConstraintsString = new StringBuilder();
        if (policyConstraints.getExclusion() != null){
            for (ExclusionPolicyConstraintType exclusion : policyConstraints.getExclusion()) {
                policyConstraintsString.append(getExclusionAsString(exclusion, pageBase));
                if (policyConstraints.getExclusion().indexOf(exclusion) < policyConstraints.getExclusion().size() - 1){
                    policyConstraintsString.append("\n");
                }
            }
        }
        if (policyConstraints.getMinAssignees() != null){
            for (MultiplicityPolicyConstraintType multiplicity : policyConstraints.getMinAssignees()){
                policyConstraintsString.append(getMultiplicityPolicyConstraintTypeAsString(multiplicity, PolicyConstraintsType.F_MIN_ASSIGNEES.getLocalPart()));
                if (policyConstraints.getMinAssignees().indexOf(multiplicity) < policyConstraints.getMinAssignees().size() - 1){
                    policyConstraintsString.append("\n");
                }
            }
        }
        if (policyConstraints.getMaxAssignees() != null){
            for (MultiplicityPolicyConstraintType multiplicity : policyConstraints.getMaxAssignees()){
                policyConstraintsString.append(getMultiplicityPolicyConstraintTypeAsString(multiplicity, PolicyConstraintsType.F_MAX_ASSIGNEES.getLocalPart()));
                if (policyConstraints.getMinAssignees().indexOf(multiplicity) < policyConstraints.getMinAssignees().size() - 1){
                    policyConstraintsString.append("\n");
                }
            }
        }
        if (policyConstraints.getModification() != null){
            for (ModificationPolicyConstraintType modification : policyConstraints.getModification()){
                policyConstraintsString.append(getModificationAsString(modification));
                if (policyConstraints.getModification().indexOf(modification) < policyConstraints.getModification().size() - 1){
                    policyConstraintsString.append("\n");
                }
            }
        }
        if (policyConstraints.getAssignment() != null){
            for (AssignmentPolicyConstraintType assignment : policyConstraints.getAssignment()){
                policyConstraintsString.append(getAssignmentAsString(assignment));
                if (policyConstraints.getAssignment().indexOf(assignment) < policyConstraints.getAssignment().size() - 1){
                    policyConstraintsString.append("\n");
                }
            }
        }
        if (policyConstraints.getTimeValidity() != null){
            for (TimeValidityPolicyConstraintType timeValidity : policyConstraints.getTimeValidity()){
                policyConstraintsString.append(getTimeValidityAsString(timeValidity));
                if (policyConstraints.getTimeValidity().indexOf(timeValidity) < policyConstraints.getTimeValidity().size() - 1){
                    policyConstraintsString.append("\n");
                }
            }
        }
        if (policyConstraints.getSituation() != null){
            for (PolicySituationPolicyConstraintType situation : policyConstraints.getSituation()){
                policyConstraintsString.append(getSituationAsString(situation));
                if (policyConstraints.getSituation().indexOf(situation) < policyConstraints.getSituation().size() - 1){
                    policyConstraintsString.append("\n");
                }
            }
        }

        return policyConstraintsString.toString();
    }

    public static String getSituationAsString(PolicySituationPolicyConstraintType situation){
        if (situation == null){
            return "";
        }
        StringBuilder sb = new StringBuilder(PolicyConstraintsType.F_SITUATION.getLocalPart() + ":");
        if (situation.getSituation() != null){
            for (String situationValue : situation.getSituation()) {
                sb.append(" " + situation.getSituation());
                if (situation.getSituation().indexOf(situationValue) < situation.getSituation().size() - 1){
                    sb.append(",");
                }
            }
        }
        return sb.toString();
    }
    public static String getTimeValidityAsString(TimeValidityPolicyConstraintType timeValidity){
        if (timeValidity == null){
            return null;
        }
        StringBuilder sb = new StringBuilder(PolicyConstraintsType.F_TIME_VALIDITY.getLocalPart());
        if (timeValidity.getItem() != null){
            sb.append(" " + TimeValidityPolicyConstraintType.F_ITEM.getLocalPart() + ": " + timeValidity.getItem().toString() + ";");
        }
        if (timeValidity.isAssignment() != null){
            sb.append(" " + TimeValidityPolicyConstraintType.F_ASSIGNMENT.getLocalPart() + ": " + Boolean.toString(timeValidity.isAssignment()) + ";");
        }
        if (timeValidity.getActivateOn() != null){
            sb.append(" " + TimeValidityPolicyConstraintType.F_ACTIVATE_ON.getLocalPart() + ": " + timeValidity.getActivateOn().toString() + ";");
        }
        if (timeValidity.getDeactivateOn() != null){
            sb.append(" " + TimeValidityPolicyConstraintType.F_DEACTIVATE_ON.getLocalPart() + ": " + timeValidity.getDeactivateOn().toString() + ";");
        }
        return sb.toString();
    }

    public static String getAssignmentAsString(AssignmentPolicyConstraintType assignment){
        if (assignment == null){
            return "";
        }
        StringBuilder sb = new StringBuilder(PolicyConstraintsType.F_ASSIGNMENT.getLocalPart());
        if (assignment.getOperation() != null){
            sb.append(" " + AssignmentPolicyConstraintType.F_OPERATION.getLocalPart() + ":");
            for (ModificationTypeType type : assignment.getOperation()){
                sb.append(" " + type.value());
                if (assignment.getOperation().indexOf(type) < assignment.getOperation().size() - 1){
                    sb.append(", ");
                }
            }
        }
        sb.append(" " + getRelationsListAsString(assignment.getRelation()));
        return sb.toString();
    }

    public static String getModificationAsString(ModificationPolicyConstraintType modification){
        if (modification == null){
            return "";
        }
        StringBuilder sb = new StringBuilder(PolicyConstraintsType.F_MODIFICATION.getLocalPart());
        if (modification.getOperation() != null && modification.getOperation().size() > 0){
            sb.append(" " + ModificationPolicyConstraintType.F_OPERATION.getLocalPart() + ":");
            for (ChangeTypeType type : modification.getOperation()){
                sb.append(" " + type.value());
                if (modification.getOperation().indexOf(type) < modification.getOperation().size() - 1){
                    sb.append(", ");
                }
            }

        }
        if (modification.getItem() != null && modification.getItem().size() > 0){
            sb.append(" " + ModificationPolicyConstraintType.F_ITEM.getLocalPart() + ":");
            for (ItemPathType path : modification.getItem()){
                sb.append(" " + path.toString());
                if (modification.getItem().indexOf(path) < modification.getItem().size() - 1){
                    sb.append(", ");
                }
            }

        }
        return sb.toString();
    }

    public static String getMultiplicityPolicyConstraintTypeAsString(MultiplicityPolicyConstraintType multiplicity, String name){
        if (multiplicity == null){
            return "";
        }
        StringBuilder sb = new StringBuilder(name);
        if (multiplicity.getMultiplicity() != null){
            sb.append(" " + multiplicity.getMultiplicity());
        }
        sb.append(" " +  getRelationsListAsString(multiplicity.getRelation()));
        return sb.toString();
    }

    public static String getExclusionAsString(ExclusionPolicyConstraintType exclusion, PageBase pageBase){
        if (exclusion == null){
            return "";
        }
        StringBuilder sb = new StringBuilder(PolicyConstraintsType.F_EXCLUSION.getLocalPart());
        if (exclusion.getTargetRef() != null){
            sb.append(" ").append(WebModelServiceUtils.resolveReferenceName(exclusion.getTargetRef(), pageBase));
        }
        if (exclusion.getPolicy() != null){
            sb.append(" ").append(exclusion.getPolicy().value());
        }
        return sb.toString();
    }

    public static String getRelationsListAsString(List<QName> relationsList){
        if (relationsList == null || relationsList.size() == 0){
            return "";
        }
        StringBuilder sb = new StringBuilder(MultiplicityPolicyConstraintType.F_RELATION.getLocalPart() + ":");
        for (QName relation : relationsList){
            sb.append(" " + RelationTypes.getRelationType(relation));
        }
        return sb.toString();
    }

    public static String getPolicyActionsAsString(PolicyActionsType policyActions){
        if (policyActions == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (policyActions.getEnforcement() != null){
            sb.append(PolicyActionsType.F_ENFORCEMENT.getLocalPart());
        }
        if (policyActions.getApproval() != null){
            sb.append(sb.length() > 0 ? ", " : "").append(PolicyActionsType.F_APPROVAL.getLocalPart());
        }
        if (policyActions.getRemediation() != null){
            sb.append(sb.length() > 0 ? ", " : "").append(PolicyActionsType.F_REMEDIATION.getLocalPart());
        }
        if (policyActions.getPrune() != null){
            sb.append(sb.length() > 0 ? ", " : "").append(PolicyActionsType.F_PRUNE.getLocalPart());
        }
        if (policyActions.getCertification() != null){
            sb.append(sb.length() > 0 ? ", " : "").append(PolicyActionsType.F_CERTIFICATION.getLocalPart());
        }
        if (policyActions.getNotification() != null){
            sb.append(sb.length() > 0 ? ", " : "").append(PolicyActionsType.F_NOTIFICATION.getLocalPart());
        }
        return sb.toString();
    }
}
