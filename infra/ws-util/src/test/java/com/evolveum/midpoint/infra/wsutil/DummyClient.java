/**
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.infra.wsutil;

import java.net.MalformedURLException;

/**
 * @author semancik
 *
 */
public class DummyClient extends AbstractWebServiceClient<DummyPort, DummyService>{

	@Override
	protected DummyService createService() throws MalformedURLException {
		return new DummyService();
	}

	@Override
	protected Class<DummyPort> getPortClass() {
		return DummyPort.class;
	}

	@Override
	protected String getDefaultUsername() {
		return "defaultUser";
	}

	@Override
	protected String getDefaultPassword() {
		return "defaultPassword";
	}

	@Override
	protected String getDefaultEndpointUrl() {
		return "defaultEndpoint";
	}

	@Override
	protected int invoke(DummyPort port) {
		System.out.println("INVOKE");
		return 0;
	}

}
