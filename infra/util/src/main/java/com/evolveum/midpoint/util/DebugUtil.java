/*
 * Copyright (c) 2010-2014 Evolveum
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

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

/**
 *
 * @author semancik
 */
public class DebugUtil {

	private static int SHOW_LIST_MEMBERS = 3;
	
	private static boolean detailedDebugDump = false;
	
	public static boolean isDetailedDebugDump() {
		return detailedDebugDump;
	}

	public static void setDetailedDebugDump(boolean detailedDebugDump) {
		DebugUtil.detailedDebugDump = detailedDebugDump;
	}
	
	public static String formatElementName(QName elementName) {
		if (elementName == null) {
			return "null";
		}
		if (detailedDebugDump) {
			return PrettyPrinter.prettyPrint(elementName);
		} else {
			return elementName.getLocalPart();
		}
	}

	public static String dump(DebugDumpable dumpable) {
		if (dumpable == null) {
			return "null";
		}
		return dumpable.debugDump();
	}
	
	public static String dump(Object object) {
		if (object == null) {
			return "null";
		}
		if (object instanceof DebugDumpable) {
			return ((DebugDumpable)object).debugDump();
		}
		if (object instanceof Map) {
			StringBuilder sb = new StringBuilder();
			debugDumpMapMultiLine(sb, (Map)object, 0);
			return sb.toString();
		}
		if (object instanceof Collection) {
			return debugDump((Collection)object);
		}
		return object.toString();
	}

	public static String debugDump(Collection<?> dumpables) {
		return debugDump(dumpables, 0);
	}

	public static String debugDump(Collection<?> dumpables, int indent) {
		StringBuilder sb = new StringBuilder();
		debugDump(sb, dumpables, indent, true);
		return sb.toString();
	}
	
	public static void debugDump(StringBuilder sb, Collection<?> dumpables, int indent, boolean openCloseSymbols) {
		debugDump(sb, dumpables, indent, openCloseSymbols, null);
	}
	
	public static void debugDump(StringBuilder sb, Collection<?> dumpables, int indent, boolean openCloseSymbols, String dumpSuffix) {
        if (dumpables == null) {
            return;
        }

		if (openCloseSymbols) {
			indentDebugDump(sb, indent);
			sb.append(getCollectionOpeningSymbol(dumpables));
			if (dumpSuffix != null) {
				sb.append(dumpSuffix);
			}
			sb.append("\n");
		}
		Iterator<?> iterator = dumpables.iterator();
		while (iterator.hasNext()) {
			Object item = iterator.next();
			if (item == null) {
				indentDebugDump(sb, indent + 1);
				sb.append("null");
			} else if (item instanceof DebugDumpable) {
				sb.append(((DebugDumpable)item).debugDump(indent + 1));
			} else {
				indentDebugDump(sb, indent + 1);
				sb.append(item.toString());
			}
			if (iterator.hasNext()) {
				sb.append("\n");
			}
		}
		if (openCloseSymbols) {
			if (!dumpables.isEmpty()) {
				sb.append("\n");
			}
			indentDebugDump(sb, indent);
			sb.append(getCollectionClosingSymbol(dumpables));
		}
	}

    public static Object debugDump(DebugDumpable dd) {
        return debugDump(dd, 0);
    }

    public static String debugDump(DebugDumpable dd, int indent) {
		if (dd == null) {
			StringBuilder sb = new StringBuilder();
			indentDebugDump(sb, indent + 1);
			sb.append("null");
			return sb.toString();
		} else {
			return dd.debugDump(indent);
		}
	}
	
	public static String debugDump(Object object, int indent) {
		if (object == null) {
			StringBuilder sb = new StringBuilder();
			indentDebugDump(sb, indent + 1);
			sb.append("null");
			return sb.toString();
		}
		if (object instanceof DebugDumpable) {
			return ((DebugDumpable)object).debugDump(indent);
		} else if (object instanceof Collection) {
			return debugDump((Collection<?>)object, indent);
		} else {
			StringBuilder sb = new StringBuilder();
			indentDebugDump(sb, indent + 1);
			sb.append(object.toString());
			return sb.toString();
		}
	}
	
	public static void debugDumpLabel(StringBuilder sb, String label, int indent) {
		indentDebugDump(sb, indent);
		sb.append(label).append(":");
	}
	
	public static void debugDumpWithLabel(StringBuilder sb, String label, DebugDumpable dd, int indent) {
		debugDumpLabel(sb, label, indent);
		if (dd == null) {
			sb.append(" null");
		} else {
			sb.append("\n");
			sb.append(dd.debugDump(indent + 1));
		}
	}
	
	public static void debugDumpWithLabel(StringBuilder sb, String label, String val, int indent) {
		debugDumpLabel(sb, label, indent);
		sb.append(" ");
		sb.append(val);
	}

	public static void debugDumpWithLabel(StringBuilder sb, String label, boolean val, int indent) {
		debugDumpLabel(sb, label, indent);
		sb.append(" ");
		sb.append(val);
	}

	public static void debugDumpWithLabel(StringBuilder sb, String label, int val, int indent) {
		debugDumpLabel(sb, label, indent);
		sb.append(" ");
		sb.append(val);
	}

	public static void debugDumpWithLabel(StringBuilder sb, String label, long val, int indent) {
		debugDumpLabel(sb, label, indent);
		sb.append(" ");
		sb.append(val);
	}

	public static void debugDumpWithLabel(StringBuilder sb, String label, Collection<? extends DebugDumpable> dds, int indent) {
		debugDumpLabel(sb, label, indent);
		if (dds == null) {
			sb.append(" null");
		} else if (dds.isEmpty()) {
			sb.append(" ");
			sb.append(getCollectionOpeningSymbol(dds));
			sb.append(getCollectionClosingSymbol(dds));
		} else {
			sb.append("\n");
			sb.append(debugDump(dds, indent + 1));
		}
	}
	
	public static <K, V> void debugDumpWithLabel(StringBuilder sb, String label, Map<K, V> map, int indent) {
		debugDumpLabel(sb, label, indent);
		if (map == null) {
			sb.append(" null");
		} else {
			sb.append("\n");
			debugDumpMapMultiLine(sb, map, indent + 1);
		}
	}
	
	public static void debugDumpWithLabelToString(StringBuilder sb, String label, Object object, int indent) {
		debugDumpLabel(sb, label, indent);
		if (object == null) {
			sb.append(" null");
		} else {
			sb.append(" ");
			sb.append(object.toString());
		}
	}
	
	public static void debugDumpWithLabelToStringLn(StringBuilder sb, String label, Object object, int indent) {
		debugDumpWithLabelToString(sb, label, object, indent);
		sb.append("\n");
	}

	public static String debugDumpXsdAnyProperties(Collection<?> xsdAnyCollection, int indent) {
		StringBuilder sb = new StringBuilder();
		indentDebugDump(sb, indent);
		sb.append(getCollectionOpeningSymbol(xsdAnyCollection));
		for (Object element : xsdAnyCollection) {
			sb.append("\n");
			indentDebugDump(sb, indent+1);
			sb.append(PrettyPrinter.prettyPrintElementAsProperty(element));
		}
		sb.append("\n");
		indentDebugDump(sb, indent);
		sb.append(getCollectionClosingSymbol(xsdAnyCollection));
		return sb.toString();
	}

	public static String getCollectionOpeningSymbol(Collection<?> col) {
		if (col instanceof List) {
			return "[";
		}
		if (col instanceof Set) {
			return "{";
		}
		return col.getClass().getSimpleName()+"(";
	}

	public static String getCollectionClosingSymbol(Collection<?> col) {
		if (col instanceof List) {
			return "]";
		}
		if (col instanceof Set) {
			return "}";
		}
		return ")";
	}

	public static void indentDebugDump(StringBuilder sb, int indent) {
		for(int i = 0; i < indent; i++) {
			sb.append(DebugDumpable.INDENT_STRING);
		}
	}
	
	public static <K, V> String debugDumpMapMultiLine(Map<K, V> map) {
		StringBuilder sb = new StringBuilder();
		debugDumpMapMultiLine(sb, map, 0);
		return sb.toString();
	}

	public static <K, V> void debugDumpMapMultiLine(StringBuilder sb, Map<K, V> map, int indent) {
		debugDumpMapMultiLine(sb, map, indent, false);
	}
	
	public static <K, V> void debugDumpMapMultiLine(StringBuilder sb, Map<K, V> map, int indent, boolean openCloseSymbols) {
		debugDumpMapMultiLine(sb, map, indent, openCloseSymbols, null);
	}
	
	public static <K, V> void debugDumpMapMultiLine(StringBuilder sb, Map<K, V> map, int indent, boolean openCloseSymbols, String dumpSuffix) {
		int inindent = indent;
		if (openCloseSymbols) {
			indentDebugDump(sb,indent);
			sb.append("(");
			if (dumpSuffix != null) {
				sb.append(dumpSuffix);
			}
			sb.append("\n");
			inindent++;
		}
		Iterator<Entry<K, V>> i = map.entrySet().iterator();
		while (i.hasNext()) {
			Entry<K,V> entry = i.next();
			indentDebugDump(sb,inindent);
			sb.append(PrettyPrinter.prettyPrint(entry.getKey()));
			sb.append(" => ");
			V value = entry.getValue();
			if (value == null) {
				sb.append("null");
			} else if (value instanceof DebugDumpable) {
				sb.append("\n");
				sb.append(((DebugDumpable)value).debugDump(inindent+1));
			} else {
				sb.append(value);
			}
			if (i.hasNext()) {
				sb.append("\n");
			}
		}
		if (openCloseSymbols) {
			sb.append("\n");
			indentDebugDump(sb,indent);
			sb.append(")");
		}
	}

	public static <K, V> void debugDumpMapSingleLine(StringBuilder sb, Map<K, V> map, int indent) {
		Iterator<Entry<K, V>> i = map.entrySet().iterator();
		while (i.hasNext()) {
			Entry<K,V> entry = i.next();
			indentDebugDump(sb,indent);
			sb.append(PrettyPrinter.prettyPrint(entry.getKey()));
			sb.append(" => ");
			V value = entry.getValue();
			if (value == null) {
				sb.append("null");
			} else {
				sb.append(value);
			}
			if (i.hasNext()) {
				sb.append("\n");
			}
		}
	}

	public static <T> String valueAndClass(T value) {
		if (value == null) {
			return "null";
		}
		return value.getClass().getSimpleName()+":"+value.toString();
	}

	public static String formatDate(Long millis) {
		if (millis == null) {
			return "null";
		}
		Date date = new Date(millis);
		return PrettyPrinter.prettyPrint(date);
	}

	public static String excerpt(String input, int maxChars) {
		if (input == null) {
			return null;
		}
		int eolIndex = input.indexOf('\n');
		if (eolIndex >= 0) {
			maxChars = eolIndex;
		}
		if (input.length() <= maxChars) {
			return input;
		}
		return input.substring(0, maxChars)+"...";
	}

	public static String fixIndentInMultiline(int indent, String indentString, String s) {
		int cr = s.indexOf('\r');
		int lf = s.indexOf('\n');
		String searchFor;
		if (cr < 0 && lf < 0) {
			return s;
		} else if (cr >= 0 && lf >= 0) {
			searchFor = "\r\n";
		} else if (cr >= 0 && lf < 0) {
			searchFor = "\r";
		} else {
			searchFor = "\n";
		}

		StringBuilder indentation = new StringBuilder();
		for(int i = 0; i < indent; i++) {
			indentation.append(indentString);
		}
		String indentationString = indentation.toString();

		return s.replace(searchFor, System.lineSeparator() + indentationString);
	}

}
