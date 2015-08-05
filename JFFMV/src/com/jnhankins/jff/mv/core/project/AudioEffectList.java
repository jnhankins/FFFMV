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
import com.jnhankins.jff.mv.core.PropertyChangeAdapter;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author Jeremiah N. Hankins
 */
public class AudioEffectList extends PropertyChangeAdapter implements Serializable {
    /**
     * The {@link Project} containing this {@code KeyFlameList}.
     */
    private final Project project;
    
    /**
     * The list of audio effect {@link Entry} objects.
     */
    private final ArrayList<Entry> entries = new ArrayList();
    
    protected AudioEffectList(Project project) {
        this.project = project;
    }
    
    /**
     * Returns the {@link Project} containing the {@code KeyFlameList}.
     * 
     * @return the {@code Project} containing the {@code KeyFlameList}
     */
    public Project getProject() {
        return project;
    }
    
    
    
    
    public void applyEffects(double currTime, Flame flame) {
        for (int i=0; i<entries.size(); i++) {
            Entry e = entries.get(i);
            double startTime = e.getStartTime();
            double endTime = startTime + e.getDuration();
            if (startTime <= currTime && currTime <= endTime) {
                
            }
        }
    }
    
    
    
    /**
     * {@code Entry} combines an {@code ProjectFlame} with a 
     * {@link #getTime() time}.
     */
    public class Entry extends PropertyChangeAdapter implements Serializable {
        /**
         * Indicates that the {@link #getProjectFlame() flame} has changed.
         */
        public static final String ENTRY_EFFECT_CHANGED_PROPERTY = "effect";
        
        /**
         * Indicates that the {@link #getTimeStart() start time} has changed.
         */
        public static final String ENTRY_START_TIME_CHANGED_PROPERTY = "startTime";
        
        /**
         * Indicates that the {@link #getDuration() duration} has changed.
         */
        public static final String ENTRY_DURATION_CHANGED_PROPERTY = "duration";
        
        /**
         * The {@link EffectDefinition} used by this audio effect entry.
         */
        private EffectDefinition effect;
        
        /**
         * The starting time for this audio effect entry in seconds.
         */
        private double startTime;
        
        /**
         * The length of time in seconds that the effect will remain active.
         */
        private double duration;
        
        /**
         * Constructs a new {@code Entry} using the specified audio effect,
         * starting time, and duration.
         *
         * @param effect the audio effect
         * @param startTime the starting time in seconds
         * @param duration the duration of the effect in seconds
         */
        protected Entry(EffectDefinition effect, double startTime, double duration) {
            this.effect = effect;
            this.startTime = startTime;
            this.duration = duration;
        }
        
        /**
         * Return the {@code KeyFlameList} containing this {@code Entry}, or
         * {@code null} if this {@code Entry} has been removed.
         * 
         * @return the {@code KeyFlameList} containing this {@code Entry}
         */
        public AudioEffectList getKeyFlameList() {
            if (!entries.contains(this))
                return null;
            return AudioEffectList.this;
        }

        /**
         * Returns this {@code Entry}'s {@link EffectDefinition}.
         * 
         * @return this {@code Entry}'s {@link EffectDefinition}.
         */
        public EffectDefinition getAudioEffect() {
            return effect;
        }

        /**
         * Returns this {@code Entry}'s starting time in seconds.
         * 
         * @return this {@code Entry}'s starting time in seconds.
         */
        public double getStartTime() {
            return startTime;
        }
        
        /**
         * Returns this {@code Entry}'s duration in seconds.
         * 
         * @return this {@code Entry}'s duration in seconds.
         */
        public double getDuration() {
            return duration;
        }
        
//        /**
//         * Sets the {@code ProjectFlame}.
//         * <p>
//         * If the {@code time} is changed successfully, the project's
//         * {@code isSaved} flag will be set to {@code false} and a
//         * {@link #ENTRY_FLAME_CHANGED_PROPERTY} event will be fired.
//         * 
//         * @param effect the {@code AudioEffect}
//         * @throws IllegalArgumentException if {@code effect} is {@code null}
//         */
//        public void setAudioEffect(AudioEffect effect) {
//            if (effect == null)
//                throw new IllegalArgumentException("effect is null");
//            if (effect.getProject() != project)
//                throw new IllegalArgumentException("the pflame's project does not match this key flame list's project");
//            ProjectFlame oldFlame = this.pflame;
//            ProjectFlame newFlame = pflame;
//            if (oldFlame != newFlame) {
//                this.pflame = pflame;
//                project.setIsSaved(false);
//                firePropertyChange(ENTRY_FLAME_CHANGED_PROPERTY, oldFlame, newFlame);
//            }
//        }
//        
//        /**
//         * Sets the time in seconds. 
//         * <p>
//         * If the {@code time} is changed successfully, the project's
//         * {@code isSaved} flag will be set to {@code false} and a
//         * {@link #ENTRY_TIME_CHANGED_PROPERTY} event will be fired.
//         *
//         * @param time the time in seconds
//         * @throws IllegalArgumentException if {@code time} is not in range (-inf,inf)
//         */
//        public void setTime(double time) {
//            if (!(Double.NEGATIVE_INFINITY<time && time<Double.POSITIVE_INFINITY))
//                throw new IllegalArgumentException("time is not in range (-inf,inf): "+time);
//            double oldTime = this.time;
//            double newTime = time;
//            if (oldTime != newTime) {
//                this.time = time;
//                entries.remove(this);
//                int endtryIndex = 0;
//                while (endtryIndex < entries.size()) {
//                    Entry e = entries.get(endtryIndex);
//                    if (time < e.getTime())
//                        break;
//                    endtryIndex++;
//                }
//                entries.add(endtryIndex, this);
//                project.setIsSaved(false);
//                firePropertyChange(ENTRY_TIME_CHANGED_PROPERTY, oldTime, newTime);
//            }
//        }
    }
}
