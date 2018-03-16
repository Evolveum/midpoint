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
package com.evolveum.midpoint.web.session;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.CacheListener;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@Component
public class RepositoryChangeListener implements CacheListener {
	
	private @Autowired CacheDispatcher dispatcher;
	private @Autowired ModelInteractionService modelInteractionService;
	
	@PostConstruct
	public void registerListener() {
		dispatcher.registerCacheListener(this);
	}
	

	@Override
	public <O extends ObjectType> void invalidateCache(Class<O> type, String oid) {
		
		if (FunctionLibraryType.class.equals(type)) {
			modelInteractionService.clearCaches();
		}
	}

}
