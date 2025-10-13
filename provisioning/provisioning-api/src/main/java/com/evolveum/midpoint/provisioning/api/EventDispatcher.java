/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.api;

/**
 * Dispatcher of change notifications.
 *
 * Instances that implement this interface relay notification from the source of the change notification to the
 * destinations. The destinations are chosen dynamically, using a publish-subscribe mechanism.
 *
 * This interface also includes ResourceObjectChangeListener. By invoking the notifyChange(..) operation of this
 * interface the change will be relayed to all registered listeners.
 *
 * @author Katka Valalikova
 * @author Radovan Semancik
 *
 * TODO shouldn't we distinguish names of the registration/deregistration methods?
 */
public interface EventDispatcher extends ResourceObjectChangeListener, ResourceOperationListener,
        ExternalResourceEventListener, ShadowDeathListener {

    void registerListener(ResourceObjectChangeListener listener);
    void registerListener(ResourceOperationListener listener);
    void registerListener(ExternalResourceEventListener listener);
    void registerListener(ShadowDeathListener listener);

    void unregisterListener(ResourceObjectChangeListener listener);
    void unregisterListener(ResourceOperationListener listener);
    void unregisterListener(ExternalResourceEventListener listener);
    void unregisterListener(ShadowDeathListener listener);
}
