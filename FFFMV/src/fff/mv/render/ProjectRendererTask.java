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

package fff.mv.render;

import fff.flame.Flame;
import fff.mv.core.KeyFlameList;
import fff.mv.core.Project;
import fff.render.FlameRendererCallback;
import fff.render.FlameRendererTask;

/**
 *
 * @author Jeremiah N. Hankins
 */
public class ProjectRendererTask extends FlameRendererTask {
    private final Project project;
    private final KeyFlameList keyFlameList;
    private final double startSec;
    private final double lengthSec;
    private final double frameRate;
    private final int frameCount;
    private int frameIndex;
    private Flame flame;
    
    
    public ProjectRendererTask(FlameRendererCallback callback, Project project) {
        this(callback, project, 0);
    }
        
    public ProjectRendererTask(
            FlameRendererCallback callback, 
            Project project,
            int startFrameIndex)
    {
        super(callback, project.getRendererSettings().getSettings());
        this.project = project;
        keyFlameList = project.getKeyFlameList();
        startSec = project.getStartSec();
        lengthSec = project.getLengthSec();
        frameRate = project.getFrameRate();
        frameCount = Math.max((int)Math.ceil(lengthSec/frameRate), 1);
        frameIndex = startFrameIndex;
    }

    @Override
    public boolean hasNextFlame() {
        return frameIndex <= frameCount;
        
    }
    
    @Override
    public Flame getNextFlame() {
        // Get the time for the frame
        double time = getNextFrameTime();
        // Increment the frame index
        frameIndex++;
        // Get the base flame from the keyflame list
        flame = keyFlameList.getFlame(time, flame);
        // TODO: Apply effectsS
        // Return the flame
        return flame;
    }
    
    /**
     * Returns the total number of frames needed to animate the project.
     * 
     * @return the total number of frames needed to animate the project
     */
    public int getFrameCount() {
        return frameCount;
    }
    
    /**
     * Returns the frame number for the flame that was was most recently 
     * returned by {@link #getNextFlame()} or {@code startFrameIndex - 1} if
     * {@code getNextFlame()} has not yet been called.
     * 
     * @return the frame number for the previous frame
     */
    public int getPrevFrameIndex() {
        return frameIndex - 1;
    }
    
    /**
     * Returns the time for the next frame that will be returned by
     * {@link #getNextFlame()} or {@code startSec - 1/frameRate} if
     * {@code getNextFlame()} has not yet been called.
     * 
     * @return the time for the next frame
     */
    public double getPrevFrameTime() {
        return startSec+(frameIndex-1)/frameRate;
    }
    
    /**
     * Returns the frame number for the next flame that will be returned by
     * {@link #getNextFlame()} or the total number of flames if the task does
     * not have a next flame. The frame number of the first flame is {@code 0}.
     * 
     * @return the frame number for the next frame
     */
    public int getNextFrameIndex() {
        return frameIndex;
    }
    
    /**
     * Returns the time for the next frame that will be returned by
     * {@link #getNextFlame()} or the total length of the animated sequence if
     * the task does not have a next flame.
     * 
     * @return the time for the next frame
     */
    public double getNextFrameTime() {
        return startSec+frameIndex/frameRate;
    }
}
