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

import fff.mv.util.PropertyChangeAdapter;
import java.io.File;
import java.io.Serializable;

/**
 *
 * @author Jeremiah N. Hankins
 */
public class Project extends PropertyChangeAdapter implements Serializable {
    /**
     * Identifies a change to the {@code isSaved} flag.
     */
    public static final String SAVED_STATE_CHANGED_PROPERTY = "isSaved";
    
    /**
     * Identifies a change to the file associated with the project.
     */
    public static final String FILE_CHANGED_PROPERTY = "file";
    
    /**
     * Identifies a change to the name of the project.
     */
    public static final String NAME_CHANGED_PROPERTY = "name";
    
    /**
     * {@code true} if the {@code Project} and all of its subcomponents have not
     * been modified since the last time the {@code Project} was successfully
     * read or written to its {@link #file}.
     */
    private boolean isSaved;
    
    /**
     * The file backing the project.
     */
    private File file;
    
    /**
     * The name of the project.
     */
    private String name;
    
    /**
     * The set of flames contained in the project.
     */
    private final ProjectFlameSet flameSet;
    
    /**
     * The key-flame list.
     */
    private final KeyFlameList keyFlameList;
    
    
    /**
     * Constructs a new empty FFFMV {@code Project}.
     */
    public Project() {
        // Not saved yet
        isSaved = false;
        // No Backing file
        file = null;
        // Default name
        name = "New Project";
        // Empty project flame set
        flameSet = new ProjectFlameSet(this);
        // Empty key-flame list
        keyFlameList = new KeyFlameList(this);
    }
    
    /**
     * Returns the value of the {@code isSaved} flag.
     * <p>
     * Returns {@code true} if the project has not been modified since the last
     * time the project was successfully last written to or read from its
     * {@link #getFile() file}. Returns {@code false} if the project has been
     * modified and has not been saved since the most recent modification. Also
     * returns {@code false} if the project is not associated with a file yet,
     * i.e. {@link #getFile()} returns {@code null}.
     * <p>
     * Note: The file backing this project is not monitored for changes. If the
     * project is saved to a file, then that file is deleted, this method will
     * not detect the deletion and continue to return true until the project
     * is modified programmatically.
     * 
     * @return {@code true} if the project has not been modified since the last
     * time the project was successfully last written to or read from its file
     */
    public boolean isSaved() {
        return isSaved;
    }
    
    /**
     * Sets the value of the {@code isSaved} flag.
     * <p>
     * If the {@code isSaved} flag is modified as a result of calling this 
     * method a {@link #SAVED_STATE_CHANGED_PROPERTY} property event will be 
     * fired.
     * 
     * @param isSaved the new value of the {@code isSaved} flag
     */
    protected void setSaved(boolean isSaved) {
        boolean oldIsSaved = this.isSaved;
        boolean newIsSaved = isSaved;
        if (oldIsSaved != newIsSaved) {
            this.isSaved = isSaved;
            firePropertyChange(SAVED_STATE_CHANGED_PROPERTY, oldIsSaved, newIsSaved);
        }
    }
    
    /**
     * Returns the {@link File} backing the project or {@code null} if the 
     * project has not yet been associated with a file.
     * 
     * @return the {@code File} backing the project or {@code null} if the
     * project has not yet been associated with a file
     */
    public File getFile() {
        return file;
    }
    
    /**
     * Sets the {@link File} associated with the project.
     * <p>
     * If the file is changed successfully, then the {@code isSaved} flag will
     * be set to {@code false}, and a {@link #FILE_CHANGED_PROPERTY} property
     * event will be fired. If the specified file is equal to the current file,
     * then this method has no effect.
     * 
     * @param file the {@code File} to associate with the project
     * @throws IllegalArgumentException if {@code file} is {@code null}
     */
    public void setFile(File file) {
        if (file == null)
            throw new IllegalArgumentException("file is null");
        File oldFile = this.file;
        File newFile = file;
        if (newFile.equals(oldFile)) {
            this.file = file;
            setSaved(false);
            firePropertyChange(FILE_CHANGED_PROPERTY, oldFile, newFile);
        }
    }
    
    /**
     * Returns the name of the project.
     * 
     * @return the name of the project
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name of the project.
     * <p>
     * If the name is changed successfully, then the {@code isSaved} flag will
     * be set to {@code false}, and a {@link #NAME_CHANGED_PROPERTY} property
     * event will be fired. If the specified name is equal to the current name,
     * then this method has no effect.
     * 
     * @param name the new name of the project
     * @throws IllegalArgumentException if {@code name} is {@code null} or empty
     */
    public void setName(String name) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name is null or empty");
        String oldName = this.name;
        String newName = name;
        if (!newName.equals(oldName)) {
            this.name = name;
            setSaved(false);
            firePropertyChange(NAME_CHANGED_PROPERTY, oldName, newName);
        }
    }
    
    /**
     * Returns the project's {@link ProjectFlameSet}.
     * 
     * @return the project's {@link ProjectFlameSet}
     */
    public ProjectFlameSet getProjectFlameSet() {
        return flameSet;
    }
    
    /**
     * Returns the project's {@link KeyFlameList}.
     * 
     * @return the project's {@link KeyFlameList}
     */
    public KeyFlameList getKeyFlameList() {
        return keyFlameList;
    }
}
