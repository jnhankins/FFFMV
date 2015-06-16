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

package fff.mv.core.audio;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jtransforms.utils.ConcurrencyUtils;


/**
 * {@code AudioData} contains RMS and FFT data for an audio source.
 * <p>
 * Data 
 * contained in an {@code AudioData} object is divided into "frames". Each frame
 * contains data for a sequence of samples gathered from the audio input stream
 * taken from the audio source.  The sample sequences are centered such that
 * time {@code frameIndex/getFrameRate()} is the time of the center at the
 * frame. Each frame contains both RMS and FFT data for the audio input stream
 * samples used to generate the frame.
 * <p>
 * Because decoding the audio, calculating the RMS, performing the FFT for each
 * frame can take a considerable amount of time (seconds to minutes), an
 * {@code AudioData} object may be made available before its contents have been
 * completely filled. For this reason, checking {@link #getFrameCount()} before
 * each method call that requires a frame index is advisable to prevent
 * exceptions. Further, the state of the {@code AudioData} can be queried via
 * {@link #getState()}, so that it can be known when the value returned by 
 * {@code getFrameCount()} will no longer change.
 * <p>
 * This class is also helps to decouple audio data from audio analysis, which
 * produces audio data.
 *
 * @author Jeremiah N. Hankins
 */
public class AudioData {
    /**
     * Represents the state of an {@code AudioData} object.
     */
    public enum State {
        /**
         * A state indicating that work on producing an {@code AudioData}
         * instance has not started yet.
         */
        READY,
        /**
         * A state indicating that the work on an {@code AudioData} instance has
         * started but has not been completed or canceled.
         */
        WORKING,
        /**
         * A state indicating that the work on {an {@code AudioData} instance}
         * has been completed successfully.
         */
        COMPLETED,
        /**
         * A state indicating that the work on an {@code AudioData} instance has
         * been canceled before completion.
         */
        CANCELED,
    }
    
    /**
     * The current state of the audio data.
     */
    protected State state;
    
    /**
     * The source file for the audio data.
     */
    protected final File audioFile;
    
    /**
     * The sample rate of the audio input stream.
     */
    protected final int sampleRate;
    
    /**
     * The length of the FFT (the number of samples used per FFT).
     */
    protected final int fftLength;
    
    /**
     * The amount that two adjacent FFT frames will overlap.
     * A value of {@code 0} indicates that no data is shared between FFTs, a
     * value of {@code 0.5} indicates that the FFTs overlap by 50%, etc.
     */
    protected final double fftOverlap;
    
    /**
     * The maximum RMS value.
     */
    protected float rmsMax;
    
    /**
     * A list of the RMS values for each frame.
     */
    protected final List<Float> rmsData;
    
    /**
     * The maximum of the FFT values.
     */
    protected float fftMax;
    
    /**
     * An array of maximum FFT bin amplitudes.
     */
    protected final float[] fftBinMax;
    
    /**
     * A list of float arrays containing FFT amplitude data for each frame.
     */
    protected final List<float[]> fftData;
    
    /**
     * A list of the frequency for each FFT bin.
     */
    protected final float[] fftBinFreq;
    
    /**
     * The number of audio data frames.
     */
    protected int frameCount;
    
    /**
     * Constructs a new {@code AudioData} object.
     * 
     * @param audioFile the source file for the audio data
     * @param sampleRate the sample rate of the input audio stream
     * @param fftLength the number of samples per FFT
     * @param fftOverlap the amount that two adjacent FFTs overlap
     * @throws IllegalArgumentException if {@code audioFile} is {@code null}
     * @throws IllegalArgumentException if {@code sampleRate} is not in range 1,inf)
     * @throws IllegalArgumentException if {@code fftLength} is not a power of two greater than 1
     * @throws IllegalArgumentException if {@code fftOverlap} is not in range [0,1)
    */
    protected AudioData(File audioFile, int sampleRate, int fftLength, double fftOverlap) {
        if (audioFile == null)
            throw new IllegalArgumentException("audioFile is null");
        if (sampleRate <= 0)
            throw new IllegalArgumentException("sampleRate is not in range [1,inf): "+sampleRate);
        if (!(fftLength > 1 && ConcurrencyUtils.isPowerOf2(fftLength)))
            throw new IllegalArgumentException("fftLength is not a power of two greater than 1: "+fftLength);
        if (!(0 <= fftOverlap && fftOverlap < 1))
            throw new IllegalArgumentException("fftOverlap is not in range [0,1): "+fftOverlap);
        // Store local variables
        this.audioFile = audioFile;
        this.sampleRate = sampleRate;
        this.fftLength = fftLength;
        this.fftOverlap = fftOverlap;
        // Construct the data arrays \
        rmsData = new ArrayList();
        fftData = new ArrayList();
        // Initialize maximum value trakers
        rmsMax = 0;
        fftMax = 0;
        fftBinMax = new float[fftLength/2];
        // Initialize the FFT bin freuqneices
        fftBinFreq = new float[fftLength/2];
        for (int i=0; i<fftBinFreq.length; i++)
            fftBinFreq[i] = i*((float)sampleRate/fftLength);
        // Initialize the total frame count
        frameCount = 0;
        // Set the initial state
        state = State.READY;
    }
    
    /**
     * Returns the current state of the {@code AudioData} instance.
     * 
     * @return the current state of the {@code AudioData} instance
     * @see State
     */
    public State getState() {
        return state;
    }
    
    /**
     * Returns the source file for the audio data.
     * 
     * @return the source file for the audio data
     */
    public File getAudioFile() {
        return audioFile;
    }
    
    /**
     * Returns the sample rate of the audio input stream.
     * 
     * @return the sample rate of the audio input stream
     */
    public int getSampleRate() {
        return sampleRate;
    }
    
    /**
     * Returns the number of samples used in each FFT.
     * 
     * @return the number of samples used in each FFT
     */
    public int getFFTLength() {
        return fftLength;
    }
    
    /**
     * Returns the amount that two adjacent FFTs overlap.
     * <p>
     * A value of {@code 0} indicates that no data is shared between FFTs, a
     * value of {@code 0.5} indicates that the FFTs overlap by 50%, etc.
     * 
     * @return  the amount that two adjacent FFTs overlap
     */
    public double getFFTOverlap() {
        return fftOverlap;
    }
    
    /**
     * Returns the number of FFT bins.
     * <br>
     * Equivalent to:
     * <pre>{@code fftLength/2}</pre>
     * 
     * @return the number of FFT bins
     */
    public int getFFTBinCount() {
        return fftLength/2;
    }
    
    /**
     * Returns the number of frames of data per second.
     * <br>
     * Equivalent to:
     * <pre>{@code sampleRate/(fftLength*(1-fftOverlap))}</pre>
     * 
     * @return the number of frames of data per second.
     */
    public double getFrameRate() {
        return sampleRate/(fftLength*(1-fftOverlap));
    }
    
    /**
     * Returns the number of frames of data.
     * 
     * @return the number of frames of data.
     */
    public int getFrameCount() {
        return frameCount;
    }
    
    /**
     * Returns the maximum RMS value.
     * 
     * @return the maximum RMS value
     */
    public float getRMSMax() {
        return rmsMax;
    }
    
    /**
     * Returns the RMS for the specified frame.
     * 
     * @param frame the frame index
     * @return the RMS for the specified frame
     * @throws IllegalArgumentException if {@code frame} is not in range [0,{@code getFrameCount()})
     */
    public float getRMSFrame(int frame) {
        if (!(0 <= frame && frame < getFrameCount()))
            throw new IllegalArgumentException("frame is not in range [0,"+getFrameCount()+"]: "+frame);
        return rmsData.get(frame);
    }
    
    /**
     * Returns the maximum FFT value.
     * 
     * @return the maximum FFT value
     */
    public float getFFTMax() {
        return fftMax;
    }
    
    /** 
     * Returns the maximum FFT value for the specified bin.
     * 
     * @param bin the bin index
     * @return the maximum FFT value for the specified bin
     * @throws IllegalArgumentException if {@code bin} is not in range [0,{@code getFFTBinCount()})
     */
    public float getFFTBinMax(int bin) {
        if (!(0 <= bin && bin < getFFTBinCount()))
            throw new IllegalArgumentException("bin is not in range [0,"+getFFTBinCount()+"): "+bin);
        return fftBinMax[bin];
    }
    
    /** 
     * Returns the FFT value for the specified frame and bin.
     * 
     * @param frame the frame index
     * @param bin the bin index
     * @return the maximum FFT value for the specified bin
     * @throws IllegalArgumentException if {@code frame} is not in range [0,{@code getFrameCount()})
     * @throws IllegalArgumentException if {@code bin} is not in range [0,{@code getFFTBinCount()})
     */
    public float getFFTFrameBin(int frame, int bin) {
        if (!(0 <= frame && frame < getFrameCount()))
            throw new IllegalArgumentException("frame is not in range [0,"+getFrameCount()+"]: "+frame);
        if (!(0 <= bin && bin < getFFTBinCount()))
            throw new IllegalArgumentException("bin is not in range [0,"+getFFTBinCount()+"): "+bin);
        return fftData.get(frame)[bin];
    }
    
    /**
     * Returns an array containing the FFT data for the specified frame.
     * <p>
     * If the parameterized array is not {@code null} and its length is equal to
     * {@code getFFTBinCount()} then the array will be used to store the
     * results, otherwise a new array will be allocated to store the results.
     *
     * @param frame the frame index
     * @param arr an array used to store returned values or {@code null}
     * @return an array containing the FFT data for the specified frame
     * @throws IllegalArgumentException if {@code frame} is not in range [0,{@code getFrameCount()})
     */
    public float[] getFFTFrame(int frame, float[] arr) {
        if (!(0 <= frame && frame < getFrameCount()))
            throw new IllegalArgumentException("frame is not in range [0,"+getFrameCount()+"]: "+frame);
        if (arr == null || arr.length != getFFTBinCount())
            arr = new float[getFFTBinCount()];
        System.arraycopy(fftData.get(frame), 0, arr, 0, getFFTBinCount());
        return arr;
    }
    
    /**
     * Returns the frequency for the FFT bin at the specified index.
     * 
     * @param bin the bin index
     * @return the frequency for the FFT bin at the specified index in Hz
     * @throws IllegalArgumentException if {@code bin} is not in range [0,{@code getFFTBinCount()})
     */
    public float getFFTBinFrequency(int bin) {
        if (!(0 <= bin && bin < getFFTBinCount()))
            throw new IllegalArgumentException("bin is not in range [0,"+getFFTBinCount()+"): "+bin);
        return fftBinFreq[bin];
    }
    
    /**
     * Returns an array containing the frequencies for all of the FFT bins.
     * <p>
     * If the parameterized array is not {@code null} and its length is equal to
     * {@code getFFTBinCount()} then the array will be used to store the
     * results, otherwise a new array will be allocated to store the results.
     * 
     * @param arr an array used to store returned values or {@code null}
     * @return an array containing the frequencies for all of the FFT bins in Hz
     */
    public float[] getFFTFrequncies(float[] arr) {
        if (arr == null || arr.length != getFFTBinCount())
            arr = new float[getFFTBinCount()];
        System.arraycopy(fftBinFreq, 0, arr, 0, getFFTBinCount());
        return arr;
    }
}
