/**
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.infra.wsutil;

/**
 * Run with:
 *
 * mvn exec:java -Dexec.classpathScope=test -Dexec.mainClass="com.evolveum.midpoint.infra.wsutil.DummyMain" -Dexec.args="...."
 *
 * @author semancik
 *
 */
public class DummyMain {

	public static void main(String[] args) {
		DummyClient client = new DummyClient();
		client.main(args);
	}

}
