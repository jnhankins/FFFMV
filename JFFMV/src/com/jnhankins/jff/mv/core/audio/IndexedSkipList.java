/**
 * JFFMV - An application for creating music videos using flame fractals.
 * Copyright (C) 2015 Jeremiah N. Hankins
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jnhankins.jff.mv.core.audio;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * {@code IndexedSkipList} is an implementation of a indexed skip list, which
 * is a data structure that allows fast searches within an ordered sequence of
 * elements. Fast searches are made possible by maintaining a linked hierarchy
 * of subsequences, each skipping over fewer elements. 
 * <p>
 * Elements in a skip list can be tesetd for containment, added, retrieved, and
 * removed by value in O(log(n)) time. In addition, elements in an <i>indexed</i>
 * skip list can also be retrieved and removed by index in O(log(n)) time (these
 * operations normally take O(n) time in normal skip lists).
 * <p>
 * In order to maintain a sorted order, the elements of {@code IndexedSkipList}
 * must implement the {@code Comaprable} interface. Further, because order must
 * be maintained, {@code IndexedSkipList} does not support operations which
 * insert or set element values based on indexes, such as the indexed
 * {@code add} or {@code set} methods.
 * <p>
 * The iterator for this collection is a {@code ListIterator} however the 
 * {@code add}, {@code remove}, and {@code set} methods are not supported. Also,
 * though the iterator can perform {@code next()} in O(1) time, 
 * {@code previous()} requires O(log(n)) time.
 * <p>
 * This {@code Container} does <b>not</b> permit {@code null} entries.
 * <p>
 * <b>Note that this implementation is not synchronized.</b>
 * <p>
 * <i>Credit where credit is due: This source code is base on two sources: The
 * indexed skip list algorithms are borrowed from Raymond Hettinger's Python
 * example code found
 * <a href="https://code.activestate.com/recipes/576930/">here</a>. And the idea
 * for dynamically modifying the maximum level was inspired in part by Michael
 * Naper's work found
 * <a href="http://michaelnaper.com/samplework/programming/skiplist_java/SkipList.java">here</a>.
 * </i>
 * 
 * @param <E> the the type of {@code Comparable} object contained in this list
 * 
 * @author Raymond Hettinger (2009)
 * @author Michael Naper (2010)
 * @author Jeremiah N. Hankins (2015)
 */
public class IndexedSkipList<E extends Comparable<? super E>> implements List<E> {
    /**
     * A random number generator to be used by the {@link #randomLevel()}
     * method.
     * <p>
     * A private {@code Random} member is preferable to {@code Math.random()},
     * because all {@code Math.random()} calls rely on atomic calls to a single 
     * shared {@code Random} instance.
     */
    private final Random random = new Random();
    
    /**
     * The head node.
     * This node does not contain useful data.
     */
    private final SkipNode head;
    
    /**
     * The number of elements in the list.
     */
    private int size;
    
    /**
     * Constructs a new empty {@code IndexedSkipList}.
     */
    public IndexedSkipList() {
        head = new IndexedSkipList.SkipNode(null, 0);
        Arrays.fill(head.width, 1);
        size = 0;
    }
    
    /**
     * Returns the number of elements in this list.
     * <p>
     * Complexity: O(1)
     * 
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return size;
    }
    
    /**
     * Returns {@code true} if this list contains no elements.
     * <p>
     * Complexity: O(1)
     * 
     * @return {@code true} if this list contains no elements
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }
            
    /**
     * Returns {@code true} if this list contains the specified element. More
     * formally, returns {@code true} if and only if this list contains at least
     * one element {@code e} such that {@code e.compareTo(o)==0}.
     * <p>
     * Complexity: O(log(n))
     * 
     * @param element the element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element
     * @throws ClassCastException if the type of the specified element is
     * incompatible with this list
     * @throws NullPointerException if the specified element is {@code null}
     */
    @Override
    public boolean contains(Object element) {
        return lastIndexOf(element) != -1;
    }
    
    /**
     * Returns {@code true} if this list contains all of the elements of the
     * specified collection.
     * <p>
     * Complexity: O(m*log(n)), where m is the size of the specified collection
     *
     * @param collection collection to be checked for containment in this list
     * @return {@code true} if this list contains all of the elements of the
     * specified collection
     * @throws ClassCastException if the type of the specified element is
     * incompatible with this list
     * @throws NullPointerException if the specified collection is {@code null}
     * or contains a {@code null} element
     * @see #contains(java.lang.Object)
     */
    @Override
    public boolean containsAll(Collection<?> collection) {
        for (Object o : collection)
            if (!contains((E)o))
                return false;
        return true;
    }
    
    /**
     * Returns the element at the specified position in this list.
     * <p>
     * Complexity: O(log(n))
     * 
     * @param index the index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range
     * ({@code index < 0 || index >= size()})
     */
    @Override
    public E get(int index) {
        return getNode(index).value;
    }
    
    /**
     * Adds the specified element to an appropriate position in this list. The
     * added elements position in the list will be chosen such that the list
     * maintains an ascending sorted order (determined by the {@code Comaprable}
     * interface}.
     * <p>
     * Complexity: O(log(n))
     * 
     * @param element the element to insert
     * @return {@code true}, always
     * @throws NullPointerException if the specified element is {@code null}
     */
    @Override
    public boolean add(E element) {
        int maxLevel = head.level();
        
        // Find the first node on each level where node.next[level].value >= value
        SkipNode[] pathNodes = new IndexedSkipList.SkipNode[maxLevel + 1];
        int[] pathSteps = new int[maxLevel +1];
        SkipNode node = head;
        for (int level = maxLevel; level >= 0; level--) {
            while (node.next[level] != null && element.compareTo(node.next[level].value) > 0) {
                pathSteps[level] += node.width[level];
                node = node.next[level];
            }
            pathNodes[level] = node;
        }
        
        // Determine the level for the new node
        int nodeLevel = randomLevel();
        
        // Expand the head and path arrays if the maximum level has increased
        if (nodeLevel > maxLevel) {
            head.next = Arrays.copyOf(head.next, nodeLevel+1);
            head.width = Arrays.copyOf(head.width, nodeLevel+1);
            Arrays.fill(head.width, maxLevel+1, nodeLevel+1, size);
            pathNodes = Arrays.copyOf(pathNodes, nodeLevel+1);
            pathSteps = Arrays.copyOf(pathSteps, nodeLevel+1);
            Arrays.fill(pathNodes, maxLevel+1, nodeLevel+1, head);
            pathNodes[nodeLevel] = head;
        }
        
        // Insert a link to the new node at each level
        int steps = 0;
        SkipNode newNode = new SkipNode(element, nodeLevel);
        for (int level = 0; level <= nodeLevel; level++) {
            SkipNode prevNode = pathNodes[level];
            newNode.next[level] = prevNode.next[level];
            prevNode.next[level] = newNode;
            newNode.width[level] = prevNode.width[level] - steps;
            prevNode.width[level] = steps + 1;
            steps += pathSteps[level];
        }
        for (int level = nodeLevel+1; level <= maxLevel; level++)
            pathNodes[level].width[level] += 1;
        int s = size - head.width[nodeLevel] + 2;
        for (int level = maxLevel+1; level <= nodeLevel; level++)
            newNode.width[level] = s;
        // Decrement the size
        size++;
        // Done
        return true;
    }
    
    /**
     * Adds all of the elements in the specified collection to this list by
     * iterating through its elements and adding them individually. The behavior
     * of this operation is undefined if the specified collection is modified
     * while the operation is in progress (Note that this will occur if the
     * specified collection is this list, and it's nonempty).
     * <p>
     * Complexity: O(m*log(n)), where m is the size of the specified collection
     * 
     * @param collection the collection containing elements to be added to this
     * list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the type of the specified element is
     * incompatible with this list
     * @throws NullPointerException if the specified collection is {@code null}
     * or contains a {@code null} element
     * @see #add(java.lang.Comparable) 
     */
    @Override
    public boolean addAll(Collection<? extends E> collection) {
        boolean hasChanged = false;
        for (E e : collection)
            hasChanged |= add(e);
        return hasChanged;
    }
    
    /**
     * Removes the element at the specified position in this list. Shifts any
     * subsequent elements to the left (subtracts one from their indices).
     * Returns the element that was removed from the list.
     * <p>
     * Complexity: O(log(n))
     * 
     * @param index the index of the element to remove
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range
     * ({@code index < 0 || index >= size()})
     */
    @Override
    public E remove(int index) {
        if (index < 0 || index >= size())
            throw new IndexOutOfBoundsException();
        
        // find first node on each level where node.next[level].value >= value
        int maxLevel = head.level();
        SkipNode[] pathNodes = new IndexedSkipList.SkipNode[maxLevel+1];
        SkipNode node = head;
        index += 1;
        for (int level = head.level(); level >= 0; level--) {
            while (node.next[level] != null && node.width[level] < index) {
                index -= node.width[level];
                node = node.next[level];
            }
            pathNodes[level] = node;
        }
        node = node.next[0];
        // Remove one link at each level
        int nodeLevel = node.level();
        for (int level = 0; level <= nodeLevel; level++) {
            pathNodes[level].next[level] = node.next[level];
            pathNodes[level].width[level] += node.width[level] - 1;
        }
        for (int level = nodeLevel+1; level <= maxLevel; level++) {
            pathNodes[level].width[level] -= 1;
        }
        // Decrement the size
        size--;
        // Reduce the maximum level by shrinking the head
        while (maxLevel > 0 && head.width[maxLevel] == size+1)
            maxLevel --;
        if (maxLevel < head.level()) {
            head.next = Arrays.copyOf(head.next, maxLevel+1);
            head.width = Arrays.copyOf(head.width, maxLevel+1);
        }
        return node.value;
    }
    
    /**
     * Removes the specified element from the list, if it is present. More
     * formally, removes an element {@code e} such that 
     * ({@code o.compareTo(e)==0}.
     * <p>
     * Complexity: O(log(n))
     * 
     * @param element the element to be removed
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if the specified element is {@code null}
     * @throws ClassCastException if the type of the specified element is
     * incompatible with this list
     */
    @Override
    public boolean remove(Object element) {
        E e = (E)element;
        // find first node on each level where node.next[level].value >= value
        int maxLevel = head.level();
        SkipNode[] pathNodes = new IndexedSkipList.SkipNode[maxLevel+1];
        SkipNode node = head;
        for (int level = maxLevel; level >= 0; level--) {
            while (node.next[level] != null && e.compareTo(node.next[level].value) > 0)
                node = node.next[level];
            pathNodes[level] = node;
        }
        // Have we found the element?
        node = node.next[0];
        if (node == null || e.compareTo(node.value) != 0)
            return false;
        // Remove one link at each level
        int nodeLevel = node.level();
        for (int level = 0; level <= nodeLevel; level++) {
            pathNodes[level].next[level] = node.next[level];
            pathNodes[level].width[level] += node.width[level] - 1;
        }
        for (int level = nodeLevel+1; level <= maxLevel; level++) {
            pathNodes[level].width[level] -= 1;
        }
        // Decrement the size
        size--;
        // Reduce the maximum level by shrinking the head
        while (maxLevel > 0 && head.width[maxLevel] == size+1)
            maxLevel --;
        if (maxLevel < head.level()) {
            head.next = Arrays.copyOf(head.next, maxLevel+1);
            head.width = Arrays.copyOf(head.width, maxLevel+1);
        }
        // Done
        return true;
    }
    
    /**
     * Removes from this list all of its elements that are contained in the
     * specified collection.
     * <p>
     * Complexity: O(m*log(n)), where m is the size of the specified collection
     * 
     * @param collection the collection containing elements to be removed from
     * this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the type of the specified element is
     * incompatible with this list
     * @throws NullPointerException if the specified collection is {@code null}
     * or contains a {@code null} element
     * @see #remove(java.lang.Object)
     * @see #contains(java.lang.Object) 
     */
    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean hasChanged = false;
        for (Object o : collection) 
            while (remove((E)o))
                hasChanged = true;
        return hasChanged;
    }
    
    /**
     * Retains only the elements in this list that are contained in the
     * specified collection. In other words, removes from this list all of its
     * elements that are not contained in the specified collection.
     * <p>
     * Complexity: O(n*log(n))
     * 
     * @param collection the collection containing elements to be retained in
     * this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the type of the specified element is
     * incompatible with this list
     * @throws NullPointerException if the specified collection is {@code null}
     * @see #remove(java.lang.Object)
     * @see #contains(java.lang.Object) 
     */
    @Override
    public boolean retainAll(Collection<?> collection) {
        if (collection == null)
            throw new IllegalArgumentException();
        if (isEmpty() || collection.isEmpty())
            return false;
        boolean hasChanged = false;
        SkipNode node = head.next[0];
        while (node != null) {
            E value = node.value;
            node = node.next[0];
            if (collection.contains(value)) {
                remove(value);
                hasChanged = true;
            }
        }
        return hasChanged;
    }
    
    /**
     * Removes all of the elements from this list. The list will be empty after
     * this call returns.
     * <p>
     * Complexity: O(1)
     */
    @Override
    public void clear() {
        head.next = new IndexedSkipList.SkipNode[1];
        head.width = new int[] {1};
        size = 0;
    }
    
    /**
     * Returns the index of the first occurrence of the specified element in
     * this list, or {@code -1} if this list does not contain the element. More
     * formally, returns the lowest index {@code i} such that
     * {@code get(i).compareTo(o)==0}, or {@code -1} if there is no such index.
     * <p>
     * Complexity: O(n*log(n))
     *
     * @param element the element to search for
     * @return the index of the first occurrence of the specified element in
     * this list, or {@code -1} if this list does not contain the element
     * @throws ClassCastException if the type of the specified element is
     * incompatible with this list
     * @throws NullPointerException if the specified element is {@code null}
     */
    @Override
    public int indexOf(Object element) {
        E e = (E)element;
        int index = 0;
        SkipNode node = head;
        for (int level = head.level(); level >= 0; level--) {
            while (node.next[level] != null) {
                int c = e.compareTo(node.next[level].value);
                // If the value in the node is too small keep going on this 
                // level
                if (c > 0) {
                    index += node.width[level];
                    node = node.next[level];
                } else {
                    // If the value in the node is equal to the value we're
                    // looking for, and we cannot continue by going down a level
                    // then we're done
                    if (c == 0 && (level == 0 || e.compareTo(node.next[level-1].value) < 0)) {
                        index += node.width[level];
                        return index - 1;
                    }
                    // Otherwise, we're just done with the level
                    break;
                }
            }
        }
        return -1;
    }
    
    /**
     * Returns the index of the last occurrence of the specified element in this
     * list, or {@code -1} if this list does not contain the element. More
     * formally, returns the highest index {@code i} such that
     * {@code get(i).compareTo(o)==0}, or {@code -1} if there is no such index.
     * <p>
     * Complexity: O(n*log(n))
     *
     * @param o the element to search for
     * @return the index of the last occurrence of the specified element in this
     * list, or {@code -1} if this list does not contain the element
     * @throws ClassCastException if the type of the specified element is
     * incompatible with this list
     * @throws NullPointerException if the specified element is {@code null}
     */
    @Override
    public int lastIndexOf(Object o) {
        E e = (E)o;
        int index = 0;
        SkipNode node = head;
        for (int level = head.level(); level >= 0; level--) {
            while (node.next[level] != null && e.compareTo(node.next[level].value) >= 0) {
                index += node.width[level];
                node = node.next[level];
            }
        }
        if (e.compareTo(node.value) == 0)
            return index - 1;
        return -1;
    }
    
    /**
     * Compares the specified object with this list for equality. Returns
     * {@code true} if and only if the specified object is also a list, both
     * lists have the same size, and all corresponding pairs of elements in the
     * two lists are <i>equal</i>. (Two elements {@code e1} and {@code e2} are
     * <i>equal</i> if {(@code e1==null ? e2==null : e1.equals(e2))})
     * In other words, two lists are defined to be equal if they contain the 
     * same elements in the same order.
     * 
     * @param o the object to be compared for equality with this lsit
     * @return {@code true} if the specified object is equal to this list
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof List))
            return false;
        ListIterator<E> e1 = listIterator();
        ListIterator e2 = ((List) o).listIterator();
        while (e1.hasNext() && e2.hasNext()) {
            E o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1 == null ? o2 == null : o1.equals(o2))) {
                return false;
            }
        }
        return !(e1.hasNext() || e2.hasNext());
    }
    
    /**
     * Returns the hash code value for this list.
     * <p>
     * This implementation uses
     * exactly the code that is used to define the list hash function in the
     * documentation for the {@link List#hashCode()} method.
     * 
     * @return the hash code value for this list
     */
    @Override
    public int hashCode() {
        int hashCode = 1;
        for (E e : this)
            hashCode = 31*hashCode + (e==null ? 0 : e.hashCode());
        return hashCode;
    }

    
    @Override
    public Iterator<E> iterator() {
        return new SkipIterator(0);
    }
    
    @Override
    public ListIterator<E> listIterator() {
        return new SkipIterator(0);
    }
    
    @Override
    public ListIterator<E> listIterator(int index) {
        return new SkipIterator(index);
    }
    
    @Override
    public Object[] toArray() {
        int index = 0;
        Object[] a = new Object[size];
        for (SkipNode node = head.next[0]; node != null; node = node.next[0])
            a[index++] = node.value;
        return a;
    }
    
    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            a = (T[])java.lang.reflect.Array.newInstance(
                    a.getClass().getComponentType(), size);
        int index = 0;
        Object[] result = a;
        for (SkipNode node = head.next[0]; node != null; node = node.next[0])
            result[index++] = node.value;
        if (result.length > size)
            result[size] = null;
        return a;
    }
    
    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }
    
    /**
     * Prints a diagram of the internal structure of this
     * {@code IndexedSkipList} to {@code System.out}. For best results, ensure
     * that the {@code String} objects returned by the {@code toString()} method
     * of the elements contained in the list are no wider than two characters.
     */
    public void debugPrint() {
        for (int level = head.level(); level >= 0 ; level--) {
            for (SkipNode node = head; node != null; node=node.next[level]) {
                System.out.printf("|%2s|",node.width[level]);
                for (int w = node.width[level]; w > 1; w--)
                    System.out.print("--------");
                System.out.print("--->");
            }
            System.out.println();
        }
        System.out.print("  H     ");
        for (SkipNode node = head.next[0]; node != null; node=node.next[0]) 
            System.out.printf("<%2s>    ",node.value.toString());
        System.out.println();
        
        System.out.printf(" %2d     ",size);
        for (int index=0; index < size; index++)
            System.out.printf("[%2d]    ",index);
        System.out.println();
    }

    /**
     * This operation is not supported in {@code IndexedSkipList}.
     * 
     * @param i ignored
     * @param e ignored
     * @throws UnsupportedOperationException always
     */
    @Override
    public void add(int i, E e) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * This operation is not supported in {@code IndexedSkipList}.
     * 
     * @param i ignored
     * @param c ignored
     * @return nothing
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean addAll(int i, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported in {@code IndexedSkipList}.
     * 
     * @param i ignored
     * @param e ignored
     * @return nothing
     * @throws UnsupportedOperationException always
     */
    @Override
    public E set(int i, E e) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * This operation is not supported in {@code SkipList}.
     * 
     * @param i ignored
     * @param j ignored
     * @return nothing
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<E> subList(int i, int j) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Returns a random number in the range [{@code 0}, {@code maxLevel}]
     * distributed so that the probability of returning {@code 0} is 0.25 and 
     * the probability of {@code i} is twice the probability of returning
     * {@code i+1}.
     * 
     * @return a random level index
     */
    private int randomLevel() {
        int maxLevel = (int)(1+Math.log(size+1)/Math.log(2));
        int level = (int)(Math.log(1-random.nextDouble())/Math.log(0.5));
        return Math.min(maxLevel, level);
    }
    
    /**
     * Returns the node at the specified position in this list. The head node
     * is at index {@code -1}, the first node containing a data element is at
     * index {@code 0}, etc.
     * <p>
     * Performance: O(log(n))
     * 
     * @param index the index of the node to return
     * @return the node at the specified position in this list
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     * ({@code index < 0 || index > size()})
     */
    private SkipNode getNode(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException();
        SkipNode node = head;
        index += 1;
        for (int level = head.level(); level >= 0; level--) {
            while (node.width[level] <= index) {
                index -= node.width[level];
                node = node.next[level];
            }
        }
        return node;
    }
    
    private class SkipNode {
        final E value;
        SkipNode[] next;
        int[] width;
        
        SkipNode(E value, int level) {
            this.value = value;
            next = new IndexedSkipList.SkipNode[level+1];
            width = new int[level+1];
        }
        
        public int level() {
            return next.length - 1;
        }
        
        @Override
        public String toString() {
            if (value == null)
                return "H";
            return value.toString();
        }
    }
    
    private class SkipIterator implements ListIterator<E> {
        int nextIndex;
        SkipNode nextNode;

        public SkipIterator(int index) {
            this.nextIndex = index;
            nextNode = getNode(index);
        }
        
        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public boolean hasNext() {
            return nextIndex < size;
        }
        
        @Override
        public E next() {
            if (!hasNext())
                throw new NoSuchElementException();
            E val = nextNode.value;
            nextNode = nextNode.next[0];
            nextIndex++;
            return val;
        }
        
        @Override
        public int previousIndex() {
            return nextIndex-1;
        }
        
        @Override
        public boolean hasPrevious() {
            return 0 < nextIndex;
        }

        @Override
        public E previous() {
            if (!hasPrevious())
                throw new NoSuchElementException();
            nextIndex--;
            nextNode = getNode(nextIndex);
            return nextNode.value;
        }
        
        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }
    }
}
