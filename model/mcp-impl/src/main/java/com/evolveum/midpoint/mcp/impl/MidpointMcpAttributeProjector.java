/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.mcp.api.MidpointMcpSchemaAttribute;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.expr.OrgStructFunctions;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

/**
 * Projects {@link PrismObject} fields into a flat string-keyed map (dot paths), matching MCP schema tool paths.
 */
final class MidpointMcpAttributeProjector {

    private static final Set<String> FULL_STRUCTURE_ROOTS = Set.of("assignment", "inducement", "roleMembershipRef");

    private final com.evolveum.midpoint.prism.PrismContext prismContext;
    private final ModelService modelService;
    private final OrgStructFunctions orgStructFunctions;
    private final ObjectMapper objectMapper = new ObjectMapper();

    MidpointMcpAttributeProjector(
            com.evolveum.midpoint.prism.PrismContext prismContext,
            ModelService modelService,
            OrgStructFunctions orgStructFunctions) {
        this.prismContext = prismContext;
        this.modelService = modelService;
        this.orgStructFunctions = orgStructFunctions;
    }

    /**
     * @param useSearchDefaults when true and returnAttributes omitted, use lightweight search defaults; otherwise explain defaults.
     */
    static List<String> resolveRequestedPaths(
            List<String> returnAttributes,
            String restType,
            PrismObjectDefinition<?> objDef,
            boolean useSearchDefaults) {
        if (returnAttributes == null || returnAttributes.isEmpty()) {
            return useSearchDefaults
                    ? MidpointMcpDefaultAttributes.searchDefaultsForRestType(restType)
                    : MidpointMcpDefaultAttributes.explainDefaultsForRestType(restType);
        }
        List<String> raw = new ArrayList<>();
        for (String s : returnAttributes) {
            if (s == null || s.isBlank()) {
                continue;
            }
            raw.add(s.trim());
        }
        if (raw.isEmpty()) {
            return useSearchDefaults
                    ? MidpointMcpDefaultAttributes.searchDefaultsForRestType(restType)
                    : MidpointMcpDefaultAttributes.explainDefaultsForRestType(restType);
        }
        if (raw.size() == 1 && MidpointMcpDefaultAttributes.STAR.equals(raw.getFirst())) {
            return expandStarPaths(restType, objDef);
        }
        validateExplicitPaths(raw, restType, objDef);
        return raw;
    }

    private static List<String> expandStarPaths(String restType, PrismObjectDefinition<?> objDef) {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        for (MidpointMcpSchemaAttribute a : MidpointMcpSchemaFlattener.flatten(objDef, Integer.MAX_VALUE)) {
            paths.add(a.getPath());
        }
        for (String v : MidpointMcpDefaultAttributes.VIRTUAL_PATHS) {
            if (virtualApplicable(v, restType)) {
                paths.add(v);
            }
        }
        for (String root : FULL_STRUCTURE_ROOTS) {
            paths.add(root);
        }
        paths.add(MidpointMcpDefaultAttributes.EXTENSION_STAR);
        return new ArrayList<>(paths);
    }

    private static void validateExplicitPaths(List<String> paths, String restType, PrismObjectDefinition<?> objDef) {
        Set<String> flat = new LinkedHashSet<>();
        for (MidpointMcpSchemaAttribute a : MidpointMcpSchemaFlattener.flatten(objDef, Integer.MAX_VALUE)) {
            flat.add(a.getPath());
        }
        for (String p : paths) {
            if (MidpointMcpDefaultAttributes.STAR.equals(p)) {
                throw new IllegalArgumentException("returnAttributes: '*' cannot be combined with other paths");
            }
            if (MidpointMcpDefaultAttributes.EXTENSION_STAR.equals(p)) {
                continue;
            }
            if (MidpointMcpDefaultAttributes.VIRTUAL_PATHS.contains(p)) {
                if (!virtualApplicable(p, restType)) {
                    throw new IllegalArgumentException("returnAttributes: path '" + p + "' is not valid for type " + restType);
                }
                continue;
            }
            if (FULL_STRUCTURE_ROOTS.contains(p)) {
                continue;
            }
            if (p.startsWith("extension.")) {
                String rest = p.substring("extension.".length());
                if (rest.isEmpty() || rest.contains(".")) {
                    if (!flat.contains(p)) {
                        throw new IllegalArgumentException("returnAttributes: unknown path '" + p + "'");
                    }
                } else if (!flat.contains(p)) {
                    throw new IllegalArgumentException("returnAttributes: unknown path '" + p + "'");
                }
                continue;
            }
            if (!flat.contains(p)) {
                throw new IllegalArgumentException("returnAttributes: unknown path '" + p + "'");
            }
        }
    }

    private static boolean virtualApplicable(String path, String restType) {
        String t = restType.toLowerCase(Locale.ROOT);
        return switch (path) {
            case "oid", "type", "summary" -> true;
            case "memberCount" -> "roles".equals(t) || "orgs".equals(t) || "services".equals(t);
            case "childOrgCount" -> "orgs".equals(t);
            case "capabilitiesSummary", "kindIntentSummary", "configured" -> "resources".equals(t);
            case "workItemCount",
                    "activeWorkItemCount",
                    "currentStep",
                    "request",
                    "workItems",
                    "decisionHistory",
                    "childCases",
                    "explanation" -> "cases".equals(t);
            default -> false;
        };
    }

    /**
     * @param resolveRefTargets when false, reference values omit live {@code modelService.getObject} enrichment (search performance).
     */
    LinkedHashMap<String, Object> project(
            PrismObject<? extends ObjectType> object,
            String restType,
            List<String> requestedPaths,
            String summaryText,
            Task task,
            OperationResult result,
            boolean resolveRefTargets) {

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        ObjectType bean = object.asObjectable();

        for (String path : requestedPaths) {
            if (MidpointMcpDefaultAttributes.EXTENSION_STAR.equals(path)) {
                putExtensionWildcard(object, out);
                continue;
            }
            Object value =
                    projectPath(path, object, bean, restType, summaryText, task, result, resolveRefTargets);
            if (value != null) {
                out.put(path, value);
            }
        }
        return out;
    }

    private void putExtensionWildcard(PrismObject<? extends ObjectType> object, Map<String, Object> out) {
        Item<?, ?> ext = object.findItem(ObjectType.F_EXTENSION);
        if (!(ext instanceof PrismContainer<?> extCont) || extCont.isEmpty()) {
            return;
        }
        for (PrismContainerValue<?> extVal : extCont.getValues()) {
            for (Item<?, ?> item : extVal.getItems()) {
                String local = item.getElementName().getLocalPart();
                String flatKey = "extension." + local;
                Object serialized = serializeItemShallow(item);
                if (serialized != null) {
                    out.putIfAbsent(flatKey, serialized);
                }
            }
        }
    }

    private Object projectPath(
            String path,
            PrismObject<? extends ObjectType> object,
            ObjectType bean,
            String restType,
            String summaryText,
            Task task,
            OperationResult result,
            boolean resolveRefTargets) {

        return switch (path) {
            case "oid" -> bean.getOid();
            case "type" -> restType;
            case "summary" -> summaryText;
            case "memberCount" -> resolveRefTargets ? memberCount(bean, task, result) : null;
            case "childOrgCount" -> resolveRefTargets ? childOrgCount(bean, task, result) : null;
            case "capabilitiesSummary" -> bean instanceof ResourceType r ? capabilitiesSummary(r) : null;
            case "kindIntentSummary" -> bean instanceof ResourceType r ? kindIntentSummary(r) : null;
            case "configured" -> bean instanceof ResourceType r ? resourceConfigured(r) : null;
            case "workItemCount" -> bean instanceof CaseType c ? countCaseWorkItems(c) : null;
            case "activeWorkItemCount" -> bean instanceof CaseType c ? countOpenCaseWorkItems(c) : null;
            case "currentStep" -> bean instanceof CaseType c ? MidpointMcpCaseProjector.compactCurrentStepForSearch(c) : null;
            case "request", "workItems", "decisionHistory", "childCases", "explanation" -> null;
            default -> projectPrismPath(path, object, bean, task, result, resolveRefTargets);
        };
    }

    private static int countCaseWorkItems(CaseType c) {
        return c.getWorkItem() != null ? c.getWorkItem().size() : 0;
    }

    private static int countOpenCaseWorkItems(CaseType c) {
        if (c.getWorkItem() == null) {
            return 0;
        }
        return (int) c.getWorkItem().stream().filter(wi -> wi.getCloseTimestamp() == null).count();
    }

    private Object projectPrismPath(
            String path,
            PrismObject<? extends ObjectType> object,
            ObjectType bean,
            Task task,
            OperationResult result,
            boolean resolveRefTargets) {
        if (FULL_STRUCTURE_ROOTS.contains(path)) {
            return fullStructureContainer(object, path);
        }
        if (path.startsWith("activation.")) {
            Object fromBean = activationFieldFromBean(path, bean);
            if (fromBean != null) {
                return fromBean;
            }
        }
        ItemPath itemPath = ItemPath.fromString(path);
        Item<?, ?> item = object.findItem(itemPath);
        if (item == null) {
            return null;
        }
        if (item instanceof PrismReference ref) {
            return resolveRefTargets ? referenceList(ref, task, result) : referenceListNoResolve(ref);
        }
        if (item instanceof PrismProperty<?> prop) {
            return propertyToJson(prop);
        }
        if (item instanceof PrismContainer<?> cont) {
            return containerValuesToJson(cont);
        }
        return null;
    }

    private static Object activationFieldFromBean(String path, ObjectType bean) {
        ActivationType act = null;
        if (bean instanceof FocusType focus) {
            act = focus.getActivation();
        } else if (bean instanceof ShadowType shadow) {
            act = shadow.getActivation();
        }
        if (act == null) {
            return null;
        }
        String tail = path.substring("activation.".length());
        return switch (tail) {
            case "administrativeStatus" -> enumName(act.getAdministrativeStatus());
            case "effectiveStatus" -> enumName(act.getEffectiveStatus());
            case "validFrom" -> xmlCal(act.getValidFrom());
            case "validTo" -> xmlCal(act.getValidTo());
            case "validityStatus" -> enumName(act.getValidityStatus());
            case "lockoutStatus" -> enumName(act.getLockoutStatus());
            default -> null;
        };
    }

    private static String enumName(Enum<?> e) {
        return e != null ? e.name() : null;
    }

    private static String xmlCal(XMLGregorianCalendar cal) {
        return cal != null ? cal.toXMLFormat() : null;
    }

    private Object fullStructureContainer(PrismObject<? extends ObjectType> object, String path) {
        ItemPath itemPath = ItemPath.fromString(path);
        Item<?, ?> item = object.findItem(itemPath);
        if (!(item instanceof PrismContainer<?> cont) || cont.isEmpty()) {
            return null;
        }
        List<Object> elements = new ArrayList<>();
        for (PrismContainerValue<?> pcv : cont.getValues()) {
            try {
                String json = prismContext.jsonSerializer().serialize(pcv);
                elements.add(objectMapper.readValue(json, Object.class));
            } catch (SchemaException | JsonProcessingException e) {
                // skip broken value
            }
        }
        return elements.isEmpty() ? null : elements;
    }

    private Object serializeItemShallow(Item<?, ?> item) {
        if (item instanceof PrismProperty<?> prop) {
            return propertyToJson(prop);
        }
        if (item instanceof PrismReference ref) {
            return referenceListNoResolve(ref);
        }
        if (item instanceof PrismContainer<?> cont) {
            try {
                String json = prismContext.jsonSerializer().serialize(cont);
                return objectMapper.readValue(json, Object.class);
            } catch (SchemaException | JsonProcessingException e) {
                return null;
            }
        }
        return null;
    }

    private List<Map<String, Object>> referenceList(PrismReference ref, Task task, OperationResult result) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PrismReferenceValue rv : ref.getValues()) {
            list.add(referenceMap(rv, task, result));
        }
        return list.isEmpty() ? null : list;
    }

    private List<Map<String, Object>> referenceListNoResolve(PrismReference ref) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PrismReferenceValue rv : ref.getValues()) {
            list.add(referenceMap(rv, null, null));
        }
        return list.isEmpty() ? null : list;
    }

    private Map<String, Object> referenceMap(PrismReferenceValue rv, Task task, OperationResult result) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("oid", rv.getOid());
        QName targetType = rv.getTargetType();
        if (targetType != null) {
            ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQNameIfKnown(targetType);
            m.put("type", ot != null ? ot.getRestType() : targetType.getLocalPart());
        }
        if (rv.getRelation() != null) {
            m.put("relation", rv.getRelation().getLocalPart());
        }
        String tn = targetNameFromRefValue(rv);
        if (tn != null) {
            m.put("targetName", tn);
        }
        if (task != null && result != null && StringUtils.isNotBlank(rv.getOid()) && StringUtils.isBlank(tn)) {
            try {
                Class<? extends ObjectType> clazz = targetType != null
                        ? ObjectTypes.getObjectTypeClassIfKnown(targetType)
                        : ObjectType.class;
                if (clazz == null) {
                    clazz = ObjectType.class;
                }
                PrismObject<? extends ObjectType> target = modelService.getObject(clazz, rv.getOid(), null, task, result);
                m.put("targetName", nameOf(target.asObjectable()));
                ObjectTypes ot = ObjectTypes.getObjectTypeIfKnown(target.getCompileTimeClass());
                if (ot != null) {
                    m.put("type", ot.getRestType());
                }
            } catch (ObjectNotFoundException
                    | SecurityViolationException
                    | SchemaException
                    | CommunicationException
                    | ConfigurationException
                    | ExpressionEvaluationException e) {
                // best-effort
            }
        }
        return m;
    }

    private static String targetNameFromRefValue(PrismReferenceValue rv) {
        Object tn = rv.getTargetName();
        if (tn == null) {
            return null;
        }
        if (tn instanceof PolyStringType pst) {
            return pst.getOrig();
        }
        if (tn instanceof PolyString ps) {
            return ps.getOrig();
        }
        return String.valueOf(tn);
    }

    private Object propertyToJson(PrismProperty<?> prop) {
        Collection<?> values = prop.getRealValues();
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Object> raw = new ArrayList<>(values);
        if (raw.size() == 1) {
            return simplifyRealValue(raw.getFirst());
        }
        List<Object> list = new ArrayList<>();
        for (Object v : raw) {
            list.add(simplifyRealValue(v));
        }
        return list;
    }

    private Object containerValuesToJson(PrismContainer<?> cont) {
        if (cont.isEmpty()) {
            return null;
        }
        List<Object> list = new ArrayList<>();
        for (PrismContainerValue<?> pcv : cont.getValues()) {
            try {
                String json = prismContext.jsonSerializer().serialize(pcv);
                list.add(objectMapper.readValue(json, Object.class));
            } catch (SchemaException | JsonProcessingException e) {
                // skip
            }
        }
        return list.size() == 1 ? list.getFirst() : list;
    }

    private static Object simplifyRealValue(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof PolyStringType p) {
            return p.getOrig();
        }
        if (v instanceof PolyString ps) {
            return ps.getOrig();
        }
        if (v instanceof Enum<?> e) {
            return e.name();
        }
        if (v instanceof XMLGregorianCalendar cal) {
            return cal.toXMLFormat();
        }
        if (v instanceof QName q) {
            return q.toString();
        }
        return v;
    }

    private Integer memberCount(ObjectType bean, Task task, OperationResult result) {
        try {
            if (bean instanceof RoleType role) {
                return countUsersWithAssignmentTo(role.getOid(), task, result);
            }
            if (bean instanceof OrgType org) {
                return countOrgMembers(org.getOid(), task, result);
            }
            if (bean instanceof ServiceType svc) {
                return countUsersWithAssignmentTo(svc.getOid(), task, result);
            }
        } catch (SchemaException
                | ObjectNotFoundException
                | SecurityViolationException
                | CommunicationException
                | ConfigurationException
                | ExpressionEvaluationException e) {
            return null;
        }
        return null;
    }

    private Integer childOrgCount(ObjectType bean, Task task, OperationResult result) {
        if (!(bean instanceof OrgType org) || StringUtils.isBlank(org.getOid())) {
            return null;
        }
        try {
            ObjectQuery query = prismContext.queryFor(OrgType.class)
                    .item(OrgType.F_PARENT_ORG_REF)
                    .ref(org.getOid())
                    .build();
            return modelService.countObjects(OrgType.class, query, null, task, result);
        } catch (SchemaException
                | ObjectNotFoundException
                | SecurityViolationException
                | CommunicationException
                | ConfigurationException
                | ExpressionEvaluationException e) {
            return null;
        }
    }

    private int countUsersWithAssignmentTo(String abstractRoleOid, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                .ref(abstractRoleOid)
                .build();
        return modelService.countObjects(UserType.class, query, null, task, result);
    }

    private int countOrgMembers(String orgOid, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_PARENT_ORG_REF)
                .ref(orgOid)
                .build();
        return modelService.countObjects(UserType.class, query, null, task, result);
    }

    private static String capabilitiesSummary(ResourceType resource) {
        var cap = ResourceTypeUtil.getNativeCapabilitiesCollection(resource);
        if (cap == null) {
            return null;
        }
        List<CapabilityType> caps = CapabilityUtil.getAllCapabilities(cap);
        if (caps.isEmpty()) {
            return "none";
        }
        return caps.size() + " capability(es)";
    }

    private static String kindIntentSummary(ResourceType resource) {
        if (resource.getSchemaHandling() == null || resource.getSchemaHandling().getObjectType() == null) {
            return null;
        }
        return resource.getSchemaHandling().getObjectType().size() + " object type(s)";
    }

    private static Boolean resourceConfigured(ResourceType resource) {
        return resource.getConnectorRef() != null && StringUtils.isNotBlank(resource.getConnectorRef().getOid());
    }

    private static String nameOf(ObjectType object) {
        return object.getName() != null ? object.getName().getOrig() : null;
    }

    private static String poly(PolyStringType value) {
        return value != null ? value.getOrig() : null;
    }

    static String explainSummary(ObjectType object, String restType) {
        String type = ObjectTypes.getRestTypeFromClass(object.getClass());
        String name = object.getName() != null ? object.getName().getOrig() : object.getOid();
        return switch (restType) {
            case "users" -> {
                int a = object instanceof UserType u && u.getAssignment() != null ? u.getAssignment().size() : 0;
                int p = object instanceof UserType u && u.getParentOrgRef() != null ? u.getParentOrgRef().size() : 0;
                yield "User '" + name + "' is a focus object with " + a + " assignment(s) and " + p + " parent org ref(s).";
            }
            case "roles" -> {
                int ind = object instanceof RoleType r && r.getInducement() != null ? r.getInducement().size() : 0;
                yield "Role '" + name + "' with " + ind + " inducement(s).";
            }
            case "orgs" -> {
                int po =
                        object instanceof OrgType o && o.getParentOrgRef() != null ? o.getParentOrgRef().size() : 0;
                yield "Organization '" + name + "' with " + po + " parent org ref(s).";
            }
            case "services" -> {
                int ind = object instanceof ServiceType s && s.getInducement() != null ? s.getInducement().size() : 0;
                yield "Service '" + name + "' with " + ind + " inducement(s).";
            }
            case "archetypes" -> {
                int ind =
                        object instanceof com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType a
                                && a.getInducement() != null
                                ? a.getInducement().size()
                                : 0;
                yield "Archetype '" + name + "' with " + ind + " inducement(s).";
            }
            case "resources" -> "Resource '" + name + "' — connected system.";
            case "shadows" -> "Shadow '" + name + "' — resource projection.";
            case "tasks" -> "Task '" + name + "'.";
            case "connectors" -> "Connector '" + name + "'.";
            case "nodes" -> "Node '" + name + "'.";
            case "cases" -> {
                if (object instanceof CaseType c) {
                    int wi = countCaseWorkItems(c);
                    int open = countOpenCaseWorkItems(c);
                    String state = c.getState() != null ? String.valueOf(c.getState()) : "unknown state";
                    yield "Case '" + name + "' (" + state + ") with " + open + " open / " + wi + " total work item(s).";
                }
                yield type + " '" + name + "'.";
            }
            default -> type + " '" + name + "'.";
        };
    }

    static String searchSummary(ObjectType object) {
        if (object instanceof CaseType c) {
            int wi = countCaseWorkItems(c);
            int open = countOpenCaseWorkItems(c);
            String state = c.getState() != null ? String.valueOf(c.getState()) : "unknown";
            return "cases: state " + state + ", " + open + " open work item(s), " + wi + " total";
        }
        String type = ObjectTypes.getRestTypeFromClass(object.getClass());
        String lifecycle =
                object instanceof com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType ah
                        ? StringUtils.defaultIfBlank(ah.getLifecycleState(), "no lifecycle state")
                        : "no lifecycle state";
        List<String> subtypes =
                object instanceof com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType ah2
                        ? ah2.getSubtype()
                        : List.of();
        if (subtypes.isEmpty()) {
            return type + " with " + lifecycle;
        }
        return type + " with subtypes " + String.join(", ", subtypes);
    }
}
