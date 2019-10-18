/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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

    public static <T> T fromString(String string) throws IOException, ClassNotFoundException {
        byte[] data = Base64.decodeBase64(string);
        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data));
        Object object  = objectInputStream.readObject();
        objectInputStream.close();
        return (T)object;
    }

    public static String toString(Object object) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.close();
        return new String(Base64.encodeBase64(byteArrayOutputStream.toByteArray()));
    }


}
