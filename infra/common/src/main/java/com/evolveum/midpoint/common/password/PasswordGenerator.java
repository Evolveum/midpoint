package com.evolveum.midpoint.common.password;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import javax.xml.namespace.QName;

import org.apache.commons.lang.text.StrBuilder;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CharacterClassType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.StringLimitType;

public class PasswordGenerator {

	private static final transient Trace logger = TraceManager.getTrace(PasswordGenerator.class);

	private static final Random rand =  new Random(System.currentTimeMillis());
	
	
	public static String generate(PasswordPolicyType pp, OperationResult inputResult) {
		return generate(pp, false, inputResult);
	}
	
	public static String generate(PasswordPolicyType pp, boolean generateMinimalSize, OperationResult inputResult) {
		
		if (null == pp) {
			throw new IllegalArgumentException("Provided password policy can not be null.");
		}

		if (null == inputResult) {
			throw new IllegalArgumentException("Provided operation result cannot be null");
		}
		//Define result from generator
		OperationResult generatorResult = new OperationResult("Password generator running policy :" + pp.getName());
		inputResult.addSubresult(generatorResult);
		
		//setup default values where missing
		PasswordPolicyUtils.normalize(pp);

		// Optimize usage of limits ass hashmap of limitas and key is set of
		// valid chars for each limitation
		HashMap<StringLimitType, ArrayList<String>> lims = new HashMap<StringLimitType, ArrayList<String>>();
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
		int unique = pp.getStringPolicy().getLimitations().getMinUniqueChars();	// TODO disable uniqueness

		// Initialize generator
		StringBuilder password = new StringBuilder();

		/* **********************************
		 * Try to find best characters to be first in password
		 */
		HashMap<StringLimitType, ArrayList<String>> mustBeFirst = new HashMap<StringLimitType, ArrayList<String>>();
		for (StringLimitType l : lims.keySet()) {
			if (l.isMustBeFirst()) {
				mustBeFirst.put(l, lims.get(l));
			}
		}
		
		// If any limitation was found to be first
		if (!mustBeFirst.isEmpty()) {
			HashMap<Integer, ArrayList<String>> posibleFirstChars = cardinalityCounter(mustBeFirst, null,
					false, generatorResult);
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
				return null; // EXIT
			} else {
				if (logger.isDebugEnabled()) {
					StrBuilder tmp = new StrBuilder();
					tmp.appendSeparator(", ");
					tmp.appendAll(intersectionCharacters);
					logger.trace("Generate first character intersection items [" + tmp + "] into password.");
				}
				// Generate random char into password from intersection
				password.append(intersectionCharacters.get(rand.nextInt(intersectionCharacters.size())));
			}
		}

		/* **************************************
		 * Generate rest to fullfill minimal criteria
		 */

		// Count kardinality of elements
		HashMap<Integer, ArrayList<String>> chars;
		for (int i = 0; i < minLen; i++) {
			//Find all usable characters
			chars = cardinalityCounter(lims, stringTokenizer(password.toString()), false, generatorResult);
			// If something goes badly then go out
			if (null == chars ) {
				return null;
			} 
			
			if ( chars.isEmpty()) {
				logger.trace("Minimal criterias was met. No more characters");
				break;
			}
			//Find lowest possible cardinality and then generate char
			for (int card = 1 ; card < lims.keySet().size(); card++) {
				if (chars.containsKey(card)) {
					ArrayList<String> validChars = chars.get(card);
					password.append(validChars.get(rand.nextInt(validChars.size())));
					logger.trace(password.toString());
					break;
				}
			}
		}

		// test if maximum is not exceeded
		if ( password.length() > maxLen ){
			generatorResult.recordFatalError("Unable to meet minimal criterian and not exceed maximxal size of password.");
			return null;
		}
		
		/* ***************************************
		 *  Generate chars to not exceed maximal
		 */
		
		for (int i = 0; i < minLen; i++) {
			// test if max is reached
			if ( password.length() == maxLen ){
				// no more characters maximal size is reached
				break;
			}
			
			if ( password.length() >= minLen  && generateMinimalSize) {
				//no more characters are needed
				break;
			}
			//find all usable characters
			chars = cardinalityCounter(lims, stringTokenizer(password.toString()), true, generatorResult);
			// If something goes badly then go out
			if (null == chars ) {
				return null;
			} 
			
			if ( chars.isEmpty()) {
				break;
			}
			
			for (int card = 1 ; card < lims.keySet().size(); card++) {
				if (chars.containsKey(card)) {
					ArrayList<String> validChars = chars.get(card);
					password.append(validChars.get(rand.nextInt(validChars.size())));
					logger.trace(password.toString());
					break;
				}
			}
		}
		
		if ( password.length() < minLen ){
			generatorResult.recordFatalError("Unable generate password and meet minimal size of password." + password.length() + "<" + minLen);
			return null;
		}
		
		generatorResult.recordSuccess();
		
		//Shuffle output to solve pattern like output
		StrBuilder sb = new StrBuilder(password.substring(0,1));
		ArrayList<String> shuffleBuffer = stringTokenizer(password.substring(1));
		Collections.shuffle(shuffleBuffer);
		sb.appendAll(shuffleBuffer);
		
		return sb.toString();
	}

	/******************************************************
	 * Private helper methods
	 ******************************************************/

	/**
	 * Count cardinality
	 */
	private static HashMap<Integer, ArrayList<String>> cardinalityCounter(
			HashMap<StringLimitType, ArrayList<String>> lims, ArrayList<String> password,
			Boolean skipMatchedLims, OperationResult op) {
		HashMap<String, Integer> counter = new HashMap<String, Integer>();

		for (StringLimitType l : lims.keySet()) {
			ArrayList<String> chars = lims.get(l);
			int i = 0;
			if (null != password) {
				i = charIntersectionCounter(lims.get(l), password);
			}
			// If max is exceed then error unable to continue
			if (i > l.getMaxOccurs()) {
				OperationResult o = new OperationResult("Limitation check :" + l.getDescription());
				o.recordFatalError("Exceeded maximal value for this limitation. " + i + ">"
						+ l.getMaxOccurs());
				op.addSubresult(o);
				return null;
				// if max is all ready reached or skip enabled for minimal skip
				// counting
			} else if (i == l.getMaxOccurs()) {
				continue;
				// other cases minimum is not reached
			} else if (i>= l.getMinOccurs() && ! skipMatchedLims) {
				continue;
			} 
				for (String s : chars) {
					if (null == password || !password.contains(s)) {
						if (null == counter.get(s)) {
							counter.put(s, 1);
						} else {
							counter.put(s, counter.get(s) + 1);
						}
					}
				}
			
		}

		// If need to remove disabled chars (already reached limitations)
		if (null != password) {
			for (StringLimitType l : lims.keySet()) {
				int i = charIntersectionCounter(lims.get(l), password);
				if (i > l.getMaxOccurs()) {
					OperationResult o = new OperationResult("Limitation check :" + l.getDescription());
					o.recordFatalError("Exceeded maximal value for this limitation. " + i + ">"
							+ l.getMaxOccurs());
					op.addSubresult(o);
					return null;
				} else if (i == l.getMaxOccurs()) {
					// limitation matched remove all used chars
					logger.trace("Skip " + l.getDescription());
					for (String charToRemove : lims.get(l)) {
						counter.remove(charToRemove);
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

	private static int charIntersectionCounter(ArrayList<String> a, ArrayList<String> b) {
		int ret = 0;
		for (String s : b) {
			if (a.contains(s)) {
				ret++;
			}
		}
		return ret;
	}

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

	/**
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
