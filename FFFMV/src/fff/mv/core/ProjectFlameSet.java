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

package fff.mv.core;

import fff.flame.Flame;
import fff.mv.util.PropertyChangeAdapter;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * A collection of {@link ProjectFlame} objects contained by a {@link Project}.
 * <p>
 * Each {@code ProjectFlame} must have a unique name within the project. And,
 * though no checks are made to prevent it, a single {@link Flame} object should
 * be contained in at most one {@code ProjetFlame}.
 * 
 * @author Jeremiah N. Hankins
 */
public class ProjectFlameSet extends PropertyChangeAdapter implements Serializable {
    /**
     * Indicates that a new {@link ProjectFlame} has been {@link #add(fff.flame.Flame, java.lang.String) added}.
     */
    public static final String FLAME_ADDED_PROPERTY = "flameAdded";
    
    /**
     * Indicates the a {@link ProjectFlame} has been {@link #removeFlame(fff.mv.core.ProjectFlame) removed}.
     */
    public static final String FLAME_REMOVED_PROPERTY = "flameRemoved";
    
    /**
     * The {@code Project} containing this {@code ProjectFlameSet}.
     */
    private final Project project;
    
    /**
     * The list of {@code ProjectFlame} objects. 
     */
    private final ArrayList<ProjectFlame> flameList;
    
    /**
     * Constructs a new {@code ProjectFlameSet} object.
     * 
     * @param project the {@code Project} containing this {@code ProjectFlameSet}
     */
    protected ProjectFlameSet(Project project) {
        this.project = project;
        flameList = new ArrayList();
    }
    
    /**
     * Returns the {@link Project} containing this {@code projectFlameSet}.
     * 
     * @return the {@link Project} containing this {@code projectFlameSet}.
     */
    public Project getProject() {
        return project;
    }
    
    /**
     * Returns the number of {@link ProjectFlames} in the
     * {@code ProjectFlameSet}.
     *
     * @return the number of {@link ProjectFlames} in the
     * {@code ProjectFlameSet}
     */
    public int size() {
        return flameList.size();
    }
    
    /**
     * Returns {@code true} if there are no {@link ProjectFlames} in the
     * {@code ProjectFlameSet}.
     *
     * @return {@code true} if there are no {@link ProjectFlames} in the
     * {@code ProjectFlameSet}
     */
    public boolean isEmpty() {
        return flameList.isEmpty();
    }
    
    /**
     * Returns an array containing the {@link ProjectFlame} objects contained in
     * this {@code ProjectFlameSet}. Changes to the returned array are not
     * backed by the {@code ProjectFlameSet}.
     * 
     * @return an array containing the {@code ProjectFlame} objects contained in
     * this {@code ProjectFlameSet}.
     */
    public ProjectFlame[] toArray() {
        return flameList.toArray(new ProjectFlame[0]);
    }
    
    /**
     * Returns {@code true} if the {@code ProjectFlameSet} contains a 
     * {@link ProjectFlame} with the specified {@code name}.
     * 
     * @param name the name whose presence is to be tested
     * @return {@code true} if the {@code ProjectFlameSet} contains a 
     * {@link ProjectFlame} with the specified {@code name}
     */
    public boolean contains(String name) {
        return get(name) != null;
    }
    
    /**
     * Returns the {@link ProjectFlame} associated with the specified 
     * {@code name} or {@code null} if no {@code ProjectFlame} in this
     * {@code ProjectFlameSet} is associated with the name.
     * 
     * @param name the name of the {@code ProjectFlame} to retrieve
     * @return the {@link ProjectFlame} associated with the specified
     * {@code name} or {@code null}
     */
    public ProjectFlame get(String name) {
        for (ProjectFlame pflame : flameList)
            if (pflame.getName().equals(name))
                return pflame;
        return null;
    }
    
    /**
     * Constructs a new {@link ProjectFlame} using the specified {@link Flame}
     * and {@code name}, adds it to the {@code ProjectFlameSet}, and returns
     * the new {@code ProjectFlame}. If the specified is equal to the name
     * of another {@code ProejctFlame} already contained in the 
     * {@code ProjectFlameSet}, then this method has no effect, and returns
     * {@code null}.
     * <p>
     * If the {@code ProjectFlame} is added successfully, then the project's 
     * {@code isSaved} flag will be set to {@code false}, and a 
     * {@link #FLAME_ADDED_PROPERTY} property event will be fired.
     * 
     * @param flame the {@code Flame} backing the new {@code ProjectFlame}
     * @param name the name of the new {@code ProjectFlame}
     * @return the new {@code ProjectFlame} or {@code null}
     */
    public ProjectFlame add(Flame flame, String name) {
        if (flame == null)
            throw new IllegalArgumentException("flame is null");
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name is null or empty");
        if (contains(name))
            return null;
        ProjectFlame pflame = new ProjectFlame(project, flame, name);
        flameList.add(pflame);
        project.setSaved(false);
        firePropertyChange(FLAME_ADDED_PROPERTY, null, pflame);
        return pflame;
    }
    
    /**
     * Removes the specified {@link ProjectFlame} from this
     * {@code ProjectFlameSet} and removes any {@link KeyFlameList.Entry}
     * instances in the project which reference the {@code ProjectFlame}.
     * <p>
     * If the {@code ProjectFlame} is removed successfully, then the project's
     * {@code isSaved} flag will be set to {@code false}, and a 
     * {@link #FLAME_REMOVED_PROPERTY} property event will be fired.
     * 
     * @param pflame the {@code ProjectFlame} to remove
     * @return {@code true} if the {@link ProjectFlame} was removed successfully
     */
    public boolean removeFlame(ProjectFlame pflame) {
        if (flameList.remove(pflame)) {
            project.getKeyFlameList().remove(pflame);
            project.setSaved(false);
            firePropertyChange(FLAME_REMOVED_PROPERTY, pflame, null);
            return true;
        }
        return false;
    }
}
