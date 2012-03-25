/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
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
