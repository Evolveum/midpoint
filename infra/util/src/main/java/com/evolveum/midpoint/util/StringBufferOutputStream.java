/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.util;

/*
 * Created on Dec 25, 2004
 *
 * Copyright 2005 CafeSip.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author amit
 *
 */
public class StringBufferOutputStream extends OutputStream
{
    private StringBuffer textBuffer = new StringBuffer();

    /**
     *
     */
    public StringBufferOutputStream()
    {
        super();
    }

    /*
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException
    {
        char a = (char)b;
        textBuffer.append(a);
    }

    public String toString()
    {
        return textBuffer.toString();
    }

    public void clear()
    {
        textBuffer.delete(0, textBuffer.length());
    }
}