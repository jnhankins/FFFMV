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

import com.jnhankins.jff.flame.Flame;
import com.jnhankins.jff.render.FlameRenderer;
import com.jnhankins.jff.render.RendererCallback;
import com.jnhankins.jff.render.RendererTask;

/**
 * {@code ProjectRendererTask} is an implementation of {@link RendererTask}
 * for rendering {@link Project} instances. 
 * <p>
 * A {@code ProjectRendererTask} generates a sequence of {@link Flame}
 * instances, which are retrieved via {@link #getNextFlame()}, that correspond
 * to individual frames and together form the animation for the {@code Proejct}
 * the {@code ProjectRendererTask} wraps. {@link Project#getFrameRate()} and
 * {@link Project#getLengthSec()} determine the number of frames per second of
 * animation and the total length of the animation respectively.
 * <p>
 * To generate {@code Flame} instances, {@code ProjectRendererTask} first 
 * determines the animation time for the next flame, retrieves the base flame
 * for the current time from the project's {@link KeyFlameList}, then applies
 * the effects to the base flame for that time, before returning the final
 * flame. For performance reasons, the same {@code Flame} object is used to 
 * return the results each time {@code hasNextFlame()} is invoked.
 * <p>
 * If a {@code startFrameIndex} is not specified during construction, then the
 * default {@code startFrameIndex} will be {@code 0}. The frame at index 
 * {@code 0} corresponds to the first frame at time 
 * {@link Project#getStartSec()}.
 * 
 * @author Jeremiah N. Hankins
 */
public class ProjectRendererTask extends RendererTask {
    private final KeyFlameList keyFlameList;
    private final AudioEffectList audioEffectList;
    private final double startSec;
    private final double lengthSec;
    private final double frameRate;
    private final int frameCount;
    private int frameIndex;
    private Flame flame;
    
    /**
     * Constructs a new {@code ProjectRendererTask} using the specified 
     * {@link Project} and {@link RendererCallback} function.
     * 
     * @param project the project to render
     * @param callback the callback function used by the {@link FlameRenderer}
     */
    public ProjectRendererTask(Project project, RendererCallback callback) {
        super(callback, project.getRendererSettings().getSettings());
        keyFlameList = project.getKeyFlameList();
        audioEffectList = project.getAudioEffectList();
        startSec = project.getStartTime();
        lengthSec = project.getDuration();
        frameRate = project.getFrameRate();
        frameCount = Math.max((int)Math.ceil(lengthSec*frameRate), 1);
        frameIndex = 0;
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
        // Get the base flame from the key-flame list
        flame = keyFlameList.getFlame(time, new Flame());
        // Apply the audio-effects 
        audioEffectList.applyEffects(time, flame);
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
