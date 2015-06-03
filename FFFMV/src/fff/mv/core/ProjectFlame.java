/**
 * FFFMV - An application for creating music videos using flame fractals.
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

package fff.mv.core;

import fff.flame.Flame;
import java.io.Serializable;

/**
 * {@code ProjectFlame} is a wrapper for a {@link Flame} object for use in a
 * {@link Project}.
 * <p>
 * Each {@code ProjectFlame} has a mutable {@code String} name which is
 * guaranteed to be unique within the {@code ProjectFlameSet} that contains it.
 * This name is intended to be human readable so that users can easily reference
 * the the wrapped {@code Flame} object.
 * <p>
 * This class also provides methods that allow multiple views of the underlying
 * {@code Flame} to remain synchronized as the {@code Flame} is modified.
 * See {@link #flameChanged()} for more information.
 * 
 * @author Jeremiah N. Hankins
 */
public class ProjectFlame extends PropertyChangeAdapter implements Serializable {
    /**
     * Identifies a change to the {@link #getName() name}.
     */
    public static final String NAME_CHANGED_PROPERTY = "name";
    
    /**
     * Identifies a modification within the {@link #getFlame() flame}.
     */
    public static final String FLAME_CHANGED_PROPERTY = "flame";
    
    /**
     * The project containing this {@code ProjectFlame}.
     */
    private final Project project;
    
    /**
     * The flame this {@code ProjectFlame} is wrapping.
     */
    private final Flame flame;
    
    /**
     * The name associated with this {@code ProjectFlame}.
     */
    private String name;
    
    /**
     * Constructs a new {@code ProejctFlame} using the specified 
     * {@link Project}, {@link Flame}, and {@code name}.
     * <p>
     * This method performs no checks. Illegal argument and state exception
     * checking and other logic code required to instantiate a
     * {@code ProjectFlame} should be located in
     * {@link ProjectFlameSet#add(fff.flame.Flame, java.lang.String) ProjectFlameSet.add()}.
     *
     * @param project the {@code Project} containing this {@code ProjectFlame}
     * @param flame the {@code Flame} the {@code ProejctFlame} is wrapping
     * @param name the name of the {@code ProjectFlame}
     */
    protected ProjectFlame(Project project, Flame flame, String name) {
        this.project = project;
        this.flame = flame;
        this.name = name;
    }
    
    /**
     * Returns the {@link Project} for which this {@code ProjectFlame} was
     * constructed.
     * 
     * @return the {@code Project} for which this {@code ProjectFlame} was
     * constructed
     */
    public Project getProject() {
        return project;
    }
    
    /**
     * Returns the {@link Flame} object that this {@code ProjectFlame} is
     * wrapping.
     * 
     * @return {@code Flame} object that this {@code ProjectFlame} is wrapping
     */
    public Flame getFlame() {
        return flame;
    }
    
    /**
     * Signals that the {@link Flame} object wrapped by this 
     * {@code ProjectFlame} has been modified.
     * <p>
     * When this method is invoked the project's {@code isSaved} flag is set to
     * {@code false}, and a {@link #FLAME_CHANGED_PROPERTY} event is fired.
     * <p>
     * Usage: It is likely that an application using {@code ProjectFlame} will
     * have multiple methods for viewing and modifying the state of the flame.
     * For example, an application might display the flame's genome in a
     * property sheet and the flame's image in an interactive GUI. To maintain
     * synchronization across the views, each view should listen for this
     * property event and update their display when it occurs. Conversely, each
     * view should invoke this method should invoke this method to trigger this
     * even when it modifies the flame so that the other views know that they 
     * may need to be updated.
     */
    public void flameChanged() {
        project.setIsSaved(false);
        firePropertyChange(FLAME_CHANGED_PROPERTY, null, flame);
    }
    
    /**
     * Returns the name of this {@code ProjectFlame} which uniquely identifies
     * this {@code ProjectFlame} within its {@code Project}. This name is
     * shown to the user.
     * 
     * @return the name of this {@code ProjectFlame}
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name of the {@code ProjectFlame}. If the specified name is equal
     * to the name of another {@code ProjectFlame} within this {@code Project},
     * then the name of this {@code ProjectFlame} will not change. Returns
     * {@code true} if at the end of this method call, the name of the
     * {@code ProjectFlame} is equal to the specified name, otherwise
     * {@code flase}.
     * <p>
     * If the name of this {@code ProjectFlame} is changed as a result of this
     * method call, then the project's {@code isSaved} flag will be set to
     * {@code false}, and a {@link #NAME_CHANGED_PROPERTY} property event will
     * be fired.
     * 
     * @param name the new name
     * @return {@code true} if at the end of this method call, the name of the
     * {@code ProjectFlame} is equal to the specified name, otherwise
     * {@code flase}
     * @throws IllegalArgumentException if {@code name} is {@code null} or empty
     */
    public boolean setName(String name) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name is null or empty");
        String oldName = this.name;
        String newName = name;
        // If the new name is the same as the old name, do nothing
        if (newName.equals(oldName))
            return true;
        // If any of the flames in the same project as this flame have the same
        // name, do nothing
        if (project.getProjectFlameSet().contains(name))
            return false;
        // Change the name
        this.name = name;
        project.setIsSaved(false);
        firePropertyChange(NAME_CHANGED_PROPERTY, oldName, newName);
        return true;
    }
}
