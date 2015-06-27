/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jnhankins.jff.mv.core.audio;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Random;

/**
 * {@code IndexedSkipList} is an implementation of an indexed skip list, which
 * is a data structure that allows fast searches within an ordered sequence of
 * elements. Fast searches are made possible by maintaining a linked hierarchy
 * of subsequences, each skipping over fewer elements. {@code IndexedSkipList}
 * implements all optional {@code Collection} operations and permits
 * {@code null} and repeated elements. The elements are ordered using their
 * {@link Comparable natural ordering}, or by a {@code Comparator} provided at
 * set creation time, depending on which constructor is used.
 * <p>
 * The {@code contains}, {@code indexOf}, {@code get}, {@code add}, and
 * {@code remove} methods run in amortized {@code log(n)} time (for both index
 * based and object reference based methods). While the {@code size},
 * {@code isEmpty}, {@code clear}, and {@code iterator} operations run in
 * constant time.
 * <p>
 * Skip list details: This implementation of indexed skip list uses
 * probabilistic methods for generating the internal link hierarchy. The number
 * of layers used in the link is updated dynamically as skip list grows and
 * shrinks. The number of layers, on average, should be roughly {@code log2(n)},
 * and each layer, on average, should contain roughly half as many links as the
 * layer below it.
 * <p>
 * Note that the ordering maintained by the skip (whether or not an explicit
 * comparator is provided) must be <i>consistent with equals</i>.
 * <p>
 * <b>Note that this implementation is not synchronized.</b> If multiple threads
 * access an {@code IndexedSkipList} instance concurrently, and at least one of
 * the threads modifies the skip list structurally, it must be synchronized
 * externally. (A structural modification is any operation that adds or deletes
 * one or more elements.)
 * <p>
 * The iterator returned by this class's {@code iterator} method is
 * <i>fail-fast</i>: if the skip list is structurally modified at any time after
 * the iterator is created, in any way, the iterator will throw a
 * {@code ConcurrentModificationException}. Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 * <p>
 * Note that the fail-fast behavior of an iterator cannot be guaranteed as it
 * is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification. Fail-fast iterators throw
 * {@code ConcurrentModificationException} on a best-effort basis. Therefore, it
 * would be wrong to write a program that depended on this exception for its
 * correctness: <i>the fail-fast behavior of iterators should be used only to
 * detect bugs</i>.
 * 
 * @author Jeremiah N. Hankins
 */
public class IndexedSkipList<E> implements Collection<E> {    
    /**
     * A random number generator to be used by the {@link #randomLevel()}
     * method.
     * <p>
     * A private {@code Random} member is preferable to {@code Math.random()},
     * because all {@code Math.random()} calls rely on atomic calls to a single 
     * shared {@code Random} instance.
     */
    protected final Random random = new Random();
    
    /**
     * The comparator used to maintain order in this skip list, or {@code null}
     * if the natural ordering of elements is to be used.
     */
    protected final Comparator<? super E> comparator;
    
    /**
     * The head node.
     * <p>
     * This node does not contain an element data.
     */
    protected final SkipNode<E> head;

    /**
     * The number of elements in the list.
     */
    protected int size = 0;
    
    /**
     * The number of times this list has been structurally modified. Structural
     * modifications are those that modify the list, or otherwise perturb it in
     * such a fashion that iterations in progress may yield incorrect results.
     */
    protected int modCount = 0;

    /**
     * Constructs a new, empty indexed skip list, sorted according to the
     * natural ordering of its elements. All elements inserted into the set must
     * implement the {@link Comparable} interface. Furthermore, all such
     * elements must be <i>mutually comparable</i>: {@code e1.compareTo(e2)}
     * must not throw a {@code ClassCastException} for any elements {@code e1}
     * and {@code e2} in the skip list. If the user attempts to add an element
     * to the skip list that violates this constraint (for example, the user
     * attempts to add a string element to a skip list whose elements are
     * integers), the {@code add} call will throw a {@code ClassCastException}.
     */
    public IndexedSkipList() {
        this(null, null);
    }

    /**
     * Constructs a new, empty indexed skip list, sorted according to the
     * specified comparator. All elements inserted into the set must be
     * <i>mutually comparable</i> by the specified comparator:
     * {@code comparator.compare(e1, e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and {@code e2} in
     * the skip list. If the user attempts to add an element to the skip list
     * that violates this constraint (for example, the user attempts to add a
     * string element to a skip list whose elements are integers), the
     * {@code add} call will throw a {@code ClassCastException}.
     *
     * @param comparator the comparator that will be used to order this skip
     * list, or {@code null} if the {@link Comaprable} of the elements will be
     * used
     */
    public IndexedSkipList(Comparator<? super E> comparator) {
        this(null, comparator);

    }

    /**
     * Constructs a new indexed skip list containing the elements in the
     * specified collection, sorted according to the natural ordering of its
     * elements. All elements inserted into the set must implement the
     * {@link Comparable} interface. Furthermore, all such elements must be
     * <i>mutually comparable</i>: {@code e1.compareTo(e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and {@code e2} in
     * the skip list. If the user attempts to add an element to the skip list
     * that violates this constraint (for example, the user attempts to add a
     * string element to a skip list whose elements are integers), the
     * {@code add} call will throw a {@code ClassCastException}.
     *
     * @param collection the collection whose elements will comprise the new set
     */
    public IndexedSkipList(Collection<? extends E> collection) {
        this(collection, null);
    }

    /**
     * Constructs a new indexed skip list containing the elements in the
     * specified collection, sorted according to the specified comparator. All
     * elements inserted into the set must be
     * <i>mutually comparable</i> by the specified comparator:
     * {@code comparator.compare(e1, e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and {@code e2} in
     * the skip list. If the user attempts to add an element to the skip list
     * that violates this constraint (for example, the user attempts to add a
     * string element to a skip list whose elements are integers), the
     * {@code add} call will throw a {@code ClassCastException}.
     *
     * @param collection the collection whose elements will comprise the new set
     * @param comparator the comparator that will be used to order this skip
     * list, or {@code null} if the {@link Comaprable} of the elements will be
     * used
     */
    public IndexedSkipList(Collection<? extends E> collection, Comparator<? super E> comparator) {
        // Store the comparator
        this.comparator = comparator;
        // Construct the head node
        head = new SkipNode(null, 0);
        Arrays.fill(head.width, 1);
        // Add the elements from the collection
        if (collection != null) {
            addAll(collection);
        }
    }
    
    /**
     * Returns the number of elements in this skip list.
     * <p>
     * Complexity: O(1)
     * 
     * @return the number of elements in this skip list
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns {@code true} if this skip list contains no elements.
     * <p>
     * Complexity: O(1)
     * 
     * @return {@code true} if this skip list contains no elements
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
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
    public E get(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException();
        SkipNode<E> node = head;
        index += 1;
        for (int level = head.level(); level >= 0; level--) {
            while (node.width[level] <= index) {
                index -= node.width[level];
                node = node.next[level];
            }
        }
        return node.element;
    }
    
    /**
     * Returns the index of the first occurrence of the specified element in
     * this list, or {@code -1} if this list does not contain the element. More
     * formally, returns the lowest index {@code i} such that
     * {@code (o==null ? get(i)==null : o.equals(get(i)))}, or {@code -1} if
     * there is no such index.
     * <p>
     * Complexity: O(log(n))
     *
     * @param element the element to search for
     * @return the index of the first occurrence of the specified element in
     * this skip list, or {@code -1} if this skip list does not contain the
     * element
     * @throws ClassCastException if the specified object cannot be compared
     * with the elements currently in the skip list
     * @throws NullPointerException if the specified element is {@code null} and
     * this set uses natural ordering, or its comparator does not permit
     * {@code null} elements
     */
    public int indexOf(Object element) {
        E e = (E)element;
        int index = 0;
        SkipNode<E> node = head;
        int maxLevel = head.level();
        
        if (comparator != null) {
            for (int level = maxLevel; level >= 0; level--) {
                while (node.next[level] != null) {
                    int d = comparator.compare(e, node.next[level].element);
                    // If the value in the node is too small keep going on this 
                    // level
                    if (d > 0) {
                        index += node.width[level];
                        node = node.next[level];
                    } else {
                        // If the value in the node is equal to the value we're
                        // looking for, and we cannot continue by going down a 
                        // level then we're done
                        if ((e==null ? node.element==null : e.equals(node.element)) &&
                            (level == 0 || comparator.compare(e, node.next[level-1].element) < 0)) {
                            index += node.width[level];
                            return index - 1;
                        }
                        // Otherwise, we're just done with the level
                        break;
                    }
                }
            }
        } else {
            Comparable c = (Comparable)e;
            for (int level = head.level(); level >= 0; level--) {
                while (node.next[level] != null) {
                    int d = c.compareTo((Comparable)node.next[level].element);
                    // If the value in the node is too small keep going on this 
                    // level
                    if (d > 0) {
                        index += node.width[level];
                        node = node.next[level];
                    } else {
                        // If the value in the node is equal to the value we're
                        // looking for, and we cannot continue by going down a 
                        // level then we're done
                        if ((e==null ? node.element==null : e.equals(node.element)) &&
                            (level == 0 || c.compareTo((Comparable)node.next[level-1].element) < 0)) {
                            index += node.width[level];
                            return index - 1;
                        }
                        // Otherwise, we're just done with the level
                        break;
                    }
                }
            }
        }
        return -1;
    }
    
    /**
     * Returns the index of the last occurrence of the specified element in this
     * skip list, or {@code -1} if this list does not contain the element. More
     * formally, returns the highest index {@code i} such that
     * {@code (o==null ? get(i)==null : o.equals(get(i)))}, or {@code -1} if
     * there is no such index.
     * <p>
     * Complexity: O(log(n))
     *
     * @param o the element to search for
     * @return the index of the last occurrence of the specified element in this
     * skip list, or {@code -1} if this skip list does not contain the element
     * @throws ClassCastException if the specified object cannot be compared
     * with the elements currently in the skip list
     * @throws NullPointerException if the specified element is {@code null} and
     * this set uses natural ordering, or its comparator does not permit
     * {@code null} elements
     */
    public int lastIndexOf(Object o) {
        E e = (E)o;
        int index = 0;
        SkipNode<E> node = head;
        int maxLevel = head.level();
        
        if (comparator != null) {
            for (int level = maxLevel; level >= 0; level--) {
                while (node.next[level] != null && comparator.compare(e, node.next[level].element) >= 0) {
                    index += node.width[level];
                    node = node.next[level];
                }
            }
        } else {
            Comparable c = (Comparable)e;
            for (int level = maxLevel; level >= 0; level--) {
                while (node.next[level] != null && c.compareTo((Comparable)node.next[level].element) >= 0) {
                    index += node.width[level];
                    node = node.next[level];
                }
            }
        }
        
        if (node == null || !(e==null ? node.element==null : e.equals(node.element)))
            return -1;
        return index - 1;
    }

    /**
     * Returns {@code true} if this skip list contains the specified element.
     * More formally, returns {@code true} if and only if this skip list
     * contains at least one element {@code e} such that {@code o.equals(e)}.
     * <p>
     * Complexity: O(log(n))
     * 
     * @param o element whose presence in this skip list is to be tested
     * @return {@code true} if this skip list contains the specified element
     * @throws ClassCastException if the specified object cannot be compared
     * with the elements currently in the skip list
     * @throws NullPointerException if the specified element is {@code null} and
     * this set uses natural ordering, or its comparator does not permit
     * {@code null} elements
     */
    @Override
    public boolean contains(Object o) {
        return lastIndexOf(o) != -1;
    }

    /**
     * Returns {@code true} if this skip list contains all of the elements in
     * the specified collection.
     * <p>
     * Complexity: O(m*log(n)), where m is the size of the specified collection.
     * 
     * @param collection collection to be checked for containment in this skip
     * list
     * @return {@code true} if this skip list contains all of the elements in
     * the specified collection
     * @throws ClassCastException if the elements provided cannot be compared
     * with the elements currently in the skip list
     * @throws NullPointerException if the specified collection is {@code null}
     * or if any element is {@code null} and this set uses natural ordering, or
     * its comparator does not permit {@code null} elements
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
     * Inserts the specified element into this skip list a position such that
     * the skip list will maintain a sorted order according to its comparator,
     * or if it has no comparator, its elements' natural ordering. Shifts the
     * element at that position (if any) and any subsequent elements to the
     * right (adds one to their indices).
     * <p>
     * Complexity: O(log(n))
     * 
     * @param e the element to be added to this skip list
     * @return {@code true} (as specified by 
     * {@link Collection#add(java.lang.Object) Collection.add(E))
     * @throws ClassCastException if the specified object cannot be compared
     * with the elements currently in the skip list
     * @throws NullPointerException if the specified element is {@code null} and
     * this set uses natural ordering, or its comparator does not permit
     * {@code null} elements
     */
    @Override
    public boolean add(E e) {
        int maxLevel = head.level();
        
        // Find the first node on each level where node.next[level].value >= value
        SkipNode<E>[] pathNodes = new SkipNode[maxLevel+1];
        int[] pathSteps = new int[maxLevel +1];
        SkipNode<E> node = head;
        if (comparator != null) {
            for (int level = maxLevel; level >= 0; level--) {
                while (node.next[level] != null && comparator.compare(e, node.next[level].element) > 0) {
                    pathSteps[level] += node.width[level];
                    node = node.next[level];
                }
                pathNodes[level] = node;
            }
        } else {
            Comparable c = (Comparable)e;
            for (int level = maxLevel; level >= 0; level--) {
                while (node.next[level] != null && c.compareTo((Comparable)node.next[level].element) > 0) {
                    pathSteps[level] += node.width[level];
                    node = node.next[level];
                }
                pathNodes[level] = node;
            }
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
        SkipNode newNode = new SkipNode(e, nodeLevel);
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
        
        // Increment the size
        size++;
        
        // Increment the mod count
        modCount++;
        
        // Done
        return true;
    }

    /**
     * Adds all of the elements in the specified collection to this skip list. 
     * The behavior of this operation is undefined if the specified collection
     * is modified while the operation is in progress. (This implies that the
     * behavior of this call is undefined if the specified collection is this
     * skip list, and this skip list is nonempty.)
     * <p>
     * Complexity: O(m*log(n)), where m is the size of the specified collection.
     *
     * @param collection the collection containing elements to be added to this
     * skip list
     * @return {@code true} if this skip list changed as a result of the call
     * @throws ClassCastException if the elements provided cannot be compared
     * with the elements currently in the skip list
     * @throws NullPointerException if the specified collection is {@code null}
     * or if any element is {@code null} and this set uses natural ordering, or
     * its comparator does not permit {@code null} elements
     * @see #add(java.lang.Object) 
     */
    @Override
    public boolean addAll(Collection<? extends E> collection) {
        boolean hasChanged = false;
        for (Object o : collection) 
            hasChanged |= add((E)o);
        return hasChanged;
    }

    /**
     * Removes all of the elements from this skip list. The skip list will be
     * empty after this call returns.
     * <p>
     * Complexity: O(1)
     */
    @Override
    public void clear() {
        head.next = new SkipNode[1];
        head.width = new int[] {1};
        size = 0;
        modCount++;
    }
    
    /**
     * Removes the element at the specified position in this skip list. Shifts
     * any subsequent elements to the left (subtracts one from their indices).
     * Returns the element that was removed from the list.
     * <p>
     * Complexity: O(log(n))
     * 
     * @param index the index of the element to remove
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range
     * ({@code index < 0 || index >= size()})
     */
    public E remove(int index) {
        if (index < 0 || index >= size())
            throw new IndexOutOfBoundsException();
        
        // Find the first node on each level where node.next[level].value >= value
        int maxLevel = head.level();
        SkipNode<E>[] pathNodes = new SkipNode[maxLevel+1];
        SkipNode<E> node = head;
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
        
        // Increment the mod count
        modCount++;
        
        return node.element;
    }

    /**
     * Removes the first occurrence of the specified element from this skip
     * list, if it is present. If the skip list does not contain the element, it
     * is unchanged. More formally, removes the element with the lowest index
     * {@code i} such that {@code (o==null ? get(i)==null : o.equals(get(i)))}
     * (if such an element exists). Returns {@code true} if this skip list
     * contained the specified element (or equivalently, if this skip list
     * changed as a result of the call).
     * <p>
     * Complexity: O(log(n))
     * 
     * @param o object to be removed from this skip list, if present
     * @return {@code true} if this skip list changed as a result of the call
     * @throws ClassCastException if the specified object cannot be compared
     * with the elements currently in the skip list
     * @throws NullPointerException if the specified element is {@code null} and
     * this set uses natural ordering, or its comparator does not permit
     * {@code null} elements
     */
    @Override
    public boolean remove(Object o) {
        E e = (E)o;
        int maxLevel = head.level();
        
        // Find first node on each level where node.next[level].value >= value
        SkipNode<E>[] pathNodes = new SkipNode[maxLevel+1];
        SkipNode<E> node = head;
        if (comparator != null) {
            for (int level = maxLevel; level >= 0; level--) {
                while (node.next[level] != null && comparator.compare(e, node.next[level].element) > 0)
                    node = node.next[level];
                pathNodes[level] = node;
            }
        } else {
            Comparable c = (Comparable)e;
            for (int level = maxLevel; level >= 0; level--) {
                while (node.next[level] != null && c.compareTo((Comparable)node.next[level].element) > 0)
                    node = node.next[level];
                pathNodes[level] = node;
            }
        }
        
        // Have we found the right element?
        node = node.next[0];
        if (node == null || !(e==null ? node.element==null : e.equals(node.element)))
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
        
        // Increment the mod count
        modCount++;
        
        // Done
        return true;
    }
    
    /**
     * Removes all of this skip list's elements that are also contained in the
     * specified collection. After this call returns, this skip list will
     * contain no elements in common with the specified collection.
     * <p>
     * Complexity: O(m*log(n)), where m is the size of the specified collection.
     *
     * @param collection the collection containing elements to be removed from
     * this skip list
     * @return {@code true} if this skip list changed as a result of the call
     * @throws ClassCastException if the elements provided cannot be compared
     * with the elements currently in the skip list
     * @throws NullPointerException if the specified collection is {@code null}
     * or if any element is {@code null} and this set uses natural ordering, or
     * its comparator does not permit {@code null} elements
     * @see #remove(java.lang.Object) 
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
     * Retains only the elements in this skip list that are contained in the
     * specified collection. In other words, removes from this skip list all of 
     * its elements that are not contained in the specified collection.
     * <p>
     * Complexity: O(n*p + n*log(n)), where p is the complexity of the specified
     * collection's {@code contains} method.
     * 
     * @param collection the collection containing elements to be retained in
     * this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the elements provided cannot be compared
     * with the elements currently in the skip list
     * @throws NullPointerException if the specified collection is {@code null}
     * or if any element is {@code null} and this set uses natural ordering, or
     * its comparator does not permit {@code null} elements
     * @see #remove(java.lang.Object)
     * @see #contains(java.lang.Object) 
     */
    @Override
    public boolean retainAll(Collection<?> collection) {
        if (collection.isEmpty() || isEmpty())
            return false;
        boolean hasChanged = false;
        SkipNode<E> node = head.next[0];
        while (node != null) {
            E value = node.element;
            node = node.next[0];
            if (collection.contains(value)) {
                remove(value);
                hasChanged = true;
            }
        }
        return hasChanged;
    }

    /**
     * Returns an iterator over the elements in this skip list in ascending 
     * order.
     * <p>
     * The returned {@code iterator} is <i>fail-fast</i>: if the skip list is
     * structurally modified at any time after the iterator is created, in any
     * way, the iterator will throw a {@code ConcurrentModificationException}.
     * Thus, in the face of concurrent modification, the iterator fails quickly
     * and cleanly, rather than risking arbitrary, non-deterministic behavior at
     * an undetermined time in the future.
     *
     * @return an iterator over the elements in this skip list in ascending 
     * order
     */
    @Override
    public Iterator<E> iterator() {
        return new SkipIterator();
    }
    
    /**
     * Compares the specified object with this skip list for equality. Returns
     * {@code true} if and only if the specified object is also a skip list,
     * both lists have the same size, and all corresponding pairs of elements in
     * the two lists are <i>equal</i>. (Two elements {@code e1} and {@code e2}
     * are <i>equal</i> if {(@code e1==null ? e2==null : e1.equals(e2))}). In 
     * other words, two skip lists are defined to be equal if they contain the 
     * same elements in the same order.
     * 
     * @param o the object to be compared for equality with this skip list
     * @return {@code true} if the specified object is equal to this skip list
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IndexedSkipList))
            return false;
        Iterator<E> e1 = iterator();
        Iterator e2 = ((IndexedSkipList)o).iterator();
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
     * Returns the hash code value for this skip list.
     * <p>
     * This implementation uses exactly the code that is used to define the list
     * hash function in the documentation for the
     * {@link java.util.List#hashCode()} method.
     * 
     * @return the hash code value for this skip list
     */
    @Override
    public int hashCode() {
        int hashCode = 1;
        for (E e : this)
            hashCode = 31*hashCode + (e==null ? 0 : e.hashCode());
        return hashCode;
    }
    
    /**
     * Returns an array containing all of the elements in this indexed skip list
     * in the order the elements appear in the list.
     * <p>
     * The returned array will be "safe" in that no references to it are
     * maintained by this collection. The caller is thus free to modify the
     * returned array.
     * <p>
     * This method acts as bridge between array-based and collection-based APIs.
     * 
     * @return an array containing all of the elements in this collection
     */
    @Override
    public Object[] toArray() {
        Object[] a = new Object[size];
        int index = 0;
        for (SkipNode node = head.next[0]; node != null; node = node.next[0])
            a[index++] = node.element;
        return a;
    }
    
    /**
     * Returns an array containing all of the elements in this indexed skip list
     * in the order the elements appear in the list. The runtime type of the
     * runtime type of the returned array is that of the specified array. If the
     * skip list fits in the specified array, it is returned therein. Otherwise,
     * a new array is allocated with the runtime type of the specified array and
     * the size of this skip list.
     * <p>
     * If this skip list fits in the specified array with room to spare (i.e.,
     * the array has more elements than this skip list), the element in the
     * array immediately following the last element from the skip list is set to
     * {@code null}.
     * <p>
     * Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs. Further, this method allows
     * precise control over the runtime type of the output array, and may, under
     * certain circumstances, be used to save allocation costs.
     *
     * @param <T> the runtime type of the array to contain the skip list
     * @param a the array into which the elements of this skip list are to be
     * stored, if it is big enough; otherwise, a new array of the same runtime
     * type is allocated for this purpose
     * @return an array containing all of the elements in this skip list
     * @throws NullPointerException if the specified array is {@code null}
     * @throws ArrayStoreException if the runtime type of the specified array is
     * not a supertype of the runtime type of every element in this skip list
     */
    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            a = (T[])Array.newInstance(a.getClass().getComponentType(), size);
        Object[] result = a;
        int index = 0;
        for (SkipNode node = head.next[0]; node != null; node = node.next[0])
            result[index++] = node.element;
        if (result.length > size)
            result[size] = null;
        return a;
    }
    
    /**
     * Returns a string representation of this skip list. The returned string
     * is equivalent to {@code Arrays.toString(toArray())}.
     * 
     * @return a string representation of this skip list
     */
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
            System.out.printf("<%2s>    ", node.element.toString());
        System.out.println();
        
        System.out.printf(" %2d     ",size);
        for (int index=0; index < size; index++)
            System.out.printf("[%2d]    ", index);
        System.out.println();
    }
    
    /**
     * Returns a random number in the range [{@code 0}, {@code maxLevel}]
     * distributed so that the probability of returning {@code 0} is %50 and 
     * the probability of {@code i} is twice the probability of returning
     * {@code i+1}.
     * <br><pre>{@code 
     * Let p(n) be the probability that randomLevel() will return n, then
     *   p(0) = 0.5, 
     *   p(1) = 0.25, 
     *   p(2) = 0.125,
     *   ...
     *   p(maxLevel) = 1 - sum(0 <= i < maxLevel, p(i))}</pre>
     * 
     * @return a random level index
     */
    protected int randomLevel() {
        int maxLevel = (int)(1+Math.log(size+1)/Math.log(2));
        int level = (int)(Math.log(1-random.nextDouble())/Math.log(0.5));
        return Math.min(maxLevel, level);
    }
    
    protected class SkipNode<E> {
        protected final E element;
        protected int[] width;
        protected SkipNode<E>[] next;
        
        protected SkipNode(E element, int level) {
            this.element = element;
            width = new int[level+1];
            next = new IndexedSkipList.SkipNode[level+1];
        }
        
        protected int level() {
            return next.length - 1;
        }
        
        @Override
        public String toString() {
            if (element == null)
                return "H";
            return element.toString();
        }
    }
    
    protected class SkipIterator implements Iterator<E> {
        protected final int expectedModCount = modCount;
        protected SkipNode<E> node = head.next[0];

        @Override
        public boolean hasNext() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return node != null;
        }

        @Override
        public E next() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            E e = node.element;
            node = node.next[0];
            return e;
        }
    }
}
