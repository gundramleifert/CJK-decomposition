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
public class Sign implements Comparable<Sign> {

    public final String value;
    public final int key;
//        public final String utf8;
    private final String rawdecomposition;
    private List<Sign> decomposition = null;
    private boolean isValid;
    private int length = -1;
    private int countSign = 0;
    private int countAtom = 0;
    private boolean isLeaf = false;
    private static Logger LOG = LoggerFactory.getLogger(Sign.class);
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
        final Sign other = (Sign) obj;
        if (this.key != other.key) {
            return false;
        }
        if (!Objects.equals(this.value, other.value)) {
            return false;
        }
        return true;
    }

    public Sign(Decomposer root, String value, int key, String rawdecomposition) {
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
    public void setDecomposition(List<Sign> dec) {
        decomposition = dec;
    }

    public String getRawdecomposition() {
        return rawdecomposition;
    }

    public void setAlternative(char c) {

    }

    public void setLeaf() {
        if (!this.isLeaf) {
            for (Sign sign : decomposition) {
                sign.reduceCountAtom(countAtom);
            }
        }
        isLeaf = true;
        length = 1;
    }

    public void reset() {
        countSign = 0;
        countAtom = 0;
        isLeaf = decomposition != null;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void reduceCountAtom(int sum) {
        countAtom -= sum;
        if (countAtom <= 1 && sum != 0) {
            LOG.warn("count of sign {} is reduced to {}, maybe sign can be deleted from Decomposer", this, countSign);
        }
        if (!isLeaf) {
            for (Sign sign : decomposition) {
                sign.reduceCountAtom(sum);
            }
        }
    }

    public void count(boolean asSign) {
        countAtom++;
        if (asSign) {
            countSign++;
        }
        if (decomposition != null) {
            for (Sign sign : decomposition) {
                sign.count(false);
            }
        }
    }

    public int getCountSign() {
        return countSign;
    }

    public int getCountAtom() {
        return countAtom;
    }

    public void resetLength() {
        this.length = -1;
    }

//    public int getLengthUnicode() {
//        List<Sign> decUnicode = getDec();
//        return decUnicode == null ? -1 : decUnicode.size();
//    }
    public int getLengthAtom() {
        if (decomposition == null) {
            switch (root.coding) {
                case ANY:
                case UTF8:
                    return 1;
                case IDC:
                case UTF8_IDC:
                    return DecomposerUtil.isIDCSign(this) ? 0 : 1;
                default:
                    throw new RuntimeException("unexpected state");
            }
        }
        int len = 0;
        for (Sign sign : decomposition) {
            len += sign.getLengthAtom();
        }
        return len;
    }

//    public int getLengthWithoutIDC() {
//        if (isLeaf) {
//            return DecomposerUtil.isIDCSign(this) ? 0 : 1;
//        }
//        int length = 0;
//        for (Sign sign : decomposition) {
//            length += sign.getLengthWithoutIDC();
//        }
//        return length;
//
//    }
    public int getLength() {
//        if (length > 0) {
//            return length;
//        }
        if (isLeaf) {
            switch (root.coding) {
                case ANY:
                case UTF8:
                    return 1;
                case IDC:
                case UTF8_IDC:
                    return DecomposerUtil.isIDCSign(this) ? 0 : 1;
                default:
                    throw new RuntimeException("unexpected state");
            }
        }
        length = 0;
        for (Sign sign : decomposition) {
            length += sign.getLength();
        }
//            if (length % 2 == 0) {
//                throw new RuntimeException("unexpected");
//            }
        return length;
    }

    @Override
    public String toString() {
        return String.format("[ U+%05X '%s' len=%2d/%2d (occurance=%4d/%4d) leaf=%b valid=%b]", (int) key, value, getLength(), getLengthAtom(), countSign, countAtom, isLeaf(), isValid());
//        try {
//            return String.format("[ U+%05x '%s' len= %d(%d) cntSn=%d cntAt=%d '%s' '%s']", (int) number, unicode, getLength(), getLengthAtom(), countSign, countAtom, getDec(), getDecFlat());
//        } catch (Throwable ex) {
//            return String.format("[ U+%05x '%s' len= %d(%d) cntSn=%d cntAt=%d '%s' '%s']", (int) number, unicode, getLength(), getLengthAtom(), countSign, countAtom, "?", "?");
//        }
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
        for (Sign sign : decomposition) {
            isValidDecomposition &= sign.makeValidDec();
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
    public List<Sign> getDecFlat() {
        return decomposition;
    }

    public List<Sign> getDec() {
        if (isLeaf()) {
            switch (root.coding) {
                case ANY:
                case UTF8:
                    return isValid() ? Arrays.asList(this) : null;
                case IDC:
                case UTF8_IDC:
                    if (DecomposerUtil.isIDCSign(this)) {
                        return isValid() ? new LinkedList<>() : null;
                    } else {
                        return isValid() ? Arrays.asList(this) : null;
                    }
                default:
                    throw new RuntimeException("unexpected state");
            }
        }
        List<Sign> res = new LinkedList<>();
        for (Sign child : decomposition) {
            List<Sign> dec = child.getDec();
            if (dec == null) {
//                LOG.warn("sign {} does not have a valid decomposition and is no leaf (dec = {}) -> make to leaf", this, decomposition);
//                setLeaf();
//                return getDec();
                return null;
            }
            res.addAll(dec);
        }
        return res;
    }

    public List<Sign> getAtoms() {
        LinkedList<Sign> res = new LinkedList<>();
        if (decomposition == null) {
            res.add(this);
        } else {
            for (Sign sign : decomposition) {
                res.addAll(sign.getAtoms());
            }
        }
        return res;
    }

//    public List<Sign> getDec() {
//        if (isLeaf) {
//            return Arrays.asList(this);
//        }
//        List<Sign> res = new LinkedList<>();
//        for (Sign sign : decomposition) {
//            res.addAll(sign.getDec());
//        }
//        return res;
//    }
//    private List<Sign> list getDecomposition() {
//        if (isLeaf) {
//            list.add(this);
//        } else {
//            for (Sign sign : decomposition) {
//                sign.appendAtoms(list);
//            }
//        }
//    }
    public int getScore() {
        return (getLength() - 1) * getCountAtom();
    }

    @Override
    public int compareTo(Sign o2) {
        return Integer.compare(o2.getScore(), getScore());
    }
}
