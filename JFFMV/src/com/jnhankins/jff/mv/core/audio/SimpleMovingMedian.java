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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * {@code SimpleMovingMedian} provides a mechanism for efficiently finding
 * simple moving median (aka rolling median or running median) of a time series
 * of comparable elements. In certain circumstances, a simple moving median may
 * sometimes be preferable over a simple moving average when the time series
 * experiences rare events such as rapid shocks or other anomalies.
 * 
 * @author Jeremiah N. Hankins
 * @param <E> the the type of {@code Comparable} object contained in this list
 */
public class SimpleMovingMedian<E extends Comparable<? super E>> {
    
    /**
     * The elements in first in first out order.
     */
    private final Queue<E> queue = new LinkedList();
    
    /**
     * The elements in ascending sorted order.
     */
    private final IndexedSkipList<E> skiplist = new IndexedSkipList();
    
    /**
     * The maximum number of elements to keep before discarding beings.
     */
    private final int maxSize;
    
    /**
     * If {@code true} the returned median for an odd number of elements will be
     * the element at index {@code ceil(numSamples/2)} otherwise it will be the
     * element at index {@code floor(numSamples/2)}.
     */
    private boolean useHighMedian;
    
    /**
     * Constructs a new {@code SimpleMovingMean} using the specified maximum
     * size.
     * 
     * @param maxSize the maximum number of elements in the list
     * @param useHighMedian if {@code true} the higher median should be used
     * @throws IllegalArgumentException if {@code maxSize} is not positive
     */
    public SimpleMovingMedian(int maxSize, boolean useHighMedian) {
        if (maxSize < 1)
            throw new IllegalArgumentException("maxSize must be postive: "+maxSize);
        this.maxSize = maxSize;
        this.useHighMedian = useHighMedian;
    }
    
    /**
     * Returns the maximum number of elements that will be allowed in the list
     * before old elements are discarded as new elements are added.
     * 
     * @return the maximum number of elements
     */
    public int getMaxSize() {
        return maxSize;
    }
    
    /**
     * Returns the maximum number of elements that will be allowed in the list
     * before old elements are discarded as new elements are added. If the new
     * maximum size is less than the old maximum, then the oldest entries added
     * will be discarded until the size of the list is equal to the maximum
     * specified size. Returns a {@code List} of the discarded elements in the
     * order in which they were originally added, or {@code null} if no elements
     * were discarded.
     * 
     * @param maxSize the maximum number of elements
     * @return a list of the discarded elements, or {@code null}
     * @throws IllegalArgumentException if {@code maxSize} is not positive
     */
    public List<E> setMaxSize(int maxSize) {
        if (maxSize < 1)
            throw new IllegalArgumentException("maxSize must be postive: "+maxSize);
        if (queue.size() < maxSize)
            return null;
        ArrayList<E> discardList = new ArrayList(queue.size()-maxSize);
        while (queue.size() > maxSize) {
            E e = queue.poll();
            skiplist.remove(e);
            discardList.add(e);
        }
        return discardList;
    }
    
    /**
     * Returns the {@code useHighMedian} flag.
     * 
     * @return the {@code useHighMedian} flag
     * @see #setUseHighMedian(boolean) 
     */
    public boolean getUseHighMedian() {
        return useHighMedian;
    }
    
    /**
     * Set the {@code useHighMedian} flag.
     * 
     * @param useHighMedian if {@code true} the returned median for an odd number of
     * elements will be the element at index {@code ceil(numSamples/2)}
     * otherwise it will be the element at index {@code floor(numSamples/2)}
     */
    public void setUseHighMedian(boolean useHighMedian) {
        this.useHighMedian = useHighMedian;
    }
    
    /**
     * Returns the number of elements currently in the list.
     * 
     * @return the number of elements currently in the list
     */
    public int getSize() {
        return queue.size();
    }
    
    /**
     * Appends the elements to the end of the list of elements. If the number of
     * elements in the list is greater than the maximum size, then the elements
     * from the front of the list is removed.
     * 
     * @param e the elements to add
     * @return the element that was removed, or {@code null} if no elements were
     * removed
     */
    public E addSample(E e) {
        queue.add(e);
        skiplist.add(e);
        if (queue.size() > maxSize) {
            e = queue.poll();
            skiplist.remove(e);
            return e;
        }
        return null;
    }
    
    /**
     * Clears all of the samples from the list.
     */
    public void clear() {
        queue.clear();
        skiplist.clear();
    }
    
    /**
     * Returns the median element in the list.
     * 
     * @return the median element
     */
    public E getMedian() {
        int index = getMeanIndex();
        return skiplist.get(index);
    }
    
    /**
     * Returns the index of the median element in the list.
     * <p>
     * If the number of elements is odd and {@code useHighMean()} is 
     * {@code true}, then the returned value is {@code (size()+1)/2}, otherwise
     * the returned value is {@code size()/2}.
     * 
     * @return the index of the median element in the list
     */
    public int getMeanIndex() {
        int size = queue.size();
        if (size % 2 == 1 && useHighMedian)
            return (size+1)/2;
        return size/2;
    }
}
