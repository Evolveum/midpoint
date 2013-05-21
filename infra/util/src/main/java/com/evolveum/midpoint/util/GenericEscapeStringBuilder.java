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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GenericEscapeStringBuilder implements Serializable, Appendable, CharSequence, EscapeStringBuilder {

	/**
	 * Serial version UID 
	 */
	private static final long serialVersionUID = 3473815007153617453L;

	private StringBuilder _sb;


	public GenericEscapeStringBuilder() {
		_sb = new StringBuilder();
	}

	public GenericEscapeStringBuilder(CharSequence seq) {
		_sb = new StringBuilder(seq);
	}

	public GenericEscapeStringBuilder(int capacity) {
		_sb = new StringBuilder(capacity);
	}

	public GenericEscapeStringBuilder(String str) {
		_sb = new StringBuilder(str);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(boolean)
	 */
	@Override
	public EscapeStringBuilder append(boolean b) {
		_sb.append(b);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(char)
	 */
	@Override
	public GenericEscapeStringBuilder append(char c) {
		_sb.append(c);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(char[])
	 */
	@Override
	public EscapeStringBuilder append(char[] str) {
		_sb.append(str);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(char[], int, int)
	 */
	@Override
	public EscapeStringBuilder append(char[] str, int offset, int len) {
		_sb.append(str, offset, len);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(java.lang.CharSequence)
	 */
	@Override
	public GenericEscapeStringBuilder append(CharSequence s) {
		_sb.append(s);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(java.lang.CharSequence, int, int)
	 */
	@Override
	public GenericEscapeStringBuilder append(CharSequence s, int start, int end) {
		_sb.append(s, start, end);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(double)
	 */
	@Override
	public EscapeStringBuilder append(double d) {
		_sb.append(d);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(float)
	 */
	@Override
	public EscapeStringBuilder append(float f) {
		_sb.append(f);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(int)
	 */
	@Override
	public EscapeStringBuilder append(int i) {
		_sb.append(i);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(long)
	 */
	@Override
	public EscapeStringBuilder append(long lng) {
		_sb.append(lng);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(java.lang.Object)
	 */
	@Override
	public EscapeStringBuilder append(Object obj) {
		_sb.append(obj);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(java.lang.String)
	 */
	@Override
	public EscapeStringBuilder append(String str) {
		_sb.append(str);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(java.lang.StringBuffer)
	 */
	@Override
	public EscapeStringBuilder append(StringBuffer sb) {
		this._sb.append(sb);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(java.lang.StringBuilder)
	 */
	@Override
	public EscapeStringBuilder append(StringBuilder sb) {
		this._sb.append(sb);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#append(com.evolveum.midpoint.util.GenericEscapeStringBuilder)
	 */
	@Override
	public EscapeStringBuilder append(GenericEscapeStringBuilder xsb) {
		this._sb.append(xsb);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#appendCodePoint(int)
	 */
	@Override
	public EscapeStringBuilder appendCodePoint(int codePoint) {
		_sb.appendCodePoint(codePoint);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#capacity()
	 */
	@Override
	public int capacity() {
		return _sb.capacity();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#charAt(int)
	 */
	@Override
	public char charAt(int index) {
		return charAt(index);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#codePointAt(int)
	 */
	@Override
	public int codePointAt(int index) {
		return _sb.codePointAt(index);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#codePointBefore(int)
	 */
	@Override
	public int codePointBefore(int index) {
		return _sb.codePointBefore(index);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#codePointCount(int, int)
	 */
	@Override
	public int codePointCount(int beginIndex, int endIndex) {
		return _sb.codePointCount(beginIndex, endIndex);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#eappend(java.lang.String)
	 */
	@Override
	public EscapeStringBuilder eappend(String str) {
		_sb.append(escape(str));
		return this;
	}

	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#eappend(java.lang.Object)
	 */
	@Override
	public EscapeStringBuilder eappend(Object o) {
		_sb.append(escape(o.toString()));
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#delete(int, int)
	 */
	@Override
	public EscapeStringBuilder delete(int start, int end) {
		_sb.delete(start, end);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#deleteCharAt(int)
	 */
	@Override
	public EscapeStringBuilder deleteCharAt(int index) {
		_sb.deleteCharAt(index);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#ensureCapacity(int)
	 */
	@Override
	public void ensureCapacity(int minimumCapacity) {
		_sb.ensureCapacity(minimumCapacity);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#getChars(int, int, char[], int)
	 */
	@Override
	public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
		_sb.getChars(srcBegin, srcEnd, dst, dstBegin);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#indexOf(java.lang.String)
	 */
	@Override
	public int indexOf(String str) {
		return _sb.indexOf(str);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#indexOf(java.lang.String, int)
	 */
	@Override
	public int indexOf(String str, int fromIndex) {
		return _sb.indexOf(str, fromIndex);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#insert(int, boolean)
	 */
	@Override
	public EscapeStringBuilder insert(int offset, boolean b) {
		_sb.insert(offset, b);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#insert(int, char)
	 */
	@Override
	public EscapeStringBuilder insert(int offset, char c) {
		_sb.insert(offset, c);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#insert(int, char[])
	 */
	@Override
	public EscapeStringBuilder insert(int offset, char[] str) {
		_sb.insert(offset, str);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#insert(int, char[], int, int)
	 */
	@Override
	public EscapeStringBuilder insert(int index, char[] str, int offset, int len) {
		_sb.insert(index, str, offset, len);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#insert(int, java.lang.CharSequence)
	 */
	@Override
	public EscapeStringBuilder insert(int dstOffset, CharSequence s) {
		_sb.insert(dstOffset, s);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#insert(int, java.lang.CharSequence, int, int)
	 */
	@Override
	public EscapeStringBuilder insert(int dstOffset, CharSequence s, int start, int end) {
		_sb.insert(dstOffset, s, start, end);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#insert(int, double)
	 */
	@Override
	public EscapeStringBuilder insert(int offset, double d) {
		_sb.insert(offset, d);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#insert(int, float)
	 */
	@Override
	public EscapeStringBuilder insert(int offset, float f) {
		_sb.insert(offset, f);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#insert(int, int)
	 */
	@Override
	public EscapeStringBuilder insert(int offset, int i) {
		_sb.insert(offset, i);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#insert(int, long)
	 */
	@Override
	public EscapeStringBuilder insert(int offset, long l) {
		_sb.insert(offset, l);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#insert(int, java.lang.Object)
	 */
	@Override
	public EscapeStringBuilder insert(int offset, Object obj) {
		_sb.insert(offset, obj);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#insert(int, java.lang.String)
	 */
	@Override
	public EscapeStringBuilder insert(int offset, String str) {
		_sb.insert(offset, str);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#lastIndexOf(java.lang.String)
	 */
	@Override
	public int lastIndexOf(String str) {
		return _sb.lastIndexOf(str);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#lastIndexOf(java.lang.String, int)
	 */
	@Override
	public int lastIndexOf(String str, int fromIndex) {
		return _sb.lastIndexOf(str, fromIndex);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#length()
	 */
	@Override
	public int length() {
		return _sb.length();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#offsetByCodePoints(int, int)
	 */
	@Override
	public int offsetByCodePoints(int index, int codePointOffset) {
		return _sb.offsetByCodePoints(index, codePointOffset);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#replace(int, int, java.lang.String)
	 */
	@Override
	public EscapeStringBuilder replace(int start, int end, String str) {
		_sb.replace(start, end, str);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#reverse()
	 */
	@Override
	public EscapeStringBuilder reverse() {
		_sb.reverse();
		return this;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#setCharAt(int, char)
	 */
	@Override
	public void setCharAt(int index, char ch) {
		_sb.setCharAt(index, ch);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#setLength(int)
	 */
	@Override
	public void setLength(int newLength) {
		_sb.setLength(newLength);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#subSequence(int, int)
	 */
	@Override
	public CharSequence subSequence(int start, int end) {
		return _sb.subSequence(start, end);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#substring(int)
	 */
	@Override
	public String substring(int start) {
		return _sb.substring(start);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#substring(int, int)
	 */
	@Override
	public String substring(int start, int end) {
		return _sb.substring(start, end);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#toString()
	 */
	@Override
	public String toString() {
		return _sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#toStringBuilder()
	 */
	@Override
	public StringBuilder toStringBuilder() {
		return _sb;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.EscapeStringBuilder#trimToSize()
	 */
	@Override
	public void trimToSize() {
		_sb.trimToSize();
	}
	
	// Do nothing
	private EscapeStringBuilder escape(String input) {
		return this;
	}
	
}
