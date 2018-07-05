/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uro.citlab.cjk.util;

import de.uro.citlab.cjk.types.Sign;
import de.uro.citlab.cjk.Decomposer;
import eu.transkribus.errorrate.util.ObjectCounter;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
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

    public static final Set<Character> IdeographicDescriptionCharacters;
    public static final Set<Character> StrokeCountCharacters;

    static {
        IdeographicDescriptionCharacters = new HashSet<>();
        for (char c = '\u2FF0'; c < '\u2FFC'; c++) {
            IdeographicDescriptionCharacters.add(c);
        }
        StrokeCountCharacters = new HashSet<>();
        for (char c = '\u2460'; c < '\u2474'; c++) {
            StrokeCountCharacters.add(c);
        }
    }

    public static boolean isIDC(String s) {
        return s.length() == 1 && isIDC(s.charAt(0));
    }

    public static boolean isIDC(char c) {
        return IdeographicDescriptionCharacters.contains(c);
    }

    public static boolean isStrokeC(String s) {
        return s.length() == 1 && isStrokeC(s.charAt(0));
    }

    public static boolean isStrokeC(char c) {
        return StrokeCountCharacters.contains(c);
    }

    public static boolean isIDCSign(Sign sign) {
        return isIDC(sign.value);
    }

    public static boolean hasStrokeSigns(String decomposed) {
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
        for (Sign sign : composer.signs.values()) {
            if (sign.getCountSign() > 0) {
                res.add(sign.getLength(), sign.getCountSign());
            }
        }
        return res;
    }

    public static void saveCharSet(Decomposer dec, File outFile, boolean onlyWithCounts) {
        List<String> out = new LinkedList<>();
        List<Sign> charSet = new LinkedList<>(DecomposerUtil.getCharSet(dec));
        if (onlyWithCounts) {
            charSet.removeIf((t) -> {
                return t.getCountAtom() <= 0 || t.getLength() < 1;
            });
        }
        charSet.sort((Sign o1, Sign o2) -> o2.getCountAtom() - o1.getCountAtom());
        for (Sign sign : charSet) {
            try {
//                out.add(sign.toString() + " = (len=" + length + ") " + dec.decompose(sign.unicode));
                out.add(String.format("%-50s '%s'=>'%s'", sign, DecomposerUtil.getAsString(sign.getDec()), DecomposerUtil.getAsString(sign.getAtoms())));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
//            System.out.println(key + " = (len=" + length + ") " + Decomposer.decompose(key));
        }
        FileUtil.writeLines(outFile, out);

    }

    public static void saveMap(Decomposer dec, File outFile, boolean onlyWithCounts, boolean raw) {
        List<String> out = new LinkedList<>();
        List<Sign> values = new LinkedList<>(dec.signs.values());
        if (onlyWithCounts) {
            values.removeIf((t) -> {
                return t.getCountAtom() <= 0;
            });
        }
        values.sort((o1, o2) -> {
            return o2.getCountAtom() - o1.getCountAtom();
        });
        for (Sign sign : values) {
            try {
                if (raw) {
                    out.add(String.format("%s\t%s\t%s", sign.value, DecomposerUtil.getAsString(sign.getDec()), DecomposerUtil.getAsString(sign.getAtoms())));

                } else {
//                out.add(sign.toString() + " = (len=" + length + ") " + dec.decompose(sign.unicode));
                    out.add(String.format("%-50s '%-10s'=>'%s'", sign, DecomposerUtil.getAsString(sign.getDec()), DecomposerUtil.getAsString(sign.getAtoms())));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
//            System.out.println(key + " = (len=" + length + ") " + Decomposer.decompose(key));
        }
        FileUtil.writeLines(outFile, out);

    }

    public static double getAvgLength(Decomposer composer) {
        int sum = 0, count = 0;
        ObjectCounter<Integer> lengthDistribution = getLengthDistribution(composer);
        for (Pair<Integer, Long> pair : lengthDistribution.getResultOccurrence()) {
            sum += pair.getFirst() * pair.getSecond();
            count += pair.getSecond();
        }
        return ((double) sum) / count;
    }

    private static void recalcLength(Decomposer dec) {
        for (Sign value : dec.signs.values()) {
            value.resetLength();
        }
        for (Sign value : dec.signs.values()) {
            value.getLength();
        }
    }

    public static List<double[]> reduceDecomposer(Decomposer dec, double relImprovement, int maxLength) {
        List<double[]> plot = new LinkedList<>();
        List<Double> sizeList = new LinkedList<>();
        List<Double> lengthList = new LinkedList<>();
        List<Double> lengthList2 = new LinkedList<>();
        LOG.debug("average length {}", DecomposerUtil.getAvgLength(dec));
        double length = DecomposerUtil.getAvgLength(dec);
        double firstlength = length;
        int sizeStart = getCharSet(dec).size();
        int idx = 1;
        while (true) {
            int size = getCharSet(dec).size();
            LOG.debug("leaves = {} distributionlength = {}", size, DecomposerUtil.getLengthDistribution(dec));
            DecomposerUtil.insertLeaf(dec, true, true);
            double newLength = DecomposerUtil.getAvgLength(dec);
            if (LOG.isTraceEnabled()) {
                LOG.trace(String.format("%2d: %.2f %.4f%% %.4f%%", idx, newLength, (length / newLength - 1) * 100, (newLength / firstlength) * 100));
            }//            runList.add((double) i);
            sizeList.add((double) size / sizeStart);
            lengthList.add(newLength);
            lengthList2.add(newLength);
            if (length / newLength < 1 + relImprovement) {
                length = newLength;
                break;
            }
            length = newLength;
            idx++;
        }
        LOG.debug("substituting signs longer than {}...", maxLength);
        while (insertLeaf(dec, maxLength) != null) {
            int size = getCharSet(dec).size();
            LOG.debug("leaves = {} distributionlength = {}", size, DecomposerUtil.getLengthDistribution(dec));
            double newLength = DecomposerUtil.getAvgLength(dec);
            sizeList.add((double) size / sizeStart);
            lengthList.add(length);
            lengthList2.add(newLength);
        }
        LOG.debug("substituting leaves that are only part of one sign .", maxLength);
        while (substituteLeaf(dec) != null) {
            int size = getCharSet(dec).size();
            LOG.debug("leaves = {} distributionlength = {}", size, DecomposerUtil.getLengthDistribution(dec));
            double newLength = DecomposerUtil.getAvgLength(dec);
            sizeList.add((double) size / sizeStart);
            lengthList.add(length);
            lengthList2.add(newLength);
        }

        LOG.debug("substituting ambiguous signs...", maxLength);
        while (substituteambiguousSigns(dec) != null) {
            int size = getCharSet(dec).size();
            LOG.debug("leaves = {} distributionlength = {}", size, DecomposerUtil.getLengthDistribution(dec));
            double newLength = DecomposerUtil.getAvgLength(dec);
            sizeList.add((double) size / sizeStart);
            lengthList.add(length);
            lengthList2.add(newLength);
        }

//        plot.add(toArray(runList));
        plot.add(toArray(sizeList));
        plot.add(toArray(lengthList));
        plot.add(toArray(lengthList2));
        return plot;

    }

    private static double[] toArray(List<Double> array) {
        double[] res = new double[array.size()];
        for (int i = 0; i < array.size(); i++) {
            res[i] = array.get(i);
        }
        return res;
    }

    public static Set<Sign> getCharSet(Decomposer composer) {
        HashSet<Sign> res = new LinkedHashSet<>();
        for (Sign object : composer.signs.values()) {
            if (object.isLeaf() && object.getCountAtom() > 0) {
                res.add(object);
            }
        }
        return res;
    }

    private static Pair<Double, Sign> getbestReductionWithMaxLength(Sign sign, int maxLength) {
        int length = sign.getLength();
        if (length < 0 || maxLength <= 1) {
            return new Pair<>(-1.0, sign);
        }
        List<Pair<Double, Sign>> candidates = new LinkedList<>();
        if (sign.isValid()) {
            candidates.add(new Pair<>(1.0 * sign.getCountAtom() * (length - 1), sign));
        }
        List<Sign> decFlat = sign.getDecFlat();
        if (sign.isLeaf()) {
            return candidates.get(0);
        }
        List<Integer> lengthPart = new LinkedList<>();
        int sum = 0;
        for (Sign sign1 : decFlat) {
            if (!sign1.isValidDec()) {
                return candidates.get(0);
            }
            sum += sign1.getLength();
            lengthPart.add(sign1.getLength());
        }
        for (int i = 0; i < decFlat.size(); i++) {
            candidates.add(getbestReductionWithMaxLength(decFlat.get(i), maxLength - (sum - lengthPart.get(i))));
        }
        candidates.sort((o1, o2) -> {
            return Double.compare(o2.getFirst(), o1.getFirst());
        });
        return candidates.get(0);
    }

    public static Sign substituteLeaf(Decomposer composer) {
        for (Sign value : composer.signs.values()) {
            if (value.isLeaf()) {
                continue;
            }
            List<Sign> dec = value.getDec();
//            Sign child = null;
            for (Sign sign : dec) {
                if (value.getCountAtom() == sign.getCountAtom()) {
                    LOG.debug("child {} can be substituted by parent {}", sign, value);
                    value.setLeaf();
                    return value;
                }
            }
        }
        return null;
    }

    public static String substituteambiguousSigns(Decomposer composer) {
        HashMap<String, Sign> ret = new LinkedHashMap<>();
        for (Sign parent2 : composer.signs.values()) {
            if (parent2.getCountAtom() == 0) {
                continue;
            }
            String key = composer.decompose(parent2.value);
            if (key.isEmpty()) {
                continue;
            }
            if (ret.containsKey(key)) {
                Sign parent1 = ret.get(key);
                if (parent1.isValid() && parent2.isValid()) {
                    LOG.debug("for key '{}' with length {} make both signs to leaves {} and {}", key, key.length(), parent1, parent2);
                    parent1.setLeaf();
                    parent2.setLeaf();
                    return key;
                }
            }
            ret.put(key, parent2);
        }
        return null;
    }

    public static Sign insertLeaf(Decomposer composer, int maxLength) {
        LinkedList<Sign> signs = new LinkedList<>(composer.signs.values());

        while (signs.removeIf(item -> item.getLength() <= maxLength || item.getCountAtom() == 0 || !item.isValidDec())) {
            signs.sort((o1, o2) -> {
                return Integer.compare(o2.getLength(), o1.getLength());
            });
        }
        if (signs.isEmpty()) {
            return null;
        }
        LOG.debug("try to reduce sign " + signs.get(0) + " to max length " + maxLength);
//        for (Sign sign : signs) {
        Pair<Double, Sign> best = getbestReductionWithMaxLength(signs.get(0), maxLength);
        if (best.getFirst() == 0.0) {
            best = getbestReductionWithMaxLength(signs.get(0), maxLength);
            throw new RuntimeException("sign " + best.getSecond() + " cannot be reduced to maximal length " + maxLength);
        }
        Sign newLeaf = best.getSecond();
        best.getSecond().setLeaf();
        recalcLength(composer);
        LOG.debug("sign '" + newLeaf + "' with score " + best.getFirst() + " is new leaf and reduces sign " + signs.get(0) + " to length " + signs.get(0).getLength());
        return newLeaf;
//        }
    }

    public static Sign insertLeaf(Decomposer composer, boolean mostAtoms, boolean onlyValid) {
        LinkedList<Sign> signs = new LinkedList<>(composer.signs.values());
        signs.sort(new Comparator<Sign>() {
            @Override
            public int compare(Sign o1, Sign o2) {
                if (mostAtoms) {
                    return Integer.compare((o2.getLength() - 1) * o2.getCountAtom(), (o1.getLength() - 1) * o1.getCountAtom());
                }
                return Integer.compare((o2.getLength() - 1) * o2.getCountSign(), (o1.getLength() - 1) * o1.getCountSign());
            }
        });
        int idx = 0;
        while (idx < signs.size() && signs.get(idx).isLeaf() && (!onlyValid || signs.get(idx).isValid())) {
            idx++;
        }
        Sign newLeave = signs.get(idx);
        double score = mostAtoms ? (newLeave.getLength() - 1) * newLeave.getCountAtom() : (newLeave.getLength() - 1) * newLeave.getCountSign();
        newLeave.setLeaf();
        recalcLength(composer);
        LOG.debug("sign '" + newLeave + "' with score " + score + " is new leaf");
        return newLeave;
    }

    public static void removeSign(Decomposer dec, Sign sign) {
        if (sign.getCountAtom() != sign.getCountSign()) {
            throw new RuntimeException("sign " + sign + " is part of other sign and cannot be deleted");
        }
        sign.reduceCountAtom(sign.getCountSign());
        dec.signs.remove(sign.key);

    }

    public static String getAsString(List<Sign> list) {
        StringBuilder sb = new StringBuilder();
        if (list == null) {
            return null;
        }
        for (Sign sign : list) {
            sb.append(sign.value);
        }
        return sb.toString();
    }

}
