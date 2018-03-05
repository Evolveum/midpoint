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

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AsyncWebProcess;
import com.evolveum.midpoint.web.application.AsyncWebProcessManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.time.Duration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author lazyman
 */
public abstract class AsyncUpdatePanel<V, T extends Serializable> extends BasePanel<T> {

    private static final Trace LOGGER = TraceManager.getTrace(AsyncUpdatePanel.class);

    private static final ResourceReference PRELOADER = new PackageResourceReference(ImgResources.class, "ajax-loader.gif");

    public static final int DEFAULT_TIMER_DURATION = 2; // seconds

    private IModel<V> callableParameterModel;
    private AsyncWebProcessModel processModel;

    private boolean loadingVisible = true;

    public AsyncUpdatePanel(String id, IModel<V> callableParameterModel, Duration durationSecs) {
        super(id, new Model<T>());

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
        Image image = new Image(markupId, PRELOADER);
        image.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isLoadingVisible();
            }
        });

        return image;
    }

    protected abstract Component getMainComponent(String markupId);

    /**
     * Create a callable that encapsulates the actual fetching of the data needed
     * by the panel for rendering.
     *
     * @param auth                   provides {@link org.springframework.security.core.Authentication} object (principal) for async
     *                               thread which will be used with callable
     * @param callableParameterModel Model providing access to parameters needed by the callable
     * @return A callable instance that encapsulates the logic needed to obtain the panel data
     */
    protected abstract SecurityContextAwareCallable<T> createCallable(Authentication auth, IModel<V> callableParameterModel);
}
