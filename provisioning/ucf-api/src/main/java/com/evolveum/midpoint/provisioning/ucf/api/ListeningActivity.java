/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateListeningActivityInformationType;

/**
 *
 */
public interface ListeningActivity {

	void stop();

	AsyncUpdateListeningActivityInformationType getInformation();
}
