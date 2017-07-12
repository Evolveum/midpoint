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

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author lazyman
 */
public abstract class FutureUpdateBehavior<T> extends AbstractAjaxTimerBehavior {

    private static final Trace LOGGER = TraceManager.getTrace(FutureUpdateBehavior.class);

    private transient Future<T> future;

    public FutureUpdateBehavior(Duration updateInterval, Future<T> future) {
        super(updateInterval);

        this.future = future;
    }

    @Override
    protected void onTimer(final AjaxRequestTarget target) {
        if (future == null || !future.isDone()) {
            return;
        }

        try {
            T data = future.get();
            Component component = getComponent();
            if (component instanceof BasePanel) {
            	BasePanel<T> panel = (BasePanel<T>) component;
                panel.getModel().setObject(data);
            } else {
                if (component.getDefaultModel() == null) {
                    component.setDefaultModel(new Model());
                }
                component.setDefaultModelObject(data);
            }

            stop(target);
            onPostSuccess(target);
        } catch (InterruptedException ex) {
            handleError(ex, target);
        } catch (ExecutionException ex) {
            handleError(ex, target);
        }
    }

    private void handleError(Exception ex, AjaxRequestTarget target) {
        LoggingUtils.logUnexpectedException(LOGGER, "Error occurred while fetching data", ex);

        stop(target);
        onUpdateError(target, ex);
    }

    protected abstract void onPostSuccess(AjaxRequestTarget target);

    protected abstract void onUpdateError(AjaxRequestTarget target, Exception ex);
}
