/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.async;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.BaseSimplePanel;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;

import java.io.Serializable;
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
        if (future.isDone()) {
            return;
        }

        try {
            T data = future.get();
            Component component = getComponent();
            if (component instanceof BaseSimplePanel) {
                BaseSimplePanel panel = (BaseSimplePanel) component;
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
        LoggingUtils.logException(LOGGER, "Error occurred while fetching data", ex);

        stop(target);
        onUpdateError(target, ex);
    }

    protected abstract void onPostSuccess(AjaxRequestTarget target);

    protected abstract void onUpdateError(AjaxRequestTarget target, Exception ex);
}