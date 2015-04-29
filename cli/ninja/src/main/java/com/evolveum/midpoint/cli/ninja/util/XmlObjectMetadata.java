/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.cli.ninja.util;

/**
 * @author Viliam Repan (lazyman)
 */
public class XmlObjectMetadata {

    private int startLine;
    private int endLine;
    private int serialNumber;

    private Exception exception;

    public int getEndLine() {
        return endLine;
    }

    void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }

    public int getStartLine() {
        return startLine;
    }

    void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public Exception getException() {
        return exception;
    }

    void setException(Exception exception) {
        this.exception = exception;
    }
}
