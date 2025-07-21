/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web;

import java.util.Locale;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.schema.merger.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractHigherUnitTest;

/**
 * @author lazyman
 */
public abstract class AbstractGuiUnitTest extends AbstractHigherUnitTest {

    protected ModelServiceLocator getServiceLocator() {
        return new ModelServiceLocator() {

            @Override
            public ModelService getModelService() {
                return null;
            }

            @Override
            public ModelInteractionService getModelInteractionService() {
                return null;
            }

            @Override
            public SmartIntegrationService getSmartIntegrationService() {
                return null;
            }

            @Override
            public DashboardService getDashboardService() {
                return null;
            }

            @Override
            public Task createSimpleTask(String operationName) {
                return null;
            }

            @Override
            public PrismContext getPrismContext() {
                return PrismTestUtil.getPrismContext();
            }

            @Override
            public SecurityEnforcer getSecurityEnforcer() {
                return null;
            }

            @Override
            public SecurityContextManager getSecurityContextManager() {
                return null;
            }

            @NotNull
            @Override
            public CompiledGuiProfile getCompiledGuiProfile() {
                return new CompiledGuiProfile();
            }

            @Override
            public Task getPageTask() {
                return null;
            }

            @Override
            public ExpressionFactory getExpressionFactory() {
                return null;
            }

            @Override
            public LocalizationService getLocalizationService() {
                return null;
            }

            @Override
            public Locale getLocale() {
                return Locale.US;
            }

            @Override
            public GuiComponentRegistry getRegistry() {
                return null;
            }

            @Override
            public <O extends ObjectType> PrismObjectWrapperFactory<O> findObjectWrapperFactory(PrismObjectDefinition<O> objectDef) {
                return null;
            }

            @Override
            public <I extends Item, IW extends ItemWrapper> IW createItemWrapper(I item, ItemStatus status, WrapperContext ctx) throws SchemaException {
                return null;
            }

            @Override
            public <IW extends ItemWrapper, VW extends PrismValueWrapper, PV extends PrismValue> VW createValueWrapper(IW parentWrapper, PV newValue, ValueStatus status, WrapperContext context) throws SchemaException {
                return null;
            }

            @Override
            public MidpointFormValidatorRegistry getFormValidatorRegistry() {
                return null;
            }

            @Override
            public AdminGuiConfigurationMergeManager getAdminGuiConfigurationMergeManager() {
                return null;
            }

            @Override
            public CorrelationService getCorrelationService() {
                return null;
            }

            @Override
            public SimulationResultManager getSimulationResultManager() {
                return null;
            }

            @Override
            public RoleAnalysisService getRoleAnalysisService() {
                return null;
            }

            @Override
            public TriggerHandlerRegistry getTriggerHandlerRegistry() {
                return null;
            }
        };
    }
}
