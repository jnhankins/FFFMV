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

package com.jnhankins.jff.mv.core.audio;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

/**
 *
 * @author Jeremiah N. Hankins
 */
public class BeatDetector1 {
    /**
     * The audio data source.
     */
    private final AudioData audioData;
    
    /**
     * The minimum (inclusive) FFT bin to use.
     */
    private final int minBin;
    
    /**
     * The maximum (inclusive) FFT bin to use..
     */
    private final int maxBin;
    
    private final double sensitivity;
    private final double b;
    
    private double nextEnergy;
    
    private double threshold;
    
    
    /**
     * The current frame index.
     */
    private int frameIndex;
    
    
    /**
     * Constructs a new {@code BeatDetector} for the specified {@code AudioData}
     * source using the specified parameters.
     * 
     * @param audioData the audio data source
     * @param minFrequency the minimum of the frequency range being tested for
     * beats, specified in Hz
     * @param maxFrequency the maximum of the frequency range being tested for
     * beats, specified in Hz
     * @param sensitivity 
     * 
     * @throws NullPointerException if {@code audioData} is {@code null}
     * @throws IllegalArgumentException if {@code minFrequency} or {@code maxFrequency} is not finite or {@code minFrequency} is not less than {@code maxFrequency}
     * @throws IllegalArgumentException if {@code sensitivity} is not in the range [0,1]
     */
    public BeatDetector1(AudioData audioData, 
            double minFrequency, 
            double maxFrequency, 
            double sensitivity,
            double b) {
        if (audioData == null)
            throw new NullPointerException("audioData");
        if (!(0 <= minFrequency && minFrequency < maxFrequency && maxFrequency < Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("minFrequency and maxFrequency must be finite and minFrequency must be less than maxFrequency : minFrequency="+minFrequency+" maxFrequency="+maxFrequency);
        if (! (0 <= sensitivity && sensitivity <= 1))
            throw new IllegalArgumentException("sensitivity is not in range [0,1]: "+sensitivity);
        this.audioData = audioData;
        this.sensitivity = sensitivity;
        this.b = b;
        
        double resolution = audioData.getFFTBinResolution();
        minBin = (int)(minFrequency/resolution);
        maxBin = Math.min((int)(maxFrequency/resolution), audioData.getFFTBinCount()-1);
        
        nextEnergy = 0;
        threshold = 0;
        frameIndex = 0;
    }
    
    /**
     * Returns the {@link AudioData} source for this beat detector.
     * 
     * @return the {@code AudioData} source for this beat detector
     */
    public AudioData getAudioData() {
        return audioData;
    }
    
    /**
     * Returns the minimum frequency of the range covered by this beat detector.
     * 
     * @return the minimum frequency in Hz
     */
    public double getMinimumFrequency() {
        return audioData.getFFTBinResolution()*minBin;
    }
    
    /**
     * Returns the maximum frequency of the range covered by this beat detector.
     * 
     * @return the maximum frequency in Hz
     */
    public double getMaximumFrequency() {
        return audioData.getFFTBinResolution()*(maxBin+1);
    }
    
    /**
     * 
     * @return the sensitivity
     */
    public double getSensitivity() {
        return sensitivity;
    }
    
    /**
     * Returns the current frame index. The current frame is the frame that will
     * be tested for having a beat the next time {@link #next()} is called.
     * 
     * @return the current frame index
     */
    public int getFrameIndex() {
        return frameIndex;
    }
    
    /**
     * Returns {@code true} if the audio data contains frames which have not yet
     * been examined for beats.
     * <p>
     * Equivalent to: {@code (getFrameIndex() < getAudioData().getFrameCount())}
     * 
     * @return {@code true} if the audio data contains frames which have not yet
     * been examined for beats
     */
    public boolean hasNext() {
        return frameIndex < audioData.frameCount;
    }
    
    /**
     * Returns {@code true} if a beat was detected for the current frame and
     * increments the frame index.
     * 
     * @return if a beat was detected for the current frame
     * @throws NoSuchElementException if there are no more 
     */
    public boolean next() {
        // Throw an exception if there are no more frames available
        if (!hasNext())
            throw new NoSuchElementException("no more frames available");
        
        double currEnergy = nextEnergy;
        nextEnergy = calcFrameEnergy(frameIndex);
        frameIndex++;
        
        if (currEnergy >= threshold && currEnergy > nextEnergy) {
            threshold = currEnergy*(1+b);
            return true;
        } else {
            threshold *= (1-sensitivity);
            return false;
        }
    }
    
    private double calcFrameEnergy(int fIndex) {
//        return audioData.getRMSFrame(fIndex);
        
        double energy = 0;
        float[] fftFrame = audioData.fftData.get(fIndex);
        for (int bin = minBin; bin <= maxBin; bin++)
            energy += fftFrame[bin];
        energy /= (maxBin-minBin+1);
        return energy;
    }
}
