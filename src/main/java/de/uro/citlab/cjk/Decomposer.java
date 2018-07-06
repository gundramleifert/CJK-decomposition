package de.uro.citlab.cjk;

import de.uro.citlab.cjk.util.DecomposerUtil;
import de.uro.citlab.cjk.types.Char;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * some interesting links for additional information about decomposition of
 * characters: https://github.com/cjkvi/cjkvi-ids https://github.com/chise/isd
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

    public LinkedHashMap<Integer, Char> chars = new LinkedHashMap<>();
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
        String[] file = null;
        try {
            file = new String(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("ids.txt"))).split("\n");
        } catch (IOException ex) {
            throw new RuntimeException("cannot load resource 'ids.txt'", ex);
        }
//        List<String> strings = FileUtil.readLines(f);
        //remove both rows at the beginning
        int cntErrorDecompose = 0;
        //for each line, create an entry
        for (int i = 2; i < file.length; i++) {
            String string = file[i];
            String[] split = string.split("\t");
            int idx = Integer.parseInt(split[0].substring(2), 16);
            mapper.add(idx, split[1]);
//            mapper.add(idx, split[1]);
            String decomposed = getDecomposition(split, COUNTRY);
            chars.put(idx, new Char(this, split[1], idx, DecomposerUtil.hasStrokeCharacter(decomposed) ? null : decomposed));
        }
        //add IdeographicDescriptionCharacters
        for (char c : DecomposerUtil.IDEOGRAPHIC_DESCRIPTION_CHARACTERS) {
            chars.put((int) c, new Char(this, String.valueOf(c), (int) c, null));
            mapper.add((int) c, String.valueOf(c));
        }
        //create deep structure
        for (Char value : chars.values()) {
            if (!initLeaves(value)) {
                cntErrorDecompose++;
            }
        }
        //calculate length
        for (Char value : chars.values()) {
            value.getLength();
        }
        LOG.info("{} of {} characters could not be decomposed, which is {} %.", cntErrorDecompose, chars.size(), ((double) ((cntErrorDecompose * 10000) / chars.size())) / 100);
        //remove characters where the decomposition is not in the UTF-8 unicode area.
        removeUnValidCharacters();
    }

    private boolean initLeaves(Char s) {
        String rawdecomposition = s.getRawdecomposition();
        if (rawdecomposition == null) {
            s.setLeaf();
            return s.isValid();
        }
        List<Char> dec = new LinkedList<>();
        int idx = 0;
//            System.out.println("----------------");
        while (idx < rawdecomposition.length()) {
            String singleS = rawdecomposition.substring(idx, idx + 1);
//                System.out.println(singleS.charAt(0) + " " + (int) singleS.charAt(0));
            Char singleChar = chars.get(mapper.get(singleS));
            Char character = singleChar;
            if (idx + 1 < rawdecomposition.length()) {
                String doubleS = rawdecomposition.substring(idx, idx + 2);
                Char doubleChar = chars.get(mapper.get(doubleS));
                if (singleChar != null && doubleChar != null) {
                    throw new RuntimeException("found two possible decomposition");
                }
                if (singleChar == null && doubleChar == null) {
                    throw new RuntimeException("found no possible decomposition");
                }
                idx += singleChar == null ? 1 : 0;
                character = singleChar == null ? doubleChar : singleChar;
            }
            idx++;
            dec.add(character);
        }
        s.setDecomposition(dec);
        return true;
    }

    public void count(String c) {
        Integer idx = mapper.get(c);
        if (idx == null) {
            LOG.warn(String.format("cannot interprete character '%s' with length %d and unicode U+%04X - character will be added to decomposition-list as leaf", c, c.length(), (int) c.charAt(0)));
//            throw new RuntimeException(String.format("cannot interprete character '%s' with length %d and unicode U+%04X", c, c.length(), (int) c.charAt(0)));
            int index = (int) c.charAt(0);
            mapper.add(index, c);
            chars.put(index, new Char(this, c, index, null));
            idx = index;
        }
        chars.get(idx).count(true);

    }

    public void reset() {
        for (Char value : chars.values()) {
            value.reset();
        }
    }

    private void removeUnValidCharacters() {
//        int cntNotValid = 0;
        Set<Integer> invalids = new HashSet<>();
        //reset satutus, it is was true (because leaves could have changed
        for (Char value : chars.values()) {
            if (!value.makeValidDec()) {
                invalids.add(value.key);
            }
        }
        LOG.info("{} of {} characters are not valid, which is {} %.", invalids.size(), chars.size(), ((double) ((invalids.size() * 10000) / chars.size())) / 100);
        for (Integer number : invalids) {
            chars.remove(number);
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

    public int getSizeChars() {
        return this.chars.size();
    }

    public int getSizeLeaves() {
        int cnt = 0;
        for (Char value : chars.values()) {
            if (value.isLeaf()) {
                cnt++;
            }
        }
        return cnt;
    }

    public String decompose(String c) {
        Char get = chars.get(mapper.get(c));
        if (get == null) {
            LOG.warn("cannot decompose character '{}'" + c);
            return null;
        }
        return DecomposerUtil.getAsString(get.getDec());
    }

    public String decomposeAtoms(String c) {
        Char get = chars.get(mapper.get(c));
        if (get == null) {
            return null;
        }
        return DecomposerUtil.getAsString(get.getAtoms());
    }

    public void removeSeldomLeaves(int minOccurance) {
        for (Char value : chars.values()) {
            if (value.isLeaf()) {
                if (value.getCountAtom() <= minOccurance) {
                    value.setValid(false);
                }
            }
        }
        removeUnValidCharacters();

    }

    public void removeChar(Char c) {
        if (c.getCountAtom() != c.getCountRoot()) {
            throw new RuntimeException("character " + c + " is part of other character and cannot be deleted");
        }
        c.reduceCountAtom(c.getCountRoot());
        chars.remove(c.key);
        mapper.remove(c.key);

    }

}
