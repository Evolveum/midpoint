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
package com.evolveum.midpoint.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.codec.binary.Base64;

/**
 * Collection of java.io serialization utilities.
 * 
 * WARNING: these utilities are not supposed to be used in a production code. They are intended to be used
 * in tests, prototyping and maybe some run-time diagnostics tools (but think twice before using them anyway).
 * 
 * @author Radovan Semancik
 *
 */
public class SerializationUtil {

    public static Object fromString(String string) throws IOException, ClassNotFoundException {
        byte[] data = Base64.decodeBase64(string);
        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data));
        Object object  = objectInputStream.readObject();
        objectInputStream.close();
        return object;
    }

    public static String toString(Serializable object) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.close();
        return new String(Base64.encodeBase64(byteArrayOutputStream.toByteArray()));
    }

	
}
