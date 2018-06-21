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
