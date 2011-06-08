package com.evolveum.midpoint.common.password;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import javax.xml.namespace.QName;

import org.apache.commons.lang.text.StrBuilder;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CharacterClassType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.StringLimitType;

public class PasswordGenerator {

	private static final transient Trace logger = TraceManager.getTrace(PasswordGenerator.class);

	public static String generate(PasswordPolicyType pp, OperationResult generatorResult) {
		StringBuilder sb = new StringBuilder();
		if (null == pp) {
			throw new IllegalArgumentException("Provided password policy can not be null.");
		}

		if (null == generatorResult) {
			throw new IllegalArgumentException("Provided operation result cannot be null");
		}

		PasswordPolicyUtils.normalize(pp);

		// Optimize usage of limits ass hashmap of limitas and key is set of
		// valid chars for each limitation
		HashMap<StringLimitType, ArrayList<String>> lims = new HashMap();
		for (StringLimitType l : pp.getStringPolicy().getLimitations().getLimit()) {
			if (null != l.getCharacterClass().getValue()) {
				lims.put(l, stringTokenizer(l.getCharacterClass().getValue()));
			} else {
				lims.put(
						l,
						stringTokenizer(collectCharacterClass(pp.getStringPolicy().getCharacterClass(), l
								.getCharacterClass().getRef())));
			}
		}

		// Get global limitations
		int minLen = pp.getStringPolicy().getLimitations().getMinLength();
		int maxLen = pp.getStringPolicy().getLimitations().getMaxLength();
		int unique = pp.getStringPolicy().getLimitations().getMinUniqueChars();

		// Initialize generator
		Random rand = new Random(System.currentTimeMillis());
		StringBuilder password = new StringBuilder();

		// Try to find best characters to be first in password
		HashMap<StringLimitType, ArrayList<String>> mustBeFirst = new HashMap();
		for (StringLimitType l : lims.keySet()) {
			if (l.isMustBeFirst()) {
				mustBeFirst.put(l, lims.get(l));
			}
		}

		// If any limitation was found
		if (!mustBeFirst.isEmpty()) {
			HashMap<Integer, ArrayList<String>> posibleFirstChars = cardinalityCounter(mustBeFirst, null);
			int intersectionCardinality = mustBeFirst.keySet().size();
			ArrayList<String> intersectionCharacters = posibleFirstChars.get(intersectionCardinality);
			// If no intersection was found then raise error
			if (null == intersectionCharacters || intersectionCharacters.size() == 0) {
				generatorResult
						.recordFatalError("No intersection for required first character sets in password policy:"
								+ pp.getName());
				// Log error
				if (logger.isErrorEnabled()) {
					logger.error("Unable to generate password: No intersection for required first character sets in password policy: ["
							+ pp.getName() + "] following character limitation and sets are used:");
					for (StringLimitType l : mustBeFirst.keySet()) {
						StrBuilder tmp = new StrBuilder();
						tmp.appendSeparator(", ");
						tmp.appendAll(mustBeFirst.get(l));
						logger.error("L:" + l.getDescription() + " -> [" + tmp + "]");

					}
				}
				// No more processing unrecoverable conflict
				return null; 	//EXIT
				
			} else {
				if (logger.isDebugEnabled()) {
					StrBuilder tmp = new StrBuilder();
					tmp.appendSeparator(", ");
					tmp.appendAll(intersectionCharacters);
					logger.debug("Generate first character intersection items [" + tmp + "] into password.");
				}
				// Generate random char into password from intersection
				password.append(intersectionCharacters.get(rand.nextInt(intersectionCharacters.size())));
			}

		}

		// Count kardinality of elements
		HashMap<Integer, ArrayList<String>> chars = cardinalityCounter(lims, null);

		// 3. vytvor hash mapu znak->pocet vyskytov v mnozinach
		// 4. najdi pravidla ktore maju must be first
		// 1. sprav prienik class
		// 2. vyber nahodny prvok z prieniku

		generatorResult.recordSuccess();
		return sb.toString();
	}

	// Count cardinality
	private static HashMap<Integer, ArrayList<String>> cardinalityCounter(
			HashMap<StringLimitType, ArrayList<String>> lims, ArrayList<String> skipChars) {
		HashMap<String, Integer> counter = new HashMap<String, Integer>();

		for (ArrayList<String> chars : lims.values()) {
			for (String s : chars) {
				if (null == skipChars || !skipChars.contains(s)) {
					if (null == counter.get(s)) {
						counter.put(s, 1);
					} else {
						counter.put(s, counter.get(s) + 1);
					}
				}
			}
		}
		// Transpone to better format
		HashMap<Integer, ArrayList<String>> ret = new HashMap<Integer, ArrayList<String>>();
		for (String s : counter.keySet()) {
			// if not there initialize
			if (null == ret.get(counter.get(s))) {
				ret.put(counter.get(s), new ArrayList<String>());
			}
			ret.get(counter.get(s)).add(s);
		}
		return ret;
	}

	/******************************************************
	 * Private helper methods
	 ******************************************************/
	/**
	 * Convert string to array of substrings
	 */
	private static ArrayList<String> stringTokenizer(String input) {
		ArrayList<String> l = new ArrayList<String>();
		String a[] = input.split("");
		// Add all to list
		for (int i = 0; i < a.length; i++) {
			if (!"".equals(a[i])) {
				l.add(a[i]);
			}
		}
		return l;
	}

	/*
	 * Prepare usable list of strings for generator
	 */

	private static String collectCharacterClass(CharacterClassType cc, QName ref) {
		StrBuilder l = new StrBuilder();
		if (null == cc) {
			throw new IllegalArgumentException("Character class cannot be null");
		}

		if (null != cc.getValue() && (null == ref || ref.equals(cc.getName()))) {
			l.append(cc.getValue());
		} else if (null != cc.getCharacterClass() && !cc.getCharacterClass().isEmpty()) {
			// Process all sub lists
			for (CharacterClassType subClass : cc.getCharacterClass()) {
				// If we found requested name or no name defined
				if (null == ref || ref.equals(cc.getName())) {
					l.append(collectCharacterClass(subClass, null));
				} else {
					l.append(collectCharacterClass(subClass, ref));
				}
			}
		}
		// Remove duplicity in return;
		HashSet<String> h = new HashSet<String>();
		for (String s : l.toString().split("")) {
			h.add(s);
		}
		return new StrBuilder().appendAll(h).toString();
	}
}
