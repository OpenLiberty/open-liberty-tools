/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;

import com.ibm.ws.st.core.internal.config.ConfigVars;

@SuppressWarnings("restriction")
public class Matcher {

    private static final int MIN_MATCH_PERCENTAGE = 70;

    // best match list
    private final ArrayList<String> bestMatchList = new ArrayList<String>();

    // stack
    private int stack[] = new int[32];
    private int stackIndex = 0;

    // match state
    private int maxConsecutiveMatch = 0;
    private int maxTotalMatch = 0;
    private int minTotalMatch;
    private int currentTotalMatch;
    private int currentConsecutiveMatch;

    // minimum match percentage
    private int minMatchPercent;

    // match status
    private enum MatchStatus {
        NO_MATCH,
        BEST_MATCH,
        EXACT_MATCH
    }

    public String getBestMatch(CMNamedNodeMap map, String name, List<String> excludeList) {
        return getBestMatch(map, name, excludeList, MIN_MATCH_PERCENTAGE);
    }

    public String getBestMatch(CMNamedNodeMap map,
                               String name,
                               List<String> excludeList,
                               int minMatchPercent) {
        if (map == null || name == null) {
            return null;
        }

        this.minMatchPercent = minMatchPercent;

        reset();
        for (int i = 0; i < map.getLength(); ++i) {
            final CMNode node = map.item(i);
            final String choice = node.getNodeName();

            if (match(name, choice) == MatchStatus.EXACT_MATCH) {
                break;
            }
        }

        return getBestMatchString(excludeList);
    }

    public String getBestMatch(ConfigVars vars, String toMatch, String type) {
        List<String> choiceList = vars.getSortedVars(type, true);
        return getBestMatch(choiceList, toMatch, null, MIN_MATCH_PERCENTAGE);
    }

    public String getBestMatch(ConfigVars vars,
                               String toMatch,
                               String type,
                               int minMatchPercent) {
        List<String> choiceList = vars.getSortedVars(type, true);
        return getBestMatch(choiceList, toMatch, null, minMatchPercent);
    }

    public String getBestMatch(Collection<String> choices, String toMatch, List<String> excludeList) {
        return getBestMatch(choices, toMatch, excludeList, MIN_MATCH_PERCENTAGE);
    }

    public String getBestMatch(Collection<String> choices,
                               String toMatch,
                               List<String> excludeList,
                               int minMatchPercent) {
        if (choices == null || choices.size() == 0 || toMatch == null) {
            return null;
        }

        this.minMatchPercent = minMatchPercent;

        reset();
        for (String choice : choices) {
            if (match(toMatch, choice) == MatchStatus.EXACT_MATCH) {
                break;
            }
        }

        return getBestMatchString(excludeList);
    }

    private void reset() {
        bestMatchList.clear();
        maxConsecutiveMatch = 0;
        maxTotalMatch = 0;
    }

    private String getBestMatchString(List<String> excludeList) {
        // Remove any best matches that should be excluded from
        // the list
        if (excludeList != null && !excludeList.isEmpty()) {
            bestMatchList.removeAll(excludeList);
        }

        if (bestMatchList.size() == 0) {
            return null;
        }

        if (bestMatchList.size() == 1) {
            return bestMatchList.get(0);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(bestMatchList.get(0));
        for (int i = 1; i < bestMatchList.size(); ++i) {
            sb.append(',');
            sb.append(bestMatchList.get(i));
        }

        return sb.toString();
    }

    /**
     * Check 2 strings for a potential best match
     * 
     * @param toMatch input string to match
     * @param choice choice string to match against
     * 
     * @return a match status
     */
    private MatchStatus match(String toMatch, String choice) {
        final int len1 = toMatch.length();
        final int len2 = choice.length();

        // We only do a match if toMatch.length is within a a predefined
        // tolerance of choice.length.
        if (!boundaryMatch(len1, len2)) {
            return MatchStatus.NO_MATCH;
        }

        int offset1 = 0;
        int offset2 = 0;
        int totalMatch = 0;
        int consecutiveMatch = 0;

        resetState((len1 > len2) ? len1 : len2);
        // Try to get the maximum number of character matches
        //
        // We iterate through the input string as many times until we
        // exhaust all possibilities or until there is no need to
        // match since the total number of characters won't exceed the
        // previous total
        for (;;) {
            boolean toBacktrack = true;
            for (int i = offset2; offset1 < len1 && i < len2 && currentTotalMatch <= totalMatch + len1 - offset1; ++i) {
                if (matchChar(toMatch.charAt(offset1), choice.charAt(i))) {
                    ++offset1;
                    ++totalMatch;
                    ++consecutiveMatch;
                    toBacktrack = true;
                }
                // we did not find a match (first mismatch), so we push
                // the offset information and current match count for
                // backtracking purposes
                else if (toBacktrack) {
                    toBacktrack = false;
                    // Optimization
                    //
                    // Do not push the offset information if the maximum we can
                    // match is less than the current total match. In other
                    // words, if we add the remaining characters to what was
                    // previously matched, and their total is less than the
                    // current total, just skip.
                    if (currentTotalMatch <= totalMatch + len1 - offset1 - 1) {
                        if (consecutiveMatch > currentConsecutiveMatch) {
                            currentConsecutiveMatch = consecutiveMatch;
                        }
                        consecutiveMatch = 0;
                        ensureStackCapacity(3);
                        stack[stackIndex++] = offset1 + 1;
                        stack[stackIndex++] = i;
                        stack[stackIndex++] = totalMatch;
                    }
                }
            }

            // We matched more characters
            if (totalMatch > currentTotalMatch) {
                currentTotalMatch = totalMatch;
            }
            // We matched more consecutive characters
            if (consecutiveMatch > currentConsecutiveMatch) {
                currentConsecutiveMatch = consecutiveMatch;
            }

            // Exhausted all possibilities
            if (stackIndex == 0) {
                break;
            }

            totalMatch = stack[--stackIndex];
            offset2 = stack[--stackIndex];
            offset1 = stack[--stackIndex];
        }

        return checkMatch(choice);
    }

    private void resetState(int length) {
        stackIndex = 0;
        currentTotalMatch = 0;
        currentConsecutiveMatch = 0;
        minTotalMatch = getMinimumMatch(length);
    }

    /**
     * Check to see if we had a match and if we did add it to the
     * current list of best matches.
     * 
     * @param choice best match string
     * @return match status
     */
    private MatchStatus checkMatch(String choice) {
        // We did not find a match because
        // 1. The total number of matched characters is less that the minimum
        //    number of characters to match
        // 2. The total number of matched characters is less than the maximum
        //    number of matched character
        // 3. The total number of matched characters is the same as the maximum
        //    number of matched character, but the number of consecutive matched
        //    characters is less than the maximum number of consecutive matched
        //    characters
        if (currentTotalMatch < minTotalMatch || currentTotalMatch < maxTotalMatch ||
            (currentTotalMatch == maxTotalMatch && currentConsecutiveMatch < maxConsecutiveMatch)) {
            return MatchStatus.NO_MATCH;
        }

        // a possible match
        if (currentTotalMatch > maxTotalMatch || currentConsecutiveMatch > maxConsecutiveMatch) {
            bestMatchList.clear();
            maxTotalMatch = currentTotalMatch;
            maxConsecutiveMatch = currentConsecutiveMatch;
        }

        bestMatchList.add(choice);
        if (maxTotalMatch == choice.length()) {
            return MatchStatus.EXACT_MATCH;
        }

        return MatchStatus.BEST_MATCH;
    }

    /**
     * Compare 2 characters (case is being ignored)
     * 
     * @param c1 first character
     * @param c2 second character
     * @return true if c1 and c2 match (ignore case), otherwise false
     */
    private boolean matchChar(int c1, int c2) {
        if (c1 == c2) {
            return true;
        }

        if (c1 > 0xffff || c2 > 0xffff) {
            return false;
        }

        final char uc1 = Character.toUpperCase((char) c1);
        final char uc2 = Character.toUpperCase((char) c2);
        if (uc1 == uc2) {
            return true;
        }

        return Character.toLowerCase(uc1) == Character.toLowerCase(uc2);
    }

    /**
     * Returns the minimum number of characters to match
     * 
     * @param length of choice string we are matching against
     * @return minimum number of characters to match
     */
    private int getMinimumMatch(int length) {
        final int value = length * minMatchPercent;
        int count = value / 100;
        if ((value % 100) > 0) {
            ++count;
        }

        return count;
    }

    /**
     * Check whether the length of the input string is within the
     * tolerance range for the choice string
     * 
     * @param toMatchLen length of input string
     * @param choiceLen length of choice string
     * @return true if toMatchLen within boundary of choiceLen, otherwise false
     */
    private boolean boundaryMatch(int toMatchLen, int choiceLen) {
        if ((toMatchLen * 100) < (choiceLen * minMatchPercent)) {
            return false;
        }

        if ((toMatchLen * minMatchPercent) > (choiceLen * 100)) {
            return false;
        }

        return true;
    }

    private void ensureStackCapacity(int count) {
        if (stackIndex + count > stack.length) {
            int newLen = stack.length << 1;
            int[] newStack = new int[newLen];
            System.arraycopy(stack, 0, newStack, 0, stackIndex);
            stack = newStack;
        }
    }
}
