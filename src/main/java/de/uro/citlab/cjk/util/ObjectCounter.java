/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uro.citlab.cjk.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author gundram
 * @param <E> object type
 */
public class ObjectCounter<E> implements Serializable {

    private final Map<E, Long> map = new HashMap<>();

    @Override
    public String toString() {
        return new ArrayList<>(map.entrySet()).toString();
    }

    public void add(E object) {
        if (map.containsKey(object)) {
            map.put(object, map.get(object) + 1L);
        } else {
            map.put(object, 1L);
        }
    }

    public void add(E object, long count) {
        if (map.containsKey(object)) {
            map.put(object, map.get(object) + count);
        } else {
            map.put(object, count);
        }
    }

    public void reset() {
        map.clear();
    }

    public void addAll(Collection<E> counter) {
        for (E e : counter) {
            add(e);
        }
    }

    public void addAll(ObjectCounter<E> counter) {
        for (Pair<E, Long> pair : counter.getResultOccurrence()) {
            if (map.containsKey(pair.getFirst())) {
                map.put(pair.getFirst(), map.get(pair.getFirst()) + pair.getSecond());
            } else {
                map.put(pair.getFirst(), pair.getSecond());
            }
        }
    }

    public List<Pair<E, Long>> getResultOccurrence() {
        List<Pair<E, Long>> ret = new ArrayList<>();
        for (Map.Entry<E, Long> entry : map.entrySet()) {
            ret.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        Collections.sort(ret, new Comparator<Pair<E, Long>>() {

            @Override
            public int compare(Pair<E, Long> o1, Pair<E, Long> o2) {
                return Long.compare(o2.getSecond(), o1.getSecond());
            }
        });
        return ret;
    }

    public Map<E, Long> getMap() {
        return new HashMap<>(map);
    }

    public long get(E key) {
        return map.getOrDefault(key, 0L);
    }

    public List<E> getResult() {
        List<Pair<E, Long>> resultOccurrence = getResultOccurrence();
        List<E> ret = new ArrayList<>();
        for (Pair<E, Long> pair : resultOccurrence) {
            ret.add(pair.getFirst());
        }
        return ret;
    }
}
