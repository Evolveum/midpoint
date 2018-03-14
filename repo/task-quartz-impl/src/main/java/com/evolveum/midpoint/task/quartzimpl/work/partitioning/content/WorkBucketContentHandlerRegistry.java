/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.task.quartzimpl.work.partitioning.content;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for creation of configured work bucket content handlers.
 *
 * @author mederly
 */
@Component
public class WorkBucketContentHandlerRegistry {

	private final Map<Class<? extends AbstractWorkBucketContentType>, WorkBucketContentHandler> handlers = new HashMap<>();

	@NotNull
	public WorkBucketContentHandler getHandler(AbstractWorkBucketContentType content) {
		WorkBucketContentHandler handler = handlers.get(content != null ? content.getClass() : null);
		if (handler != null) {
			return handler;
		} else {
			throw new IllegalStateException("Unknown or unsupported work bucket content type: " + content);
		}
	}

	public void registerHandler(Class<? extends AbstractWorkBucketContentType> contentClass, WorkBucketContentHandler handler) {
		handlers.put(contentClass, handler);
	}
}
