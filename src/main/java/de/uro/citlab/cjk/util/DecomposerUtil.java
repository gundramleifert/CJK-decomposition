/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uro.citlab.cjk.util;

import de.uro.citlab.cjk.types.Char;
import de.uro.citlab.cjk.Decomposer;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gundram
 */
public class DecomposerUtil {

    private static Logger LOG = LoggerFactory.getLogger(DecomposerUtil.class);

    public static final Set<Character> IDEOGRAPHIC_DESCRIPTION_CHARACTERS;
    public static final Set<Character> STROKE_COUNT_CHARACTERS;

    static {
        IDEOGRAPHIC_DESCRIPTION_CHARACTERS = new HashSet<>();
        for (char c = '\u2FF0'; c < '\u2FFC'; c++) {
            IDEOGRAPHIC_DESCRIPTION_CHARACTERS.add(c);
        }
        STROKE_COUNT_CHARACTERS = new HashSet<>();
        for (char c = '\u2460'; c < '\u2474'; c++) {
            STROKE_COUNT_CHARACTERS.add(c);
        }
    }

    public static boolean isIDC(String s) {
        return s.length() == 1 && isIDC(s.charAt(0));
    }

    public static boolean isIDC(char c) {
        return IDEOGRAPHIC_DESCRIPTION_CHARACTERS.contains(c);
    }

    public static boolean isStrokeC(String s) {
        return s.length() == 1 && isStrokeC(s.charAt(0));
    }

    public static boolean isStrokeC(char c) {
        return STROKE_COUNT_CHARACTERS.contains(c);
    }

    public static boolean isIDChar(Char c) {
        return isIDC(c.value);
    }

    public static boolean hasStrokeCharacter(String decomposed) {
        for (char c : decomposed.toCharArray()) {
            if (DecomposerUtil.isStrokeC(c)) {
                return true;
            }
        }
        return false;
    }

    public static String removeIDCs(String seq) {
        StringBuilder sb = new StringBuilder();
        for (char c : seq.toCharArray()) {
            if (!DecomposerUtil.isIDC(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static ObjectCounter<Integer> getLengthDistribution(Decomposer composer) {
        ObjectCounter<Integer> res = new ObjectCounter<>();
        for (Char c : composer.chars.values()) {
            if (c.getCountRoot() > 0) {
                res.add(c.getLength(), c.getCountRoot());
            }
        }
        return res;
    }

    public static void saveCharSet(Decomposer dec, File outFile, boolean onlyWithCounts) {
        List<String> out = new LinkedList<>();
        List<Char> charSet = new LinkedList<>(DecomposerUtil.getCharSet(dec));
        if (onlyWithCounts) {
            charSet.removeIf((t) -> {
                return t.getCountAtom() <= 0 || t.getLength() < 1;
            });
        }
        charSet.sort((Char o1, Char o2) -> o2.getCountAtom() - o1.getCountAtom());
        for (Char c : charSet) {
            try {
                out.add(String.format("%-50s '%s'=>'%s'", c, DecomposerUtil.getAsString(c.getDec()), DecomposerUtil.getAsString(c.getAtoms())));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
//            System.out.println(key + " = (len=" + length + ") " + Decomposer.decompose(key));
        }
        FileUtil.writeLines(outFile, out);

    }

    public static void saveMap(Decomposer dec, File outFile, boolean onlyWithCounts, boolean raw) {
        List<String> out = new LinkedList<>();
        List<Char> values = new LinkedList<>(dec.chars.values());
        if (onlyWithCounts) {
            values.removeIf((t) -> {
                return t.getCountAtom() <= 0;
            });
        }
        values.sort((o1, o2) -> {
            return o2.getCountAtom() - o1.getCountAtom();
        });
        for (Char c : values) {
            try {
                if (raw) {
                    out.add(String.format("%s\t%s\t%s", c.value, DecomposerUtil.getAsString(c.getDec()), DecomposerUtil.getAsString(c.getAtoms())));
                } else {
                    out.add(String.format("%-50s '%-10s'=>'%s'", c, DecomposerUtil.getAsString(c.getDec()), DecomposerUtil.getAsString(c.getAtoms())));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
//            System.out.println(key + " = (len=" + length + ") " + Decomposer.decompose(key));
        }
        FileUtil.writeLines(outFile, out);

    }

    public static double getAvgLength(Decomposer composer) {
        int[] sumAndCount = getSumAndCount(composer);
        return ((double) sumAndCount[0]) / sumAndCount[1];
    }

    private static int[] getSumAndCount(Decomposer dec) {
        int sum = 0, count = 0;
        ObjectCounter<Integer> lengthDistribution = getLengthDistribution(dec);
        for (Pair<Integer, Long> pair : lengthDistribution.getResultOccurrence()) {
            sum += pair.getFirst() * pair.getSecond();
            count += pair.getSecond();
        }
        return new int[]{sum, count};
    }

    private static void recalcLength(Decomposer dec) {
        for (Char value : dec.chars.values()) {
            value.resetLength();
        }
        for (Char value : dec.chars.values()) {
            value.getLength();
        }
    }

    public static Map<String, LinkedList<Double>> reduceDecomposer(Decomposer dec, double relImprovement, int maxLength) {
        Map<String, LinkedList<Double>> listList = new LinkedHashMap<>();
        listList.put("size", new LinkedList<>());
        listList.put("ambiguous characters", new LinkedList<>());
        listList.put("max impact", new LinkedList<>());
        listList.put("max length", new LinkedList<>());
        listList.put("same occurace", new LinkedList<>());
//        List<Double> sizeList = new LinkedList<>();
//        List<Double> lengthList = new LinkedList<>();
//        List<Double> lengthList2 = new LinkedList<>();
        double length = DecomposerUtil.getAvgLength(dec);
        LOG.debug("average length {}", length);
        double firstlength = length;
        {
            double newLength = DecomposerUtil.getAvgLength(dec);
            int sizeStart = getCharSet(dec).size();
            listList.get("size").add((double) sizeStart);
            listList.get("max impact").add(Double.NaN);
            listList.get("max length").add(Double.NaN);
            listList.get("same occurace").add(Double.NaN);
            listList.get("ambiguous characters").add(newLength);
        }
        LOG.debug("substituting ambiguous characters...", maxLength);
        while (reduceSolveAmbiguousChars(dec) != null) {
            int size = getCharSet(dec).size();
            LOG.debug("leaves = {} distributionlength = {}", size, DecomposerUtil.getLengthDistribution(dec));
            double newLength = DecomposerUtil.getAvgLength(dec);
            listList.get("size").add((double) size);
            listList.get("max impact").add(Double.NaN);
            listList.get("max length").add(Double.NaN);
            listList.get("same occurace").add(Double.NaN);
            listList.get("ambiguous characters").add(newLength);
        }
        listList.get("max impact").removeLast();
        listList.get("max impact").add(listList.get("ambiguous characters").getLast());
        int idx = 1;
        while (reduceMaxImprovement(dec, relImprovement) != null) {
            int size = getCharSet(dec).size();
            LOG.debug("leaves = {} distributionlength = {}", size, getLengthDistribution(dec));
            double newLength = getAvgLength(dec);
            if (LOG.isTraceEnabled()) {
                LOG.trace(String.format("%2d: %.2f %.4f%% %.4f%%", idx++, newLength, (length / newLength - 1) * 100, (newLength / firstlength) * 100));
            }//            runList.add((double) i);
            listList.get("size").add((double) size);
            listList.get("max impact").add(newLength);
            listList.get("max length").add(Double.NaN);
            listList.get("same occurace").add(Double.NaN);
            listList.get("ambiguous characters").add(Double.NaN);
            length = newLength;
            idx++;
        }
        listList.get("max length").removeLast();
        listList.get("max length").add(listList.get("max impact").getLast());

        LOG.debug("substituting characters longer than {}...", maxLength);
        while (reduceMaxLength(dec, maxLength) != null) {
            int size = getCharSet(dec).size();
            LOG.debug("leaves = {} distributionlength = {}", size, getLengthDistribution(dec));
            double newLength = getAvgLength(dec);
            listList.get("size").add((double) size);
            listList.get("max impact").add(Double.NaN);
            listList.get("max length").add(newLength);
            listList.get("same occurace").add(Double.NaN);
            listList.get("ambiguous characters").add(Double.NaN);
        }
        listList.get("same occurace").removeLast();
        listList.get("same occurace").add(listList.get("max length").getLast());
        LOG.debug("substituting leaves that are only part of one character .", maxLength);
        while (reduceSubstituteLeaf(dec) != null) {
            int size = getCharSet(dec).size();
            LOG.debug("leaves = {} distributionlength = {}", size, getLengthDistribution(dec));
            double newLength = getAvgLength(dec);
            listList.get("size").add((double) size);
            listList.get("max impact").add(Double.NaN);
            listList.get("max length").add(Double.NaN);
            listList.get("same occurace").add(newLength);
            listList.get("ambiguous characters").add(Double.NaN);
        }

//        plot.add(toArray(runList));
//        plot.add(toArray(sizeList));
//        plot.add(toArray(lengthList));
//        plot.add(toArray(lengthList2));
        return listList;

    }

    public static double[] toArray(List<Double> array) {
        double[] res = new double[array.size()];
        for (int i = 0; i < array.size(); i++) {
            res[i] = array.get(i);
        }
        return res;
    }

    public static Set<Char> getCharSet(Decomposer composer) {
        HashSet<Char> res = new LinkedHashSet<>();
        for (Char object : composer.chars.values()) {
            if (object.isLeaf() && object.getCountAtom() > 0) {
                res.add(object);
            }
        }
        return res;
    }

    private static Pair<Double, Char> getbestReductionWithMaxLength(Char c, int maxLength) {
        int length = c.getLength();
        if (length < 0 || maxLength <= 1) {
            return new Pair<>(-1.0, c);
        }
        List<Pair<Double, Char>> candidates = new LinkedList<>();
        if (c.isValid()) {
            candidates.add(new Pair<>(1.0 * c.getCountAtom() * (length - 1), c));
        }
        List<Char> decFlat = c.getDecFlat();
        if (c.isLeaf()) {
            return candidates.get(0);
        }
        List<Integer> lengthPart = new LinkedList<>();
        int sum = 0;
        for (Char c1 : decFlat) {
            if (!c1.isValidDec()) {
                return candidates.get(0);
            }
            sum += c1.getLength();
            lengthPart.add(c1.getLength());
        }
        for (int i = 0; i < decFlat.size(); i++) {
            candidates.add(getbestReductionWithMaxLength(decFlat.get(i), maxLength - (sum - lengthPart.get(i))));
        }
        candidates.sort((o1, o2) -> {
            return Double.compare(o2.getFirst(), o1.getFirst());
        });
        return candidates.get(0);
    }

    public static Char reduceSubstituteLeaf(Decomposer composer) {
        for (Char parent : composer.chars.values()) {
            if (parent.isLeaf() || parent.getCountAtom() == 0) {
                continue;
            }
            List<Char> dec = parent.getDec();
            for (Char child : dec) {
                if (parent.getCountAtom() == child.getCountAtom() && parent.isValid()) {
                    LOG.debug("child {} can be substituted by parent {}", child, parent);
                    parent.setLeaf();
                    return parent;
                }
            }
        }
        return null;
    }

    public static String reduceSolveAmbiguousChars(Decomposer composer) {
        HashMap<String, Char> ret = new LinkedHashMap<>();
        for (Char parent2 : composer.chars.values()) {
            if (parent2.getCountAtom() == 0) {
                continue;
            }
            String key = composer.decompose(parent2.value);
            if (key.isEmpty()) {
                continue;
            }
            if (ret.containsKey(key)) {
                Char parent1 = ret.get(key);
                if (parent1.isValid() && parent2.isValid()) {
                    LOG.debug("for key '{}' with length {} make both characters to leaves {} and {}", key, key.length(), parent1, parent2);
                    parent1.setLeaf();
                    parent2.setLeaf();
                    return key;
                }
            }
            ret.put(key, parent2);
        }
        return null;
    }

    public static Char reduceMaxLength(Decomposer composer, int maxLength) {
        LinkedList<Char> characters = new LinkedList<>(composer.chars.values());

        while (characters.removeIf(item -> item.getLength() <= maxLength || item.getCountAtom() == 0 || !item.isValidDec())) {
            characters.sort((o1, o2) -> {
                return Integer.compare(o2.getLength(), o1.getLength());
            });
        }
        if (characters.isEmpty()) {
            return null;
        }
        LOG.debug("try to reduce character " + characters.get(0) + " to max length " + maxLength);
        Pair<Double, Char> best = getbestReductionWithMaxLength(characters.get(0), maxLength);
        if (best.getFirst() == 0.0) {
            best = getbestReductionWithMaxLength(characters.get(0), maxLength);
            throw new RuntimeException("character " + best.getSecond() + " cannot be reduced to maximal length " + maxLength);
        }
        Char newLeaf = best.getSecond();
        best.getSecond().setLeaf();
        recalcLength(composer);
        LOG.debug("character '" + newLeaf + "' with score " + best.getFirst() + " is new leaf and reduces character " + characters.get(0) + " to length " + characters.get(0).getLength());
        return newLeaf;
//        }
    }

    public static Char reduceMaxImprovement(Decomposer composer, double minimalImprovement) {
        double sumLength = getSumAndCount(composer)[0];
        LinkedList<Char> chars = new LinkedList<>(composer.chars.values());
        chars.removeIf((t) -> {
            return t.isLeaf() || !t.isValid();
        });
        if (chars.isEmpty()) {
            return null;
        }
        Collections.sort(chars);
        int idx = 0;
        while (idx < chars.size() && chars.get(idx).isLeaf() && chars.get(idx).isValid()) {
            idx++;
        }
        Char newLeave = chars.get(idx);
        double score = newLeave.getScore();
        if ((sumLength - score) / sumLength > 1 - minimalImprovement) {
            return null;
        }
        newLeave.setLeaf();
        recalcLength(composer);
        LOG.debug("character '" + newLeave + "' with score " + score + " is new leaf");
        return newLeave;
    }

    public static void removeChar(Decomposer dec, Char c) {
        if (c.getCountAtom() != c.getCountRoot()) {
            throw new RuntimeException("character " + c + " is part of other characters and cannot be deleted");
        }
        c.reduceCountAtom(c.getCountRoot());
        dec.chars.remove(c.key);

    }

    public static String getAsString(List<Char> list) {
        StringBuilder sb = new StringBuilder();
        if (list == null) {
            return null;
        }
        for (Char c : list) {
            sb.append(c.value);
        }
        return sb.toString();
    }

}
