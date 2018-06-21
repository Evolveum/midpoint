/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.task.quartzimpl;

import org.springframework.stereotype.Service;

import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;

/**
 * @author semancik
 *
 * TODO: hostIdentifier
 */
@Service
public class LightweightIdentifierGeneratorImpl implements LightweightIdentifierGenerator {

	long lastTimestamp;
	int lastSequence;
	int hostIdentifier;

	public LightweightIdentifierGeneratorImpl() {
		lastTimestamp = 0;
		lastSequence = 0;
		hostIdentifier = 0;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.task.api.LightweightIdentifierGenerator#generate()
	 */
	@Override
	public synchronized LightweightIdentifier generate() {
		long timestamp = System.currentTimeMillis();
		if (timestamp == lastTimestamp) {
			// Nothing to do
		} else if (timestamp > lastTimestamp) {
			// reset the last timestamp and sequence conunter
			lastTimestamp = timestamp;
			lastSequence = 0;
		} else {
			throw new IllegalStateException("The time has moved back, possible consistency violation");
		}
		return new LightweightIdentifier(timestamp, hostIdentifier, ++lastSequence);
	}

}
