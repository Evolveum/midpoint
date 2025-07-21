/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.schema.merger.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import java.util.Locale;

import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 * Interface that allows location of model and model-like services,
 * such as ModelService and ModelInteractionService.
 * Used by GUI components that need to interact with the midPoint IDM model,
 * especially for loading data.
 * Usually implemented by PageBase and similar "central" GUI classes.
 *
 * @author Radovan Semancik
 */
public interface ModelServiceLocator {

    ModelService getModelService();

    ModelInteractionService getModelInteractionService();

    SmartIntegrationService getSmartIntegrationService();

    DashboardService getDashboardService();

    LocalizationService getLocalizationService();

    Task createSimpleTask(String operationName);

    /**
     * Returns a task, that is used to retrieve and render the entire content
     * of the page. A single task is created to render the whole page, so
     * the summary result can be collected in the task result.
     */
    Task getPageTask();

    PrismContext getPrismContext();

    SecurityEnforcer getSecurityEnforcer();

    SecurityContextManager getSecurityContextManager();

    ExpressionFactory getExpressionFactory();

    /**
     * Returns currently applicable user profile, compiled for efficient use in the user interface.
     * applicable to currently logged-in user.
     * Strictly speaking, this can be retrieved from modelInteractionService.
     * But having a separate function for that allows to get rid of
     * task and result parameters. And more importantly: this allows to
     * cache adminGuiConfig in the page (in case many components need it).
     */
    @NotNull
    CompiledGuiProfile getCompiledGuiProfile();

    default ObjectResolver getModelObjectResolver() {
        return null;
    }

    Locale getLocale();

    GuiComponentRegistry getRegistry();

    <O extends ObjectType> PrismObjectWrapperFactory<O> findObjectWrapperFactory(PrismObjectDefinition<O> objectDef);

    <I extends Item, IW extends ItemWrapper> IW createItemWrapper(I item, ItemStatus status, WrapperContext ctx) throws SchemaException;

    <IW extends ItemWrapper, VW extends PrismValueWrapper, PV extends PrismValue> VW createValueWrapper(IW parentWrapper, PV newValue, ValueStatus status, WrapperContext context) throws SchemaException;

    MidpointFormValidatorRegistry getFormValidatorRegistry();

    AdminGuiConfigurationMergeManager getAdminGuiConfigurationMergeManager();

    CorrelationService getCorrelationService();

    /**
     * Experimental, functionality will be probably later hidden behind {@link ModelInteractionService}
     */
    SimulationResultManager getSimulationResultManager();

    RoleAnalysisService getRoleAnalysisService();

    TriggerHandlerRegistry getTriggerHandlerRegistry();
}
