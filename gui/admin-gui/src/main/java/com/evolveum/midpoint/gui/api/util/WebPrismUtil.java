/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import javax.xml.namespace.QName;
import java.util.Collections;

/**
 * @author katka
 *
 */
public class WebPrismUtil {

    private static final Trace LOGGER = TraceManager.getTrace(WebPrismUtil.class);

    private static final String DOT_CLASS = WebPrismUtil.class.getName() + ".";
    private static final String OPERATION_CREATE_NEW_VALUE = DOT_CLASS + "createNewValue";

    public static <ID extends ItemDefinition<I>, I extends Item<V, ID>, V extends PrismValue> String getHelpText(ID def) {
        String doc = def.getHelp();
        if (StringUtils.isEmpty(doc)) {
            doc = def.getDocumentation();
            if (StringUtils.isEmpty(doc)) {
                return null;
            }
        }

        return doc.replaceAll("\\s{2,}", " ").trim();
    }


    public static <IW extends ItemWrapper, PV extends PrismValue, VW extends PrismValueWrapper> VW createNewValueWrapper(IW itemWrapper, PV newValue, PageBase pageBase, AjaxRequestTarget target) {
        LOGGER.debug("Adding value to {}", itemWrapper);

        OperationResult result = new OperationResult(OPERATION_CREATE_NEW_VALUE);

        VW newValueWrapper = null;
        try {

            if (!(itemWrapper instanceof PrismContainerWrapper)) {
                itemWrapper.getItem().add(newValue);
            }

            Task task = pageBase.createSimpleTask(OPERATION_CREATE_NEW_VALUE);

            WrapperContext context = new WrapperContext(task, result);
            context.setObjectStatus(itemWrapper.findObjectStatus());
            context.setShowEmpty(true);
            context.setCreateIfEmpty(true);

            newValueWrapper = pageBase.createValueWrapper(itemWrapper, newValue, ValueStatus.ADDED, context);
            itemWrapper.getValues().add(newValueWrapper);
            result.recordSuccess();

        } catch (SchemaException e) {
            LOGGER.error("Cannot create new value for {}", itemWrapper, e);
            result.recordFatalError(pageBase.createStringResource("WebPrismUtil.message.createNewValueWrapper.fatalError", newValue, e.getMessage()).getString(), e);
            target.add(pageBase.getFeedbackPanel());
        }

        return newValueWrapper;
    }

    public static <IW extends ItemWrapper> IW findItemWrapper(ItemWrapper<?,?,?,?> child, ItemPath absoluthPathToFind, Class<IW> wrapperClass) {
        PrismObjectWrapper<?> taskWrapper = child.findObjectWrapper();
        try {
            return taskWrapper.findItem(ItemPath.create(absoluthPathToFind), wrapperClass);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get obejct reference value, {}", e, e.getMessage());
            return null;
        }
    }

    public static <R extends Referencable> PrismReferenceWrapper<R> findReferenceWrapper(ItemWrapper<?,?,?,?> child, ItemPath pathToFind) {
        return findItemWrapper(child, pathToFind, PrismReferenceWrapper.class);
    }

    public static <T> PrismPropertyWrapper<T> findPropertyWrapper(ItemWrapper<?,?,?,?> child, ItemPath pathToFind) {
        return findItemWrapper(child, pathToFind, PrismPropertyWrapper.class);
    }

    public static <R extends Referencable> PrismReferenceValue findSingleReferenceValue(ItemWrapper<?,?,?,?> child, ItemPath pathToFind) {
        PrismReferenceWrapper<R> objectRefWrapper = findReferenceWrapper(child, pathToFind);
        if (objectRefWrapper == null) {
            return null;
        }

        try {
            return objectRefWrapper.getValue().getNewValue();
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get object reference value, {}", e, e.getMessage());
            return null;
        }
    }

    public static <T> PrismPropertyValue<T> findSinglePropertyValue(ItemWrapper<?,?,?,?> child, ItemPath pathToFind) {
        PrismPropertyWrapper<T> propertyWrapper = findPropertyWrapper(child, pathToFind);
        if (propertyWrapper == null) {
            return null;
        }

        try {
            return propertyWrapper.getValue().getNewValue();
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get object reference value, {}", e, e.getMessage());
            return null;
        }
    }

}
