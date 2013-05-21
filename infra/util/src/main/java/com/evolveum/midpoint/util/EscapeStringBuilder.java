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

public interface EscapeStringBuilder {

	/**
	    Appends the string representation of the boolean argument to the sequence.
	*/
	public abstract EscapeStringBuilder append(boolean b);

	/**
	    Appends the string representation of the char argument to this sequence.
	*/
	public abstract EscapeStringBuilder append(char c);

	/**
	    Appends the string representation of the char array argument to this sequence.
	*/
	public abstract EscapeStringBuilder append(char[] str);

	/**
	    Appends the string representation of a subarray of the char array argument to this sequence.
	*/
	public abstract EscapeStringBuilder append(char[] str, int offset, int len);

	/**
	    Appends the specified character sequence to this Appendable.
	*/
	public abstract EscapeStringBuilder append(CharSequence s);

	/**
	    Appends a subsequence of the specified CharSequence to this sequence.
	*/
	public abstract EscapeStringBuilder append(CharSequence s, int start, int end);

	/**
	    Appends the string representation of the double argument to this sequence.
	*/
	public abstract EscapeStringBuilder append(double d);

	/**
	    Appends the string representation of the float argument to this sequence.
	*/
	public abstract EscapeStringBuilder append(float f);

	/**
	    Appends the string representation of the int argument to this sequence.
	*/
	public abstract EscapeStringBuilder append(int i);

	/**
	    Appends the string representation of the long argument to this sequence.
	*/
	public abstract EscapeStringBuilder append(long lng);

	/**
	    Appends the string representation of the Object argument.
	*/
	public abstract EscapeStringBuilder append(Object obj);

	/**
	    Appends the specified string to this character sequence.
	*/
	public abstract EscapeStringBuilder append(String str);

	/**
	    Appends the specified StringBuffer to this sequence.
	*/
	public abstract EscapeStringBuilder append(StringBuffer sb);

	/**
	Appends the specified StringBuilder to this sequence.
	*/
	public abstract EscapeStringBuilder append(StringBuilder sb);

	/**
	Appends the specified StringBuilder to this sequence.
	*/
	public abstract EscapeStringBuilder append(GenericEscapeStringBuilder xsb);

	/**
	    Appends the string representation of the codePoint argument to this sequence.
	*/
	public abstract EscapeStringBuilder appendCodePoint(int codePoint);

	/**
	    Returns the current capacity.
	*/
	public abstract int capacity();

	/**
	    Returns the char value in this sequence at the specified index.
	*/
	public abstract char charAt(int index);

	/**
	    Returns the character (Unicode code point) at the specified index.
	*/
	public abstract int codePointAt(int index);

	/**
	    Returns the character (Unicode code point) before the specified index.
	*/
	public abstract int codePointBefore(int index);

	/**
	    Returns the number of Unicode code points in the specified text range of this sequence.
	*/
	public abstract int codePointCount(int beginIndex, int endIndex);

	/**
	 * Appends the string representation of the String argument and escape value  with XML escape
	 */
	public abstract EscapeStringBuilder eappend(String str);

	/**
	 * Appends the string representation of the Object.toString() argument and escape value  with XML escape
	 */
	public abstract EscapeStringBuilder eappend(Object o);

	/**
	    Removes the characters in a substring of this sequence.
	*/
	public abstract EscapeStringBuilder delete(int start, int end);

	/**
	    Removes the char at the specified position in this sequence.
	*/
	public abstract EscapeStringBuilder deleteCharAt(int index);

	/**
	    Ensures that the capacity is at least equal to the specified minimum.
	*/
	public abstract void ensureCapacity(int minimumCapacity);

	/**
	    Characters are copied from this sequence into the destination character array dst.
	*/
	public abstract void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin);

	/**
	    Returns the index within this string of the first occurrence of the specified substring.
	*/
	public abstract int indexOf(String str);

	/**
	    Returns the index within this string of the first occurrence of the specified substring, starting at the specified index.
	*/
	public abstract int indexOf(String str, int fromIndex);

	/**
	    Inserts the string representation of the boolean argument into this sequence.
	*/
	public abstract EscapeStringBuilder insert(int offset, boolean b);

	/**
	    Inserts the string representation of the char argument into this sequence.
	*/
	public abstract EscapeStringBuilder insert(int offset, char c);

	/**
	    Inserts the string representation of the char array argument into this sequence.
	*/
	public abstract EscapeStringBuilder insert(int offset, char[] str);

	/**
	    Inserts the string representation of a subarray of the str array argument into this sequence.
	*/
	public abstract EscapeStringBuilder insert(int index, char[] str, int offset, int len);

	/**
	    Inserts the specified CharSequence into this sequence.
	*/
	public abstract EscapeStringBuilder insert(int dstOffset, CharSequence s);

	/**
	    Inserts a subsequence of the specified CharSequence into this sequence.
	*/
	public abstract EscapeStringBuilder insert(int dstOffset, CharSequence s, int start, int end);

	/**
	    Inserts the string representation of the double argument into this sequence.
	*/
	public abstract EscapeStringBuilder insert(int offset, double d);

	/**
	    Inserts the string representation of the float argument into this sequence.
	*/
	public abstract EscapeStringBuilder insert(int offset, float f);

	/**
	    Inserts the string representation of the second int argument into this sequence.
	*/
	public abstract EscapeStringBuilder insert(int offset, int i);

	/**
	    Inserts the string representation of the long argument into this sequence.
	*/
	public abstract EscapeStringBuilder insert(int offset, long l);

	/**
	    Inserts the string representation of the Object argument into this character sequence.
	*/
	public abstract EscapeStringBuilder insert(int offset, Object obj);

	/**
	    Inserts the string into this character sequence.
	*/
	public abstract EscapeStringBuilder insert(int offset, String str);

	/**
	    Returns the index within this string of the rightmost occurrence of the specified substring.
	*/
	public abstract int lastIndexOf(String str);

	/**
	    Returns the index within this string of the last occurrence of the specified substring.
	*/
	public abstract int lastIndexOf(String str, int fromIndex);

	/**
	    Returns the length (character count).
	*/
	public abstract int length();

	/**
	    Returns the index within this sequence that is offset from the given index by codePointOffset code points.
	*/
	public abstract int offsetByCodePoints(int index, int codePointOffset);

	/**
	    Replaces the characters in a substring of this sequence with characters in the specified String.
	*/
	public abstract EscapeStringBuilder replace(int start, int end, String str);

	/**
	    Causes this character sequence to be replaced by the reverse of the sequence.
	*/
	public abstract EscapeStringBuilder reverse();

	/**
	    The character at the specified index is set to ch.
	*/
	public abstract void setCharAt(int index, char ch);

	/**
	    Sets the length of the character sequence.
	*/
	public abstract void setLength(int newLength);

	/**
	    Returns a new character sequence that is a subsequence of this sequence.
	*/
	public abstract CharSequence subSequence(int start, int end);

	/**
	    Returns a new String that contains a subsequence of characters currently contained in this character sequence.
	*/
	public abstract String substring(int start);

	/**
	    Returns a new String that contains a subsequence of characters currently contained in this sequence.
	*/
	public abstract String substring(int start, int end);

	/**
	    Returns a string representing the data in this sequence.
	*/
	public abstract String toString();

	/**
	Returns a SSttringBuilder representing the data in this sequence.
	*/
	public abstract StringBuilder toStringBuilder();

	/**
	Attempts to reduce storage used for the character sequence.
	*/
	public abstract void trimToSize();

}