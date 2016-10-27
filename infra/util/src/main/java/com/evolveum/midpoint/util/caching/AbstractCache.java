/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.util.caching;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;

/**
 * @author mederly
 */
public abstract class AbstractCache {

    private int entryCount = 0;

    public static <T extends AbstractCache> T enter(ThreadLocal<T> cacheThreadLocal, Class<T> cacheClass, Trace logger) {
        T inst = cacheThreadLocal.get();
        logger.trace("Cache: ENTER for thread {}, {}", Thread.currentThread().getName(), inst);
        if (inst == null) {
            logger.trace("Cache: creating for thread {}",Thread.currentThread().getName());
            try {
                inst = cacheClass.newInstance();
            } catch (InstantiationException|IllegalAccessException e) {
                throw new SystemException("Couldn't instantiate cache: " + e.getMessage(), e);
            }
            cacheThreadLocal.set(inst);
        }
        inst.incrementEntryCount();
        return inst;
    }

    public static <T extends AbstractCache> T exit(ThreadLocal<T> cacheThreadLocal, Trace logger) {
        T inst = cacheThreadLocal.get();
        logger.trace("Cache: EXIT for thread {}, {}", Thread.currentThread().getName(), inst);
        if (inst == null || inst.getEntryCount() == 0) {
            logger.error("Cache: Attempt to exit cache that does not exist or has entry count 0: {}", inst);
            cacheThreadLocal.set(null);
        } else {
            inst.decrementEntryCount();
            if (inst.getEntryCount() <= 0) {
                destroy(cacheThreadLocal, logger);
            }
        }
        return inst;
    }

    public static <T extends AbstractCache> void destroy(ThreadLocal<T> cacheThreadLocal, Trace logger) {
        T inst = cacheThreadLocal.get();
        if (inst != null) {
            logger.trace("Cache: DESTROY for thread {}", Thread.currentThread().getName());
            cacheThreadLocal.set(null);
        }
    }

    public void incrementEntryCount() {
        entryCount++;
    }

    public void decrementEntryCount() {
        entryCount--;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public static boolean exists(ThreadLocal<? extends AbstractCache> cacheThreadLocal) {
        return cacheThreadLocal.get() != null;
    }

    public static <T extends AbstractCache> String debugDump(ThreadLocal<T> cacheThreadLocal) {
        T inst = cacheThreadLocal.get();
        StringBuilder sb = new StringBuilder("Cache ");
        if (inst != null) {
            sb.append("exists, entry count ");
            sb.append(inst.getEntryCount());
            sb.append(", content: ");
            sb.append(inst.description());
        } else {
            sb.append("doesn't exist");
        }
        return sb.toString();
    }

    abstract public String description();

}
