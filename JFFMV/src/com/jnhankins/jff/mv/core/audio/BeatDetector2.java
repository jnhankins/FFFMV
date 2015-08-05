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
 * {@code BeatDetector} detects beats within a frequency range for a given
 * {@code AudioData} source.
 * <p>
 * An acoustic beat is characterized by a sudden increase in the sound energy
 * contained in a specific frequency range. So determining whether or not a 
 * specific audio frame contains a beat is simply a matter of comparing the
 * sound energy contained in the frequency range for the current frame to the
 * average sound energy contained in the frequency range of preceeding frames.
 * If the sound energy for the current frame is much greater than the average
 * sound energy then the frame contains a beat.
 * <p>
 * The sound energy contained in the frequency range for a specific frame is
 * calculated by summing the the values contained in the FFT bins spanning the
 * frequency range. Since music and audio signals often contain sections that
 * are more or less loud than other sections, rather than comparing a frame to
 * the rest of the entire signal, only the previous {@code n} seconds are
 * considered, where {@code n} is usually about 1 second. The energy of each
 * frame of the previous {@code n} seconds are stored in a history buffer for
 * which an average sound energy can be calculated. Because beats can be sharp
 * and very pronounced or subtle and embedded within a noisier signal, the
 * variance of the energy contained within the frames of the history buffer is
 * used to calculate an appropriate threshold for beat detection.
 * <p>
 * A frame is said to contain a beat if the following statement is {@code true}:
 * <pre>{@code (e > AVG[E] * (C1 * VAR[E]) + C0)}</pre> Where {@code e} is the
 * energy for the current frame, {@code AVG[E}} is the average energy,
 * {@code VAR[E}} is average variance, and {@link #getAdditiveConstant() C0} and
 * {@link #getMultiplicativeConstant() C1} are constants.
 * 
 * @author Jeremiah N. Hankins
 */
public class BeatDetector2 {
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
    
    /**
     * The maximum number of history frames to store and use.
     */
    private final int historyFrames;
    
    /**
     * A queue containing the energy
     */
    private final ArrayDeque<Double> energyHistory;
    
    /**
     * A running sum of the values stored in the history buffer.
     */
    private double energySum1;
    
    /**
     * A running sum of the squares of the values stored in the history buffer.
     */
    private double energySum2;
    
    /**
     * The current frame index.
     */
    private int frameIndex;
    
    private double multiplicativeConstant = -0.0025714;
    
    private double addativeConstant = 1.5142857;
    
    /**
     * Constructs a new {@code BeatDetector} for the specified {@code AudioData}
     * source using the specified parameters.
     * 
     * @param audioData the audio data source
     * @param minFrequency the minimum of the frequency range being tested for
     * beats, specified in Hz
     * @param maxFrequency the maximum of the frequency range being tested for
     * beats, specified in Hz
     * @param historyLength the number of seconds of history data against which
     * frames will be compared when attempting to determine if the frame
     * contains a beat
     * 
     * @throws NullPointerException if {@code audioData} is {@code null}
     * @throws IllegalArgumentException if {@code minFrequency} or {@code maxFrequency} is not finite or {@code minFrequency} is not less than {@code maxFrequency}
     * @throws IllegalArgumentException if {@code historyLength} is not in the range (0, inf)
     */
    public BeatDetector2(AudioData audioData, 
            double minFrequency, 
            double maxFrequency, 
            double historyLength) {
        
        if (audioData == null)
            throw new NullPointerException("audioData");
        if (!(0 <= minFrequency && minFrequency < maxFrequency && maxFrequency < Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("minFrequency and maxFrequency must be finite and minFrequency must be less than maxFrequency : minFrequency="+minFrequency+" maxFrequency="+maxFrequency);
        if (! (0 < historyLength && historyLength < Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("historyLength is not in range (0,inf): "+historyLength);
        
        this.audioData = audioData;
        
        double resolution = audioData.getFFTBinResolution();
        minBin = (int)(minFrequency/resolution);
        maxBin = Math.min((int)(maxFrequency/resolution), audioData.getFFTBinCount()-1);
        
        historyFrames = (int)Math.ceil(historyLength*audioData.getFrameRate());
        
        energyHistory = new ArrayDeque(historyFrames);
        energySum1 = 0;
        energySum2 = 0;
        
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
     * Returns the number of seconds of history data against which the next
     * frame will be compared when attempting to determine if the next frame
     * contains a beat.
     * 
     * @return the number of seconds of history data
     */
    public double getHistoryLength() {
        return historyFrames/audioData.getFrameRate();
    }
    
    /**
     * Returns the multiplicative constant.
     * 
     * @return the multiplicative constant
     */
    public double getMultiplicativeConstant() {
        return multiplicativeConstant;
    }
    
    /**
     * Sets the multiplicative constant.
     * 
     * @param multiplicativeConstant the multiplicative constant
     * @throws IllegalArgumentException if {@code multiplicativeConstant} is not finite
     */
    public void setMultiplicativeConstant(double multiplicativeConstant) {
        if (!Double.isFinite(multiplicativeConstant))
            throw new IllegalArgumentException("multiplicativeConstant must be finite: "+multiplicativeConstant);
        this.multiplicativeConstant = multiplicativeConstant;
    }
    
    /**
     * Returns the additive constant.
     * 
     * @return the additive constant
     */
    public double getAdditiveConstant() {
        return addativeConstant;
    }
    
    /**
     * Sets the additive constant.
     * 
     * @param addativeConstant the additive constant
     * @throws IllegalArgumentException if {@code addativeConstant} is not finite
     */
    public void setAdditiveConstant(double addativeConstant) {
        if (!Double.isFinite(addativeConstant))
            throw new IllegalArgumentException("addativeConstant must be finite: "+addativeConstant);
        this.addativeConstant = addativeConstant;
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
            
        // Get the energy for the next frame and update the running sums
        double energyInstant = nextFrame();

        // Calculate the average energy and variance
        int n = energyHistory.size();
        double energyAverage = energySum1/n;
        double energyVariance = (n*energySum2 - (energySum1*energySum1))/(n*(n-1));
        
        // Compute a constant that will scale the beat detection theshold
        // based on the variance of the signal
        double C = (multiplicativeConstant * energyVariance) + addativeConstant;

        System.out.println(energyInstant+"\t"+energyAverage+"\t"+energyVariance+"\t"+C+"\t"+C*energyAverage);
        
        // If the instantaneous energy supasses the beat detection 
        // threshold, then this frame contains a beat
        return energyInstant > C*energyAverage;
    }
    
    /**
     * Skips the specified number of frames.
     * 
     * @param frames the number of frames to skip
     * @return the number of frames that were actually skipped
     */
    public int skip(int frames) {
        // If the requested number of frames to skip is not positive, do nothing
        if (frames <= 0)
            return 0;
        
        // Keep track of the number of frames to skip
        int skipped = 0;
        
        // If the requested number of frames to skip is greater than the length
        // of the history buffer, and there are enough frames available to skip
        // more frames than can fit in the buffer... then skip and discard those
        // frames which will not be contained in the buffer after this method
        // call.
        int framesAvailable = audioData.frameCount-frameIndex-1;
        if (frames > historyFrames && framesAvailable > historyFrames) {
            skipped = Math.min(frames, framesAvailable) - historyFrames;
            frameIndex += skipped;
        }
        
        // Skip and perform computations on frames, one at a time, untill either
        // the requested number of frames have been skipped or there are no more
        // frames available
        while (skipped < frames && frameIndex < audioData.frameCount) {
            nextFrame();
            skipped++;
        }
        
        // Return the number of frames that were skipped
        return skipped;
    }
    
    /**
     * Returns the energy for the current frame, updates the history buffer and 
     * running sums and increments the frame index.
     * 
     * @return the instantaneous energy for the current frame
     */
    private double nextFrame() {
        // Get the new energy
        double newEnergy = 0;
        float[] fftFrame = audioData.fftData.get(frameIndex);
        for (int bin = minBin; bin <= maxBin; bin++)
            newEnergy += fftFrame[bin];
        newEnergy /= (maxBin-minBin+1);
        
        // Get the old energy
        double oldEnergy = 0;
        if (energyHistory.size() == historyFrames)
            oldEnergy = energyHistory.pop();

        // Store the new energy
        energyHistory.push(newEnergy);
        
        // Update the running sums
        energySum1 += newEnergy - oldEnergy;
        energySum2 += newEnergy*newEnergy - oldEnergy*oldEnergy;
        
        // Increment the frame index
        frameIndex++;    
        
        // Return the instantaneous eneryg for the current frame
        return newEnergy;
    }
    
}
