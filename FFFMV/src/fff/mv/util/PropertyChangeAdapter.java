/**
 * FastFlameFractalMusicVideo (FFF)
 * An application for creating music videos using flame fractals.
 * Copyright (C) 2015  Jeremiah N. Hankins
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package fff.mv.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * This is a utility class that privately wraps a {@link PropertyChangeSupport} 
 * instance and publicly exposes some of its methods.
 * <p>
 * The {@link PropertyChangeSupport} object is marked as transient and uses lazy
 * initialization so that subclasses of {@code PropertyChangeAdapter} can
 * implement {@code Serializable} without serializing the list of
 * {@code PropertyChangeListener}.
 * 
 * @see java.beans.PropertyChangeSupport
 * @see java.beans.PropertyChangeListener
 * @see java.beans.PropertyChangeEvent
 * 
 * @author Jeremiah N. Hankins
 */
public abstract class PropertyChangeAdapter {
    
    /**
     * Property change implementation.
     */
    private transient PropertyChangeSupport pcs;
    
    /**
     * Returns the wrapped {@link PropertyChangeSupport} object.
     * 
     * @return the wrapped {@link PropertyChangeSupport} object
     */
    private PropertyChangeSupport pcs() {
        if (pcs == null)
            pcs = new PropertyChangeSupport(this);
        return pcs;
    }
    
    /**
     * Add a {@link java.beans.PropertyChangeListener PropertyChangeListener} to
     * the listener list. The listener is registered for all properties. The
     * same listener object may be added more than once, and will be called as
     * many times as it is added. If {@code listener} is {@code null}, no
     * exception is thrown and no action is taken.
     * 
     * @param listener the {@code PropertyChangelistener} to added
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs().addPropertyChangeListener(listener);
    }
    
    
    /**
     * Add a {@link java.beans.PropertyChangeListener PropertyChangeListener}
     * for a specific property. The listener will be invoked only when a call on
     * {@code firePropertyChange} names that specific property. The same
     * listener object may be added more than once. For each property, the
     * listener will be invoked the number of times it was added for that
     * property. If {@code propertyName} or {@code listener} is {@code null}, no
     * exception is thrown and no action is taken.
     * 
     * @param propteryName the name of the property to listener on
     * @param listener the {@code PropertyChangelistener} to be added
     */
    public void addPropertyChangeListener(
            String propteryName, 
            PropertyChangeListener listener) {
        pcs().addPropertyChangeListener(propteryName, listener);
        
    }

    /**
     * Remove a {@link java.beans.PropertyChangeListener PropertyChangeListener}
     * from the listener list. This removes a PropertyChangeListener that was
     * registered for all properties. If {@code listener} was added more than
     * once to the same event source, it will be notified one less time after
     * being removed. If {@code listener} is {@code null}, or was never added,
     * no exception is thrown and no action is taken.
     *
     * @param listener the {@code PropertyChangelistener} to be removed
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs().removePropertyChangeListener(listener);
    }
    
    /**
     * Remove a @link java.beans.PropertyChangeListener PropertyChangeListener}
     * for a specific property. If {@code listener} was added more than once to
     * the same event source for the specified property, it will be notified one
     * less time after being removed. If {@code propertyName} is {@code null},
     * no exception is thrown and no action is taken. If {@code listener} is
     * {@code null}, or was never added for the specified property, no exception
     * is thrown and no action is taken.
     *
     * @param propertyName  the name of the property that was listened on.
     * @param listener the  {@code PropertyChangelistener} to be removed
     */
    public void removePropertyChangeListener(
            String propertyName, 
            PropertyChangeListener listener) {
        pcs().removePropertyChangeListener(propertyName, listener);
    }
    
    /**
     * Report a bound property update to any registered listeners. No event is
     * fired if old and new are equal and both not {@code null}.
     * 
     * @param propertyName the programmatic name of the property that was changed
     * @param oldValue the old value of the property
     * @param newValue the new value of the property
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        pcs().firePropertyChange(propertyName, oldValue, oldValue);
    }
}
