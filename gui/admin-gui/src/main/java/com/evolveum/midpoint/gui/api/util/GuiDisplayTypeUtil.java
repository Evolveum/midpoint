/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.page.admin.server.dto.ApprovalOutcomeIcon;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;

public class GuiDisplayTypeUtil {

    private static final Trace LOGGER = TraceManager.getTrace(GuiDisplayTypeUtil.class);

    public static DisplayType createDisplayType(String iconCssClass) {
        return createDisplayType(iconCssClass, "", "");
    }

    public static DisplayType createDisplayType(ApprovalOutcomeIcon caseIcon) {
        return createDisplayType(caseIcon.getIcon(), "", caseIcon.getTitle());
    }

    public static DisplayType createDisplayType(OperationResultStatusPresentationProperties OperationIcon) {
        return createDisplayType(OperationIcon.getIcon(), "", OperationIcon.getStatusLabelKey());
    }

    public static DisplayType createDisplayType(String iconCssClass, String iconColor, String title) {
        return createDisplayType(iconCssClass, iconColor, WebComponentUtil.createPolyFromOrigString(title));
    }

    public static DisplayType createDisplayType(String iconCssClass, String iconColor, String label, String title) {
        return createDisplayType(iconCssClass, iconColor, WebComponentUtil.createPolyFromOrigString(label), WebComponentUtil.createPolyFromOrigString(title));
    }

    public static DisplayType createDisplayType(String iconCssClass, PolyStringType title) {
        return createDisplayType(iconCssClass, "", title);
    }

    public static DisplayType createDisplayType(String iconCssClass, String iconColor, PolyStringType title) {
        return createDisplayType(iconCssClass, iconColor, null, title);
    }

    public static DisplayType createDisplayType(String iconCssClass, String iconColor, PolyStringType label, PolyStringType title) {
        DisplayType displayType = new DisplayType();
        IconType icon = new IconType();
        icon.setCssClass(iconCssClass != null ? iconCssClass.trim() : iconCssClass);
        icon.setColor(iconColor);
        displayType.setIcon(icon);
        displayType.setLabel(label);

        displayType.setTooltip(title);
        return displayType;
    }

    public static <O extends ObjectType> DisplayType getArchetypePolicyDisplayType(O object, PageBase pageBase) {
        if (object == null) {
            return null;
        }

        return getArchetypePolicyDisplayType(object.asPrismObject(), pageBase);
    }

    public static <O extends ObjectType> DisplayType getArchetypePolicyDisplayType(PrismObject<O> object, PageBase pageBase) {
        if (object != null) {
            ArchetypePolicyType archetypePolicy = WebComponentUtil.getArchetypeSpecification(object, pageBase);
            if (archetypePolicy != null) {
                return archetypePolicy.getDisplay();
            }
        }
        return null;
    }

    public static DisplayType getAssignmentObjectRelationDisplayType(PageBase pageBase, AssignmentObjectRelation assignmentTargetRelation,
            String defaultTitleKey) {
        if (assignmentTargetRelation == null) {
            return createDisplayType("", "", pageBase.createStringResource(defaultTitleKey, "", "").getString());
        }

        String typeTitle = "";
        if (CollectionUtils.isNotEmpty(assignmentTargetRelation.getArchetypeRefs())) {
            OperationResult result = new OperationResult(pageBase.getClass().getSimpleName() + "." + "loadArchetypeObject");
            try {
                ArchetypeType archetype = pageBase.getModelObjectResolver().resolve(assignmentTargetRelation.getArchetypeRefs().get(0), ArchetypeType.class,
                        null, null, pageBase.createSimpleTask(result.getOperation()), result);
                if (archetype != null) {
                    DisplayType archetypeDisplayType = archetype.getArchetypePolicy() != null ? archetype.getArchetypePolicy().getDisplay() : null;
                    String archetypeTooltip = archetypeDisplayType != null && archetypeDisplayType.getLabel() != null &&
                            StringUtils.isNotEmpty(archetypeDisplayType.getLabel().getOrig()) ?
                            archetypeDisplayType.getLabel().getOrig() :
                            (archetype.getName() != null && StringUtils.isNotEmpty(archetype.getName().getOrig()) ?
                                    archetype.getName().getOrig() : null);
                    typeTitle = StringUtils.isNotEmpty(archetypeTooltip) ?
                            pageBase.createStringResource("abstractRoleMemberPanel.withType", archetypeTooltip).getString() : "";
                }
            } catch (Exception ex) {
                LOGGER.error("Couldn't load archetype object. " + ex.getLocalizedMessage());
            }
        } else if (CollectionUtils.isNotEmpty(assignmentTargetRelation.getObjectTypes())) {
            QName type = !CollectionUtils.isEmpty(assignmentTargetRelation.getObjectTypes()) ?
                    assignmentTargetRelation.getObjectTypes().get(0) : null;
            String typeName = type != null ? pageBase.createStringResource("ObjectTypeLowercase." + type.getLocalPart()).getString() : null;
            typeTitle = StringUtils.isNotEmpty(typeName) ?
                    pageBase.createStringResource("abstractRoleMemberPanel.withType", typeName).getString() : "";
        }

        QName relation = !CollectionUtils.isEmpty(assignmentTargetRelation.getRelations()) ?
                assignmentTargetRelation.getRelations().get(0) : null;

        String relationValue = "";
        String relationTitle = "";
        if (relation != null) {
            RelationDefinitionType def = WebComponentUtil.getRelationDefinition(relation);
            if (def != null) {
                DisplayType displayType = null;
                if (def.getDisplay() == null) {
                    displayType = new DisplayType();
                } else {
                    displayType = createDisplayType(def.getDisplay().getCssClass());
                    if (def.getDisplay().getIcon() != null) {
                        displayType.setIcon(new IconType());
                        displayType.getIcon().setCssClass(def.getDisplay().getIcon().getCssClass());
                        displayType.getIcon().setColor(def.getDisplay().getIcon().getColor());
                    }
                    displayType.setLabel(def.getDisplay().getLabel());
                }
                if (displayType.getLabel() != null) {
                    relationValue = WebComponentUtil.getTranslatedPolyString(displayType.getLabel());
                } else {
                    String relationKey = "RelationTypes." + RelationTypes.getRelationTypeByRelationValue(relation);
                    relationValue = pageBase.createStringResource(relationKey).getString();
                    if (StringUtils.isEmpty(relationValue) || relationKey.equals(relationValue)) {
                        relationValue = relation.getLocalPart();
                    }
                }

                relationTitle = pageBase.createStringResource("abstractRoleMemberPanel.withRelation", relationValue).getString();

                if (displayType.getIcon() == null || StringUtils.isEmpty(displayType.getIcon().getCssClass())) {
                    displayType.setIcon(WebComponentUtil.createIconType(""));
                }
                displayType.setTooltip(WebComponentUtil.createPolyFromOrigString(pageBase.createStringResource(defaultTitleKey, typeTitle, relationTitle).getString()));
                return displayType;
            }
        }
        return createDisplayType("", "", pageBase.createStringResource(defaultTitleKey, typeTitle, relationTitle).getString());
    }

    public static DisplayType getNewObjectDisplayTypeFromCollectionView(CompiledObjectCollectionView view, PageBase pageBase) {
        DisplayType displayType = view != null ? view.getDisplay() : null;
        if (displayType == null) {
            displayType = createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green", "");
        }
        if (PolyStringUtils.isEmpty(displayType.getTooltip()) && !PolyStringUtils.isEmpty(displayType.getLabel())) {
            StringBuilder sb = new StringBuilder();
            sb.append(pageBase.createStringResource("MainObjectListPanel.newObject").getString());
            sb.append(" ");
            sb.append(displayType.getLabel().getOrig().toLowerCase());
            displayType.setTooltip(WebComponentUtil.createPolyFromOrigString(sb.toString()));
        }
        return view != null ? view.getDisplay() : null;
    }

    public static <O extends ObjectType> DisplayType getDisplayTypeForObject(PrismObject<O> obj, OperationResult result, PageBase pageBase) {
        if (obj == null) {
            return null;
        }

        return getDisplayTypeForObject(obj.asObjectable(), result, pageBase);
    }

    public static <O extends ObjectType> DisplayType getDisplayTypeForObject(O obj, OperationResult result, PageBase pageBase) {
        if (obj == null) {
            return null;
        }
        DisplayType displayType = getArchetypePolicyDisplayType(obj.asPrismObject(), pageBase);

        if (displayType == null) {
            displayType = createDisplayType(WebComponentUtil.createDefaultIcon(obj.asPrismObject()),
                    "", ColumnUtils.getIconColumnTitle(obj, result));
        }
        return displayType;
    }

}
