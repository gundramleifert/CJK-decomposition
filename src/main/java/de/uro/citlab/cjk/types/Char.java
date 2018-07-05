/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uro.citlab.cjk.types;

import de.uro.citlab.cjk.Decomposer;
import de.uro.citlab.cjk.util.DecomposerUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gundram
 */
public class Char implements Comparable<Char> {

    public final String value;
    public final int key;
//        public final String utf8;
    private final String rawdecomposition;
    private List<Char> decomposition = null;
    private boolean isValid;
    private int length = -1;
    private int countRoot = 0;
    private int countAtom = 0;
    private boolean isLeaf = false;
    private static Logger LOG = LoggerFactory.getLogger(Char.class);
    private final Decomposer root;

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + this.key;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Char other = (Char) obj;
        if (this.key != other.key) {
            return false;
        }
        if (!Objects.equals(this.value, other.value)) {
            return false;
        }
        return true;
    }

    public Char(Decomposer root, String value, int key, String rawdecomposition) {
        this.value = value;
        this.key = key;
        this.rawdecomposition = value.equals(rawdecomposition) ? null : rawdecomposition;
        if (this.rawdecomposition == null) {
            isLeaf = true;
        }
        this.root = root;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

//    public boolean isValid() {
//        return isValid;
//    }
    public void setDecomposition(List<Char> dec) {
        decomposition = dec;
    }

    public String getRawdecomposition() {
        return rawdecomposition;
    }

    public void setAlternative(char c) {

    }

    public void setLeaf() {
        if (!this.isLeaf) {
            for (Char c : decomposition) {
                c.reduceCountAtom(countAtom);
            }
        }
        isLeaf = true;
        length = 1;
    }

    public void reset() {
        countRoot = 0;
        countAtom = 0;
        isLeaf = decomposition != null;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void reduceCountAtom(int sum) {
        countAtom -= sum;
        if (countAtom <= 1 && sum != 0) {
            LOG.warn("count of character {} is reduced to {}, maybe character can be deleted from Decomposer", this, countRoot);
        }
        if (!isLeaf) {
            for (Char c : decomposition) {
                c.reduceCountAtom(sum);
            }
        }
    }

    public void count(boolean asRoot) {
        countAtom++;
        if (asRoot) {
            countRoot++;
        }
        if (decomposition != null) {
            decomposition.forEach((c) -> {
                c.count(false);
            });
        }
    }

    public int getCountRoot() {
        return countRoot;
    }

    public int getCountAtom() {
        return countAtom;
    }

    public void resetLength() {
        this.length = -1;
    }

    public int getLengthAtom() {
        if (decomposition == null) {
            switch (root.coding) {
                case ANY:
                case UTF8:
                    return 1;
                case IDC:
                case UTF8_IDC:
                    return DecomposerUtil.isIDChar(this) ? 0 : 1;
                default:
                    throw new RuntimeException("unexpected state");
            }
        }
        int len = 0;
        for (Char c : decomposition) {
            len += c.getLengthAtom();
        }
        return len;
    }

    public int getLength() {
        if (isLeaf) {
            switch (root.coding) {
                case ANY:
                case UTF8:
                    return 1;
                case IDC:
                case UTF8_IDC:
                    return DecomposerUtil.isIDChar(this) ? 0 : 1;
                default:
                    throw new RuntimeException("unexpected state");
            }
        }
        length = 0;
        for (Char c : decomposition) {
            length += c.getLength();
        }
//            if (length % 2 == 0) {
//                throw new RuntimeException("unexpected");
//            }
        return length;
    }

    @Override
    public String toString() {
        return String.format("[ U+%05X '%s' len=%2d/%2d (occurance=%4d/%4d) leaf=%b valid=%b]", (int) key, value, getLength(), getLengthAtom(), countRoot, countAtom, isLeaf(), isValid());
    }

    public boolean isValid() {
        switch (root.coding) {
            case ANY:
            case IDC:
                return true;
            case UTF8:
            case UTF8_IDC:
                return value.length() == 1;
            default:
                throw new RuntimeException("");
        }
    }

    public boolean makeValidDec() {
        boolean isValidDecomposition = true;
        if (isLeaf()) {
            return isValid();
        }
        for (Char c : decomposition) {
            isValidDecomposition &= c.makeValidDec();
        }
        if (!isValidDecomposition) {
            if (isValid()) {
                setLeaf();
                return true;
            }
        }
        return isValidDecomposition;
    }

    public boolean isValidDec() {
        return getDec() != null;
    }

//    public boolean hasDec() {
//        return decomposition != null;
//    }
    public List<Char> getDecFlat() {
        return decomposition;
    }

    public List<Char> getDec() {
        if (isLeaf()) {
            switch (root.coding) {
                case ANY:
                case UTF8:
                    return isValid() ? Arrays.asList(this) : null;
                case IDC:
                case UTF8_IDC:
                    if (DecomposerUtil.isIDChar(this)) {
                        return isValid() ? new LinkedList<>() : null;
                    } else {
                        return isValid() ? Arrays.asList(this) : null;
                    }
                default:
                    throw new RuntimeException("unexpected state");
            }
        }
        List<Char> res = new LinkedList<>();
        for (Char child : decomposition) {
            List<Char> dec = child.getDec();
            if (dec == null) {
                return null;
            }
            res.addAll(dec);
        }
        return res;
    }

    public List<Char> getAtoms() {
        LinkedList<Char> res = new LinkedList<>();
        if (decomposition == null) {
            res.add(this);
        } else {
            for (Char c : decomposition) {
                res.addAll(c.getAtoms());
            }
        }
        return res;
    }

    public int getScore() {
        return (getLength() - 1) * getCountAtom();
    }

    @Override
    public int compareTo(Char o2) {
        return Integer.compare(o2.getScore(), getScore());
    }
}
