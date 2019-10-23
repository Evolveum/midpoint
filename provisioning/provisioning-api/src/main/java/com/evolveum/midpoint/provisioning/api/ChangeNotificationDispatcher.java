/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
 */
public interface ChangeNotificationDispatcher extends ResourceObjectChangeListener, ResourceOperationListener, ResourceEventListener {

    void registerNotificationListener(ResourceObjectChangeListener listener);
    void registerNotificationListener(ResourceOperationListener listener);
    void registerNotificationListener(ResourceEventListener listener);

    void unregisterNotificationListener(ResourceObjectChangeListener listener);
    void unregisterNotificationListener(ResourceOperationListener listener);
    void unregisterNotificationListener(ResourceEventListener listener);
}
