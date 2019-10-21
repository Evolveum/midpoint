/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.execution;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Source: Eamonn McManus
 * http://weblogs.java.net/blog/emcmanus/archive/2007/05/making_a_jmx_co.html
 *
 */
public class JmxClient {

    public static JMXConnector connectWithTimeout(
            final JMXServiceURL url, final Map<String,Object> env, long timeout, TimeUnit unit)
            throws IOException {

        final BlockingQueue<Object> mailbox = new ArrayBlockingQueue<>(1);
        ExecutorService executor =
                Executors.newSingleThreadExecutor(DAEMON_THREAD_FACTORY);
        executor.submit(new Runnable() {
            public void run() {
                try {
                    JMXConnector connector = JMXConnectorFactory.connect(url, env);
                    if (!mailbox.offer(connector))
                        connector.close();
                } catch (Throwable t) {
                    mailbox.offer(t);
                }
            }
        });
        Object result;
        try {
            result = mailbox.poll(timeout, unit);
            if (result == null) {
                if (!mailbox.offer(""))
                    result = mailbox.take();
            }
        } catch (InterruptedException e) {
            throw initCause(new InterruptedIOException(e.getMessage()), e);
        } finally {
            executor.shutdown();
        }
        if (result == null)
            throw new SocketTimeoutException("Connect timed out: " + url);
        if (result instanceof JMXConnector)
            return (JMXConnector) result;
        try {
            throw (Throwable) result;
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable e) {
            // In principle this can't happen but we wrap it anyway
            throw new IOException(e.toString(), e);
        }
    }

    private static <T extends Throwable> T initCause(T wrapper, Throwable wrapped) {
        wrapper.initCause(wrapped);
        return wrapper;
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        }
    }
    private static final ThreadFactory DAEMON_THREAD_FACTORY = new DaemonThreadFactory();
}
