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

package com.jnhankins.jff.mv.core.project;

import com.jnhankins.jff.mv.core.PropertyChangeAdapter;
import com.jnhankins.jff.render.RendererSettings;
import java.io.File;
import java.io.Serializable;

/**
 * {@code Proejct} encapsulates parameters used by JFFMV to create a music 
 * video using flame fractals.
 * <p>
 * It has a human-readable {@code String} name to help the user identify the
 * project. The default name of a newly constructed {@code Project} is "New
 * Project."
 * <p>
 * It can be associated with a {@code File} for reading and writing to disk, and
 * {@code Project} maintains a {@code isSaved} boolean flag to keep track
 * whether or not the project has been modified in some way since the last time
 * the project was saved.
 * <p>
 * {@code Project} contains a {@link ProjectFlameSet} which contains
 * {@link ProjectFlame} objects that wrap {@link com.jnhankins.jff.flame.Flame}
 * instances. {@code Project} also contains a {@link KeyFlameList} which
 * provides methods for generating smooth animations.
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
     * Identifies a change to the frame rate.
     */
    public static final String FRAME_RATE_CHANGED_PROPERTY = "frameRate";
    
    /**
     * Identifies a change to the start time offset.
     */
    public static final String START_SEC_CHANGED_PROPERTY = "startSec";
    
    /**
     * Identifies a change to the length of the animation.
     */
    public static final String LENGTH_SEC_CHANGED_PROPERTY = "lengthSec";
    
    /**
     * {@code true} if the {@code Project} and all of its subcomponents have not
     * been modified since the last time the {@code Project} was successfully
     * read or written to its {@link #file}.
     */
    private boolean isSaved;
    
    /**
     * File backing the project.
     */
    private File file;
    
    /**
     * Name of the project.
     * Cannot be null or empty.
     */
    private String name;
    
    /**
     * Set of flames contained in the project.
     */
    private final ProjectFlameSet flameSet;
    
    /**
     * Key-flame list.
     */
    private final KeyFlameList keyFlameList;
    
    /**
     * Image settings used by a {@code FlameRenderer}.
     */
    private final ProjectRendererSettings rendererSettings;
    
    /**
     * The time scale units.
     * <p>
     * The frame rate is {@code timeScale/timeStep}.
     */
    private int timeScale;
    
    /**
     * The number of time time scale units per frame.
     * <p>
     * The frame rate is {@code timeScale/timeStep}.
     */
    private int timeStep;
    
    /**
     * Animation starting point offset time in seconds.
     */
    private double startSec;
    
    /**
     * Length of animation in seconds.
     * Must be positive.
     */
    private double lengthSec;
    
    /**
     * Constructs a new empty JFFMV {@code Project}.
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
        // Default image settings
        rendererSettings = new ProjectRendererSettings(this);
        // 25 fps
        timeStep = 1;
        timeScale = 25;
        // No starting time offset
        startSec = 0;
        // 60 seconds of animation (boring blank animation)
        lengthSec = 60;
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
    protected void setIsSaved(boolean isSaved) {
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
            setIsSaved(false);
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
            setIsSaved(false);
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
    
    /**
     * Returns the project's {@link ProjectRendererSettings} which wraps the
     * {@link RendererSettings}.
     * 
     * @return the project's {@link ProjectRendererSettings}
     */
    public ProjectRendererSettings getRendererSettings() {
        return rendererSettings;
    }
    
    /**
     * Returns the number of time scale units per frame.
     * 
     * @return the number of time scale units per frame
     * @see #setFrameRate(int, int) 
     */
    public int getTimeStep() {
        return timeStep;
    }
    
    /**
     * Returns the time scale units in fractions of a second.
     * 
     * @return the time scale units in fractions of a second
     * @see #setFrameRate(int, int) 
     */
    public int getTimeScale() {
        return timeScale;
    }
    
    /**
     * Returns the frame rate (frames per second).
     * <br>
     * Equivalent to: <pre>{@code getTimeScale()/(double)getTimeStep()}</pre>
     * 
     * @return the frame rate (frames per second)
     * @see #setFrameRate(int, int) 
     */
    public double getFrameRate() {
        return timeScale/(double)timeStep;
    }
    
    /**
     * Sets the frame rate (frames per second) through the parameters
     * {@code timeStep} and {@code timeScale} which determine the frame rate
     * by the formula {@code timeStep/timeScale}.
     * <p>
     * If the frame rate is changed successfully, then the {@code isSaved} flag
     * will be set to {@code false}, and a {@link #FRAME_RATE_CHANGED_PROPERTY}
     * property event will be fired. If the specified time step and scale are 
     * equal to the current time step and scale, then this method has no effect.
     * 
     * @param timeStep the number of time scale units between frames
     * @param timeScale time scale units in fractions of a second
     * @throws IllegalArgumentException {@code timeStep} is not in range [1,inf)
     * @throws IllegalArgumentException {@code timeScale} is not in range [1,inf)
     */
    public void setFrameRate(int timeStep, int timeScale) {
        if (timeStep < 1)
            throw new IllegalArgumentException("timeStep is not in range [1,inf): "+timeStep);
        if (timeScale < 1)
            throw new IllegalArgumentException("timeScale is not in range [1,inf): "+timeScale);
        if (timeStep != this.timeStep || timeScale != this.timeScale) {
            double oldFrameRate = getFrameRate();
            this.timeStep = timeStep;
            this.timeScale = timeScale;
            double newFrameRate = getFrameRate();
            setIsSaved(false);
            firePropertyChange(FRAME_RATE_CHANGED_PROPERTY, oldFrameRate, newFrameRate);
            
        }
    }
    
    /**
     * Returns the offset for the beginning of the animation in seconds.
     * 
     * @return the offset for the beginning of the animation in seconds
     */
    public double getStartSec() {
        return startSec;
    }
    
    /**
     * Sets the offset for the beginning of the animation in seconds.
     * <p>
     * If the start offset is changed successfully, then the {@code isSaved}
     * flag will be set to {@code false}, and a
     * {@link #LENGTH_SEC_CHANGED_PROPERTY} property event will be fired. If the
     * specified start time is equal to the current start time, then this method
     * has no effect.
     * 
     * @param startSec the offset for the beginning of the animation in seconds
     * @throws IllegalArgumentException if {@code startSec} is not in range (-inf, inf)
     */
    public void setStartSec(double startSec) {
        if (!(0<=startSec && startSec<Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("startSec is not in range (-inf, inf): "+startSec);
        double oldStartSec = this.startSec;
        double newStartSec = startSec;
        if (oldStartSec != newStartSec) {
            this.startSec = startSec;
            setIsSaved(false);
            firePropertyChange(START_SEC_CHANGED_PROPERTY, oldStartSec, newStartSec);
        }
    }
    
    /**
     * Returns the length of the animation in seconds.
     * 
     * @return the length of the animation in seconds
     */
    public double getLengthSec() {
        return lengthSec;
    }
    
    /**
     * Sets the length of the animation in seconds.
     * <p>
     * If the length is changed successfully, then the {@code isSaved} flag will
     * be set to {@code false}, and a {@link #LENGTH_SEC_CHANGED_PROPERTY}
     * property event will be fired. If the specified length is equal to the
     * current length, then this method has no effect.
     * 
     * @param lengthSec the length of the animation in seconds.
     * @throws IllegalArgumentException if {@code lengthSec} is not in range (0,inf)
     */
    public void setLengthSec(double lengthSec) {
        if (!(0<=lengthSec && lengthSec<Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("lengthSec is not in range [0,inf): "+lengthSec);
        double oldLengthSec = this.lengthSec;
        double newLengthSec = lengthSec;
        if (oldLengthSec != newLengthSec) {
            this.lengthSec = lengthSec;
            setIsSaved(false);
            firePropertyChange(LENGTH_SEC_CHANGED_PROPERTY, oldLengthSec, newLengthSec);
        }
    }
}
