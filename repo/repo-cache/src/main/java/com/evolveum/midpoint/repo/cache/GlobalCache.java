/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.cache2k.Cache2kBuilder;
import org.cache2k.processor.EntryProcessor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Component
public class GlobalCache {

	private org.cache2k.Cache<GlobalCacheObjectKey, GlobalCacheObjectValue> objectCache;
	private org.cache2k.Cache<QueryKey, SearchResultList> queryCache;

	@PostConstruct
	public void initialize() {
		objectCache = new Cache2kBuilder<GlobalCacheObjectKey, GlobalCacheObjectValue>() {}
				.name("objectCache")
				.expireAfterWrite(5, TimeUnit.MINUTES)
				.build();
		queryCache = new Cache2kBuilder<QueryKey, SearchResultList>() {}
				.name("queryCache")
				.expireAfterWrite(5, TimeUnit.MINUTES)
				.build();
	}

	public <T extends ObjectType> GlobalCacheObjectValue<T> getObject(GlobalCacheObjectKey key) {
		GlobalCacheObjectValue rv = objectCache.peek(key);
		//System.out.println("get(" + key + ") => " + rv);
		//noinspection unchecked
		return rv;
	}

	public void removeObject(GlobalCacheObjectKey cacheKey) {
		//System.out.println("Removing: " + cacheKey);
		objectCache.remove(cacheKey);
	}

	public <T extends ObjectType> void putObject(GlobalCacheObjectKey key, GlobalCacheObjectValue<T> cacheObject) {
		objectCache.put(key, cacheObject);
		//System.out.println("Putting: " + key + " => " + cacheObject);
	}

	public <T extends ObjectType> SearchResultList<PrismObject<T>> getQuery(QueryKey key) {
		//noinspection unchecked
		return queryCache.peek(key);
	}

	public void removeQuery(QueryKey cacheKey) {
		//System.out.println("Removing: " + cacheKey);
		queryCache.remove(cacheKey);
	}

	public <T extends ObjectType> void putQuery(QueryKey key, SearchResultList<PrismObject<T>> cacheObject) {
		queryCache.put(key, cacheObject);
		//System.out.println("Putting: " + key + " => " + cacheObject);
	}

	public void invokeOnAllQueries(EntryProcessor<QueryKey, SearchResultList, Void> entryProcessor) {
		queryCache.invokeAll(queryCache.keys(), entryProcessor);
	}
}
