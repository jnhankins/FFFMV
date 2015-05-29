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
import fff.render.FlameRendererSettings;
import java.io.Serializable;

/**
 * {@code ProjectFlameSettings} is a wrapper for a {@link FlameRendererSettings}
 * object for use in a {@link Project}.
 * <p>
 */
public class ProjectFlameSettings extends PropertyChangeAdapter implements Serializable {
    /**
     * Identifies a change to the {@link #getSettings() settings}.
     */
    public static final String SETTINGS_CHANGED_PROPERTY = "settings";
    
    /**
     * The project containing this {@code ProjectFlameSettings}.
     */
    private final Project project;
    
    /**
     * The settings this {@code ProjectFlameSettings} is wrapping.
     */
    private final FlameRendererSettings settings;
    
    /**
     * Constructs a new {@code ProjectFlameSettings} object for the specified 
     * {@link Project}.
     * 
     * @param project the {@code Project} containing this {@code PRojectFlameSettings}
     */
    protected ProjectFlameSettings(Project project) {
        this.project = project;
        settings = new FlameRendererSettings();
    }
    
    /**
     * Returns the {@link Project} containing this {@code ProjectFlameSettings}.
     * 
     * @return the {@code Project} containing this {@code ProjectFlameSettings}
     * constructed
     */
    public Project getProject() {
        return project;
    }

    /**
     * Returns the {@link FlameRendererSettings} object that this
     * {@code ProjectFlameSettings} is wrapping.
     *
     * @return {@code FlameRendererSettings} object that this
     * {@code ProjectFlameSettings} is wrapping
     */
    public FlameRendererSettings getSettings() {
        return settings;
    }
    
    /**
     * Signals that the {@link FlameRendererSettings} object wrapped by this 
     * {@code ProjectFlameSettings} has been modified.
     * <p>
     * When this method is invoked the project's {@code isSaved} flag is set to
     * {@code false}, and a {@link #SETTINGS_CHANGED_PROPERTY} event is fired.
     */
    public void settingsChanged() {
        project.setSaved(false);
        firePropertyChange(SETTINGS_CHANGED_PROPERTY, null, settings);
    }
}
