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

package fff.mv.core.project;

import fff.mv.core.PropertyChangeAdapter;
import fff.render.RendererSettings;
import java.io.Serializable;

/**
 * {@code ProjectRendererSettings} is a wrapper for a {@link RendererSettings}
 * object for use in a {@link Project}.
 */
public class ProjectRendererSettings extends PropertyChangeAdapter implements Serializable {
    /**
     * Identifies a change to the {@link #getSettings() settings}.
     */
    public static final String SETTINGS_CHANGED_PROPERTY = "settings";
    
    /**
     * The project containing this {@code ProjectRendererSettings}.
     */
    private final Project project;
    
    /**
     * The settings this {@code ProjectRendererSettings} is wrapping.
     */
    private final RendererSettings settings;
    
    /**
     * Constructs a new {@code ProjectFlameSettings} object for the specified 
     * {@link Project}.
     * 
     * @param project the {@code Project} containing this {@code PRojectFlameSettings}
     */
    protected ProjectRendererSettings(Project project) {
        this.project = project;
        settings = new RendererSettings();
    }
    
    /**
     * Returns the {@link Project} containing this {@code ProjectRendererSettings}.
     * 
     * @return the {@code Project} containing this {@code ProjectRendererSettings}
     * constructed
     */
    public Project getProject() {
        return project;
    }

    /**
     * Returns the {@link RendererSettings} object that this
     * {@code ProjectRendererSettings} is wrapping.
     *
     * @return {@code RendererSettings} object that this
     * {@code ProjectRendererSettings} is wrapping
     */
    public RendererSettings getSettings() {
        return settings;
    }
    
    /**
     * Signals that the {@link RendererSettings} object wrapped by this 
     * {@code ProjectRendererSettings} has been modified.
     * <p>
     * When this method is invoked the project's {@code isSaved} flag is set to
     * {@code false}, and a {@link #SETTINGS_CHANGED_PROPERTY} event is fired.
     */
    public void settingsChanged() {
        project.setIsSaved(false);
        firePropertyChange(SETTINGS_CHANGED_PROPERTY, null, settings);
    }
}
