package de.uro.citlab.cjk;

import de.uro.citlab.cjk.util.DecomposerUtil;
import de.uro.citlab.cjk.util.Gnuplot;
import de.uro.citlab.cjk.util.FileUtil;
import de.uro.citlab.cjk.types.Sign;
import eu.transkribus.errorrate.util.ObjectCounter;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * some interesting links for additional information about decomposition of
 * signs: https://github.com/cjkvi/cjkvi-ids https://github.com/chise/isd
 * https://github.com/kawabata/ids
 *
 * @author gundram
 */
public class Decomposer {

    public class Mapper {

        private HashMap<Integer, String> fw = new LinkedHashMap<>();
        private HashMap<String, Integer> bw = new LinkedHashMap<>();

        public void remove(int integer) {
            String remove = fw.remove(integer);
            if (remove == null) {
                throw new RuntimeException("index " + integer + " does not have to be removed.");
            }
            bw.remove(remove);
        }

        public void add(int integer, String character) {
            String put = fw.put(integer, character);
            if (put != null) {
                throw new RuntimeException("double value vor key " + integer + " " + character + " <> " + put);
            }
            Integer put1 = bw.put(character, integer);
            if (put1 != null) {
                throw new RuntimeException("double value vor calue " + character + " " + integer + " <> " + put1);
            }
        }

        public Integer get(String in) {
            return bw.get(in);
        }

        public String get(Integer in) {
            return fw.get(in);
        }

    }

    public LinkedHashMap<Integer, Sign> signs = new LinkedHashMap<>();
    private static Logger LOG = LoggerFactory.getLogger(Decomposer.class);
    public char COUNTRY = 'G';
    private final Mapper mapper = new Mapper();
    public final Coding coding;

    public enum Coding {
        ANY,
        UTF8,
        IDC,
        UTF8_IDC,
    }

    public Decomposer(Coding coding) {
        this.coding = coding;
        ClassLoader classLoader = Decomposer.class.getClassLoader();
        File f = new File(classLoader.getResource("CJK/ids.txt").getFile());
        List<String> strings = FileUtil.readLines(f);
        //remove both rows at the beginning
        strings.remove(0);
        strings.remove(0);
        int cntErrorDecompose = 0;
        //for each line, create an entry
        for (String string : strings) {
            String[] split = string.split("\t");
            int idx = Integer.parseInt(split[0].substring(2), 16);
            mapper.add(idx, split[1]);
//            mapper.add(idx, split[1]);
            String decomposed = getDecomposition(split, COUNTRY);
            signs.put(idx, new Sign(this, split[1], idx, DecomposerUtil.hasStrokeSigns(decomposed) ? null : decomposed));
        }
        //add IdeographicDescriptionCharacters
        for (char c : DecomposerUtil.IdeographicDescriptionCharacters) {
            signs.put((int) c, new Sign(this, String.valueOf(c), (int) c, null));
            mapper.add((int) c, String.valueOf(c));
        }
        //create deep structure
        for (Sign value : signs.values()) {
//            DecomposerUtil.initLeaves(value, signs, mapper,true);
            if (!initLeaves(value)) {
                cntErrorDecompose++;
            }
        }
        //calculate length
        for (Sign value : signs.values()) {
            value.getLength();
        }
        LOG.info("{} of {} signs could not be decomposed, which is {} %.", cntErrorDecompose, signs.size(), ((double) ((cntErrorDecompose * 10000) / signs.size())) / 100);
//        for (Sign value : signs.values()) {
//            List<Sign> unicodeDecomposition = value.getDec();
//            int length = unicodeDecomposition == null ? 0 : unicodeDecomposition.size();
//            if (length == 0 && !value.isValid()) {
//                continue;
//            }
//        }
        //remove signs where the decomposition is not in the UTF-8 unicode area.
        removeUnValidSigns();
    }

    private boolean initLeaves(Sign s) {
        String rawdecomposition = s.getRawdecomposition();
        if (rawdecomposition == null) {
            s.setLeaf();
            return s.isValid();
        }
        List<Sign> dec = new LinkedList<>();
        int idx = 0;
//            System.out.println("----------------");
        while (idx < rawdecomposition.length()) {
            String singleS = rawdecomposition.substring(idx, idx + 1);
//                System.out.println(singleS.charAt(0) + " " + (int) singleS.charAt(0));
            Sign singleSign = signs.get(mapper.get(singleS));
            Sign sign = singleSign;
            if (idx + 1 < rawdecomposition.length()) {
                String doubleS = rawdecomposition.substring(idx, idx + 2);
                Sign doubleSign = signs.get(mapper.get(doubleS));
                if (singleSign != null && doubleSign != null) {
                    throw new RuntimeException("found two possible decomposition");
                }
                if (singleSign == null && doubleSign == null) {
                    throw new RuntimeException("found no possible decomposition");
                }
                idx += singleSign == null ? 1 : 0;
                sign = singleSign == null ? doubleSign : singleSign;
            }
            idx++;
            dec.add(sign);
        }
        s.setDecomposition(dec);
        return true;
    }

    public void count(String c) {
        Integer idx = mapper.get(c);
        if (idx == null) {
            LOG.warn(String.format("cannot interprete sign '%s' with length %d and unicode U+%04X - sign will be added to decomposition-list as leaf", c, c.length(), (int) c.charAt(0)));
//            throw new RuntimeException(String.format("cannot interprete sign '%s' with length %d and unicode U+%04X", c, c.length(), (int) c.charAt(0)));
            int index = (int) c.charAt(0);
            mapper.add(index, c);
            signs.put(index, new Sign(this, c, index, null));
            idx = index;
        }
        signs.get(idx).count(true);

    }

    public void reset() {
        for (Sign value : signs.values()) {
            value.reset();
        }
    }

    private final void removeUnValidSigns() {
//        int cntNotValid = 0;
        Set<Integer> invalids = new HashSet<>();
        //reset satutus, it is was true (because leaves could have changed
        for (Sign value : signs.values()) {
            if (!value.makeValidDec()) {
                invalids.add(value.key);
            }
        }
        LOG.info("{} of {} signs are not valid, which is {} %.", invalids.size(), signs.size(), ((double) ((invalids.size() * 10000) / signs.size())) / 100);
        for (Integer number : invalids) {
            signs.remove(number);
            mapper.remove(number);
        }

    }

    private static String getDecomposition(String[] split, char country) {
        if (split.length == 3) {
            if (split[2].matches(".*\\[[A-Z]+\\].*")) {
                return split[2].substring(0, split[2].indexOf("["));
            }
            return split[2];
        }
        for (int i = 2; i < split.length; i++) {
            String string = split[i];
            if (string.matches(".*\\[.*" + country + ".*\\].*")) {
                return string.substring(0, string.indexOf("["));
            }
        }
        String s = split[2];
        LOG.trace("cannot find result for country '{}' in line '{}. Take first component {}", country, split, s);
        int idx = s.indexOf("[");
        return idx < 0 ? s : s.substring(0, idx);
    }

    public int getSizeSigns() {
        return this.signs.size();
    }

    public int getSizeLeaves() {
        int cnt = 0;
        for (Sign value : signs.values()) {
            if (value.isLeaf()) {
                cnt++;
            }
        }
        return cnt;
    }

    public String decompose(String c) {
        Sign get = signs.get(mapper.get(c));
        if (get == null) {
            LOG.warn("cannot decompose sign '{}'" + c);
            return null;
        }
        return DecomposerUtil.getAsString(get.getDec());
    }

    public String decomposeAtoms(String c) {
        Sign get = signs.get(mapper.get(c));
        if (get == null) {
            return null;
        }
        return DecomposerUtil.getAsString(get.getAtoms());
    }

    public void removeSeldomLeaves(int minOccurance) {
        for (Sign value : signs.values()) {
            if (value.isLeaf()) {
                if (value.getCountAtom() <= minOccurance) {
                    value.setValid(false);
                }
            }
        }
        removeUnValidSigns();

    }

    public void removeSign(Sign sign) {
        if (sign.getCountAtom() != sign.getCountSign()) {
            throw new RuntimeException("sign " + sign + " is part of other sign and cannot be deleted");
        }
        sign.reduceCountAtom(sign.getCountSign());
        signs.remove(sign.key);

    }

}
