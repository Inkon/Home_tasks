package ru.ifmo.ctddev.zernov.arrayset;

import java.util.*;
import java.util.stream.Collectors;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private Comparator<? super E> comparator;
    private List<E> list;

    public ArraySet() {
        this(null, null);
    }

    public ArraySet(Collection<? extends E> c) {
        this(c, null);
    }

    public ArraySet(Collection<? extends E> c, Comparator<? super E> cmp) {
        if (c != null) {
            list = new ArrayList<>(c.size());
            TreeSet<E> set = new TreeSet<>(cmp);
            for (E obj : c) {
                set.add(obj);
            }
            list.addAll(set.stream().collect(Collectors.toList()));
            list = Collections.unmodifiableList(list);
        } else {
            list = Collections.emptyList();
        }
        comparator = cmp;
    }

    private ArraySet(int fromIndex, int toIndex, List<E> subList, Comparator<? super E> cmp) {
        list = fromIndex < toIndex ? subList.subList(fromIndex, toIndex) : Collections.emptyList();
        comparator = cmp;
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(list, (E) o, comparator()) >= 0;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return list.get(0);
        }
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return list.get(size() - 1);
        }
    }

    private int findFromIndex(E fromElement, boolean inclusive) {
        int fromIndex = Collections.binarySearch(list, fromElement, comparator());
        return (fromIndex < 0) ? ~fromIndex : inclusive ? fromIndex : fromIndex + 1;
    }

    private int findToIndex(E toElement, boolean inclusive) {
        int toIndex = Collections.binarySearch(list, toElement, comparator());
        return (toIndex < 0) ? ~toIndex : (inclusive ? toIndex + 1 : toIndex);
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return new ArraySet<>(findFromIndex(fromElement, fromInclusive), findToIndex(toElement, toInclusive), list, comparator);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new ArraySet<>(0, findToIndex(toElement, inclusive), list, comparator);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new ArraySet<>(findFromIndex(fromElement, inclusive), size(), list, comparator);
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public NavigableSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(0, size(), list, Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public E lower(E e) {
        int index = findToIndex(e, false);
        return index > 0 ? list.get(index - 1) : null;
    }

    @Override
    public E floor(E e) {
        int index = findToIndex(e, true);
        return index > 0 ? list.get(index - 1) : null;
    }

    @Override
    public E ceiling(E e) {
        int index = findFromIndex(e, true);
        return index < size() ? list.get(index) : null;
    }

    @Override
    public E higher(E e) {
        int index = findFromIndex(e, false);
        return index < size() ? list.get(index) : null;
    }

    @Override
    public E pollFirst() {
        return size() == 0 ? null : list.get(0);
    }

    @Override
    public E pollLast() {
        return size() == 0 ? null : list.get(size() - 1);
    }
}
