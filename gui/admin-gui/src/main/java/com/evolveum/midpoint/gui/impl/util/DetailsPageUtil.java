/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.page.admin.application.PageApplication;
import com.evolveum.midpoint.gui.impl.page.admin.application.PageApplications;
import com.evolveum.midpoint.gui.impl.page.admin.policy.PagePolicies;
import com.evolveum.midpoint.gui.impl.page.admin.policy.PagePolicy;
import com.evolveum.midpoint.gui.impl.page.admin.policy.PagePolicyHistory;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisCluster;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisOutlier;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession;

import com.evolveum.midpoint.gui.impl.page.admin.schema.PageSchema;

import com.evolveum.midpoint.gui.impl.page.admin.schema.PageSchemas;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.INamedParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.menu.PageTypes;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.archetype.PageArchetype;
import com.evolveum.midpoint.gui.impl.page.admin.cases.PageCase;
import com.evolveum.midpoint.gui.impl.page.admin.mark.PageMark;
import com.evolveum.midpoint.gui.impl.page.admin.messagetemplate.PageMessageTemplate;
import com.evolveum.midpoint.gui.impl.page.admin.messagetemplate.PageMessageTemplates;
import com.evolveum.midpoint.gui.impl.page.admin.objectcollection.PageObjectCollection;
import com.evolveum.midpoint.gui.impl.page.admin.objecttemplate.PageObjectTemplate;
import com.evolveum.midpoint.gui.impl.page.admin.org.PageOrg;
import com.evolveum.midpoint.gui.impl.page.admin.org.PageOrgHistory;
import com.evolveum.midpoint.gui.impl.page.admin.report.PageReport;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageShadow;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRoleHistory;
import com.evolveum.midpoint.gui.impl.page.admin.service.PageService;
import com.evolveum.midpoint.gui.impl.page.admin.service.PageServiceHistory;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.PageSimulationResult;
import com.evolveum.midpoint.gui.impl.page.admin.task.PageTask;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUserHistory;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageMounter;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public final class DetailsPageUtil {

    private static final Trace LOGGER = TraceManager.getTrace(DetailsPageUtil.class);

    public static final Map<Class<? extends ObjectType>, Class<? extends PageBase>> OBJECT_DETAILS_PAGE_MAP;
    // only pages that support 'advanced search' are currently listed here (TODO: generalize)
    public static final Map<Class<?>, Class<? extends PageBase>> OBJECT_LIST_PAGE_MAP;
    public static final Map<Class<?>, Class<? extends PageBase>> OBJECT_HISTORY_PAGE_MAP;

    static {
        OBJECT_DETAILS_PAGE_MAP = new HashMap<>();
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(UserType.class, PageUser.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(ApplicationType.class, PageApplication.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(OrgType.class, PageOrg.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(RoleType.class, PageRole.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(ServiceType.class, PageService.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(PolicyType.class, PagePolicy.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(ResourceType.class, PageResource.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(TaskType.class, PageTask.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(ReportType.class, PageReport.class);

        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(CaseType.class, PageCase.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(ArchetypeType.class, PageArchetype.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(ShadowType.class, PageShadow.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(ObjectCollectionType.class, PageObjectCollection.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(ObjectTemplateType.class, PageObjectTemplate.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(MessageTemplateType.class, PageMessageTemplate.class);

        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(SimulationResultType.class, PageSimulationResult.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(MarkType.class, PageMark.class);

        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(RoleAnalysisSessionType.class, PageRoleAnalysisSession.class);
        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(RoleAnalysisClusterType.class, PageRoleAnalysisCluster.class);

        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(SchemaType.class, PageSchema.class);

        DetailsPageUtil.OBJECT_DETAILS_PAGE_MAP.put(RoleAnalysisOutlierType.class, PageRoleAnalysisOutlier.class);

    }

    static {
        OBJECT_LIST_PAGE_MAP = new HashMap<>();
        DetailsPageUtil.OBJECT_LIST_PAGE_MAP.put(UserType.class, PageUsers.class);
        DetailsPageUtil.OBJECT_LIST_PAGE_MAP.put(RoleType.class, PageRoles.class);
        DetailsPageUtil.OBJECT_LIST_PAGE_MAP.put(ServiceType.class, PageServices.class);
        DetailsPageUtil.OBJECT_LIST_PAGE_MAP.put(PolicyType.class, PagePolicies.class);
        DetailsPageUtil.OBJECT_LIST_PAGE_MAP.put(ApplicationType.class, PageApplications.class);
        DetailsPageUtil.OBJECT_LIST_PAGE_MAP.put(ResourceType.class, PageResources.class);
        DetailsPageUtil.OBJECT_LIST_PAGE_MAP.put(TaskType.class, PageTasks.class);
        DetailsPageUtil.OBJECT_LIST_PAGE_MAP.put(PageMessageTemplate.class, PageMessageTemplates.class);
        DetailsPageUtil.OBJECT_LIST_PAGE_MAP.put(SchemaType.class, PageSchemas.class);
    }

    static {
        OBJECT_HISTORY_PAGE_MAP = new HashMap<>();
        DetailsPageUtil.OBJECT_HISTORY_PAGE_MAP.put(PageService.class, PageServiceHistory.class);
        DetailsPageUtil.OBJECT_HISTORY_PAGE_MAP.put(PageRole.class, PageRoleHistory.class);
        DetailsPageUtil.OBJECT_HISTORY_PAGE_MAP.put(PageOrg.class, PageOrgHistory.class);
        DetailsPageUtil.OBJECT_HISTORY_PAGE_MAP.put(PagePolicy.class, PagePolicyHistory.class);
        DetailsPageUtil.OBJECT_HISTORY_PAGE_MAP.put(PageUser.class, PageUserHistory.class);
    }

    public static <AHT extends AssignmentHolderType> void initNewObjectWithReference(PageBase pageBase, QName type, List<ObjectReferenceType> newReferences) throws SchemaException {
        PrismContext prismContext = pageBase.getPrismContext();
        PrismObjectDefinition<AHT> def = prismContext.getSchemaRegistry().findObjectDefinitionByType(type);
        PrismObject<AHT> obj = def.instantiate();
        AHT assignmentHolder = obj.asObjectable();
        initNewObjectWithReference(pageBase, assignmentHolder, newReferences);
    }

    public static <AHT extends AssignmentHolderType> void initNewObjectWithReference(
            PageBase pageBase, AHT assignmentHolder, List<ObjectReferenceType> newReferences) {
        if (newReferences != null) {
            newReferences.forEach(ref -> {
                AssignmentType assignment = new AssignmentType();
                assignment.setTargetRef(ref);
                assignmentHolder.getAssignment().add(assignment);

                // Set parentOrgRef in any case. This is not strictly correct.
                // The parentOrgRef should be added by the projector. But
                // this is needed to successfully pass through security
                // TODO: fix MID-3234
                //  see also TreeTablePanel.initObjectForAdd
                if (ref.getType() != null && OrgType.COMPLEX_TYPE.equals(ref.getType())) {
                    if (ref.getRelation() == null || pageBase.getRelationRegistry().isStoredIntoParentOrgRef(ref.getRelation())) {
                        assignmentHolder.getParentOrgRef().add(ref.clone());
                    }
                }

            });
        }

        dispatchToNewObject(assignmentHolder, pageBase);
    }

    public static void dispatchToNewObject(@NotNull AssignmentHolderType newObject, @NotNull PageBase pageBase) {
        dispatchToObjectDetailsPage(newObject.asPrismObject(), true, pageBase);
    }

    public static void dispatchToObjectDetailsPage(PrismReferenceValue objectRef, Component component, boolean failIfUnsupported) {
        if (objectRef == null) {
            return; //TODO is this correct?
        }
        dispatchToObjectDetailsPage(objectRef.asReferencable(), component, failIfUnsupported);
    }

    public static void dispatchToObjectDetailsPage(Referencable objectRef, Component component, boolean failIfUnsupported) {
        if (objectRef == null) {
            return; // should not occur
        }
        Validate.notNull(objectRef.getOid(), "No OID in objectRef");
        Validate.notNull(objectRef.getType(), "No type in objectRef");
        Class<? extends ObjectType> targetClass = ObjectTypes.getObjectTypeFromTypeQName(objectRef.getType()).getClassDefinition();
        dispatchToObjectDetailsPage(targetClass, objectRef.getOid(), component, failIfUnsupported);
    }

    public static void dispatchToObjectDetailsPage(PrismObject obj, Component component) {
        dispatchToObjectDetailsPage(obj, false, component);
    }

    public static boolean isNewDesignEnabled() {
        try {
            CompiledGuiProfile profile = WebComponentUtil.getCompiledGuiProfile();
            return profile.isUseNewDesign();
        } catch (Exception ex) {
            //if somthing happen just return true, by default we want new design
            return true;
        }
    }
    // shows the actual object that is passed via parameter (not its state in repository)
    public static void dispatchToObjectDetailsPage(PrismObject obj, boolean isNewObject, Component component) {
        dispatchToObjectDetailsPage(obj, isNewObject, false, component);
    }

    // shows the actual object that is passed via parameter (not its state in repository)
    public static void dispatchToObjectDetailsPage(PrismObject obj, boolean isNewObject, boolean showWizard, Component component) {
        Class<?> newObjectPageClass = isNewObject ? getNewlyCreatedObjectPage(obj.getCompileTimeClass()) : getObjectDetailsPage(obj.getCompileTimeClass());
        if (newObjectPageClass == null) {
            throw new IllegalArgumentException("Cannot determine details page for " + obj.getCompileTimeClass());
        }

        Constructor<?> constructor;
        try {
            PageBase page;
            if (isNewDesignEnabled()) {
                if (showWizard) {
                    constructor = newObjectPageClass.getConstructor(PrismObject.class, boolean.class);
                    page = (PageBase) constructor.newInstance(obj, showWizard);
                } else {
                    constructor = newObjectPageClass.getConstructor(PrismObject.class);
                    page = (PageBase) constructor.newInstance(obj);
                }
            } else {
                constructor = newObjectPageClass.getConstructor(PrismObject.class, boolean.class);
                page = (PageBase) constructor.newInstance(obj, isNewObject);
            }

            if (component.getPage() instanceof PageBase pb) {
                // this way we have correct breadcrumbs
                pb.navigateToNext(page);
            } else {
                component.setResponsePage(page);
            }
        } catch (NoSuchMethodException | SecurityException e) {
            throw new SystemException("Unable to locate constructor (PrismObject) in " + newObjectPageClass

                    + ": " + e.getMessage(), e);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new SystemException("Error instantiating " + newObjectPageClass + ": " + e.getMessage(), e);
        }
    }

    public static void dispatchToObjectDetailsPage(Class<? extends ObjectType> objectClass, String oid, Component component, boolean failIfUnsupported) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        Class<? extends PageBase> page = getObjectDetailsPage(objectClass);
        if (page != null) {
            ((PageBase) component.getPage()).navigateToNext(page, parameters);
        } else if (failIfUnsupported) {
            throw new SystemException("Cannot determine details page for " + objectClass);
        }
    }

    public static void dispatchToListPage(Class<? extends Containerable> objectClass, String collectionViewId, Component component, boolean failIfUnsupported) {
        QName type = WebComponentUtil.containerClassToQName(PrismContext.get(), objectClass);
        PageTypes pageTypes = PageTypes.getPageTypesByType(type);
        if (pageTypes != null) {
            Class<? extends PageBase> listPage = pageTypes.getListClass();
            PageParameters pageParameters = new PageParameters();
            pageParameters.add(PageBase.PARAMETER_OBJECT_COLLECTION_NAME, collectionViewId);
            if (listPage != null) {
                ((PageBase) component.getPage()).navigateToNext(listPage, pageParameters);
            }
        }
        if (failIfUnsupported) {
            throw new SystemException("Cannot determine details page for " + objectClass);
        }
    }

    public static void redirectFromDashboardWidget(GuiActionType action, PageBase pageBase) {
        redirectFromDashboardWidget(action, null, pageBase);
    }

    public static void redirectFromDashboardWidget(GuiActionType action, PageParameters params, PageBase pageBase) {
        if (params == null) {
            params = new PageParameters();
        }

        RedirectionTargetType redirectionTarget = action.getTarget();
        if (redirectionTarget == null) {
            return;
        }

        String url = redirectionTarget.getTargetUrl();
        String pageClass = redirectionTarget.getPageClass();

        Class<? extends WebPage> webPageClass = null;
        if (StringUtils.isNotEmpty(url)) {
            if (new UrlValidator().isValid(url)) {

                StringBuilder sb = new StringBuilder(url);
                if (CollectionUtils.isNotEmpty(params.getAllNamed())) {
                    sb.append("?");

                    List<INamedParameters.NamedPair> pairs = params.getAllNamed();
                    for (INamedParameters.NamedPair p : pairs) {
                        sb.append(p.getKey()).append("=").append(p.getValue());
                        if (params.getAllNamed().indexOf(p) < params.getAllNamed().size() - 1) {
                            sb.append("&");
                        }
                    }
                }

                throw new RedirectToUrlException(sb.toString());
            }
            webPageClass = PageMounter.getUrlClassMap().get(url);
        }

        try {
            if (webPageClass == null) {
                webPageClass = (Class<? extends WebPage>) Class.forName(pageClass);
            }

            String panelType = redirectionTarget.getPanelIdentifier();
            if (panelType != null) {
                params.set(AbstractPageObjectDetails.PARAM_PANEL_ID, panelType);
            }

            String collectionIdentifier = redirectionTarget.getCollectionIdentifier();
            if (collectionIdentifier != null) {
                params.set(PageBase.PARAMETER_OBJECT_COLLECTION_NAME, collectionIdentifier);
            }
            if (pageBase != null) {
                pageBase.navigateToNext(webPageClass, params);
            }

        } catch (Throwable e) {
            LOGGER.trace("Problem with redirecting to page: {}, reason: {}", webPageClass, e.getMessage(), e);
        }

    }

    public static boolean isRedirectionTargetNotEmpty(GuiActionType action) {
        if (action == null || action.getTarget() == null) {
            return false;
        }
        RedirectionTargetType target = action.getTarget();
        return !StringUtils.isAllEmpty(target.getTargetUrl(), target.getPageClass(), target.getPanelIdentifier(), target.getCollectionIdentifier());
    }

    public static boolean hasDetailsPage(PrismObject<?> object) {
        Class<?> clazz = object.getCompileTimeClass();
        return hasDetailsPage(clazz);
    }

    public static boolean hasDetailsPage(Class<?> clazz) {
        return OBJECT_DETAILS_PAGE_MAP.containsKey(clazz);
    }

    public static Class<? extends PageBase> getObjectDetailsPage(Class<? extends ObjectType> type) {
        return OBJECT_DETAILS_PAGE_MAP.get(type);
    }

    public static Class<? extends ObjectType> getObjectTypeForDetailsPage(PageBase pageType) {
        var objectDetailsPages = OBJECT_DETAILS_PAGE_MAP.entrySet();
        for (Map.Entry<Class<? extends ObjectType>, Class<? extends PageBase>> detailsPage : objectDetailsPages) {
            if (detailsPage.getValue().equals(pageType.getPageClass())) {
                return detailsPage.getKey();
            }
        }

        return null;
    }

    public static Class<? extends PageBase> getNewlyCreatedObjectPage(Class<? extends ObjectType> type) {
        return OBJECT_DETAILS_PAGE_MAP.get(type);
    }

    public static Class<? extends PageBase> getObjectListPage(Class<? extends ObjectType> type) {
        return OBJECT_LIST_PAGE_MAP.get(type);
    }

    public static Class<? extends PageBase> getPageHistoryDetailsPage(Class<?> page) {
        return OBJECT_HISTORY_PAGE_MAP.get(page);
    }
}
