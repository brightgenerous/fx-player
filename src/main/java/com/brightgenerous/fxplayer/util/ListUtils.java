package com.brightgenerous.fxplayer.util;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ListUtils {

    public static interface IConverter<T, S> {

        T convert(S obj);
    }

    private ListUtils() {
    }

    public static <T, S> List<T> converts(List<S> list, IConverter<T, S> converter) {
        List<T> ret = new ArrayList<>(list.size());
        for (Iterator<S> itr = new LinkedList<>(list).descendingIterator(); itr.hasNext();) {
            S obj = itr.next();
            ret.add(converter.convert(obj));
        }
        return ret;
    }

    public static <T, S> List<T> convertsReversely(List<S> list, IConverter<T, S> converter) {
        List<T> ret = new ArrayList<>(list.size());
        for (Iterator<S> itr = new LinkedList<>(list).descendingIterator(); itr.hasNext();) {
            S obj = itr.next();
            ret.add(0, converter.convert(obj));
        }
        return ret;
    }

    public static <T, S> List<T> convertsAlternately(List<S> list, IConverter<T, S> converter) {
        List<T> ret = new ArrayList<>(list.size());
        Deque<S> deq = new LinkedList<>(list);
        List<T> tails = new ArrayList<>((list.size() / 2) + 1);
        while (!deq.isEmpty()) {
            {
                S info = deq.pollFirst();
                ret.add(converter.convert(info));
            }
            if (!deq.isEmpty()) {
                S info = deq.pollLast();
                tails.add(0, converter.convert(info));
            }
        }
        ret.addAll(tails);
        return ret;
    }

    public static <T> List<T> toReverse(List<T> list) {
        List<T> ret = new ArrayList<>(list.size());
        for (Iterator<T> itr = new LinkedList<>(list).descendingIterator(); itr.hasNext();) {
            T obj = itr.next();
            ret.add(obj);
        }
        return ret;
    }

    public static <T> List<T> toAlternate(List<T> list) {
        List<T> ret = new ArrayList<>(list.size());
        Deque<T> deq = new LinkedList<>(list);
        while (!deq.isEmpty()) {
            {
                T obj = deq.pollFirst();
                ret.add(obj);
            }
            if (!deq.isEmpty()) {
                T obj = deq.pollLast();
                ret.add(obj);
            }
        }
        return ret;
    }
}
