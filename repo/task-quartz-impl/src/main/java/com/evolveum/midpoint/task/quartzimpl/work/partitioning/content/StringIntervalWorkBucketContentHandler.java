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

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringIntervalWorkBucketContentType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class StringIntervalWorkBucketContentHandler extends IntervalWorkBucketContentHandler {

	@PostConstruct
	public void register() {
		registry.registerHandler(StringIntervalWorkBucketContentType.class, this);
	}

	@Override
	protected boolean hasNoBoundaries(AbstractWorkBucketContentType bucketContent) {
		StringIntervalWorkBucketContentType cnt = (StringIntervalWorkBucketContentType) bucketContent;
		return cnt == null || cnt.getFrom() == null && cnt.getTo() == null;
	}

	@Override
	protected Object getFrom(AbstractWorkBucketContentType content) {
		return ((StringIntervalWorkBucketContentType) content).getFrom();
	}

	@Override
	protected Object getTo(AbstractWorkBucketContentType content) {
		return ((StringIntervalWorkBucketContentType) content).getTo();
	}
}
