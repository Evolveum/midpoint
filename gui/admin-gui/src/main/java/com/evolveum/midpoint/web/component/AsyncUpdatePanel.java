/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AsyncWebProcess;
import com.evolveum.midpoint.web.application.AsyncWebProcessManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessModel;
import com.evolveum.midpoint.web.security.MidPointApplication;

/**
 * @author lazyman
 */
public abstract class AsyncUpdatePanel<V, T extends Serializable> extends BasePanel<T> {

    private static final Trace LOGGER = TraceManager.getTrace(AsyncUpdatePanel.class);

    public static final int DEFAULT_TIMER_DURATION = 2; // seconds

    private IModel<V> callableParameterModel;
    private AsyncWebProcessModel processModel;

    private boolean loadingVisible = true;

    public AsyncUpdatePanel(String id, IModel<V> callableParameterModel, Duration durationSecs) {
        super(id, new Model<>());

        this.callableParameterModel = callableParameterModel;
        this.processModel = new AsyncWebProcessModel<>();

        AjaxSelfUpdatingTimerBehavior selfUpdateBehavior = new AjaxSelfUpdatingTimerBehavior(durationSecs) {

            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                AsyncWebProcess process = processModel.getObject();

                if (!process.isDone()) {
                    return;
                }

                loadingVisible = false;

                try {
                    Future future = process.getFuture();
                    Object result = future.get();
                    Component component = getComponent();
                    if (component instanceof BasePanel) {
                        BasePanel panel = (BasePanel<T>) component;
                        panel.getModel().setObject(result);
                    } else {
                        if (component.getDefaultModel() == null) {
                            component.setDefaultModel(new Model());
                        }
                        component.setDefaultModelObject(result);
                    }

                    stop(target);
                    onPostSuccess(target);
                } catch (InterruptedException ex) {
                    handleError(ex, target);
                } catch (ExecutionException ex) {
                    handleError(ex, target);
                }

                AsyncWebProcessManager manager = MidPointApplication.get().getAsyncWebProcessManager();
                manager.removeProcess(processModel.getId());
            }

            private void handleError(Exception ex, AjaxRequestTarget target) {
                LoggingUtils.logUnexpectedException(LOGGER, "Error occurred while fetching data", ex);

                stop(target);
                onUpdateError(target, ex);
            }
        };
        add(selfUpdateBehavior);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        PageBase page = getPageBase();
        AsyncWebProcessManager manager = page.getAsyncWebProcessManager();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        manager.submit(processModel.getId(), createCallable(auth, callableParameterModel));
    }

    protected abstract void onPostSuccess(AjaxRequestTarget target);

    protected abstract void onUpdateError(AjaxRequestTarget target, Exception ex);

    protected boolean isLoadingVisible() {
        return loadingVisible;
    }

    protected Component getLoadingComponent(final String markupId) {
        return new Label(markupId);
    }

    protected abstract Component getMainComponent(final String markupId);

    /**
     * Create a callable that encapsulates the actual fetching of the data needed
     * by the panel for rendering.
     *
     * @param auth provides {@link org.springframework.security.core.Authentication} object (principal) for async
     * thread which will be used with callable
     * @param callableParameterModel Model providing access to parameters needed by the callable
     * @return A callable instance that encapsulates the logic needed to obtain the panel data
     */
    protected abstract SecurityContextAwareCallable<T> createCallable(Authentication auth, IModel<V> callableParameterModel);
}
