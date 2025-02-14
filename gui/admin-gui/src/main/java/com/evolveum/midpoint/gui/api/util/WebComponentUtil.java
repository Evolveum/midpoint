/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.util;

import static com.evolveum.midpoint.gui.api.page.PageBase.createStringResourceStatic;

import java.io.PrintWriter;
import java.io.Serial;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.input.converter.DateConverter;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.web.page.admin.server.dto.ApprovalOutcomeIcon;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.visit.IVisitor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.format.DateTimeFormat;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.common.AvailableLocale;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.AdminLTESkin;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.GuiChannel;
import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.AbstractAssignmentTypePanel;
import com.evolveum.midpoint.gui.impl.page.login.module.PageLogin;
import com.evolveum.midpoint.gui.impl.page.self.PageOrgSelfProfile;
import com.evolveum.midpoint.gui.impl.page.self.PageRoleSelfProfile;
import com.evolveum.midpoint.gui.impl.page.self.PageServiceSelfProfile;
import com.evolveum.midpoint.gui.impl.page.self.PageUserSelfProfile;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.impl.binding.AbstractMutableObjectable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.schema.util.task.TaskTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.DisplayableValueChoiceRenderer;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerGroupDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.wf.api.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Utility class containing miscellaneous methods used mostly in Wicket
 * components.
 *
 * @author lazyman
 */
public final class WebComponentUtil {

    private static final Trace LOGGER = TraceManager.getTrace(WebComponentUtil.class);

    private static final String KEY_BOOLEAN_NULL = "Boolean.NULL";
    private static final String KEY_BOOLEAN_TRUE = "Boolean.TRUE";
    private static final String KEY_BOOLEAN_FALSE = "Boolean.FALSE";

    public static RestartResponseException restartOnLoginPageException() {
        return new RestartResponseException(PageLogin.class);
    }

    public static void setTaskStateBeforeSave(
            PrismObjectWrapper<TaskType> taskWrapper, boolean runEnabled, PageBase pageBase, AjaxRequestTarget target) {
        try {
            PrismPropertyWrapper<TaskExecutionStateType> executionState = taskWrapper.findProperty(ItemPath.create(TaskType.F_EXECUTION_STATE));
            PrismPropertyWrapper<TaskSchedulingStateType> schedulingState = taskWrapper.findProperty(ItemPath.create(TaskType.F_SCHEDULING_STATE));
            if (executionState == null || schedulingState == null) {
                throw new SchemaException("Task cannot be set as running, no execution status or scheduling status present");
            }

            if (runEnabled) {
                executionState.getValue().setRealValue(TaskExecutionStateType.RUNNABLE);
                schedulingState.getValue().setRealValue(TaskSchedulingStateType.READY);
            } else {
                if (!ItemStatus.ADDED.equals(taskWrapper.getStatus())) {
                    return;
                }
                executionState.getValue().setRealValue(TaskExecutionStateType.SUSPENDED);
                schedulingState.getValue().setRealValue(TaskSchedulingStateType.SUSPENDED);
            }

            PrismReferenceWrapper<Referencable> taskOwner = taskWrapper.findReference(ItemPath.create(TaskType.F_OWNER_REF));
            if (taskOwner == null) {
                return;
            }
            PrismReferenceValueWrapperImpl<Referencable> taskOwnerValue = taskOwner.getValue();
            if (taskOwnerValue == null) {
                return;
            }

            if (taskOwnerValue.getNewValue() == null || taskOwnerValue.getNewValue().isEmpty()) {
                GuiProfiledPrincipal guiPrincipal = AuthUtil.getPrincipalUser();
                if (guiPrincipal == null) {
                    //BTW something very strange must happened
                    return;
                }
                FocusType focus = guiPrincipal.getFocus();
                taskOwnerValue.setRealValue(ObjectTypeUtil.createObjectRef(focus, SchemaConstants.ORG_DEFAULT));
            }
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error while finishing task settings.", e);
            target.add(pageBase.getFeedbackPanel());
        }
    }

    public static IModel<String> createLocalizedModelForBoolean(Boolean object) {
        return () -> {
            String key;
            if (object == null) {
                key = KEY_BOOLEAN_NULL;
            } else {
                key = object ? KEY_BOOLEAN_TRUE : KEY_BOOLEAN_FALSE;
            }

            return com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate(key);
        };
    }

    public enum AssignmentOrder {

        ASSIGNMENT(0),
        INDUCEMENT(1);

        private final int order;

        AssignmentOrder(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    public static String nl2br(String text) {
        if (text == null) {
            return null;
        }
        return StringEscapeUtils.escapeHtml4(text).replace("\n", "<br/>");
    }

    public static String getTypeLocalized(ObjectReferenceType ref) {
        ObjectTypes type = ref != null ? ObjectTypes.getObjectTypeFromTypeQName(ref.getType()) : null;
        ObjectTypeGuiDescriptor descriptor = ObjectTypeGuiDescriptor.getDescriptor(type);
        if (descriptor == null) {
            return null;
        }
        return createStringResourceStatic(descriptor.getLocalizationKey()).getString();
    }

    public static String getReferencedObjectNames(List<ObjectReferenceType> refs, boolean showTypes) {
        return getReferencedObjectNames(refs, showTypes, true);
    }

    public static String getReferencedObjectNames(List<ObjectReferenceType> refs, boolean showTypes, boolean translate) {
        return refs.stream()
                .map(ref -> emptyIfNull(getName(ref, translate)) + (showTypes ? (" (" + emptyIfNull(getTypeLocalized(ref)) + ")") : ""))
                .collect(Collectors.joining(", "));
    }

    private static String emptyIfNull(String s) {
        return s != null ? s : "";
    }

    public static String getReferencedObjectDisplayNameAndName(Referencable ref, boolean loadObject, PageBase pageBase) {
        if (ref == null || ref.asReferenceValue().isEmpty()) {
            return "";
        }
        if (ref.getOid() != null && ref.asReferenceValue().getObject() == null && !loadObject) {
            return getReferencedObjectDisplayNamesAndNames(ref, false, true);
        }
        PrismObject<ObjectType> prismObject = ref.asReferenceValue().getObject();
        if (prismObject == null) {
            prismObject = WebModelServiceUtils.loadObject(ref, pageBase);
        }
        if (prismObject == null) {
            return getReferencedObjectDisplayNamesAndNames(ref, false, true);
        }
        ObjectType object = prismObject.asObjectable();
        String displayName = null;
        if (object instanceof UserType) {
            displayName = getTranslatedPolyString(((UserType) object).getFullName());
        } else if (object instanceof AbstractRoleType) {
            displayName = getTranslatedPolyString(((AbstractRoleType) object).getDisplayName());
        }
        String name = getTranslatedPolyString(object.getName());
        return StringUtils.isNotEmpty(displayName) ? displayName + " (" + name + ")" : name;
    }

    public static String getReferencedObjectDisplayNamesAndNames(List<ObjectReferenceType> refs, boolean showTypes) {
        return refs.stream()
                .map(ref -> emptyIfNull(getDisplayNameAndName(ref)) + (showTypes ? (" (" + emptyIfNull(getTypeLocalized(ref)) + ")") : ""))
                .collect(Collectors.joining(", "));
    }

    public static String getReferencedObjectDisplayNamesAndNames(Referencable ref, boolean showTypes) {
        return getReferencedObjectDisplayNamesAndNames(ref, showTypes, true);
    }

    public static String getReferencedObjectDisplayNamesAndNames(Referencable ref, boolean showTypes, boolean translate) {
        if (ref == null) {
            return "";
        }
        String name = ref.getTargetName() == null ? "" :
                (translate ? ref.getTargetName().getOrig() : "");
        if (StringUtils.isEmpty(name)) {
            name = ref.getOid();
        }
        StringBuilder sb = new StringBuilder(name);
        if (showTypes) {
            sb.append(" (");
            ObjectTypes type = ObjectTypes.getObjectTypeFromTypeQName(ref.getType());
            ObjectTypeGuiDescriptor descriptor = ObjectTypeGuiDescriptor.getDescriptor(type);
            if (descriptor == null) {
                return null;
            }
            sb.append(emptyIfNull(createStringResourceStatic(descriptor.getLocalizationKey()).getString())).append(")");
        }
        return sb.toString();
    }

    public static <O extends ObjectType> List<O> loadReferencedObjectList(List<ObjectReferenceType> refList, String operation,
            PageAdminLTE pageBase) {
        return loadReferencedObjectList(refList, operation, pageBase.createSimpleTask(operation), pageBase);
    }

    public static <O extends ObjectType> List<O> loadReferencedObjectList(List<ObjectReferenceType> refList, String operation,
            Task task, PageAdminLTE pageBase) {
        List<O> loadedObjectsList = new ArrayList<>();
        if (refList == null) {
            return loadedObjectsList;
        }
        refList.forEach(objectRef -> {
            OperationResult result = new OperationResult(operation);
            PrismObject<O> loadedObject = WebModelServiceUtils.resolveReferenceNoFetch(objectRef, pageBase, task, result);
            if (loadedObject != null) {
                loadedObjectsList.add(loadedObject.asObjectable());
            }
        });
        return loadedObjectsList;
    }

    public static void addAjaxOnUpdateBehavior(WebMarkupContainer container) {
        container.visitChildren((component, visit) -> {
            if (component instanceof InputPanel) {
                addAjaxOnBlurUpdateBehaviorToComponent(((InputPanel) component).getBaseFormComponent());
            } else if (component instanceof FormComponent) {
                addAjaxOnBlurUpdateBehaviorToComponent(component);
            }
        });
    }

    private static void addAjaxOnBlurUpdateBehaviorToComponent(final Component component) {
        component.setOutputMarkupId(true);
        component.add(new AjaxFormComponentUpdatingBehavior("blur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
    }

    public static String resolveLocalizableMessage(LocalizableMessage localizableMessage, Component component) {
        if (localizableMessage == null) {
            return null;
        } else if (localizableMessage instanceof SingleLocalizableMessage) {
            return resolveLocalizableMessage((SingleLocalizableMessage) localizableMessage, component);
        } else if (localizableMessage instanceof LocalizableMessageList) {
            return resolveLocalizableMessage((LocalizableMessageList) localizableMessage, component);
        } else {
            throw new AssertionError("Unsupported localizable message type: " + localizableMessage);
        }
    }

    private static String resolveLocalizableMessage(SingleLocalizableMessage localizableMessage, Component component) {
        if (localizableMessage == null) {
            return null;
        }
        while (localizableMessage.getFallbackLocalizableMessage() != null) {
            if (localizableMessage.getKey() != null) {
                Localizer localizer = Application.get().getResourceSettings().getLocalizer();
                if (localizer.getStringIgnoreSettings(localizableMessage.getKey(), component, null, null) != null) {
                    break; // the key exists => we can use the current localizableMessage
                }
            }
            if (localizableMessage.getFallbackLocalizableMessage() instanceof SingleLocalizableMessage) {
                localizableMessage = (SingleLocalizableMessage) localizableMessage.getFallbackLocalizableMessage();
            } else {
                return resolveLocalizableMessage(localizableMessage.getFallbackLocalizableMessage(), component);
            }
        }
        String key = localizableMessage.getKey() != null ? localizableMessage.getKey() : localizableMessage.getFallbackMessage();
        String defaultValue = localizableMessage.getFallbackMessage();
        if (defaultValue != null) {
            defaultValue = defaultValue.replace('{', '(').replace('}', ')');
        }

        StringResourceModel stringResourceModel = new StringResourceModel(key, component)
                .setModel(new Model<String>())
                .setDefaultValue(defaultValue)
                .setParameters(resolveArguments(localizableMessage.getArgs(), component));
        //System.out.println("GUI: Resolving [" + key + "]: to [" + rv + "]");
        return stringResourceModel.getString();
    }

    // todo deduplicate with similar method in LocalizationServiceImpl
    private static String resolveLocalizableMessage(LocalizableMessageList msgList, Component component) {
        String separator = resolveIfPresent(msgList.getSeparator(), component);
        String prefix = resolveIfPresent(msgList.getPrefix(), component);
        String suffix = resolveIfPresent(msgList.getPostfix(), component);
        return msgList.getMessages().stream()
                .map(m -> resolveLocalizableMessage(m, component))
                .collect(Collectors.joining(separator, prefix, suffix));
    }

    private static String resolveIfPresent(LocalizableMessage msg, Component component) {
        return msg != null ? resolveLocalizableMessage(msg, component) : "";
    }

    private static Object[] resolveArguments(Object[] args, Component component) {
        if (args == null) {
            return null;
        }
        Object[] rv = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof LocalizableMessage) {
                rv[i] = resolveLocalizableMessage(((LocalizableMessage) args[i]), component);
            } else {
                rv[i] = args[i];
            }
        }
        return rv;
    }

    // TODO add other classes; probably move to some enum
    @Nullable
    public static String getAuthorizationActionForTargetClass(Class targetClass) {
        if (UserType.class.equals(targetClass)) {
            return AuthorizationConstants.AUTZ_UI_USER_URL;
        } else if (OrgType.class.equals(targetClass)) {
            return AuthorizationConstants.AUTZ_UI_ORG_UNIT_URL;
        } else if (RoleType.class.equals(targetClass)) {
            return AuthorizationConstants.AUTZ_UI_ROLE_URL;
        } else if (ServiceType.class.equals(targetClass)) {
            return AuthorizationConstants.AUTZ_UI_SERVICE_URL;
        } else if (ResourceType.class.equals(targetClass)) {
            return AuthorizationConstants.AUTZ_UI_RESOURCE_URL;
        } else {
            return null;
        }
    }

    public static void safeResultCleanup(OperationResult result, Trace logger) {
        try {
            result.cleanupResultDeeply();
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(logger, "Couldn't clean up the operation result", t);
        }
    }

    /**
     * Default list view setting should never be needed. Always check setting for specific
     * object type (and archetype).
     */
    @Deprecated
    public static CompiledObjectCollectionView getDefaultGuiObjectListType(PageBase pageBase) {
        return pageBase.getCompiledGuiProfile().getDefaultObjectCollectionView();
    }

    public static DateValidator getRangeValidator(Form<?> form, ItemPath path) {
        DateValidator validator = null;
        List<DateValidator> validators = form.getBehaviors(DateValidator.class);
        if (validators != null) {
            for (DateValidator val : validators) {
                if (path.equivalent(val.getIdentifier())) {
                    validator = val;
                    break;
                }
            }
        }

        if (validator == null) {
            validator = new DateValidator();
            validator.setIdentifier(path);
            form.add(validator);
        }

        return validator;
    }

    public static boolean isItemVisible(List<ItemPath> visibleItems, ItemPath itemToBeFound) {
        return ItemPathCollectionsUtil.containsSubpathOrEquivalent(visibleItems, itemToBeFound);

    }

    public static Class<?> qnameToClass(PrismContext prismContext, QName type) {
        return prismContext.getSchemaRegistry().determineCompileTimeClass(type);
    }

    public static <T extends ObjectType> Class<T> qnameToClass(PrismContext prismContext, QName type, Class<T> returnType) {
        return prismContext.getSchemaRegistry().determineCompileTimeClass(type);
    }

    public static <T extends ObjectType> QName classToQName(PrismContext prismContext, Class<T> clazz) {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz).getTypeName();
    }

    public static <T extends ObjectType> QName classToQName(Class<T> clazz) {
        return PrismContext.get().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(clazz).getTypeName();
    }

    public static <T extends Containerable> QName containerClassToQName(PrismContext prismContext, Class<T> clazz) {
        return prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(clazz).getTypeName();
    }

    public static QName anyClassToQName(PrismContext prismContext, Class<?> clazz) {
        if (ObjectReferenceType.class.equals(clazz)) {
            return ObjectReferenceType.COMPLEX_TYPE;
        }
        if (ObjectType.class.isAssignableFrom(clazz)) {
            return classToQName(prismContext, (Class<ObjectType>) clazz);
        }
        return containerClassToQName(prismContext, (Class<Containerable>) clazz);
    }

    public static Class<? extends Serializable> qnameToAnyClass(PrismContext prismContext, QName qName) {
        if (QNameUtil.match(ObjectReferenceType.COMPLEX_TYPE, qName)) {
            return ObjectReferenceType.class;
        }
        return qnameToContainerClass(prismContext, qName);
    }

    public static <C extends Containerable> Class<C> qnameToContainerClass(PrismContext prismContext, QName type) {
        ComplexTypeDefinition def = prismContext.getSchemaRegistry().findComplexTypeDefinitionByType(type);
        if (def == null) {
            return null;
        }
        return (Class<C>) def.getCompileTimeClass();
    }

    public static boolean canSuspendTask(TaskType task, PageBase pageBase) {
        return pageBase.isAuthorized(ModelAuthorizationAction.SUSPEND_TASK, task.asPrismObject())
                && (isRunnableTask(task) || isRunningTask(task) || isWaitingTask(task));
    }

    public static boolean canResumeTask(TaskType task, PageBase pageBase) {
        return pageBase.isAuthorized(ModelAuthorizationAction.RESUME_TASK, task.asPrismObject())
                && (isSuspendedTask(task) || (isClosedTask(task) && isRecurringTask(task)));
    }

    public static boolean canRunNowTask(TaskType task, PageBase pageBase) {
        return pageBase.isAuthorized(ModelAuthorizationAction.RUN_TASK_IMMEDIATELY, task.asPrismObject())
                && !isRunningTask(task)
                && (isRunnableTask(task) || (isClosedTask(task) && !isRecurringTask(task)));
    }

    /** Checks user-visible state, not the technical (scheduling) state. So RUNNABLE means the task is not actually running. */
    public static boolean isRunnableTask(TaskType task) {
        return task != null && task.getExecutionState() == TaskExecutionStateType.RUNNABLE;
    }

    public static boolean isRunningTask(TaskType task) {
        return task != null && task.getExecutionState() == TaskExecutionStateType.RUNNING;
    }

    /** Checks user-visible state, not the technical (scheduling) state. */
    public static boolean isWaitingTask(TaskType task) {
        return task != null && task.getExecutionState() == TaskExecutionStateType.WAITING;
    }

    /** Checks user-visible state, not the technical (scheduling) state. */
    public static boolean isSuspendedTask(TaskType task) {
        return task != null && task.getExecutionState() == TaskExecutionStateType.SUSPENDED;
    }

    /** Checks user-visible state, not the technical (scheduling) state. But for closed tasks, these are equivalent. */
    public static boolean isClosedTask(TaskType task) {
        return task != null && task.getExecutionState() == TaskExecutionStateType.CLOSED;
    }

    public static boolean isRecurringTask(TaskType task) {
        return task != null && TaskTypeUtil.isTaskRecurring(task);
    }

    public static boolean isReconciliation(TaskType task) {
        return isArchetypedTask(task, SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK);
    }

    public static boolean isRecomputation(TaskType task) {
        return isArchetypedTask(task, SystemObjectsType.ARCHETYPE_RECOMPUTATION_TASK);
    }

    public static boolean isReport(TaskType task) {
        return isArchetypedTask(task, SystemObjectsType.ARCHETYPE_REPORT_TASK)
                || isArchetypedTask(task, SystemObjectsType.ARCHETYPE_REPORT_EXPORT_CLASSIC_TASK)
                || isArchetypedTask(task, SystemObjectsType.ARCHETYPE_REPORT_EXPORT_DISTRIBUTED_TASK)
                || isArchetypedTask(task, SystemObjectsType.ARCHETYPE_REPORT_IMPORT_CLASSIC_TASK);
    }

    public static boolean isImport(TaskType task) {
        return isArchetypedTask(task, SystemObjectsType.ARCHETYPE_IMPORT_TASK);
    }

    public static boolean isLiveSync(TaskType task) {
        return isArchetypedTask(task, SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK);
    }

    private static boolean isArchetypedTask(TaskType taskType, SystemObjectsType archetype) {
        ObjectReferenceType archetypeRef = getArchetypeReference(taskType);
        if (archetypeRef == null) {
            return false;
        }
        return archetype.value().equals(archetypeRef.getOid());
    }

    private static ObjectReferenceType getArchetypeReference(AssignmentHolderType assignmentHolder) {
        ObjectReferenceType archetypeRef = null;
        if (assignmentHolder.getAssignment() == null || assignmentHolder.getAssignment().size() == 0) {
            return null;
        }
        for (AssignmentType assignment : assignmentHolder.getAssignment()) {
            if (isArchetypeAssignment(assignment)) {
                archetypeRef = assignment.getTargetRef();
            }
        }
        return archetypeRef;
    }

    private static String getArchetypeOid(AssignmentHolderType assignmentHolder) {
        ObjectReferenceType archetypeRef = getArchetypeReference(assignmentHolder);
        if (archetypeRef != null) {
            return archetypeRef.getOid();
        }
        return null;
    }

    public static boolean isAuthorized(String... action) {
        if (action == null || action.length == 0) {
            return true;
        }
        List<String> actions = Arrays.asList(action);
        return isAuthorized(actions);
    }

    public static boolean isAuthorized(Collection<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return true;
        }
        Roles roles = new Roles(AuthorizationConstants.AUTZ_ALL_URL);
        roles.add(AuthorizationConstants.AUTZ_GUI_ALL_URL);
        roles.addAll(actions);
        return ((AuthenticatedWebApplication) AuthenticatedWebApplication.get()).hasAnyRole(roles);
    }

    public static boolean isAuthorized(Class<? extends ObjectType> clazz) {
        Class<? extends PageBase> detailsPage = DetailsPageUtil.getObjectDetailsPage(clazz);
        if (detailsPage == null) {
            return false;
        }
        PageDescriptor descriptor = detailsPage.getAnnotation(PageDescriptor.class);
        AuthorizationAction[] actions = descriptor.action();
        List<String> actionUris = new ArrayList<>();
        for (AuthorizationAction action : actions) {
            actionUris.add(action.actionUri());
        }
        return isAuthorized(actionUris);
    }

    // TODO: move to util component
    public static Integer safeLongToInteger(Long l) {
        if (l == null) {
            return null;
        }

        if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
            throw new IllegalArgumentException(
                    "Couldn't transform long '" + l + "' to int, too big or too small.");
        }

        return (int) l.longValue();
    }

    public static List<QName> createSupportedTargetTypeList(QName targetTypeFromDef) {
        if (targetTypeFromDef == null || ObjectType.COMPLEX_TYPE.equals(targetTypeFromDef)) {
            return ObjectTypeListUtil.createObjectTypeList();
        }

        if (AbstractRoleType.COMPLEX_TYPE.equals(targetTypeFromDef)) {
            return ObjectTypeListUtil.createAbstractRoleTypeList();
        }

        if (FocusType.COMPLEX_TYPE.equals(targetTypeFromDef)) {
            return ObjectTypeListUtil.createFocusTypeList();
        }

        if (AssignmentHolderType.COMPLEX_TYPE.equals(targetTypeFromDef)) {
            return ObjectTypeListUtil.createAssignmentHolderTypeQnamesList();
        }

        return Collections.singletonList(targetTypeFromDef);
    }

    /**
     * Takes a collection of object types (classes) that may contain abstract types. Returns a collection
     * that only contain concrete types.
     *
     * @param <O> common supertype for all the types in the collections
     * <p>
     * TODO: move to schema component
     */
    public static <O extends ObjectType> List<QName> resolveObjectTypesToQNames(Collection<Class<? extends O>> types, PrismContext prismContext) {
        if (types == null) {
            return null;
        }
        List<QName> concreteTypes = new ArrayList<>(types.size());
        for (Class<? extends O> type : types) {
            if (type == null || type.equals(ObjectType.class)) {
                MiscUtil.addAllIfNotPresent(concreteTypes, ObjectTypeListUtil.createObjectTypeList());
            } else if (type.equals(FocusType.class)) {
                MiscUtil.addAllIfNotPresent(concreteTypes, ObjectTypeListUtil.createFocusTypeList());
            } else if (type.equals(AbstractRoleType.class)) {
                MiscUtil.addAllIfNotPresent(concreteTypes, ObjectTypeListUtil.createAbstractRoleTypeList());
            } else {
                MiscUtil.addIfNotPresent(concreteTypes, classToQName(prismContext, type));
            }
        }
        return concreteTypes;
    }

    /**
     * @deprecated see {@link com.evolveum.midpoint.gui.api.util.LocalizationUtil#createKeyForEnum(Enum)}
     */
    @Deprecated
    public static <T extends Enum> String createEnumResourceKey(T value) {
        return com.evolveum.midpoint.gui.api.util.LocalizationUtil.createKeyForEnum(value);
    }

    public static <T extends Enum> IModel<String> createLocalizedModelForEnum(T value, Component comp) {
        String key = createEnumResourceKey(value);
        if (value == null) {
            return Model.of("");
        }

        return new StringResourceModel(key, comp, null);
    }

    public static <T extends Enum> IModel<List<T>> createReadonlyModelFromEnum(final Class<T> type) {
        return () -> {
            List<T> list = new ArrayList<>();
            Collections.addAll(list, type.getEnumConstants());

            return list;
        };
    }

    /**
     * Simulates task category using task archetype.
     */
    @Experimental
    public static IModel<String> createSimulatedCategoryNameModel(final Component component,
            final IModel<SelectableBean<TaskType>> taskModel) {
        return () -> {
            PageBase pageBase = getPageBase(component);
            TaskType task = taskModel.getObject().getValue();
            DisplayType display = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(task, pageBase);
            return getTranslatedLabel(display);
        };
    }

    @Experimental
    private static String getTranslatedLabel(DisplayType display) {
        if (display == null) {
            return "";
        }
        if (display.getLabel() != null) {
            return getTranslatedPolyString(display.getLabel());
        } else if (display.getSingularLabel() != null) {
            return getTranslatedPolyString(display.getSingularLabel());
        } else if (display.getPluralLabel() != null) {
            return getTranslatedPolyString(display.getPluralLabel());
        } else {
            return "";
        }
    }

    public static IModel<String> createCategoryNameModel(final IModel<String> categorySymbolModel) {
        return () -> createStringResourceStatic(
                "pageTasks.category." + categorySymbolModel.getObject()).getString();
    }

    public static <E extends Enum> DropDownChoicePanel<E> createEnumPanel(Class<E> clazz, String id,
            final IModel<E> model, final Component component) {
        return createEnumPanel(clazz, id, model, component, true);

    }

    public static <E extends Enum> DropDownChoicePanel<E> createEnumPanel(Class<E> clazz, String id,
            final IModel<E> model, final Component component, boolean allowNull) {
        return createEnumPanel(id, WebComponentUtil.createReadonlyModelFromEnum(clazz),
                model, component, allowNull);
    }

    public static <E extends Enum> DropDownChoicePanel<E> createEnumPanel(String id,
            IModel<List<E>> choicesList, final IModel<E> model, final Component component, boolean allowNull) {
        return createEnumPanel(id, choicesList, model, component, allowNull, null);
    }

    public static <E extends Enum> DropDownChoicePanel<E> createEnumPanel(String id,
            IModel<List<E>> choicesList, final IModel<E> model, final Component component, boolean allowNull, String nullValidDisplayValue) {
        return new DropDownChoicePanel<E>(id, model, choicesList, getEnumChoiceRenderer(component), allowNull) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String getNullValidDisplayValue() {
                return nullValidDisplayValue != null && StringUtils.isNotEmpty(nullValidDisplayValue.trim()) ?
                        nullValidDisplayValue : super.getNullValidDisplayValue();
            }
        };
    }

    public static <E extends Enum<E>> IChoiceRenderer<E> getEnumChoiceRenderer(Component component) {
        return new IChoiceRenderer<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public E getObject(String id, IModel<? extends List<? extends E>> choices) {
                if (StringUtils.isBlank(id)) {
                    return null;
                }
                return choices.getObject().get(Integer.parseInt(id));
            }

            @Override
            public Object getDisplayValue(E object) {
                return WebComponentUtil.createLocalizedModelForEnum(object, component).getObject();
            }

        };
    }

    public static DropDownChoicePanel createEnumPanel(final PrismPropertyDefinition def, String id,
            final IModel model) {
        final Object o = model.getObject();

        final IModel<List<DisplayableValue>> enumModelValues = () -> getDisplayableValues(def.getAllowedValues());

        return new DropDownChoicePanel(id, model, enumModelValues, new DisplayableValueChoiceRenderer(getDisplayableValues(def.getAllowedValues())), true);
    }

    private static <T> List<DisplayableValue<T>> getDisplayableValues(Collection<T> allowedValues) {
        List<DisplayableValue<T>> values = null;
        if (allowedValues != null) {
            values = new ArrayList<>(allowedValues.size());
            for (T v : allowedValues) {
                if (v instanceof DisplayableValue) {
                    values.add(((DisplayableValue) v));
                }
            }
        }
        return values;
    }

    public static String getName(ObjectType object) {
        return getName(object, true);
    }

    public static String getName(ObjectType object, boolean translate) {
        if (object == null) {
            return null;
        }

        return getName(object.asPrismObject(), translate);
    }

    public static String getEffectiveName(ObjectType object, QName propertyName) {
        return getEffectiveName(object, propertyName, true);
    }

    public static String getEffectiveName(ObjectType object, QName propertyName, boolean translate) {
        if (object == null) {
            return null;
        }

        return getEffectiveName(object.asPrismObject(), propertyName, translate);
    }

    public static <O extends ObjectType> String getEffectiveName(PrismObject<O> object, QName propertyName) {
        return getEffectiveName(object, propertyName, true);
    }

    public static <O extends ObjectType> String getEffectiveName(PrismObject<O> object, QName propertyName, boolean translate) {
        if (object == null) {
            return null;
        }

        PrismProperty prop = object.findProperty(ItemName.fromQName(propertyName));

        if (prop != null) {
            Object realValue = prop.getRealValue();
            if (prop.getDefinition().getTypeName().equals(DOMUtil.XSD_STRING)) {
                return (String) realValue;
            } else if (realValue instanceof PolyString) {
                return translate ? com.evolveum.midpoint.gui.api.util.LocalizationUtil.translatePolyString((PolyString) realValue)
                        : WebComponentUtil.getOrigStringFromPoly((PolyString) realValue);
            }
        }

        PolyString name = getValue(object, ObjectType.F_NAME, PolyString.class);
        if (name == null) {
            return null;
        }
        return translate ? com.evolveum.midpoint.gui.api.util.LocalizationUtil.translatePolyString(name)
                : WebComponentUtil.getOrigStringFromPoly(name);
    }

    /**
     * @deprecated See {@link com.evolveum.midpoint.gui.api.util.LocalizationUtil}
     */
    @Deprecated
    public static String getTranslatedPolyString(PolyStringType value) {
        return com.evolveum.midpoint.gui.api.util.LocalizationUtil.translatePolyString(value);
    }

    public static <O extends ObjectType> String getName(ObjectReferenceType ref, PageBase pageBase, String operation) {
        String name = getName(ref);
        if (StringUtils.isEmpty(name) || name.equals(ref.getOid())) {
            String oid = ref.getOid();
            Class<O> type = (Class<O>) ObjectType.class;
            PrismObject<O> object = WebModelServiceUtils.loadObject(type, oid, pageBase,
                    pageBase.createSimpleTask(operation), new OperationResult(operation));
            if (object != null) {
                name = object.getName().getOrig();
            }
        }
        return name;
    }

    public static String getDisplayNameOrName(ObjectReferenceType ref, PageBase pageBase, String operation) {
        return getDisplayNameOrName(ref, pageBase, operation, true);
    }

    public static <O extends ObjectType> String getDisplayNameOrName(ObjectReferenceType ref, PageBase pageBase,
            String operation, boolean translate) {
        String name = getName(ref, translate);
        if (StringUtils.isEmpty(name) || name.equals(ref.getOid())) {
            String oid = ref.getOid();
            Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
                    .createCollection(GetOperationOptions.createNoFetch());
            Class<O> type = ref.getType() != null ? (Class<O>) qnameToClass(pageBase.getPrismContext(), ref.getType()) : (Class<O>) ObjectType.class;
            PrismObject<O> object = WebModelServiceUtils.loadObject(type, oid, pageBase,
                    pageBase.createSimpleTask(operation), new OperationResult(operation));
            if (object != null) {
                name = getDisplayNameOrName(object, true);
            }
        }
        return name;
    }

    public static String getEffectiveName(ObjectReferenceType ref, QName propertyName,
            PageBase pageBase, String operation) {
        return getEffectiveName(ref, propertyName, pageBase, operation, true);
    }

    public static <O extends ObjectType> String getEffectiveName(Referencable ref, QName propertyName,
            PageBase pageBase, String operation, boolean translate) {
        PrismObject<O> object = WebModelServiceUtils.loadObject(ref, pageBase,
                pageBase.createSimpleTask(operation), new OperationResult(operation));

        if (object == null) {
            return "Not Found";
        }

        return getEffectiveName(object, propertyName, translate);

    }

    public static String getName(ObjectReferenceType ref) {
        return getName(ref, true);
    }

    public static String getName(Referencable ref, boolean translate) {
        if (ref == null) {
            return null;
        }
        if (ref.getTargetName() != null) {
            if (translate) {
                return getTranslatedPolyString(ref.getTargetName());
            }
            return getOrigStringFromPoly(ref.getTargetName());
        }
        if (ref.asReferenceValue().getObject() != null) {
            return getName(ref.asReferenceValue().getObject(), translate);
        }
        return ref.getOid();
    }

    public static String getName(PrismObject object) {
        return getName(object, true);
    }

    public static String getName(PrismObject object, boolean translate) {
        if (object == null) {
            return null;
        }
        PolyString name = getValue(object, ObjectType.F_NAME, PolyString.class);
        if (name == null) {
            return null;
        }
        if (translate) {
            return com.evolveum.midpoint.gui.api.util.LocalizationUtil.translatePolyString(name);
        }
        return name.getOrig();
    }

    //todo copied from DashboardServiceImpl; may be move to som util class?
    public static DisplayType combineDisplay(DisplayType display, DisplayType variationDisplay) {
        DisplayType combinedDisplay = new DisplayType();
        if (variationDisplay == null) {
            return display;
        }
        if (display == null) {
            return variationDisplay;
        }
        if (StringUtils.isBlank(variationDisplay.getColor())) {
            combinedDisplay.setColor(display.getColor());
        } else {
            combinedDisplay.setColor(variationDisplay.getColor());
        }
        if (StringUtils.isBlank(variationDisplay.getCssClass())) {
            combinedDisplay.setCssClass(display.getCssClass());
        } else {
            combinedDisplay.setCssClass(variationDisplay.getCssClass());
        }
        if (StringUtils.isBlank(variationDisplay.getCssStyle())) {
            combinedDisplay.setCssStyle(display.getCssStyle());
        } else {
            combinedDisplay.setCssStyle(variationDisplay.getCssStyle());
        }
        if (variationDisplay.getHelp() == null) {
            combinedDisplay.setHelp(display.getHelp());
        } else {
            combinedDisplay.setHelp(variationDisplay.getHelp());
        }
        if (variationDisplay.getLabel() == null) {
            combinedDisplay.setLabel(display.getLabel());
        } else {
            combinedDisplay.setLabel(variationDisplay.getLabel());
        }
        if (variationDisplay.getSingularLabel() == null) {
            combinedDisplay.setSingularLabel(display.getSingularLabel());
        } else {
            combinedDisplay.setSingularLabel(variationDisplay.getSingularLabel());
        }
        if (variationDisplay.getPluralLabel() == null) {
            combinedDisplay.setPluralLabel(display.getPluralLabel());
        } else {
            combinedDisplay.setPluralLabel(variationDisplay.getPluralLabel());
        }
        if (variationDisplay.getTooltip() == null) {
            combinedDisplay.setTooltip(display.getTooltip());
        } else {
            combinedDisplay.setTooltip(variationDisplay.getTooltip());
        }
        if (variationDisplay.getIcon() == null) {
            combinedDisplay.setIcon(display.getIcon());
        } else if (display.getIcon() != null) {
            IconType icon = new IconType();
            if (StringUtils.isBlank(variationDisplay.getIcon().getCssClass())) {
                icon.setCssClass(display.getIcon().getCssClass());
            } else {
                icon.setCssClass(variationDisplay.getIcon().getCssClass());
            }
            if (StringUtils.isBlank(variationDisplay.getIcon().getColor())) {
                icon.setColor(display.getIcon().getColor());
            } else {
                icon.setColor(variationDisplay.getIcon().getColor());
            }
            if (StringUtils.isBlank(variationDisplay.getIcon().getImageUrl())) {
                icon.setImageUrl(display.getIcon().getImageUrl());
            } else {
                icon.setImageUrl(variationDisplay.getIcon().getImageUrl());
            }
            combinedDisplay.setIcon(icon);
        }

        return combinedDisplay;
    }

    public static String getItemDefinitionDisplayNameOrName(ItemDefinition def) {
        if (def == null) {
            return null;
        }
        String name = getItemDefinitionDisplayName(def);
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        return def.getItemName().getLocalPart();
    }

    public static String getItemDefinitionDisplayName(ItemDefinition def) {
        if (def == null) {
            return null;
        }

        if (def.getDisplayName() != null) {
            StringResourceModel nameModel = PageBase.createStringResourceStatic(def.getDisplayName());
            if (StringUtils.isNotEmpty(nameModel.getString())) {
                return nameModel.getString();
            }
        }
        if (def instanceof ResourceAttributeDefinition && StringUtils.isNotEmpty(def.getDisplayName())) {
            return def.getDisplayName();
        }
        return null;
    }

    private static String getAcquisitionDescription(ProvenanceAcquisitionType acquisitionType) {
        if (acquisitionType == null) {
            return null;
        }

        if (acquisitionType.getResourceRef() != null && acquisitionType.getResourceRef().getOid() != null) {
            return getDisplayName(acquisitionType.getResourceRef());
        }

        if (acquisitionType.getOriginRef() != null && acquisitionType.getOriginRef().getOid() != null) {
            return getName(acquisitionType.getOriginRef());
        }

        return GuiChannel.findChannel(acquisitionType.getChannel()).getLocalizationKey(); //TODO NPE
    }

    public static String getDisplayNameOrName(PrismObject object) {
        return getDisplayNameOrName(object, true);
    }

    public static String getDisplayNameOrName(PrismObject object, boolean translate) {
        return getDisplayNameOrName(object, translate, null);
    }

    public static String getDisplayNameOrName(PrismObject object, boolean translate, LocalizationService localizationService) {
        if (object == null) {
            return null;
        }

        String displayName = getDisplayName(object, translate, localizationService);
        return StringUtils.isNotEmpty(displayName) ? displayName : getName(object, translate);
    }

    public static String getDisplayNameOrName(Referencable ref) {
        return getDisplayNameOrName(ref, true);
    }

    public static String getDisplayNameOrName(Referencable ref, boolean translate) {
        if (ref == null) {
            return null;
        }
        String displayName = getDisplayName(ref, translate);
        return StringUtils.isNotEmpty(displayName) ? displayName : getName(ref, translate);
    }

    public static String getDisplayNameAndName(PrismObject<?> object) {
        String displayName = getDisplayName(object);
        String name = getName(object);

        if (StringUtils.isEmpty(displayName)) {
            return name;
        }

        if (StringUtils.isEmpty(name)) {
            return displayName;
        }

        return displayName + " (" + name + ")";
    }

    // <display-name> (<name>) OR simply <name> if there's no display name
    public static String getDisplayNameAndName(ObjectReferenceType ref) {
        return getDisplayNameOrName(ref, true);
    }

    public static String getDisplayName(ObjectReferenceType ref) {
        return getDisplayName(ref, true);
    }

    public static String getDisplayName(Referencable ref, boolean translate) {
        if (translate) {
            return getTranslatedPolyString(ObjectTypeUtil.getDisplayName(ref));
        } else {
            return PolyString.getOrig(ObjectTypeUtil.getDisplayName(ref));
        }
    }

    public static String getDisplayName(PrismObject object) {
        return getDisplayName(object, true);
    }

    public static String getDisplayName(PrismObject object, boolean translate) {
        return getDisplayName(object, translate, null);
    }

    public static String getDisplayName(PrismObject object, boolean translate, LocalizationService localizationService) {
        if (object == null) {
            return "";
        }
        if (translate) {
            if (localizationService == null) {
                return com.evolveum.midpoint.gui.api.util.LocalizationUtil.translatePolyString(ObjectTypeUtil.getDisplayName(object));
            } else {
                return com.evolveum.midpoint.gui.api.util.LocalizationUtil.translatePolyString(PolyString.toPolyString(ObjectTypeUtil.getDisplayName(object)));
            }
        } else {
            return PolyString.getOrig(ObjectTypeUtil.getDisplayName(object));
        }
    }

    @Contract("null -> null; !null -> !null")
    public static PolyStringType createPolyFromOrigString(String str) {
        return createPolyFromOrigString(str, null);
    }

    @Contract("null, _ -> null; !null, _ -> !null")
    public static PolyStringType createPolyFromOrigString(String str, String key) {
        if (str == null) {
            return null;
        }

        PolyStringType poly = new PolyStringType();
        poly.setOrig(str);

        if (StringUtils.isNotEmpty(key)) {
            PolyStringTranslationType translation = new PolyStringTranslationType();
            translation.setKey(key);
            poly.setTranslation(translation);
        }

        return poly;
    }

    public static String getOrigStringFromPoly(PolyString str) {
        return str != null ? str.getOrig() : null;
    }

    public static String getOrigStringFromPoly(PolyStringType str) {
        return str != null ? str.getOrig() : null;
    }

    public static String getOrigStringFromPolyOrEmpty(PolyStringType str) {
        return str != null ? str.getOrig() : "";
    }

    public static <T> T getValue(PrismContainerValue object, QName propertyName, Class<T> type) {
        if (object == null) {
            return null;
        }

        PrismProperty property = object.findProperty(ItemName.fromQName(propertyName));
        if (property == null || property.isEmpty()) {
            return null;
        }

        return (T) property.getRealValue(type);
    }

    public static <T> T getContainerValue(PrismContainerValue object, QName containerName) {
        if (object == null) {
            return null;
        }

        PrismContainer container = object.findContainer(containerName);
        if (container == null || container.isEmpty()) {
            return null;
        }

        PrismContainerValue containerValue = container.getValue();

        if (containerValue == null || containerValue.isEmpty()) {
            return null;
        }

        return (T) containerValue.getValue();
    }

    public static <T> T getValue(PrismContainer object, QName propertyName, Class<T> type) {
        if (object == null) {
            return null;
        }

        return getValue(object.getValue(), propertyName, type);
    }

    public static void encryptCredentials(ObjectDelta delta, boolean encrypt, MidPointApplication app) {
        if (delta == null || delta.isEmpty()) {
            return;
        }

        PropertyDelta propertyDelta = delta.findPropertyDelta(SchemaConstants.PATH_CREDENTIALS_PASSWORD_VALUE);
        if (propertyDelta == null) {
            return;
        }

        Collection<PrismPropertyValue<ProtectedStringType>> values = propertyDelta
                .getValues(ProtectedStringType.class);
        for (PrismPropertyValue<ProtectedStringType> value : values) {
            ProtectedStringType string = value.getValue();
            encryptProtectedString(string, encrypt, app);
        }
    }

    public static void encryptCredentials(ObjectDelta delta, boolean encrypt, ModelServiceLocator serviceLocator) {
        if (delta == null || delta.isEmpty()) {
            return;
        }

        PropertyDelta propertyDelta = delta.findPropertyDelta(SchemaConstants.PATH_CREDENTIALS_PASSWORD_VALUE);
        if (propertyDelta == null) {
            return;
        }

        Collection<PrismPropertyValue<ProtectedStringType>> values = propertyDelta
                .getValues(ProtectedStringType.class);
        for (PrismPropertyValue<ProtectedStringType> value : values) {
            ProtectedStringType string = value.getValue();
            encryptProtectedString(string, encrypt, serviceLocator);
        }
    }

    public static void encryptProtectedString(ProtectedStringType string, boolean encrypt,
            ModelServiceLocator serviceLocator) {
        if (string == null) {
            return;
        }
        Protector protector = serviceLocator.getPrismContext().getDefaultProtector();
        try {
            if (encrypt) {
                if (StringUtils.isEmpty(string.getClearValue())) {
                    return;
                }
                protector.encrypt(string);
            } else {
                if (string.getEncryptedDataType() == null) {
                    return;
                }
                protector.decrypt(string);
            }
        } catch (EncryptionException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't encrypt protected string", ex);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't encrypt/decrypt protected string", e);
        }
    }

    public static void encryptCredentials(PrismObject object, boolean encrypt, MidPointApplication app) {
        PrismContainer password = object.findContainer(SchemaConstants.PATH_CREDENTIALS_PASSWORD);
        if (password == null) {
            return;
        }
        PrismProperty protectedStringProperty = password.findProperty(PasswordType.F_VALUE);
        if (protectedStringProperty == null
                || protectedStringProperty.getRealValue(ProtectedStringType.class) == null) {
            return;
        }

        ProtectedStringType string = (ProtectedStringType) protectedStringProperty
                .getRealValue(ProtectedStringType.class);

        encryptProtectedString(string, encrypt, app);
    }

    public static void encryptCredentials(PrismObject object, boolean encrypt, ModelServiceLocator serviceLocator) {
        PrismContainer password = object.findContainer(SchemaConstants.PATH_CREDENTIALS_PASSWORD);
        if (password == null) {
            return;
        }
        PrismProperty protectedStringProperty = password.findProperty(PasswordType.F_VALUE);
        if (protectedStringProperty == null
                || protectedStringProperty.getRealValue(ProtectedStringType.class) == null) {
            return;
        }

        ProtectedStringType string = (ProtectedStringType) protectedStringProperty
                .getRealValue(ProtectedStringType.class);

        encryptProtectedString(string, encrypt, serviceLocator);
    }

    public static void encryptProtectedString(ProtectedStringType string, boolean encrypt,
            MidPointApplication app) {
        if (string == null) {
            return;
        }
        Protector protector = app.getProtector();
        try {
            if (encrypt) {
                if (StringUtils.isEmpty(string.getClearValue())) {
                    return;
                }
                protector.encrypt(string);
            } else {
                if (string.getEncryptedDataType() == null) {
                    return;
                }
                protector.decrypt(string);
            }
        } catch (EncryptionException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't encrypt protected string", ex);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't encrypt/decrypt protected string", e);
        }
    }

    public static <T extends Selectable> List<T> getSelectedData(Table table) {
        List<T> objects = new ArrayList<>();
        table.getDataTable().visitChildren(SelectableDataTable.SelectableRowItem.class,
                (IVisitor<SelectableDataTable.SelectableRowItem<T>, Void>) (row, visit) -> {
                    if (row.getModelObject().isSelected()) {
                        objects.add(row.getModelObject());
                    }
                });
        return objects;
    }

    public static void clearProviderCache(IDataProvider provider) {
        if (provider == null) {
            return;
        }
        if (provider instanceof BaseSortableDataProvider) {
            ((BaseSortableDataProvider) provider).clearCache();
        }
        if (provider instanceof SelectableBeanContainerDataProvider) {
            ((SelectableBeanContainerDataProvider) provider).clearSelectedObjects();
        }
    }

    public static boolean showResultInPage(OperationResult result) {
        if (result == null) {
            return false;
        }

        return !result.isSuccess() && !result.isHandledError() && !result.isInProgress();
    }

    public static String formatDate(XMLGregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }

        return formatDate(XmlTypeConverter.toDate(calendar));
    }

    public static String formatDate(Date date) {
        return formatDate(null, date);
    }

    public static String formatDate(String format, Date date) {
        if (date == null) {
            return null;
        }

        if (StringUtils.isEmpty(format)) {
            format = "EEEE, d. MMM yyyy HH:mm:ss";
        }
        Locale locale = Session.get().getLocale();
        if (locale == null) {
            locale = Locale.US;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, locale);
        return dateFormat.format(date);
    }

    public static String getLocalizedDatePattern(String style) {
        return DateTimeFormat.patternForStyle(style, getCurrentLocale());
    }

    public static Locale getCurrentLocale() {
        Locale locale = Session.get().getLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return locale;
    }

    public static String getLocalizedDate(XMLGregorianCalendar date, String style) {
        return getLocalizedDate(XmlTypeConverter.toDate(date), style);
    }

    public static String getLocalizedDate(Date date, String style) {
        if (date == null) {
            return null;
        }
        DateConverter converter = new DateConverter(getLocalizedDatePattern(style), true);
        return converter.convertToString(date, getCurrentLocale());
    }

    public static String getShortDateTimeFormattedValue(XMLGregorianCalendar date, PageBase pageBase) {
        return getShortDateTimeFormattedValue(XmlTypeConverter.toDate(date), pageBase);
    }

    public static String getShortDateTimeFormattedValue(Date date, PageBase pageBase) {
        if (date == null) {
            return "";
        }
        String dateTimeFormat = getShortDateTimeFormat(pageBase);
        return getLocalizedDate(date, dateTimeFormat);
    }

    public static String getLongDateTimeFormattedValue(XMLGregorianCalendar date, PageBase pageBase) {
        return getLongDateTimeFormattedValue(XmlTypeConverter.toDate(date), pageBase);
    }

    public static String getLongDateTimeFormattedValue(Date date, PageBase pageBase) {
        if (date == null) {
            return "";
        }
        String longDateTimeFormat = getLongDateTimeFormat(pageBase);
        return getLocalizedDate(date, longDateTimeFormat);
    }

    public static String getShortDateTimeFormat(PageBase pageBase) {
        AdminGuiConfigurationDisplayFormatsType displayFormats = pageBase.getCompiledGuiProfile().getDisplayFormats();
        if (displayFormats == null || StringUtils.isEmpty(displayFormats.getShortDateTimeFormat())) {
            return DateLabelComponent.SHORT_MEDIUM_STYLE;
        } else {
            return displayFormats.getShortDateTimeFormat();
        }
    }

    public static String getLongDateTimeFormat(PageBase pageBase) {
        AdminGuiConfigurationDisplayFormatsType displayFormats = pageBase.getCompiledGuiProfile().getDisplayFormats();
        if (displayFormats == null || StringUtils.isEmpty(displayFormats.getLongDateTimeFormat())) {
            return DateLabelComponent.LONG_MEDIUM_STYLE;
        } else {
            return displayFormats.getLongDateTimeFormat();
        }
    }

    public static Boolean isActivationEnabled(PrismObject object, ItemPath propertyName) {
        Validate.notNull(object);

        PrismContainer<ActivationType> activation = object.findContainer(UserType.F_ACTIVATION); // this is equal to account activation...
        if (activation == null) {
            return null;
        }

        ActivationStatusType status = activation
                .getPropertyRealValue(propertyName, ActivationStatusType.class);
        if (status == null) {
            return null;
        }

        return ActivationStatusType.ENABLED.equals(status);
    }

    public static boolean isSuccessOrHandledError(OperationResult result) {
        if (result == null) {
            return false;
        }

        return result.isSuccess() || result.isHandledError();
    }

    public static boolean isSuccessOrHandledError(OperationResultType resultType) {
        if (resultType == null) {
            return false;
        }
        return resultType.getStatus() == OperationResultStatusType.SUCCESS || resultType.getStatus() == OperationResultStatusType.HANDLED_ERROR;
    }

    public static boolean isSuccessOrHandledErrorOrWarning(OperationResult result) {
        if (result == null) {
            return false;
        }

        return result.isSuccess() || result.isHandledError() || result.isWarning();
    }

    public static boolean isTemplateCategory(@NotNull ResourceType resource) {
        return Boolean.TRUE.equals(resource.isTemplate())
                || Boolean.TRUE.equals(resource.isAbstract());
    }

    public static ObjectFilter evaluateExpressionsInFilter(ObjectFilter objectFilter, VariablesMap variables, OperationResult result, PageBase pageBase) {
        try {
            return ExpressionUtil.evaluateFilterExpressions(
                    objectFilter, variables,
                    MiscSchemaUtil.getExpressionProfile(),
                    pageBase.getExpressionFactory(),
                    "collection filter", pageBase.createSimpleTask(result.getOperation()), result);
        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException |
                ConfigurationException | SecurityViolationException ex) {
            result.recordPartialError("Unable to evaluate filter exception, ", ex);
            pageBase.error("Unable to evaluate filter exception, " + ex.getMessage());
        }
        return objectFilter;
    }

    public static ObjectFilter evaluateExpressionsInFilter(ObjectFilter objectFilter, OperationResult result, PageBase pageBase) {
        VariablesMap variables = new VariablesMap();
        return evaluateExpressionsInFilter(objectFilter, variables, result, pageBase);
    }

    public static void refreshFeedbacks(MarkupContainer component, final AjaxRequestTarget target) {
        component.visitChildren(IFeedback.class, (IVisitor<Component, Void>) (component1, visit) -> target.add(component1));
    }

    /*
     * Methods used for providing prismContext into various objects.
     */
    public static void revive(LoadableModel<?> loadableModel, PrismContext prismContext)
            throws SchemaException {
        if (loadableModel != null) {
            loadableModel.revive(prismContext);
        }
    }

    public static void reviveObject(Object object, PrismContext prismContext) throws SchemaException {
        if (object == null) {
            return;
        }
        if (object instanceof Collection) {
            for (Object item : (Collection) object) {
                reviveObject(item, prismContext);
            }
        } else if (object instanceof Revivable) {
            ((Revivable) object).revive(prismContext);
        }
    }

    // useful for components other than those inheriting from PageBase
    public static PrismContext getPrismContext(Component component) {
        return ((MidPointApplication) component.getApplication()).getPrismContext();
    }

    public static List<String> getChannelList() {
        List<String> channels = new ArrayList<>();

        for (GuiChannel channel : GuiChannel.values()) {
            channels.add(channel.getUri());
        }

        return channels;
    }

    public static List<QName> getMatchingRuleList() {
        List<QName> list = new ArrayList<>();

        list.add(PrismConstants.DEFAULT_MATCHING_RULE_NAME);
        list.add(PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME);
        list.add(PrismConstants.POLY_STRING_STRICT_MATCHING_RULE_NAME);
        list.add(PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME);
        list.add(PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME);
        list.add(PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME);
        list.add(PrismConstants.EXCHANGE_EMAIL_ADDRESSES_MATCHING_RULE_NAME);
        list.add(PrismConstants.UUID_MATCHING_RULE_NAME);
        list.add(PrismConstants.XML_MATCHING_RULE_NAME);

        return list;
    }

    public static String createHumanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) {return bytes + "B";}
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f%sB", bytes / Math.pow(unit, exp), pre);
    }

    public static void setCurrentPage(Table table, ObjectPaging paging) {
        if (table == null) {
            return;
        }

        if (paging == null || paging.getOffset() == null) {
            table.getDataTable().setCurrentPage(0);
            return;
        }

        long itemsPerPage = table.getDataTable().getItemsPerPage();
        long page = ((paging.getOffset() + itemsPerPage) / itemsPerPage) - 1;
        if (page < 0) {
            page = 0;
        }

        // update ordering (sortparam) directly in provider
        if (paging.hasOrdering() && (table.getDataTable().getDataProvider() instanceof SortableDataProvider provider)) {
            ItemPath path = paging.getPrimaryOrderingPath();
            if (path != null) {
                OrderDirection direction = paging.getPrimaryOrderingDirection();
                SortOrder order = direction == OrderDirection.DESCENDING ? SortOrder.DESCENDING : SortOrder.ASCENDING;
                provider.setSort(path.toString(), order);
            }
        }

        table.getDataTable().setCurrentPage(page);
    }

    public static PageBase getPageBase(Component component) {
        return getPage(component, PageBase.class);
    }

    public static <P extends PageAdminLTE> P getPage(Component component, Class<P> pageClass) {
        Page page = component.getPage();
        if (pageClass.isAssignableFrom(page.getClass())) {
            return (P) page;
        } else {
            throw new IllegalStateException("Couldn't determine page base for " + page);
        }
    }

    public static String debugHandler(IRequestHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof RenderPageRequestHandler) {
            return "RenderPageRequestHandler(" + ((RenderPageRequestHandler) handler).getPageClass().getName()
                    + ")";
        } else {
            return handler.toString();
        }
    }

    // todo specify functionality of this method
    public static ItemPath joinPath(ItemPath path1, ItemPath path2) {
        ItemPath path = ItemPath.emptyIfNull(path1);
        ItemPath deltaPath = ItemPath.emptyIfNull(path2);
        List<Object> newPath = new ArrayList<>();

        Object firstDeltaSegment = deltaPath.first();
        for (Object seg : path.getSegments()) {
            if (ItemPath.segmentsEquivalent(seg, firstDeltaSegment)) {
                break;
            }
            newPath.add(seg);
        }
        newPath.addAll(deltaPath.getSegments());

        return ItemPath.create(newPath);
    }

    public static String getPanelIdentifierFromParams(PageParameters pageParameters) {
        StringValue panelIdentifierParam = pageParameters.get(AbstractPageObjectDetails.PARAM_PANEL_ID);
        String panelIdentifier = null;
        if (panelIdentifierParam != null && !panelIdentifierParam.isEmpty()) {
            panelIdentifier = panelIdentifierParam.toString();
        }
        return panelIdentifier;
    }

    @NotNull
    public static TabbedPanel<ITab> createTabPanel(
            String id, final PageBase parentPage, final List<ITab> tabs, TabbedPanel.RightSideItemProvider provider) {
        return createTabPanel(id, parentPage, tabs, provider, null);
    }

    @NotNull
    public static TabbedPanel<ITab> createTabPanel(
            String id, final PageBase parentPage, final List<ITab> tabs, TabbedPanel.RightSideItemProvider provider,
            final String tabChangeParameter) {

        TabbedPanel<ITab> tabPanel = new TabbedPanel<>(id, tabs, provider) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onTabChange(int index) {
                if (tabChangeParameter != null) {
                    parentPage.updateBreadcrumbParameters(tabChangeParameter, index);
                }
            }

            @Override
            protected WebMarkupContainer newLink(String linkId, final int index) {
                return new AjaxSubmitLink(linkId) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        super.onError(target);
                        target.add(parentPage.getFeedbackPanel());
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        super.onSubmit(target);

                        setSelectedTab(index);
                        if (target != null) {
                            target.add(findParent(TabbedPanel.class));
                        }
                        target.add(parentPage.getFeedbackPanel());
                    }

                };
            }
        };
        tabPanel.setOutputMarkupId(true);
        return tabPanel;
    }

    public static Component createHelp(String id) {
        Label helpLabel = new Label(id);
        helpLabel.add(new InfoTooltipBehavior());
        return helpLabel;
    }

    private static void debugDumpComponentTree(StringBuilder sb, Component c, int level) {
        DebugUtil.indentDebugDump(sb, level);
        sb.append(c).append("\n");
        if (c instanceof MarkupContainer) {
            for (Component sub : (MarkupContainer) c) {
                debugDumpComponentTree(sb, sub, level + 1);
            }
        }
    }

    public static String exceptionToString(String message, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(message);
        e.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    public static Behavior visibleIfFalse(final NonEmptyModel<Boolean> model) {
        return new VisibleEnableBehaviour() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !model.getObject();
            }
        };
    }

    public static Behavior enabledIfFalse(final NonEmptyModel<Boolean> model) {
        return new VisibleEnableBehaviour() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return !model.getObject();
            }
        };
    }

    public static String getStringParameter(PageParameters params, String key) {
        if (params == null || params.get(key) == null) {
            return null;
        }

        StringValue value = params.get(key);
        if (StringUtils.isBlank(value.toString())) {
            return null;
        }

        return value.toString();
    }

    public static Integer getIntegerParameter(PageParameters params, String key) {
        if (params == null || params.get(key) == null) {
            return null;
        }

        StringValue value = params.get(key);
        if (!StringUtils.isNumeric(value.toString())) {
            return null;
        }

        return value.toInteger();
    }

    public static boolean getElementVisibility(UserInterfaceElementVisibilityType visibilityType) {
        return getElementVisibility(visibilityType, new ArrayList<>());
    }

    public static boolean getElementVisibility(UserInterfaceElementVisibilityType visibilityType, List<String> requiredAuthorizations) {
        if (UserInterfaceElementVisibilityType.HIDDEN.equals(visibilityType) ||
                UserInterfaceElementVisibilityType.VACANT.equals(visibilityType)) {
            return false;
        }
        if (UserInterfaceElementVisibilityType.VISIBLE.equals(visibilityType)) {
            return true;
        }
        if (UserInterfaceElementVisibilityType.AUTOMATIC.equals(visibilityType)) {
            return WebComponentUtil.isAuthorized(requiredAuthorizations);
        }
        return true;
    }

    public static <AR extends AbstractRoleType> IModel<String> createAbstractRoleConfirmationMessage(String actionName,
            ColumnMenuAction action, MainObjectListPanel<AR> abstractRoleTable, PageBase pageBase) {
        List<AR> selectedRoles = new ArrayList<>();
        if (action.getRowModel() == null) {
            selectedRoles.addAll(abstractRoleTable.getSelectedRealObjects());
        } else {
            selectedRoles.add(((SelectableBeanImpl<AR>) action.getRowModel().getObject()).getValue());
        }
        OperationResult result = new OperationResult("Search Members");
        boolean atLeastOneWithMembers = false;
        for (AR selectedRole : selectedRoles) {
            ObjectQuery query = pageBase.getPrismContext().queryFor(FocusType.class)
                    .item(FocusType.F_ROLE_MEMBERSHIP_REF)// TODO MID-3581
                    .ref(ObjectTypeUtil.createObjectRef(selectedRole, pageBase.getPrismContext()).asReferenceValue())
                    .maxSize(1)
                    .build();
            List<PrismObject<FocusType>> members = WebModelServiceUtils.searchObjects(FocusType.class, query, result, pageBase);
            if (CollectionUtils.isNotEmpty(members)) {
                atLeastOneWithMembers = true;
                break;
            }
        }
        String members = atLeastOneWithMembers ? ".members" : "";
        ObjectTypes objectType = ObjectTypes.getObjectType(abstractRoleTable.getType());
        String propertyKeyPrefix = switch (objectType) {
            case SERVICE -> "pageServices";
            case ROLE -> "pageRoles";
            case ORG -> "pageOrgs";
            default -> "";
        };

        if (action.getRowModel() == null) {
            return pageBase.createStringResource(propertyKeyPrefix + ".message.confirmationMessageForMultipleObject" + members,
                    actionName, abstractRoleTable.getSelectedObjectsCount());
        } else {
            return pageBase.createStringResource(propertyKeyPrefix + ".message.confirmationMessageForSingleObject" + members,
                    actionName, ((ObjectType) ((SelectableBeanImpl) action.getRowModel().getObject()).getValue()).getName());
        }
    }

    /**
     * Returns name of the collection suitable to be displayed in the menu or other labels.
     * E.g. "All tasks", "Active employees".
     */
    public static PolyStringType getCollectionLabel(DisplayType viewDisplayType) {
        if (viewDisplayType == null) {
            return null;
        }
        PolyStringType viewPluralLabel = viewDisplayType.getPluralLabel();
        if (viewPluralLabel != null) {
            return viewPluralLabel;
        }
        return viewDisplayType.getLabel();
    }

    public static void saveObjectLifeCycle(
            @NotNull PrismObject resource,
            String operation,
            AjaxRequestTarget target,
            PageBase pageBase) {
        Task task = pageBase.createSimpleTask(operation);
        OperationResult parentResult = new OperationResult(operation);
        saveLifeCycleStateOnPath(resource, ObjectType.F_LIFECYCLE_STATE, target, task, parentResult, pageBase);
    }

    public static void saveLifeCycleStateOnPath(
            @NotNull PrismObject resource,
            ItemPath pathToProperty,
            AjaxRequestTarget target,
            Task task,
            OperationResult parentResult,
            PageBase pageBase) {
        try {
            Object realValue = resource.findProperty(pathToProperty).getRealValue();
            ObjectDelta<ResourceType> objectDelta = pageBase.getPrismContext().deltaFactory().object()
                    .createModificationReplaceProperty(
                            ResourceType.class, resource.getOid(), pathToProperty, realValue);

            pageBase.getModelService().executeChanges(MiscUtil.createCollection(objectDelta), null, task, parentResult);

        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                | ExpressionEvaluationException | CommunicationException | ConfigurationException
                | PolicyViolationException | SecurityViolationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error changing resource lifecycle state", e);
            parentResult.recordFatalError(
                    pageBase.createStringResource("OperationalButtonsPanel.changeLifecycleState.failed").getString(), e);
        }

        parentResult.computeStatus();
        pageBase.showResult(parentResult, "OperationalButtonsPanel.changeLifecycleState.failed");
        target.add(pageBase.getFeedbackPanel());
    }

    public static List<String> prepareAutoCompleteList(LookupTableType lookupTable, String input) {
        List<String> values = new ArrayList<>();

        if (lookupTable == null) {
            return values;
        }

        List<LookupTableRowType> rows = lookupTable.getRow();

        for (LookupTableRowType row : rows) {
            String value = com.evolveum.midpoint.gui.api.util.LocalizationUtil.translateLookupTableRowLabel(row);
            if (input == null || input.isEmpty()) {
                values.add(value);
            } else if (value != null && value.toLowerCase().contains(input.toLowerCase())) {
                values.add(value);
            }
        }

        return values;
    }

    public static DropDownChoice<Boolean> createTriStateCombo(String id, IModel<Boolean> model) {
        final IChoiceRenderer<Boolean> renderer = new IChoiceRenderer<>() {

            @Override
            public Boolean getObject(String id, IModel<? extends List<? extends Boolean>> choices) {
                return id != null ? choices.getObject().get(Integer.parseInt(id)) : null;
            }

            @Override
            public String getDisplayValue(Boolean object) {
                return WebComponentUtil.createLocalizedModelForBoolean(object).getObject();
            }
        };

        DropDownChoice<Boolean> dropDown = new DropDownChoice<>(id, model, createChoices(), renderer) {

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                StringResourceModel model = PageBase.createStringResourceStatic(KEY_BOOLEAN_NULL);

                return model.getString();
            }
        };
        dropDown.setNullValid(true);

        return dropDown;
    }

    public static boolean isAllNulls(Iterable<?> array) {
        return StreamSupport.stream(array.spliterator(), true).allMatch(Objects::isNull);
    }

    private static IModel<List<Boolean>> createChoices() {
        return () -> {
            List<Boolean> list = new ArrayList<>();
            list.add(null);
            list.add(Boolean.TRUE);
            list.add(Boolean.FALSE);

            return list;
        };
    }

    public static Class<?> getPreviousPageClass(PageBase parentPage) {
        List<Breadcrumb> breadcrumbs = parentPage.getBreadcrumbs();
        if (breadcrumbs == null || breadcrumbs.size() < 2) {
            return null;
        }
        Breadcrumb previousBreadcrumb = breadcrumbs.get(breadcrumbs.size() - 2);
        Class<?> page = null;
        if (previousBreadcrumb != null) {
            page = previousBreadcrumb.getPageClass();
        }
        return page;
    }

    @NotNull
    public static List<InlineMenuItem> createMenuItemsFromActions(@NotNull List<GuiActionType> actions, String operation,
            PageBase pageBase, @NotNull Supplier<Collection<? extends ObjectType>> selectedObjectsSupplier) {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        actions.forEach(action -> {
            if (action.getTaskTemplateRef() == null) {
                return;
            }
            String templateOid = action.getTaskTemplateRef().getOid();
            if (StringUtils.isEmpty(templateOid)) {
                return;
            }

            IModel<String> label = () -> {
                DisplayType display = action.getDisplay();
                if (display == null || display.getLabel() == null) {
                    return action.getIdentifier();
                }

                return com.evolveum.midpoint.gui.api.util.LocalizationUtil.translatePolyString(display.getLabel());
            };
            menuItems.add(new InlineMenuItem(label) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<SelectableBean<ObjectType>>() {
                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            pageBase.taskAwareExecutor(target, operation)
                                    .runVoid((task, result) -> {
                                        Collection<String> oids;
                                        if (getRowModel() != null) {
                                            oids = Collections.singletonList(getRowModel().getObject().getValue().getOid());
                                        } else {
                                            oids = CollectionUtils.emptyIfNull(selectedObjectsSupplier.get())
                                                    .stream()
                                                    .map(AbstractMutableObjectable::getOid)
                                                    .filter(Objects::nonNull)
                                                    .collect(Collectors.toSet());
                                        }
                                        if (!oids.isEmpty()) {
                                            pageBase.getModelInteractionService().submitTaskFromTemplate(
                                                    templateOid,
                                                    ActivityCustomization.forOids(oids),
                                                    task, result);
                                        } else {
                                            result.recordWarning(
                                                    pageBase.createStringResource(
                                                                    "WebComponentUtil.message.createMenuItemsFromActions.warning")
                                                            .getString());
                                        }
                                    });
                        }
                    };
                }
            });
        });
        return menuItems;
    }

    public static ObjectFilter getAssignableRolesFilter(PrismObject<? extends FocusType> focusObject, Class<? extends AbstractRoleType> type, AssignmentOrder assignmentOrder,
            OperationResult result, Task task, PageBase pageBase) {
        return getAssignableRolesFilter(focusObject, type, null, assignmentOrder, result, task, pageBase);
    }

    public static ObjectFilter getAssignableRolesFilter(PrismObject<? extends FocusType> focusObject, Class<? extends AbstractRoleType> type,
            QName relation, AssignmentOrder assignmentOrder,
            OperationResult result, Task task, PageBase pageBase) {
        ObjectFilter filter = null;
        LOGGER.debug("Loading objects which can be assigned");
        try {
            ModelInteractionService mis = pageBase.getModelInteractionService();
            RoleSelectionSpecification roleSpec =
                    mis.getAssignableRoleSpecification(focusObject, type, assignmentOrder.getOrder(), task, result);
            filter = roleSpec.getGlobalFilter();
            if (relation != null) {
                ObjectFilter relationFilter = roleSpec.getRelationFilter(relation);
                if (filter == null) {
                    return relationFilter;
                } else if (relationFilter == null) {
                    return filter;
                } else {
                    return pageBase.getPrismContext().queryFactory().createOr(filter, relationFilter);
                }
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load available roles", ex);
            result.recordFatalError(pageBase.createStringResource("WebComponentUtil.message.getAssignableRolesFilter.fatalError").getString(), ex);
        } finally {
            result.recomputeStatus();
        }
        if (!result.isSuccess() && !result.isHandledError()) {
            pageBase.showResult(result);
        }
        return filter;
    }

    public static ObjectFilter getAccessibleForAssignmentObjectsFilter(OperationResult result, Task task, PageBase pageBase) {
        ObjectFilter filter = null;
        LOGGER.debug("Loading users which are allowed to be an object during assign operation.");
        try {
            ModelInteractionService mis = pageBase.getModelInteractionService();
            filter = mis.getAccessibleForAssignmentObjectsFilter(UserType.class, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load users which are allowed to be an object during assign operation.", ex);
            result.recordFatalError(pageBase.createStringResource(
                    "WebComponentUtil.message.getAccessibleForAssignmentObjectsFilter.fatalError").getString(), ex);
        } finally {
            result.recomputeStatus();
        }
        if (!result.isSuccess() && !result.isHandledError()) {
            pageBase.showResult(result);
        }
        return filter;
    }

    /**
     * This seems to be just invalid piece of JS code.
     * die() function not really defined, and there's no use of about="XXX" attribute in html for jquery to find.
     */
    @Deprecated
    public static Behavior getSubmitOnEnterKeyDownBehavior(String submitButtonAboutAttribute) {
        return new Behavior() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void bind(Component component) {
                super.bind(component);

                component.add(AttributeModifier.replace("onkeydown",
                        Model.of("if(event.keyCode == 13) {"
                                + "event.die();"
                                + "$('[about=\"" + submitButtonAboutAttribute + "\"]').click();"
                                + "}")));
            }
        };
    }

    public static Behavior getBlurOnEnterKeyDownBehavior() {
        return new Behavior() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void bind(Component component) {
                super.bind(component);
                component.add(AttributeModifier.replace("onkeydown", Model.of("if(event.keyCode == 13) {event.target.blur();}")));
            }
        };
    }

    public static Behavior preventSubmitOnEnterKeyDownBehavior() {
        return new Behavior() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void bind(Component component) {
                super.bind(component);
                component.add(AttributeModifier.replace("onkeydown", Model.of("if(event.keyCode == 13) {event.preventDefault();}")));
            }
        };
    }

    public static List<QName> getAssignableRelationsList(PrismObject<? extends FocusType> focusObject, Class<? extends AbstractRoleType> type,
            AssignmentOrder assignmentOrder,
            OperationResult result, Task task, PageBase pageBase) {
        List<QName> relationsList = null;
        LOGGER.debug("Loading assignable relations list");
        try {
            ModelInteractionService mis = pageBase.getModelInteractionService();
            RoleSelectionSpecification roleSpec =
                    mis.getAssignableRoleSpecification(focusObject, type, assignmentOrder.getOrder(), task, result);
            relationsList = roleSpec != null && roleSpec.getGlobalFilter() == null && roleSpec.getRelationMap() != null ?
                    new ArrayList<>(roleSpec.getRelationMap().keySet()) : null;
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load assignable relations list", ex);
            result.recordFatalError(pageBase.createStringResource("WebComponentUtil.message.getAssignableRelationsList.fatalError").getString(), ex);
        } finally {
            result.recomputeStatus();
        }
        if (!result.isSuccess() && !result.isHandledError()) {
            pageBase.showResult(result);
        }
        return relationsList;
    }

    public static String getReferenceObjectTextValue(ObjectReferenceType ref, PrismReferenceDefinition referenceDef, PageBase pageBase) {
        if (ref == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (ref.getObject() != null) {
            sb.append(com.evolveum.midpoint.gui.api.util.LocalizationUtil.translatePolyString(ref.getObject().getName()));
        } else if (ref.getTargetName() != null && StringUtils.isNotEmpty(ref.getTargetName().getOrig())) {
            sb.append(com.evolveum.midpoint.gui.api.util.LocalizationUtil.translatePolyString(ref.getTargetName()));
        }
        if (StringUtils.isNotEmpty(ref.getOid())) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(pageBase.createStringResource("ReferencePopupPanel.oid").getString());
            sb.append(ref.getOid());
        }
        if (ref.getRelation() != null) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(pageBase.createStringResource("ReferencePopupPanel.relation").getString());
            sb.append(ref.getRelation().getLocalPart());
        }
        if (ref.getType() != null) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            if (referenceDef != null) {
                List<QName> supportedTypes = createSupportedTargetTypeList(referenceDef.getTargetTypeName());
                if (supportedTypes != null && supportedTypes.size() > 1) {
                    sb.append(ref.getType().getLocalPart());
                }
            } else {
                sb.append(ref.getType().getLocalPart());
            }
        }
        return sb.toString();
    }

    public static String formatDurationWordsForLocal(long durationMillis, boolean suppressLeadingZeroElements,
            boolean suppressTrailingZeroElements) {
        return formatDurationWordsForLocal(durationMillis, suppressLeadingZeroElements, suppressTrailingZeroElements, null);
    }

    /**
     * uses the one without "comp" parameter
     *
     * @param durationMillis
     * @param suppressLeadingZeroElements
     * @param suppressTrailingZeroElements
     * @param comp
     * @return
     */
    @Deprecated
    public static String formatDurationWordsForLocal(long durationMillis, boolean suppressLeadingZeroElements,
            boolean suppressTrailingZeroElements, Component comp) {

        Localizer localizer = comp != null ? comp.getLocalizer() : Application.get().getResourceSettings().getLocalizer();

        String duration = formatDuration(durationMillis, suppressLeadingZeroElements, suppressTrailingZeroElements);

        duration = StringUtils.replaceOnce(duration, "seconds", localizer.getString("WebComponentUtil.formatDurationWordsForLocal.seconds", comp));
        duration = StringUtils.replaceOnce(duration, "minutes", localizer.getString("WebComponentUtil.formatDurationWordsForLocal.minutes", comp));
        duration = StringUtils.replaceOnce(duration, "hours", localizer.getString("WebComponentUtil.formatDurationWordsForLocal.hours", comp));
        duration = StringUtils.replaceOnce(duration, "days", localizer.getString("WebComponentUtil.formatDurationWordsForLocal.days", comp));
        duration = StringUtils.replaceOnce(duration, "second", localizer.getString("WebComponentUtil.formatDurationWordsForLocal.second", comp));
        duration = StringUtils.replaceOnce(duration, "minute", localizer.getString("WebComponentUtil.formatDurationWordsForLocal.minute", comp));
        duration = StringUtils.replaceOnce(duration, "hour", localizer.getString("WebComponentUtil.formatDurationWordsForLocal.hour", comp));
        duration = StringUtils.replaceOnce(duration, "day", localizer.getString("WebComponentUtil.formatDurationWordsForLocal.day", comp));

        return duration;
    }

    private static String formatDuration(long durationMillis, boolean suppressLeadingZeroElements,
            boolean suppressTrailingZeroElements) {
        String duration = DurationFormatUtils.formatDuration(durationMillis, "d' days 'H' hours 'm' minutes 's' seconds 'S' miliseconds'");
        String tmp;
        if (suppressLeadingZeroElements) {
            duration = " " + duration;
            tmp = StringUtils.replaceOnce(duration, " 0 days", "");
            if (tmp.length() != duration.length()) {
                duration = tmp;
                tmp = StringUtils.replaceOnce(tmp, " 0 hours", "");
                if (tmp.length() != duration.length()) {
                    duration = tmp;
                    tmp = StringUtils.replaceOnce(tmp, " 0 minutes", "");
                    if (tmp.length() != duration.length()) {
                        tmp = StringUtils.replaceOnce(tmp, " 0 seconds", "");
                        duration = tmp;
                        if (tmp.length() != tmp.length()) {
                            duration = StringUtils.replaceOnce(tmp, " 0 miliseconds", "");
                        }
                    }

                }
            }

            if (!duration.isEmpty()) {
                duration = duration.substring(1);
            }
        }

        if (suppressTrailingZeroElements) {
            tmp = StringUtils.replaceOnce(duration, " 000 miliseconds", "");
            if (tmp.length() != duration.length()) {
                duration = tmp;
                tmp = StringUtils.replaceOnce(duration, " 0 seconds", "");
                if (tmp.length() != duration.length()) {
                    duration = tmp;
                    tmp = StringUtils.replaceOnce(tmp, " 0 minutes", "");
                    if (tmp.length() != duration.length()) {
                        duration = tmp;
                        tmp = StringUtils.replaceOnce(tmp, " 0 hours", "");
                        if (tmp.length() != duration.length()) {
                            duration = StringUtils.replaceOnce(tmp, " 0 days", "");
                        }
                    }
                }
            }
        }

        duration = " " + duration;
        duration = StringUtils.replaceOnce(duration, " 1 miliseconds", " 1 milisecond");
        duration = StringUtils.replaceOnce(duration, " 1 seconds", " 1 second");
        duration = StringUtils.replaceOnce(duration, " 1 minutes", " 1 minute");
        duration = StringUtils.replaceOnce(duration, " 1 hours", " 1 hour");
        duration = StringUtils.replaceOnce(duration, " 1 days", " 1 day");
        duration = duration.trim();

        //not a perfect solution, but w.g we don't want to show 012ms but rather 12ms, won't work for 001ms.. this will be still 01ms :)
        if (duration.startsWith("0")) {
            duration = duration.substring(1);
        }
        return duration;
    }

    public static <O extends ObjectType> ArchetypePolicyType getArchetypeSpecification(PrismObject<O> object, ModelServiceLocator locator) {
        if (object == null) {
            return null;
        }

        OperationResult result = new OperationResult("loadArchetypeSpecificationFor" + getName(object));

        if (!object.canRepresent(AssignmentHolderType.class)) {
            return null;
        }
        ArchetypePolicyType spec = null;
        try {
            if (ArchetypeType.class.equals(object.getCompileTimeClass())) {
                spec = locator.getModelInteractionService().mergeArchetypePolicies((PrismObject<ArchetypeType>) object, result);
            } else {
                spec = locator.getModelInteractionService().determineArchetypePolicy((PrismObject<? extends AssignmentHolderType>) object, result);
            }
        } catch (SchemaException | ConfigurationException ex) {
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error("Cannot load ArchetypeInteractionSpecification for object {}: {}", object, ex.getLocalizedMessage());
        }
        return spec;
    }

    //TODO unify createAccountIcon with createCompositeIconForObject
    public static <O extends ObjectType> CompositedIcon createCompositeIconForObject(O obj, OperationResult result, PageBase pageBase) {
        if (obj instanceof ShadowType) {
            return createAccountIcon((ShadowType) obj, pageBase, true);
        }

        DisplayType basicIconDisplayType = GuiDisplayTypeUtil.getDisplayTypeForObject(obj, result, pageBase);
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder();
        if (basicIconDisplayType == null) {
            return new CompositedIconBuilder().build();
        }
        //TODO trigger

        String iconColor = GuiDisplayTypeUtil.getIconColor(basicIconDisplayType);

        CompositedIconBuilder builder = iconBuilder.setBasicIcon(
                        GuiDisplayTypeUtil.getIconCssClass(basicIconDisplayType), IconCssStyle.IN_ROW_STYLE)
                .appendColorHtmlValue(StringUtils.isNotEmpty(iconColor) ? iconColor : "");

        if (obj instanceof ResourceType) {
            return builder.build();
        }

        IconType lifecycleStateIcon = IconAndStylesUtil.getIconForLifecycleState(obj);
        IconType activationStatusIcon = IconAndStylesUtil.getIconForActivationStatus(obj);

        StringBuilder title = new StringBuilder(getOrigStringFromPolyOrEmpty(basicIconDisplayType.getTooltip()));
        if (lifecycleStateIcon != null) {
            builder.appendLayerIcon(lifecycleStateIcon, IconCssStyle.BOTTOM_LEFT_FOR_COLUMN_STYLE);  // TODO: do we really want to expect not null icon for layerIcon?
            appendLifecycleState(title, lifecycleStateIcon, obj, pageBase);
        }
        if (activationStatusIcon != null) {
            builder.appendLayerIcon(activationStatusIcon, IconCssStyle.BOTTOM_RIGHT_FOR_COLUMN_STYLE);
            appendActivationStatus(title, activationStatusIcon, obj, pageBase);
        }

        addMultiNodeTaskInformation(obj, builder);

        if (StringUtils.isNotEmpty(title.toString())) {
            builder.setTitle(title.toString());
        }
        return builder.build();
    }

    private static <O extends ObjectType> void addMultiNodeTaskInformation(O obj, CompositedIconBuilder builder) {
        if (obj instanceof TaskType && ActivityStateUtil.isManageableTreeRoot((TaskType) obj)) {
            IconType icon = new IconType();
            icon.setCssClass(GuiStyleConstants.CLASS_OBJECT_NODE_ICON_COLORED);
            builder.appendLayerIcon(icon, IconCssStyle.BOTTOM_RIGHT_FOR_COLUMN_STYLE);
        }
    }

    public static CompositedIcon createAccountIcon(ShadowType shadow, PageBase pageBase, boolean isColumn) {
        String iconCssClass = IconAndStylesUtil.createShadowIcon(shadow.asPrismObject());
        CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setBasicIcon(iconCssClass, IconCssStyle.BOTTOM_RIGHT_FOR_COLUMN_STYLE);

        String title = createTriggerTooltip(shadow.getTrigger(), pageBase);
        appendTriggerInfo(title, isColumn, builder);

        ResourceType resource = ProvisioningObjectsUtil.resolveResource(shadow, isColumn, pageBase);

        title = appendMaintenanceInfoAndUpdateTitle(title, resource, isColumn, pageBase, builder);

        if (ShadowUtil.isDead(shadow)) {
            appendDeadInfo(title, isColumn, pageBase, builder);
            return builder.build();
        }

        if (ProvisioningObjectsUtil.activationNotSupported(resource)) {
            appendNotSupportedActivation(title, isColumn, pageBase, builder);
            return builder.build();
        }

        ActivationType activation = shadow.getActivation();
        if (activation == null) {
            builder.setTitle(pageBase.createStringResource("accountIcon.activation.unknown").getString()
                    + (StringUtils.isNotBlank(title) ? ("\n" + title) : ""));
            appendUndefinedIcon(builder);
            return builder.build();
        }

        if (isShadowLocked(activation, pageBase)) {
            appendLockedTitle(title, isColumn, pageBase, builder);
            return builder.build();
        }

        ActivationStatusType value = activation.getAdministrativeStatus();
        if (value == null) {
            builder.setTitle(pageBase.createStringResource("accountIcon.activation.unknown").getString()
                    + (StringUtils.isNotBlank(title) ? ("\n" + title) : ""));
            appendUndefinedIcon(builder);
            return builder.build();
        }
        builder.setTitle(pageBase.createStringResource("ActivationStatusType." + value).getString()
                + (StringUtils.isNotBlank(title) ? ("\n" + title) : ""));

        switch (value) {
            case DISABLED -> {
                if (isColumn) {
                    appendIcon(builder, "fe fe-no-line " + GuiStyleConstants.RED_COLOR, IconCssStyle.CENTER_FOR_COLUMN_STYLE);
                } else {
                    appendIcon(builder, "fe fe-no-line " + GuiStyleConstants.RED_COLOR, IconCssStyle.CENTER_STYLE);
                }
                return builder.build();
            }
            case ARCHIVED -> {
                if (isColumn) {
                    appendIcon(builder, "fa fa-archive " + GuiStyleConstants.RED_COLOR, IconCssStyle.BOTTOM_RIGHT_FOR_COLUMN_STYLE);
                } else {
                    appendIcon(builder, "fa fa-archive " + GuiStyleConstants.RED_COLOR, IconCssStyle.BOTTOM_RIGHT_STYLE);
                }
                return builder.build();
            }
        }

        return builder.build();
    }

    private static void appendLockedTitle(String title, boolean isColumn, PageBase pageBase, CompositedIconBuilder builder) {
        IconType icon = new IconType();
        icon.setCssClass("fa fa-lock " + GuiStyleConstants.RED_COLOR);
        if (isColumn) {
            builder.appendLayerIcon(icon, IconCssStyle.BOTTOM_RIGHT_FOR_COLUMN_STYLE);
        } else {
            builder.appendLayerIcon(icon, IconCssStyle.BOTTOM_RIGHT_STYLE);
        }
        builder.setTitle(pageBase.createStringResource("LockoutStatusType.LOCKED").getString()
                + (StringUtils.isNotBlank(title) ? ("\n" + title) : ""));
    }

    private static boolean isShadowLocked(ActivationType activation, PageBase pageBase) {
        LockoutStatusType lockoutStatus = activation.getLockoutStatus();
        XMLGregorianCalendar lockoutExpirationTimestamp = activation.getLockoutExpirationTimestamp();
        return (LockoutStatusType.LOCKED == lockoutStatus)
                || (lockoutExpirationTimestamp != null && pageBase.getClock().isPast((lockoutExpirationTimestamp)));
    }

    private static void appendNotSupportedActivation(String title, boolean isColumn, PageBase pageBase, CompositedIconBuilder builder) {
        IconType icon = new IconType();
        icon.setCssClass("fa fa-ban " + GuiStyleConstants.RED_COLOR);
        if (isColumn) {
            builder.appendLayerIcon(icon, IconCssStyle.BOTTOM_RIGHT_FOR_COLUMN_STYLE);
        } else {
            builder.appendLayerIcon(icon, IconCssStyle.BOTTOM_RIGHT_STYLE);
        }
        builder.setTitle(pageBase.createStringResource("accountIcon.activation.notSupported").getString()
                + (StringUtils.isNotBlank(title) ? ("\n" + title) : ""));
    }

    private static void appendDeadInfo(String title, boolean isColumn, PageBase pageBase, CompositedIconBuilder builder) {
        IconType icon = new IconType();
        icon.setCssClass("fa fa-times-circle " + GuiStyleConstants.RED_COLOR);
        if (isColumn) {
            builder.appendLayerIcon(icon, IconCssStyle.BOTTOM_RIGHT_FOR_COLUMN_STYLE);
        } else {
            builder.appendLayerIcon(icon, IconCssStyle.BOTTOM_RIGHT_MAX_ICON_STYLE);
        }
        builder.setTitle(pageBase.createStringResource("FocusProjectionsTabPanel.deadShadow").getString()
                + (StringUtils.isNotBlank(title) ? ("\n" + title) : ""));
    }

    private static String appendMaintenanceInfoAndUpdateTitle(String title, ResourceType resource, boolean isColumn, PageBase pageBase, CompositedIconBuilder builder) {
        if (resource == null || !ResourceTypeUtil.isInMaintenance(resource)) {
            return title;
        }

        IconType icon = new IconType();
        icon.setCssClass("fa fa-wrench " + GuiStyleConstants.CLASS_ICON_STYLE_MAINTENANCE);
        if (isColumn) {
            builder.appendLayerIcon(icon, IconCssStyle.BOTTOM_LEFT_FOR_COLUMN_STYLE);
        } else {
            builder.appendLayerIcon(icon, IconCssStyle.BOTTOM_LEFT_STYLE);
        }
        if (StringUtils.isNotBlank(title)) {
            return title + "\n " + pageBase.createStringResource("ChangePasswordPanel.legendMessage.maintenance").getString();
        } else {
            return pageBase.createStringResource("ChangePasswordPanel.legendMessage.maintenance").getString();
        }
    }

    private static void appendTriggerInfo(String title, boolean isColumn, CompositedIconBuilder builder) {
        if (StringUtils.isNotBlank(title)) {
            IconType icon = new IconType();
            icon.setCssClass(GuiStyleConstants.ICON_FAR_CLOCK + " " + GuiStyleConstants.BLUE_COLOR);
            if (isColumn) {
                builder.appendLayerIcon(icon, IconCssStyle.TOP_RIGHT_FOR_COLUMN_STYLE);
            } else {
                builder.appendLayerIcon(icon, IconCssStyle.TOP_RIGHT_MAX_ICON_STYLE);
            }
        }
    }

    private static void appendUndefinedIcon(CompositedIconBuilder builder) {
        appendIcon(builder, "fa fa-question " + GuiStyleConstants.RED_COLOR, IconCssStyle.BOTTOM_RIGHT_FOR_COLUMN_STYLE);

    }

    private static void appendIcon(CompositedIconBuilder builder, String cssClass, LayeredIconCssStyle iconCssStyle) {
        IconType icon = new IconType();
        icon.setCssClass(cssClass);
        builder.appendLayerIcon(icon, iconCssStyle);
    }

    private static String createTriggerTooltip(List<TriggerType> triggers, PageBase pageBase) {
        if (CollectionUtils.isEmpty(triggers)) {
            return null;
        }

        List<String> triggerTooltips = new ArrayList<>();
        for (TriggerType trigger : triggers) {
            XMLGregorianCalendar time = trigger.getTimestamp();

            if (time == null) {
                triggerTooltips.add(pageBase.getString("CheckTableHeader.triggerUnknownTime"));
            } else {
                triggerTooltips.add(pageBase.getString("CheckTableHeader.triggerPlanned", WebComponentUtil.formatDate(time)));
            }
        }

        return StringUtils.join(triggerTooltips, '\n');
    }

    private static <O extends ObjectType> void appendLifecycleState(StringBuilder title, IconType lifecycleStateIcon, O obj, PageBase pageBase) {
        if (StringUtils.isEmpty(lifecycleStateIcon.getCssClass())) {
            return;
        }

        if (title.length() > 0) {
            title.append("\n");
        }
        title.append(pageBase.createStringResource("ObjectType.lifecycleState.title", obj.getLifecycleState()).getString());
    }

    private static <O extends ObjectType> void appendActivationStatus(StringBuilder title, IconType activationStatusIcon, O obj, PageBase pageBase) {
        ActivationType activation = getActivation(obj);
        if (StringUtils.isEmpty(activationStatusIcon.getCssClass()) || activation == null) {
            return;
        }

        if (!title.isEmpty()) {
            title.append("\n");
        }
        String lockedStatus = LockoutStatusType.LOCKED == activation.getLockoutStatus() ? activation.getLockoutStatus().value() : "";
        String effectiveStatus = activation.getEffectiveStatus() != null ? activation.getEffectiveStatus().value() : "";

        if (!effectiveStatus.isEmpty()) {
            String localeEffectiveStatus = pageBase
                    .createStringResource("ActivationDescriptionHandler.ActivationStatusType."
                            + effectiveStatus.toUpperCase())
                    .getString();

            if (localeEffectiveStatus != null) {
                effectiveStatus = localeEffectiveStatus;
            }
        }

        title.append(pageBase.createStringResource("CapabilitiesType.activationStatus").getString())
                .append(": ")
                .append(StringUtils.isNotEmpty(lockedStatus) ? lockedStatus : effectiveStatus);

    }

    private static <O extends ObjectType> ActivationType getActivation(O obj) {
        if (obj instanceof FocusType) {
            return ((FocusType) obj).getActivation();
        }

        if (obj instanceof ShadowType) {
            return ((ShadowType) obj).getActivation();
        }

        return null;

    }

    @Contract("_,_,_,null -> null")
    public static CompositedIconBuilder getAssignmentRelationIconBuilder(PageBase pageBase, AssignmentObjectRelation relationSpec,
            IconType relationIcon, IconType actionButtonIcon) {
        CompositedIconBuilder builder = new CompositedIconBuilder();
        if (relationSpec == null) {
            if (actionButtonIcon == null) {
                return null;
            }
            builder.setBasicIcon(actionButtonIcon, IconCssStyle.IN_ROW_STYLE)
                    .appendColorHtmlValue(GuiDisplayTypeUtil.removeStringAfterSemicolon(actionButtonIcon.getColor()));
            return builder;
        }
        DisplayType objectTypeDisplay = null;
        if (CollectionUtils.isNotEmpty(relationSpec.getArchetypeRefs())) {
            try {
                String operation = pageBase.getClass().getSimpleName() + "." + "loadArchetypeObject";
                ArchetypeType archetype = pageBase.getModelObjectResolver().resolve(relationSpec.getArchetypeRefs().get(0), ArchetypeType.class,
                        null, null, pageBase.createSimpleTask(operation),
                        new OperationResult(operation));
                if (archetype.getArchetypePolicy() != null) {
                    objectTypeDisplay = archetype.getArchetypePolicy().getDisplay();
                }
            } catch (Exception ex) {
                LOGGER.error("Couldn't load archetype object, " + ex.getLocalizedMessage());
            }
        }
        if (objectTypeDisplay == null) {
            objectTypeDisplay = new DisplayType();
        }
        if (objectTypeDisplay.getIcon() == null) {
            objectTypeDisplay.setIcon(new IconType());
        }
        QName objectType = CollectionUtils.isNotEmpty(relationSpec.getObjectTypes()) ? relationSpec.getObjectTypes().get(0) : null;
        if (StringUtils.isEmpty(GuiDisplayTypeUtil.getIconCssClass(objectTypeDisplay)) && objectType != null) {
            objectTypeDisplay.getIcon().setCssClass(IconAndStylesUtil.createDefaultBlackIcon(objectType));
        }
        if (StringUtils.isNotEmpty(GuiDisplayTypeUtil.getIconCssClass(objectTypeDisplay))) {
            builder.setBasicIcon(objectTypeDisplay.getIcon(), IconCssStyle.IN_ROW_STYLE)
                    .appendColorHtmlValue(GuiDisplayTypeUtil.getIconColor(objectTypeDisplay))
                    .appendLayerIcon(actionButtonIcon, IconCssStyle.BOTTOM_RIGHT_MAX_ICON_STYLE)
                    .appendLayerIcon(relationIcon, IconCssStyle.TOP_RIGHT_MAX_ICON_STYLE);
        } else {
            builder.setBasicIcon(actionButtonIcon, IconCssStyle.IN_ROW_STYLE)
                    .appendColorHtmlValue(GuiDisplayTypeUtil.removeStringAfterSemicolon(actionButtonIcon.getColor()));
        }
        return builder;
    }

    public static IModel<String> getIconUrlModel(IconType icon) {
        return new ReadOnlyModel<>(() -> {
            if (icon == null || StringUtils.isEmpty(icon.getImageUrl())) {
                return null;
            }
            String sUrl = icon.getImageUrl();
            URI uri = null;
            try {
                uri = URI.create(sUrl);
            } catch (Exception ex) {
                LOGGER.debug("Image url '" + sUrl + "' is not proper URI");
            }

            if (uri == null) {
                return null;
            }

            if (uri.isAbsolute()) {
                return sUrl;
            }

            List<String> segments = RequestCycle.get().getUrlRenderer().getBaseUrl().getSegments();
            if (segments == null || segments.size() < 2) {
                return sUrl;
            }

            String prefix = StringUtils.repeat("../", segments.size() - 1);
            if (!sUrl.startsWith("/")) {
                return prefix + sUrl;
            }

            return StringUtils.left(prefix, prefix.length() - 1) + sUrl;
        });

    }

    // FIXME this uses old-style token processing
    public static void deleteSyncTokenPerformed(AjaxRequestTarget target, ResourceType resourceType, PageBase pageBase) {
        String resourceOid = resourceType.getOid();
        String handlerUri = "http://midpoint.evolveum.com/xml/ns/public/model/synchronization/task/live-sync/handler-3";
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resourceOid);
        PrismObject<TaskType> task;

        OperationResult result = new OperationResult(pageBase.getClass().getName() + "." + "deleteSyncToken");
        ObjectQuery query = pageBase.getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(resourceOid)
                .and().item(TaskType.F_HANDLER_URI).eq(handlerUri)
                .build();

        List<PrismObject<TaskType>> taskList = WebModelServiceUtils.searchObjects(TaskType.class, query, result, pageBase);

        if (taskList.size() != 1) {
            pageBase.error(pageBase.createStringResource("pageResource.message.invalidTaskSearch").getString());
        } else {
            task = taskList.get(0);
            PrismProperty<?> property = task.findProperty(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.SYNC_TOKEN));

            if (property != null) {
                Object value = property.getRealValue();

                ObjectDelta<TaskType> delta = pageBase.getPrismContext().deltaFactory().object().createModifyDelta(task.getOid(),
                        pageBase.getPrismContext().deltaFactory().property()
                                .createModificationDeleteProperty(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.SYNC_TOKEN), property.getDefinition(), value),
                        TaskType.class);
                saveTask(delta, result, pageBase);
            }
        }

        result.recomputeStatus();
        pageBase.showResult(result);
        target.add(pageBase.getFeedbackPanel());
    }

    /**
     * The idea is to divide the list of AssignmentObjectRelation objects in such way that each AssignmentObjectRelation
     * in the list will contain not more than 1 relation. This will simplify creating of a new_assignment_button
     * on some panels
     */
    public static List<AssignmentObjectRelation> divideAssignmentRelationsByRelationValue(List<AssignmentObjectRelation> initialRelationsList) {
        if (CollectionUtils.isEmpty(initialRelationsList)) {
            return initialRelationsList;
        }
        List<AssignmentObjectRelation> combinedRelationList = new ArrayList<>();
        initialRelationsList.forEach(assignmentTargetRelation -> {
            if (CollectionUtils.isEmpty(assignmentTargetRelation.getObjectTypes()) &&
                    CollectionUtils.isEmpty(assignmentTargetRelation.getRelations())) {
                return;
            }
            if (CollectionUtils.isEmpty(assignmentTargetRelation.getRelations())) {
                combinedRelationList.add(assignmentTargetRelation);
            } else {
                assignmentTargetRelation.getRelations().forEach(relation -> {
                    AssignmentObjectRelation relationObj = new AssignmentObjectRelation();
                    relationObj.setObjectTypes(assignmentTargetRelation.getObjectTypes());
                    relationObj.setRelations(Collections.singletonList(relation));
                    relationObj.setArchetypeRefs(assignmentTargetRelation.getArchetypeRefs());
                    relationObj.setDescription(assignmentTargetRelation.getDescription());
                    combinedRelationList.add(relationObj);
                });
            }
        });
        return combinedRelationList;
    }

    public static List<AssignmentObjectRelation> divideAssignmentRelationsByAllValues(
            List<AssignmentObjectRelation> initialAssignmentRelationsList) {
        return divideAssignmentRelationsByAllValues(initialAssignmentRelationsList, false);
    }

    /**
     * The idea is to divide the list of AssignmentObjectRelation objects in such way that each AssignmentObjectRelation
     * in the list will contain not more than 1 relation, not more than 1 object type and not more than one archetype reference.
     * This will simplify creating of a new_assignment_button
     */
    public static List<AssignmentObjectRelation> divideAssignmentRelationsByAllValues(
            List<AssignmentObjectRelation> initialAssignmentRelationsList, boolean addDefaultObjectRelations) {
        if (initialAssignmentRelationsList == null) {
            return null;
        }
        List<AssignmentObjectRelation> dividedByRelationList = divideAssignmentRelationsByRelationValue(initialAssignmentRelationsList);
        List<AssignmentObjectRelation> resultList = new ArrayList<>();
        dividedByRelationList.forEach(assignmentObjectRelation -> {
            if (CollectionUtils.isNotEmpty(assignmentObjectRelation.getObjectTypes())) {
                assignmentObjectRelation.getObjectTypes().forEach(objectType -> {
                    if (CollectionUtils.isNotEmpty(assignmentObjectRelation.getArchetypeRefs())) {
                        if (addDefaultObjectRelations) {
                            //add at first type+relation combination without archetypeRef to cover default views (e.g. all users)
                            AssignmentObjectRelation defaultViewRelation = new AssignmentObjectRelation();
                            defaultViewRelation.setObjectTypes(Collections.singletonList(objectType));
                            defaultViewRelation.setRelations(assignmentObjectRelation.getRelations());
                            defaultViewRelation.setDescription(assignmentObjectRelation.getDescription());
                            if (!assignmentObjectRelationAlreadyExists(resultList, defaultViewRelation)) {
                                resultList.add(defaultViewRelation);
                            }
                        }
                        assignmentObjectRelation.getArchetypeRefs().forEach(archetypeRef -> {
                            AssignmentObjectRelation newRelation = new AssignmentObjectRelation();
                            newRelation.setObjectTypes(Collections.singletonList(objectType));
                            newRelation.setRelations(assignmentObjectRelation.getRelations());
                            newRelation.setArchetypeRefs(Collections.singletonList(archetypeRef));
                            newRelation.setDescription(assignmentObjectRelation.getDescription());
                            if (!assignmentObjectRelationAlreadyExists(resultList, newRelation)) {
                                resultList.add(newRelation);
                            }
                        });
                    } else {
                        AssignmentObjectRelation newRelation = new AssignmentObjectRelation();
                        newRelation.setObjectTypes(Collections.singletonList(objectType));
                        newRelation.setRelations(assignmentObjectRelation.getRelations());
                        newRelation.setArchetypeRefs(assignmentObjectRelation.getArchetypeRefs());
                        newRelation.setDescription(assignmentObjectRelation.getDescription());
                        if (!assignmentObjectRelationAlreadyExists(resultList, newRelation)) {
                            resultList.add(newRelation);
                        }
                    }
                });
            } else {
                if (CollectionUtils.isNotEmpty(assignmentObjectRelation.getArchetypeRefs())) {
                    assignmentObjectRelation.getArchetypeRefs().forEach(archetypeRef -> {
                        AssignmentObjectRelation newRelation = new AssignmentObjectRelation();
                        newRelation.setObjectTypes(assignmentObjectRelation.getObjectTypes());
                        newRelation.setRelations(assignmentObjectRelation.getRelations());
                        newRelation.setArchetypeRefs(Collections.singletonList(archetypeRef));
                        newRelation.setDescription(assignmentObjectRelation.getDescription());
                        resultList.add(newRelation);
                    });
                } else {
                    AssignmentObjectRelation newRelation = new AssignmentObjectRelation();
                    newRelation.setObjectTypes(assignmentObjectRelation.getObjectTypes());
                    newRelation.setRelations(assignmentObjectRelation.getRelations());
                    newRelation.setArchetypeRefs(assignmentObjectRelation.getArchetypeRefs());
                    newRelation.setDescription(assignmentObjectRelation.getDescription());
                    resultList.add(newRelation);
                }
            }
        });
        return resultList;
    }

    /**
     * it's expected that the list of AssignmentObjectRelation will be pre-prepared in such manner that each AssignmentObjectRelation
     * in the list will contain only one object type, one relation and one archetypeRef. This methods compares only the first
     * items in these lists
     *
     * @param list
     * @param relation
     * @return
     */
    public static boolean assignmentObjectRelationAlreadyExists(List<AssignmentObjectRelation> list, AssignmentObjectRelation relation) {
        if (CollectionUtils.isEmpty(list)) {
            return false;
        }
        for (AssignmentObjectRelation rel : list) {
            if (CollectionUtils.isNotEmpty(rel.getRelations()) && CollectionUtils.isEmpty(relation.getRelations())
                    || CollectionUtils.isEmpty(rel.getRelations()) && CollectionUtils.isNotEmpty(relation.getRelations())) {
                continue;
            }
            if (CollectionUtils.isNotEmpty(rel.getRelations()) && CollectionUtils.isNotEmpty(relation.getRelations())) {
                if (!QNameUtil.match(rel.getRelations().get(0), (relation.getRelations().get(0)))) {
                    continue;
                }
            }
            if (CollectionUtils.isNotEmpty(rel.getObjectTypes()) && CollectionUtils.isEmpty(relation.getObjectTypes())
                    || CollectionUtils.isEmpty(rel.getObjectTypes()) && CollectionUtils.isNotEmpty(relation.getObjectTypes())) {
                continue;
            }
            if (CollectionUtils.isNotEmpty(rel.getObjectTypes()) && CollectionUtils.isNotEmpty(relation.getObjectTypes())) {
                if (!QNameUtil.match(rel.getObjectTypes().get(0), relation.getObjectTypes().get(0))) {
                    continue;
                }
            }
            if (CollectionUtils.isNotEmpty(rel.getArchetypeRefs()) && CollectionUtils.isEmpty(relation.getArchetypeRefs())
                    || CollectionUtils.isEmpty(rel.getArchetypeRefs()) && CollectionUtils.isNotEmpty(relation.getArchetypeRefs())) {
                continue;
            }
            if (CollectionUtils.isNotEmpty(rel.getArchetypeRefs()) && CollectionUtils.isNotEmpty(relation.getArchetypeRefs())) {
                if (!rel.getArchetypeRefs().get(0).equals(relation.getArchetypeRefs().get(0))) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }

    private static Collection<ObjectDeltaOperation<? extends ObjectType>> saveTask(ObjectDelta<TaskType> delta, OperationResult result, PageBase pageBase) {
        Task opTask = pageBase.createSimpleTask(pageBase.getClass().getName() + "." + "saveTask");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(delta.debugDump());
        }

        Collection<ObjectDeltaOperation<? extends ObjectType>> ret = null;
        try {
            ret = pageBase.getModelService().executeChanges(MiscUtil.createCollection(delta), null, opTask, result);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save task.", e);
            result.recordFatalError(pageBase.createStringResource("WebModelUtils.couldntSaveTask").getString(), e);
        }
        result.recomputeStatus();
        return ret;
    }

    public static <T> List<T> sortDropDownChoices(IModel<? extends List<? extends T>> choicesModel, IChoiceRenderer<T> renderer) {
        return choicesModel.getObject().stream().sorted((choice1, choice2) -> {
            if (choice1 == null || choice2 == null) {
                return 0;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(renderer.getDisplayValue(choice1).toString(), renderer.getDisplayValue(choice2).toString());

        }).collect(Collectors.toList());
    }

    public static VisualizationDto createVisualizationDto(CaseWorkItemType caseWorkItem, PageBase pageBase, String operation) {
        if (caseWorkItem == null) {
            return null;
        }
        return createVisualizationDto(CaseTypeUtil.getCase(caseWorkItem), pageBase, operation);
    }

    public static List<EvaluatedTriggerGroupDto> computeTriggers(ApprovalContextType wfc, Integer stage) {
        List<EvaluatedTriggerGroupDto> triggers = new ArrayList<>();
        if (wfc == null) {
            return triggers;
        }
        EvaluatedTriggerGroupDto.UniquenessFilter uniquenessFilter = new EvaluatedTriggerGroupDto.UniquenessFilter();
        List<List<EvaluatedPolicyRuleType>> rulesPerStageList = ApprovalContextUtil.getRulesPerStage(wfc);
        for (int i = 0; i < rulesPerStageList.size(); i++) {
            Integer stageNumber = i + 1;
            boolean highlighted = stageNumber.equals(stage);
            EvaluatedTriggerGroupDto group = EvaluatedTriggerGroupDto.initializeFromRules(rulesPerStageList.get(i), highlighted, uniquenessFilter);
            triggers.add(group);
        }
        return triggers;
    }

    public static VisualizationDto createVisualizationDto(CaseType caseObject, PageBase pageBase, String operation) {
        if (caseObject == null || caseObject.getApprovalContext() == null) {
            return null;
        }
        ObjectReferenceType objectRef = caseObject.getObjectRef();

        OperationResult result = new OperationResult(operation);
        Task task = pageBase.createSimpleTask(operation);
        try {
            Visualization visualization = VisualizationUtil.visualizeObjectTreeDeltas(caseObject.getApprovalContext().getDeltasToApprove(),
                    CaseTypeUtil.isClosed(caseObject) ? "pageWorkItem.changesApplied" : "pageWorkItem.delta",
                    pageBase.getPrismContext(), pageBase.getModelInteractionService(), objectRef, task, result);
            return new VisualizationDto(visualization);
        } catch (SchemaException | ExpressionEvaluationException ex) {
            LOGGER.error("Unable to create delta visualization for case {}: {}", caseObject, ex.getLocalizedMessage(), ex);
        }
        return null;
    }

    public static VisualizationDto createVisualizationDtoForManualCase(CaseType caseObject, PageBase pageBase, String operation) {
        if (caseObject == null || caseObject.getManualProvisioningContext() == null ||
                caseObject.getManualProvisioningContext().getPendingOperation() == null) {
            return null;
        }
        ObjectReferenceType objectRef = caseObject.getObjectRef();
        OperationResult result = new OperationResult(operation);
        Task task = pageBase.createSimpleTask(operation);
        try {
            Visualization deltaVisualization = VisualizationUtil.visualizeObjectDeltaType(caseObject.getManualProvisioningContext().getPendingOperation().getDelta(),
                    CaseTypeUtil.isClosed(caseObject) ? "pageWorkItem.changesApplied" : "pageWorkItem.changesToBeApplied", pageBase.getPrismContext(), pageBase.getModelInteractionService(), objectRef, task, result);
            return new VisualizationDto(deltaVisualization);
        } catch (SchemaException | ExpressionEvaluationException ex) {
            LOGGER.error("Unable to create delta visualization for case {}: {}", caseObject, ex.getLocalizedMessage(), ex);
        }
        return null;
    }

    public static void workItemApproveActionPerformed(AjaxRequestTarget target, CaseWorkItemType workItem,
            Component formPanel, PrismObject<UserType> powerDonor, boolean approved, OperationResult result, PageBase pageBase) {
        if (workItem == null) {
            return;
        }
        CaseType parentCase = CaseTypeUtil.getCase(workItem);
        AbstractWorkItemOutputType output = workItem.getOutput();
        if (output == null) {
            output = new AbstractWorkItemOutputType();
        }
        output.setOutcome(ApprovalUtils.toUri(approved));
        if (WorkItemTypeUtil.getComment(workItem) != null) {
            output.setComment(WorkItemTypeUtil.getComment(workItem));
        }
        if (WorkItemTypeUtil.getEvidence(workItem) != null) {
            output.setEvidence(WorkItemTypeUtil.getEvidence(workItem));
        }
        if (CaseTypeUtil.isManualProvisioningCase(parentCase)) {
            Task task = pageBase.createSimpleTask(result.getOperation());
            try {
                WorkItemId workItemId = WorkItemId.create(parentCase.getOid(), workItem.getId());
                pageBase.getCaseService().completeWorkItem(workItemId, output, task, result);
            } catch (Exception ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Unable to complete work item, ", ex);
                result.recordFatalError(ex);
            }
        } else {

            Task task = pageBase.createSimpleTask(result.getOperation());
            try {
                try {
                    ObjectDelta<?> additionalDelta = null;
                    if (formPanel instanceof DynamicFormPanel) {
                        if (approved) {
                            boolean requiredFieldsPresent = ((DynamicFormPanel<?>) formPanel).checkRequiredFields(pageBase);
                            if (!requiredFieldsPresent) {
                                target.add(pageBase.getFeedbackPanel());
                                return;
                            }
                        }
                        additionalDelta = ((DynamicFormPanel<?>) formPanel).getObjectDelta();
                        if (additionalDelta != null) {
                            pageBase.getPrismContext().adopt(additionalDelta);
                        }
                    }
                    assumePowerOfAttorneyIfRequested(result, powerDonor, pageBase);
                    pageBase.getCaseService().completeWorkItem(WorkItemId.of(workItem),
                            output, additionalDelta, task, result);
                } finally {
                    dropPowerOfAttorneyIfRequested(result, powerDonor, pageBase);
                }
            } catch (Exception ex) {
                result.recordFatalError(pageBase.createStringResource("WebModelUtils.couldntSaveWorkItem").getString(), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save work item", ex);
            }
        }
        result.computeStatusIfUnknown();
        pageBase.showResult(result);
    }

    public static OperationResultStatusPresentationProperties caseOutcomeUriToPresentation(String outcome) {
        if (outcome == null) {
            return OperationResultStatusPresentationProperties.IN_PROGRESS;
        } else if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE)) {
            return OperationResultStatusPresentationProperties.SUCCESS;
        } else if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT)) {
            return OperationResultStatusPresentationProperties.FATAL_ERROR;
        } else {
            return OperationResultStatusPresentationProperties.UNKNOWN;
        }
    }

    public static ApprovalOutcomeIcon caseOutcomeUriToIcon(String outcome) {
        if (outcome == null) {
            return ApprovalOutcomeIcon.IN_PROGRESS;
        } else if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE)) {
            return ApprovalOutcomeIcon.APPROVED;
        } else if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT)) {
            return ApprovalOutcomeIcon.REJECTED;
        } else if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_SKIP)) {
            return ApprovalOutcomeIcon.SKIPPED;
        } else {
            return ApprovalOutcomeIcon.UNRECOGNIZED;
        }
    }

    public static List<ObjectOrdering> createMetadataOrdering(SortParam<String> sortParam, String metadataProperty, PrismContext prismContext) {
        if (sortParam != null && sortParam.getProperty() != null) {
            OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
            if (sortParam.getProperty().equals(metadataProperty)) {
                return Collections.singletonList(
                        prismContext.queryFactory().createOrdering(
                                ItemPath.create(ReportDataType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), order));
            }
            return Collections.singletonList(
                    prismContext.queryFactory().createOrdering(
                            ItemPath.fromString(sortParam.getProperty()), order));
        } else {
            return null;
        }
    }

    public static void claimWorkItemActionPerformed(CaseWorkItemType workItemToClaim,
            String operation, AjaxRequestTarget target, PageBase pageBase) {
        Task task = pageBase.createSimpleTask(operation);
        OperationResult mainResult = task.getResult();
        CaseService caseService = pageBase.getCaseService();
        OperationResult result = mainResult.createSubresult(operation);
        try {
            caseService.claimWorkItem(WorkItemId.of(workItemToClaim), task, result);
            result.computeStatusIfUnknown();
        } catch (ObjectNotFoundException | SecurityViolationException | RuntimeException | SchemaException |
                ObjectAlreadyExistsException | CommunicationException | ConfigurationException |
                ExpressionEvaluationException e) {
            result.recordPartialError(pageBase.createStringResource("pageWorkItems.message.partialError.released").getString(), e);
        }
        if (mainResult.isUnknown()) {
            mainResult.recomputeStatus();
        }

        if (mainResult.isSuccess()) {
            mainResult.recordStatus(OperationResultStatus.SUCCESS,
                    pageBase.createStringResource("pageWorkItems.message.success.claimed").getString());
        }

        pageBase.showResult(mainResult);

//        pageBase.resetWorkItemCountModel();
        target.add(pageBase);

    }

    public static void releaseWorkItemActionPerformed(CaseWorkItemType workItemToClaim,
            String operation, AjaxRequestTarget target, PageBase pageBase) {
        Task task = pageBase.createSimpleTask(operation);
        OperationResult mainResult = task.getResult();
        CaseService caseService = pageBase.getCaseService();
        OperationResult result = mainResult.createSubresult(operation);

        try {
            caseService.releaseWorkItem(WorkItemId.of(workItemToClaim), task, result);
            result.computeStatusIfUnknown();
        } catch (ObjectNotFoundException | SecurityViolationException | RuntimeException | SchemaException |
                ObjectAlreadyExistsException | CommunicationException | ConfigurationException |
                ExpressionEvaluationException e) {
            result.recordPartialError(pageBase.createStringResource("pageWorkItems.message.partialError.released").getString(), e);
        }
        if (mainResult.isUnknown()) {
            mainResult.recomputeStatus();
        }

        if (mainResult.isSuccess()) {
            mainResult.recordStatus(OperationResultStatus.SUCCESS,
                    pageBase.createStringResource("pageWorkItems.message.success.simple.released").getString());
        }

        pageBase.showResult(mainResult);
        target.add(pageBase);
    }

    public static void assumePowerOfAttorneyIfRequested(OperationResult result, PrismObject<UserType> powerDonor, PageBase pageBase) {
        if (powerDonor != null) {
            WebModelServiceUtils.assumePowerOfAttorney(powerDonor, pageBase.getModelInteractionService(), pageBase.getTaskManager(), result);
        }
    }

    public static void dropPowerOfAttorneyIfRequested(OperationResult result, PrismObject<UserType> powerDonor, PageBase pageBase) {
        if (powerDonor != null) {
            WebModelServiceUtils.dropPowerOfAttorney(pageBase.getModelInteractionService(), pageBase.getTaskManager(), result);
        }
    }

    public static <T> T runUnderPowerOfAttorneyIfNeeded(CheckedProducer<T> producer, PrismObject<? extends FocusType> powerDonor,
            PageBase pageBase, Task task, OperationResult result) throws CommonException {
        if (powerDonor != null) {
            return pageBase.getModelInteractionService().runUnderPowerOfAttorneyChecked(producer, powerDonor, task, result);
        } else {
            return producer.get();
        }
    }

    @NotNull
    public static List<VisualizationDto> computeChangesCategorizationList(ChangesByState changesByState, ObjectReferenceType objectRef,
            ModelInteractionService modelInteractionService, PrismContext prismContext, Task opTask,
            OperationResult thisOpResult) throws SchemaException, ExpressionEvaluationException {
        List<VisualizationDto> changes = new ArrayList<>();
        if (!changesByState.getApplied().isEmpty()) {
            changes.add(createTaskChangesDto("TaskDto.changesApplied", "card-outline-left-success", changesByState.getApplied(),
                    modelInteractionService, prismContext, objectRef, opTask, thisOpResult));
        }
        if (!changesByState.getBeingApplied().isEmpty()) {
            changes.add(createTaskChangesDto("TaskDto.changesBeingApplied", "card-outline-left-info", changesByState.getBeingApplied(),
                    modelInteractionService, prismContext, objectRef, opTask, thisOpResult));
        }
        if (!changesByState.getWaitingToBeApplied().isEmpty()) {
            changes.add(createTaskChangesDto("TaskDto.changesWaitingToBeApplied", "card-outline-left-warning",
                    changesByState.getWaitingToBeApplied(), modelInteractionService, prismContext, objectRef, opTask, thisOpResult));
        }
        if (!changesByState.getWaitingToBeApproved().isEmpty()) {
            changes.add(createTaskChangesDto("TaskDto.changesWaitingToBeApproved", "card-outline-left-primary",
                    changesByState.getWaitingToBeApproved(), modelInteractionService, prismContext, objectRef, opTask, thisOpResult));
        }
        if (!changesByState.getRejected().isEmpty()) {
            changes.add(createTaskChangesDto("TaskDto.changesRejected", "card-outline-left-danger", changesByState.getRejected(),
                    modelInteractionService, prismContext, objectRef, opTask, thisOpResult));
        }
        if (!changesByState.getCanceled().isEmpty()) {
            changes.add(createTaskChangesDto("TaskDto.changesCanceled", "card-outline-left-danger", changesByState.getCanceled(),
                    modelInteractionService, prismContext, objectRef, opTask, thisOpResult));
        }
        return changes;
    }

    private static VisualizationDto createTaskChangesDto(String titleKey, String boxClassOverride, ObjectTreeDeltas deltas, ModelInteractionService modelInteractionService,
            PrismContext prismContext, ObjectReferenceType objectRef, Task opTask, OperationResult result) throws SchemaException, ExpressionEvaluationException {
        ObjectTreeDeltasType deltasType = ObjectTreeDeltas.toObjectTreeDeltasType(deltas);
        Visualization visualization = VisualizationUtil.visualizeObjectTreeDeltas(deltasType, titleKey, prismContext, modelInteractionService, objectRef, opTask, result);
        VisualizationDto visualizationDto = new VisualizationDto(visualization);
        visualizationDto.setBoxClassOverride(boxClassOverride);
        return visualizationDto;
    }

    public static String getMidpointCustomSystemName(PageAdminLTE pageBase, String defaultSystemNameKey) {
        if (MidPointApplication.get().getSubscriptionState().isInactiveOrDemo()) {
            return pageBase.createStringResource(defaultSystemNameKey).getString();
        }

        DeploymentInformationType deploymentInfo = MidPointApplication.get().getDeploymentInfo();
        return deploymentInfo != null && StringUtils.isNotEmpty(deploymentInfo.getSystemName()) ?
                deploymentInfo.getSystemName() : pageBase.createStringResource(defaultSystemNameKey).getString();
    }

    public static String getObjectListPageStorageKey(String additionalKeyValue) {
        if (StringUtils.isEmpty(additionalKeyValue)) {
            return null;
        }
        return SessionStorage.KEY_OBJECT_LIST + "." + additionalKeyValue;
    }

    public static AssignmentHolderType getObjectFromAddDeltaForCase(CaseType aCase) {
        if (aCase != null && aCase.getApprovalContext() != null
                && aCase.getApprovalContext().getDeltasToApprove() != null) {
            ObjectTreeDeltasType deltaTree = aCase.getApprovalContext().getDeltasToApprove();
            if (deltaTree != null && deltaTree.getFocusPrimaryDelta() != null) {
                ObjectDeltaType primaryDelta = deltaTree.getFocusPrimaryDelta();
                if (primaryDelta != null && (primaryDelta.getItemDelta() == null || primaryDelta.getItemDelta().isEmpty())
                        && primaryDelta.getObjectToAdd() != null && primaryDelta.getObjectToAdd() instanceof AssignmentHolderType
                        && ChangeType.ADD.equals(ChangeType.toChangeType(primaryDelta.getChangeType()))) {
                    return (AssignmentHolderType) primaryDelta.getObjectToAdd();
                }
            }
        }
        return null;
    }

    public static boolean isResourceRelatedTask(TaskType task) {
        return WebComponentUtil.hasArchetypeAssignment(task, SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value())
                || WebComponentUtil.hasArchetypeAssignment(task, SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value())
                || WebComponentUtil.hasArchetypeAssignment(task, SystemObjectsType.ARCHETYPE_IMPORT_TASK.value());
    }

    public static boolean isRefreshEnabled(PageBase pageBase, QName type) {
        CompiledGuiProfile compiledGuiProfile = pageBase.getCompiledGuiProfile();
        List<CompiledObjectCollectionView> views = compiledGuiProfile.getObjectCollectionViews();
        if (CollectionUtils.isEmpty(views)) {
            return false;
        }

        for (CompiledObjectCollectionView view : views) {
            if (QNameUtil.match(type, view.getContainerType())) {
                if (view.getRefreshInterval() != null) {
                    return true;
                }

            }
        }
        return false;
    }

    public static Long xgc2long(XMLGregorianCalendar gc) {
        return gc != null ? XmlTypeConverter.toMillis(gc) : null;
    }

    public static String getSimpleChannel(String chanelUri) {
        if (chanelUri == null) {
            return null;
        }
        int i = chanelUri.indexOf('#');
        if (i < 0) {
            return chanelUri;
        }
        return chanelUri.substring(i + 1);
    }

    public static Collection<String> getIntentsForKind(PrismObject<ResourceType> resource, ShadowKindType kind) {

        if (kind == null) {
            return Collections.emptyList();
        }

        ResourceSchema refinedSchema;
        try {
            refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
            if (refinedSchema != null) {
                return refinedSchema.getIntentsForKind(kind);
            } else {
                return List.of();
            }
        } catch (SchemaException | ConfigurationException e) {
            return List.of();
        }
    }

    public static Class<? extends PageBase> resolveSelfPage() {
        FocusType focusType = WebModelServiceUtils.getLoggedInFocus();
        if (focusType instanceof UserType) {
            return PageUserSelfProfile.class;
        }
        if (focusType instanceof OrgType) {
            return PageOrgSelfProfile.class;
        }
        if (focusType instanceof RoleType) {
            return PageRoleSelfProfile.class;
        }
        if (focusType instanceof ServiceType) {
            return PageServiceSelfProfile.class;
        }
        return null;
    }

    public static <I extends Item> PrismObject<LookupTableType> findLookupTable(ItemDefinition<I> definition, PageBase page) {
        PrismReferenceValue valueEnumerationRef = definition.getValueEnumerationRef();
        return findLookupTable(valueEnumerationRef, page);
    }

    public static PrismObject<LookupTableType> findLookupTable(PrismReferenceValue valueEnumerationRef, PageBase page) {
        if (valueEnumerationRef == null) {
            return null;
        }

        String lookupTableUid = valueEnumerationRef.getOid();
        Task task = page.createSimpleTask("loadLookupTable");
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils.createLookupTableRetrieveOptions(page.getSchemaService());
        return WebModelServiceUtils.loadObject(LookupTableType.class, lookupTableUid, options, page, task, result);
    }

    public static <AH extends AssignmentHolderType> boolean hasAnyArchetypeAssignment(AH assignmentHolder) {
        if (assignmentHolder.getAssignment() == null) {
            return false;
        }
        List<AssignmentType> archetypeAssignments = assignmentHolder.getAssignment()
                .stream()
                .filter(WebComponentUtil::isArchetypeAssignment).collect(Collectors.toList());
        return CollectionUtils.isNotEmpty(archetypeAssignments);
    }

    public static boolean isArchetypeAssignment(AssignmentType assignmentType) {
        if (assignmentType.getTargetRef() == null) {
            return false;
        }
        return QNameUtil.match(assignmentType.getTargetRef().getType(), ArchetypeType.COMPLEX_TYPE);
    }

    public static boolean isDelegationAssignment(AssignmentType assignmentType) {
        if (assignmentType.getTargetRef() == null) {
            return false;
        }
        return QNameUtil.match(assignmentType.getTargetRef().getType(), UserType.COMPLEX_TYPE);
    }

    public static <AH extends AssignmentHolderType> boolean hasArchetypeAssignment(AH assignmentHolder, String archetypeOid) {
        if (assignmentHolder.getAssignment() == null) {
            return false;
        }
        List<AssignmentType> archetypeAssignments = assignmentHolder.getAssignment()
                .stream()
                .filter(assignmentType -> WebComponentUtil.isArchetypeAssignment(assignmentType)
                        && archetypeOid.equals(assignmentType.getTargetRef().getOid()))
                .collect(Collectors.toList());
        return CollectionUtils.isNotEmpty(archetypeAssignments);
    }

    /**
     * @deprecated Use {@link com.evolveum.midpoint.gui.api.util.LocalizationUtil#findLocale()}
     */
    @Deprecated
    public static <F extends FocusType> Locale getLocale() {
        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal == null) {
            return AvailableLocale.getDefaultLocale();
        }

        Locale locale;
        if (principal.getCompiledGuiProfile().getLocale() != null) {
            locale = principal.getCompiledGuiProfile().getLocale();
        } else {
            //noinspection unchecked
            F focus = (F) principal.getFocus();

            locale = LocalizationUtil.toLocale(FocusTypeUtil.languageOrLocale(focus));
        }

        if (locale == null) {
            if (ThreadContext.getSession() == null) {
                return AvailableLocale.getDefaultLocale();
            }

            locale = Session.get().getLocale();
        }

        locale = AvailableLocale.getBestMatchingLocale(locale);
        if (locale != null) {
            return locale;
        }

        return AvailableLocale.getDefaultLocale();
    }

    public static Collator getCollator() {
        Locale locale = WebComponentUtil.getLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.SECONDARY);       // e.g. "a" should be different from "á"
        collator.setDecomposition(Collator.FULL_DECOMPOSITION);
        return collator;
    }

    public static CompositedIcon createCreateReportIcon() {
        final CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setBasicIcon(IconAndStylesUtil.createReportIcon(), IconCssStyle.IN_ROW_STYLE);
        IconType plusIcon = new IconType();
        plusIcon.setCssClass(GuiStyleConstants.CLASS_ADD_NEW_OBJECT);
        plusIcon.setColor("green");
        builder.appendLayerIcon(plusIcon, LayeredIconCssStyle.BOTTOM_RIGHT_STYLE);
        return builder.build();
    }

    public static CompiledObjectCollectionView getCollectionViewByObject(AssignmentHolderType assignmentHolder, PageBase pageBase) {
        String archetypeOid = getArchetypeOid(assignmentHolder);
        if (!StringUtils.isEmpty(archetypeOid)) {
            List<CompiledObjectCollectionView> collectionViews =
                    pageBase.getCompiledGuiProfile().getObjectCollectionViews();
            for (CompiledObjectCollectionView view : collectionViews) {
                if (view.getCollection() != null && view.getCollection().getCollectionRef() != null
                        && archetypeOid.equals(view.getCollection().getCollectionRef().getOid())) {
                    return view;
                }
            }
        }
        QName type = classToQName(pageBase.getPrismContext(), assignmentHolder.getClass());
        return pageBase.getCompiledGuiProfile().findObjectCollectionView(type, null);
    }

    public static CredentialsPolicyType getPasswordCredentialsPolicy(PrismObject<? extends FocusType> focus, PageAdminLTE parentPage, Task task) {
        LOGGER.debug("Getting credentials policy");
        CredentialsPolicyType credentialsPolicyType = null;
        try {
            credentialsPolicyType = parentPage.getModelInteractionService().getCredentialsPolicy(focus, task, task.getResult());
            task.getResult().recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load credentials policy", ex);
            task.getResult().recordFatalError(
                    parentPage.createStringResource("PageAbstractSelfCredentials.message.getPasswordCredentialsPolicy.fatalError", ex.getMessage()).getString(), ex);
        } finally {
            task.getResult().computeStatus();
        }
        return credentialsPolicyType;
    }

    public static ValuePolicyType getPasswordValuePolicy(CredentialsPolicyType credentialsPolicy,
            String operation, PageAdminLTE parentPage) {
        ValuePolicyType valuePolicyType = null;
        MidPointPrincipal user = AuthUtil.getPrincipalUser();
        try {
            if (user != null) {
                Task task = parentPage.createSimpleTask(operation);
                valuePolicyType = resolvePasswordValuePolicy(credentialsPolicy, task, parentPage);
            } else {
                valuePolicyType = parentPage.getSecurityContextManager().runPrivileged((Producer<ValuePolicyType>) () -> {
                    Task task = parentPage.createAnonymousTask(operation);
                    return resolvePasswordValuePolicy(credentialsPolicy, task, parentPage);
                });
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't load password value policy for focus " + (user != null ? user.getFocusPrismObject() : null), e);
        }
        return valuePolicyType;
    }

    private static ValuePolicyType resolvePasswordValuePolicy(CredentialsPolicyType credentialsPolicy, Task task, PageAdminLTE parentPage) {
        if (credentialsPolicy != null && credentialsPolicy.getPassword() != null
                && credentialsPolicy.getPassword().getValuePolicyRef() != null) {
            PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.resolveReferenceNoFetch(
                    credentialsPolicy.getPassword().getValuePolicyRef(), parentPage, task, task.getResult());
            if (valuePolicy != null) {
                return valuePolicy.asObjectable();
            }
        }
        return null;
    }

    @Contract("_,true->!null")
    public static Long getTimestampAsLong(XMLGregorianCalendar cal, boolean currentIfNull) {
        Long calAsLong = MiscUtil.asMillis(cal);
        if (calAsLong == null) {
            if (currentIfNull) {
                return System.currentTimeMillis();
            }
            return null;
        }
        return calAsLong;
    }

    public static String getTaskProgressDescription(TaskInformation taskInformation, boolean longForm, PageBase pageBase) {

        // TODO use progress.toLocalizedString after it's implemented
        String progressDescription = taskInformation.getProgressDescription(longForm);

        if (longForm) {
            String temp1 = StringUtils.replaceOnce(progressDescription, "of", pageBase.getString("TaskSummaryPanel.progress.of"));
            return StringUtils.replaceOnce(temp1, "buckets", pageBase.getString("TaskSummaryPanel.progress.buckets"));
        } else {
            return progressDescription;
        }
    }

    public static List<DisplayableValue<?>> getAllowedValues(SearchFilterParameterType parameter, ModelServiceLocator modelServiceLocator) {
        if (parameter == null || parameter.getAllowedValuesExpression() == null) {
            return new ArrayList<>();
        }
        return getAllowedValues(parameter.getAllowedValuesExpression(), modelServiceLocator);
    }

    public static List<DisplayableValue<?>> getAllowedValues(ExpressionType expression, ModelServiceLocator modelServiceLocator) {
        List<DisplayableValue<?>> allowedValues = new ArrayList<>();
        if (expression == null) {
            return allowedValues;
        }
        Task task = modelServiceLocator.createSimpleTask("evaluate expression for allowed values");
        Object value;
        try {
            value = ExpressionUtil.evaluateExpressionNative(null, new VariablesMap(), null,
                    expression, MiscSchemaUtil.getExpressionProfile(),
                    modelServiceLocator.getExpressionFactory(), "evaluate expression for allowed values", task, task.getResult());
        } catch (Exception e) {
            LOGGER.error("Couldn't execute expression " + expression, e);
            if (modelServiceLocator instanceof PageBase) {
                ((PageBase) modelServiceLocator).error(
                        com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate(
                                "FilterSearchItem.message.error.evaluateAllowedValuesExpression", new Object[] { expression }));
            }
            return allowedValues;
        }
        if (value instanceof PrismPropertyValue) {
            value = ((PrismPropertyValue) value).getRealValue();
        }

        if (!(value instanceof Set)) {
            LOGGER.error("Exception return unexpected type, expected Set<PPV<DisplayableValue>>, but was " + (value == null ? null : value.getClass()));
            if (modelServiceLocator instanceof PageBase) {
                ((PageBase) modelServiceLocator).error(
                        com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate(
                                "FilterSearchItem.message.error.wrongType", new Object[] { expression }));
            }
            return allowedValues;
        }

        if (!((Set<?>) value).isEmpty()) {
            if (!(((Set<?>) value).iterator().next() instanceof PrismPropertyValue)
                    || !(((PrismPropertyValue) (((Set<?>) value).iterator().next())).getValue() instanceof DisplayableValue)) {
                LOGGER.error("Exception return unexpected type, expected Set<PPV<DisplayableValue>>, but was " + (value == null ? null : value.getClass()));
                if (modelServiceLocator instanceof PageBase) {
                    ((PageBase) modelServiceLocator).error(
                            com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate(
                                    "FilterSearchItem.message.error.wrongType", new Object[] { expression }));
                }
                return allowedValues;
            }

            return (List<DisplayableValue<?>>) ((Set<PrismPropertyValue<?>>) value).stream()
                    .map(PrismPropertyValue::getValue).collect(Collectors.toList());
        }
        return allowedValues;
    }

    public static String getCollectionNameParameterValueAsString(PageBase pageBase) {
        StringValue stringValue = getCollectionNameParameterValue(pageBase);
        return stringValue == null ? null : stringValue.toString();
    }

    public static StringValue getCollectionNameParameterValue(PageAdminLTE parentPage) {
        PageParameters parameters = parentPage.getPageParameters();
        return parameters == null ? null : parameters.get(PageBase.PARAMETER_OBJECT_COLLECTION_NAME);
    }

    public static <C extends Containerable> GuiObjectListViewType getPrincipalUserObjectListView(PageAdminLTE parentPage,
            FocusType principalFocus, @NotNull Class<C> viewType, boolean createIfNotExist, String defaultIdentifier) {
        if (!(principalFocus instanceof UserType)) {
            return null;
        }
        StringValue collectionViewParameter = WebComponentUtil.getCollectionNameParameterValue(parentPage);
        String viewName = !collectionViewParameter.isNull() && !collectionViewParameter.isEmpty() ? collectionViewParameter.toString() : defaultIdentifier;
        AdminGuiConfigurationType adminGui = ((UserType) principalFocus).getAdminGuiConfiguration();
        if (adminGui == null) {
            if (!createIfNotExist) {
                return null;
            }
            adminGui = new AdminGuiConfigurationType();
            ((UserType) principalFocus).setAdminGuiConfiguration(adminGui);
        }
        GuiObjectListViewsType views = adminGui.getObjectCollectionViews();
        if (views == null) {
            if (!createIfNotExist) {
                return null;
            }
            views = new GuiObjectListViewsType();
            adminGui.objectCollectionViews(views);
        }
        GuiObjectListViewType objectListView = null;
        if (StringUtils.isNotEmpty(viewName)) {
            for (GuiObjectListViewType view : views.getObjectCollectionView()) {
                if (viewName.equals(view.getIdentifier())) {
                    objectListView = view;
                }
            }
        } else {
            for (GuiObjectListViewType view : views.getObjectCollectionView()) {
                if (view.getType().equals(WebComponentUtil.containerClassToQName(PrismContext.get(), viewType))) {
                    objectListView = view;
                }
            }
        }
        if (objectListView == null) {
            if (!createIfNotExist) {
                return null;
            }
            objectListView = new GuiObjectListViewType();
            objectListView.setType(WebComponentUtil.containerClassToQName(PrismContext.get(), viewType));
            views.getObjectCollectionView().add(objectListView);
        }
        return objectListView;
    }

    public static <T> DropDownChoicePanel createDropDownChoices(String id, IModel<DisplayableValue<T>> model,
            IModel<List<DisplayableValue<T>>> choices, boolean allowNull) {
        return new DropDownChoicePanel<>(id, model, choices, new DisplayableChoiceRenderer<>(), allowNull);
    }

    public static Map<IconCssStyle, IconType> createMainButtonLayerIcon(DisplayType mainButtonDisplayType) {
        if (mainButtonDisplayType.getIcon() != null && mainButtonDisplayType.getIcon().getCssClass() != null &&
                mainButtonDisplayType.getIcon().getCssClass().contains(GuiStyleConstants.CLASS_ADD_NEW_OBJECT)) {
            return Collections.emptyMap();
        }
        Map<IconCssStyle, IconType> layerIconMap = new HashMap<>();
        layerIconMap.put(IconCssStyle.BOTTOM_RIGHT_STYLE, IconAndStylesUtil.createIconType(GuiStyleConstants.CLASS_PLUS_CIRCLE, "green"));
        return layerIconMap;
    }

    public static <T extends AssignmentHolderType> void addNewArchetype(
            PrismObjectWrapper<T> object, String archetypeOid, AjaxRequestTarget target, PageBase pageBase) {
        try {
            PrismContainerWrapper<AssignmentType> archetypeAssignment = object.findContainer(TaskType.F_ASSIGNMENT);
            PrismContainerValue<AssignmentType> archetypeAssignmentValue = archetypeAssignment.getItem().createNewValue();
            AssignmentType newArchetypeAssignment = archetypeAssignmentValue.asContainerable();
            newArchetypeAssignment.setTargetRef(ObjectTypeUtil.createObjectRef(archetypeOid, ObjectTypes.ARCHETYPE));
            WebPrismUtil.createNewValueWrapper(archetypeAssignment, archetypeAssignmentValue, pageBase, target);
        } catch (SchemaException e) {
            LOGGER.error("Exception during assignment lookup, reason: {}", e.getMessage(), e);
            pageBase.error("Cannot set selected handler: " + e.getMessage());
        }
    }

    public static boolean isImportReport(ReportType report) {
        ReportBehaviorType behavior = report.getBehavior();
        return behavior != null && DirectionTypeType.IMPORT.equals(behavior.getDirection());
    }

    public static IModel<String> createMappingDescription(IModel<PrismContainerValueWrapper<MappingType>> model) {
        return () -> {

            if (model == null || model.getObject() == null) {
                return null;
            }

            MappingType mappingType = model.getObject().getRealValue();
            if (mappingType == null) {
                return null;
            }

            List<VariableBindingDefinitionType> sources = mappingType.getSource();
            String sourceString = sources.stream().map(s -> s != null && s.getPath() != null ? s.getPath().toString() : "").collect(Collectors.joining(", "));
            String strength = "";
            if (mappingType.getStrength() != null) {
                strength = mappingType.getStrength().toString();
            }

            String target = "";
            VariableBindingDefinitionType targetv = mappingType.getTarget();
            if (targetv != null && targetv.getPath() != null) {
                target += targetv.getPath().toString();
            }

            if (target.isBlank()) {
                return sourceString + (strength.isBlank() ? "" : "(" + strength + ")");
            }

            return target + (strength.isBlank() ? "" : "(" + strength + ")");
        };
    }

    //TODO
    public static <T extends ObjectType> Component createPanel(Class<? extends Panel> panelClass, String markupId, ObjectDetailsModels<T> objectDetailsModels, ContainerPanelConfigurationType panelConfig) {
        if (AbstractAssignmentTypePanel.class.isAssignableFrom(panelClass)) {
            try {
                Panel panel = ConstructorUtils.invokeConstructor(panelClass, markupId, objectDetailsModels.getObjectWrapperModel(), panelConfig);
                panel.setOutputMarkupId(true);

                var isHistoricalData = objectDetailsModels instanceof FocusDetailsModels<?>
                        && ((FocusDetailsModels<?>) objectDetailsModels).isHistoricalObject();
                ((AbstractAssignmentTypePanel) panel).setHistoricalData(isHistoricalData);

                return panel;
            } catch (Throwable e) {
                LOGGER.trace("No constructor found for (String, LoadableModel, ContainerPanelConfigurationType). Continue with lookup.", e);
                return null;
            }
        }

        try {
            Panel panel = ConstructorUtils.invokeConstructor(panelClass, markupId, objectDetailsModels, panelConfig);
            panel.setOutputMarkupId(true);
            return panel;
        } catch (Throwable e) {
            e.printStackTrace();
            LOGGER.trace("No constructor found for (String, LoadableModel, ContainerPanelConfigurationType). Continue with lookup.", e);
        }
        return null;
    }

    public static PrismObject<ResourceType> findResource(PrismPropertyWrapper itemWrapper, PrismPropertyPanelContext panelCtx) {
        PrismObjectWrapper<?> objectWrapper = itemWrapper.findObjectWrapper();
        if (objectWrapper == null) {
            return null;
        }

        if (ResourceType.class.equals(objectWrapper.getObject().getCompileTimeClass())) {
            return (PrismObject<ResourceType>) objectWrapper.getObject();
        } else if (TaskType.class.equals(objectWrapper.getObject().getCompileTimeClass())) {
            PrismReferenceValue objectRef = findResourceReference(itemWrapper);

            if (objectRef == null || objectRef.getOid() == null) {
                return null;
            }

            Task task = panelCtx.getPageBase().createSimpleTask("load resource");
            return WebModelServiceUtils.loadObject(objectRef, ResourceType.COMPLEX_TYPE, panelCtx.getPageBase(), task, task.getResult());
        }
        return null;
    }

    private static PrismReferenceValue findResourceReference(PrismPropertyWrapper<QName> itemWrapper) {
        PrismContainerValueWrapper<?> parent = itemWrapper.getParent();
        if (parent == null) {
            return null;
        }
        try {
            PrismReferenceWrapper<Referencable> resourceRefWrapper = parent.findReference(ResourceObjectSetType.F_RESOURCE_REF);
            if (resourceRefWrapper == null) {
                return null;
            }

            return resourceRefWrapper.getValue().getNewValue();
        } catch (SchemaException e) {
            return null;
        }
    }

    public static ContainerPanelConfigurationType getContainerConfiguration(GuiObjectDetailsPageType pageConfig, String panelType) {
        Optional<ContainerPanelConfigurationType> config = pageConfig
                .getPanel()
                .stream()
                .filter(containerConfig -> panelType.equals(containerConfig.getPanelType()))
                .findFirst();
        return config.orElse(null);
    }

    /**
     * only for 'old' object details pages. Should be removed after only new design will be present.
     */
    @Deprecated
    public static <O extends ObjectType> SummaryPanelSpecificationType getSummaryPanelSpecification(Class<O> type, CompiledGuiProfile compiledGuiProfile) {
        GuiObjectDetailsPageType guiObjectDetailsType = compiledGuiProfile.findObjectDetailsConfiguration(type);
        if (guiObjectDetailsType == null) {
            return null;
        }
        return guiObjectDetailsType.getSummaryPanel();
    }

    public static void addDisabledClassBehavior(Component comp) {
        if (comp == null) {
            return;
        }

        comp.add(AttributeAppender.append("class", () -> !comp.isEnabledInHierarchy() ? "disabled" : null));
    }

    public static CompiledGuiProfile getCompiledGuiProfile(Page page) {
        if (page instanceof PageAdminLTE) {
            return ((PageAdminLTE) page).getCompiledGuiProfile();
        }

        return getCompiledGuiProfile();
    }

    public static CompiledGuiProfile getCompiledGuiProfile() {
        MidPointApplication app = MidPointApplication.get();
        ModelInteractionService service = app.getModelInteractionService();

        Task task = app.createSimpleTask("get compiled gui profile");
        OperationResult result = task.getResult();
        try {
            return service.getCompiledGuiProfile(task, result);
        } catch (Throwable e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot retrieve compiled user profile", e);
            if (InternalsConfig.nonCriticalExceptionsAreFatal()) {
                throw new SystemException("Cannot retrieve compiled user profile: " + e.getMessage(), e);
            } else {
                // Just return empty admin GUI config, so the GUI can go on (and the problem may get fixed)
                return new CompiledGuiProfile();
            }
        }
    }

    public static <T extends FocusType> IResource createJpegPhotoResource(PrismObject<T> object) {
        if (object == null) {
            return null;
        }

        return createJpegPhotoResource(object.asObjectable());
    }

    public static IResource createJpegPhotoResource(FocusType focus) {
        if (focus == null) {
            return null;
        }

        byte[] photo = focus.getJpegPhoto();

        if (photo == null) {
            return null;
        }

        return new ByteArrayResource("image/jpeg", photo);
    }

    public static AdminLTESkin getMidPointSkin() {
        DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
        if (info == null || StringUtils.isEmpty(info.getSkin())) {
            return AdminLTESkin.SKIN_DEFAULT;
        }

        String skin = info.getSkin();
        return AdminLTESkin.create(skin);
    }

    public static void createToastForUpdateObject(AjaxRequestTarget target, Class<? extends ObjectType> type) {
        createToastForObject("AbstractWizardPanel.updateObject", classToQName(type), target);
    }

    public static void createToastForCreateObject(AjaxRequestTarget target, Class<? extends ObjectType> type) {
        createToastForObject("AbstractWizardPanel.createObject", classToQName(type), target);
    }

    public static void createToastForUpdateObject(AjaxRequestTarget target, QName type) {
        createToastForObject("AbstractWizardPanel.updateObject", type, target);
    }

    public static void createToastForCreateObject(AjaxRequestTarget target, QName type) {
        createToastForObject("AbstractWizardPanel.createObject", type, target);
    }

    private static void createToastForObject(String key, QName type, AjaxRequestTarget target) {
        new Toast()
                .success()
                .title(com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate(
                        key,
                        new Object[] { translateMessage(ObjectTypeUtil.createTypeDisplayInformation(type, true)) }))
                .icon("fas fa-circle-check")
                .autohide(true)
                .delay(5_000)
                .body(com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate(
                        key + ".text",
                        new Object[] { translateMessage(ObjectTypeUtil.createTypeDisplayInformation(type, false)) }))
                .show(target);
    }

    public static <O extends ObjectType> String getLabelForType(Class<O> type, boolean startsWithUppercase) {
        return translateMessage(ObjectTypeUtil.createTypeDisplayInformation(type.getSimpleName(), startsWithUppercase));
    }

    public static void showToastForRecordedButUnsavedChanges(AjaxRequestTarget target, PrismContainerValueWrapper value) {
        Collection<ItemDelta> deltas = List.of();
        try {
            deltas = value.getDeltas();
        } catch (SchemaException e) {
            //couldn't get deltas of items
        }

        if (!deltas.isEmpty()) {
            new Toast()
                    .warning()
                    .title(PageBase.createStringResourceStatic("WebComponentUtil.recordedButUnsavedChanges.title").getString())
                    .icon("fa fa-exclamation")
                    .autohide(true)
                    .delay(5_000)
                    .body(PageBase.createStringResourceStatic("WebComponentUtil.recordedButUnsavedChanges.body").getString())
                    .show(target);
        }
    }

    /**
     * @deprecated See {@link com.evolveum.midpoint.gui.api.util.LocalizationUtil}
     */
    @Deprecated
    public static String translateMessage(LocalizableMessage msg) {
        return com.evolveum.midpoint.gui.api.util.LocalizationUtil.translateMessage(msg);
    }

    public static CompiledObjectCollectionView getCompiledObjectCollectionView(
            GuiObjectListViewType listViewType, ContainerPanelConfigurationType config, PageBase pageBase) {
        if (listViewType == null) {
            return null;
        }

        Task task = pageBase.createSimpleTask("Compile collection");
        OperationResult result = task.getResult();
        try {
            CompiledObjectCollectionView compiledCollectionViewFromPanelConfiguration = new CompiledObjectCollectionView();
            pageBase.getModelInteractionService().compileView(compiledCollectionViewFromPanelConfiguration, listViewType, task, result);
            return compiledCollectionViewFromPanelConfiguration;
        } catch (Throwable e) {
            LOGGER.error("Cannot compile object collection view for panel configuration {}. Reason: {}", config, e.getMessage(), e);
            result.recordFatalError("Cannot compile object collection view for panel configuration " + config + ". Reason: " + e.getMessage(), e);
            pageBase.showResult(result);
        }
        return null;
    }

    public static ItemPath getPath(GuiObjectColumnType column) {
        if (column == null) {
            return null;
        }

        if (column.getPath() == null) {
            return null;
        }

        return column.getPath().getItemPath();
    }

    public static boolean hasPopupableParent(@NotNull Component component) {
        Component parent = component.getParent();
        while (parent != null) {
            if (parent instanceof Popupable) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    public static boolean isEnabledExperimentalFeatures() {
        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal == null) {
            return false;
        }

        CompiledGuiProfile profile = principal.getCompiledGuiProfile();

        return BooleanUtils.isTrue(profile.isEnableExperimentalFeatures());
    }

    public static boolean isDarkModeEnabled() {
        MidPointAuthWebSession session = MidPointAuthWebSession.get();
        return session.getSessionStorage().getMode() == SessionStorage.Mode.DARK;
    }

    public static LookupTableType loadLookupTable(String lookupTableOid, PageAdminLTE parentPage) {
        Task task = parentPage.createSimpleTask("Load lookup table");
        OperationResult result = task.getResult();
        Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils
                .createLookupTableRetrieveOptions(parentPage.getSchemaService());
        PrismObject<LookupTableType> prismLookupTable =
                WebModelServiceUtils.loadObject(LookupTableType.class, lookupTableOid, options, parentPage, task, result);
        if (prismLookupTable != null) {
            return prismLookupTable.asObjectable();
        }
        return null;
    }

    public static <O extends AssignmentHolderType> List<String> getArchetypeOidsListByHolderType(
            Class<O> holderType, PageBase pageBase) {
        OperationResult result = new OperationResult("loadArchetypeOidsListByHolderType");
        List<String> oidsList = new ArrayList<>();
        try {
            List<ArchetypeType> filteredArchetypes =
                    pageBase.getModelInteractionService().getFilteredArchetypesByHolderType(holderType, result);
            oidsList = filteredArchetypes
                    .stream()
                    .map(AbstractMutableObjectable::getOid)
                    .collect(Collectors.toList());
        } catch (SchemaException ex) {
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error(
                    "Couldn't load assignment target specification for the object type {} , {}",
                    holderType,
                    ex.getLocalizedMessage());
        }
        return oidsList;
    }

    public static <C extends Containerable> LoadableModel<PrismContainerDefinition<C>> getContainerDefinitionModel(Class<C> clazz) {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<C> load() {
                return PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(clazz);
            }
        };
    }
}
