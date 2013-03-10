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

import com.evolveum.midpoint.web.component.GuiComponents;
import com.evolveum.midpoint.web.component.resource.img.ImgResources;
import com.evolveum.midpoint.web.component.util.BaseSimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.time.Duration;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author lazyman
 */
public abstract class AsyncUpdatePanel<V, T> extends BaseSimplePanel {

    private static final ResourceReference PRELOADER =
            new PackageResourceReference(ImgResources.class, "preloader-panel.gif");

    /**
     * Duration in seconds.
     */
    public static final int DEFAULT_TIMER_DURATION = 2;

    private static final String ID_LOADING = "loading";
    private static final String ID_BODY = "body";

    protected transient Future<T> future;
    private boolean loadingVisible = true;

    public AsyncUpdatePanel(String id, IModel<V> callableParameterModel) {
        this(id, callableParameterModel, Duration.seconds(DEFAULT_TIMER_DURATION));
    }

    public AsyncUpdatePanel(String id, IModel<V> callableParameterModel, Duration durationSecs) {
        super(id, new Model());
        future = GuiComponents.submitCallable(createCallable(callableParameterModel));
        FutureUpdateBehavior<T> behaviour = new FutureUpdateBehavior<T>(durationSecs, future) {

            @Override
            protected void onPostSuccess(AjaxRequestTarget target) {
                loadingVisible = false;

                AsyncUpdatePanel.this.onPostSuccess(target);
            }

            @Override
            protected void onUpdateError(AjaxRequestTarget target, Exception ex) {
                loadingVisible = false;

                AsyncUpdatePanel.this.onUpdateError(target, ex);
            }
        };
        add(behaviour);
    }

    protected void initLayout() {
        add(getLoadingComponent(ID_LOADING));
        add(new Label(ID_BODY));
    }

    protected void onPostSuccess(AjaxRequestTarget target) {
        replace(getMainComponent(ID_BODY));

        target.add(this);
    }

    protected void onUpdateError(AjaxRequestTarget target, Exception ex) {
        String message = "Error occurred while fetching data: " + ex.getMessage();
        Label errorLabel = new Label(ID_BODY, message);
        replace(errorLabel);

        target.add(this);
    }

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

    protected abstract Component getMainComponent(final String markupId);

    /**
     * Create a callable that encapsulates the actual fetching of the data needed
     * by the panel for rendering.
     *
     * @param callableParameterModel Model providing access to parameters needed by the callable
     * @return A callable instance that encapsulates the logic needed to obtain the panel data
     */
    protected abstract Callable<T> createCallable(IModel<V> callableParameterModel);
}
