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

package fff.mv.io;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Jeremiah N. Hankins
 */
public abstract class VideoEncoder implements Closeable {
    /**
     * The output file.
     */
    private final File destFile;
    
    /**
     * The width of the video in pixels.
     */
    private final int width;
    
    /**
     * The height of the video in pixels.
     */
    private final int height;
    
    /**
     * The time scale units.
     * <p>
     * The frame rate is {@code timeScale/timeStep}.
     */
    private final int timeScale;
    
    /**
     * The number of time time scale units per frame.
     * <p>
     * The frame rate is {@code timeScale/timeStep}.
     */
    private final int timeStep;
    
    /**
     * The audio file source or {@code null} if no audio source has been set.
     */
    private File audioFile;
    
    /**
     * The frame index.
     */
    private long frameIndex;
    
    /**
     * {@code true} if the {@link #close()} method has been called.
     */
    private boolean isClosed;
    
    /**
     * Initializes the protected fields of {@code VideoEncoder}.
     * 
     * @param destFile the destination destFile
     * @param width the width of the image in pixels
     * @param height the height of the image in pixels
     * @param timeStep the number of time scale units between frames
     * @param timeScale time scale units in fractions of a second
     * @throws IllegalArgumentException {@code destFile} is {@code null}
     * @throws IllegalArgumentException {@code width} is not in range [1,inf)
     * @throws IllegalArgumentException {@code height} is not in range [1,inf)
     * @throws IllegalArgumentException {@code timeStep} is not in range [1,inf)
     * @throws IllegalArgumentException {@code timeScale} is not in range [1,inf)
     */
    protected VideoEncoder(File destFile, int width, int height, int timeStep, int timeScale) {
        if (destFile == null)
            throw new IllegalArgumentException("file is null");
        if (width < 1)
            throw new IllegalArgumentException("width is not in range [1,inf): "+width);
        if (height < 1)
            throw new IllegalArgumentException("height is not in range [1,inf): "+height);
        if (timeStep < 1)
            throw new IllegalArgumentException("timeStep is not in range [1,inf): "+timeStep);
        if (timeScale < 1)
            throw new IllegalArgumentException("timeScale is not in range [1,inf): "+timeScale);
        this.destFile = destFile;
        this.width = width;
        this.height = height;
        this.timeStep = timeStep;
        this.timeScale = timeScale;
        audioFile = null;
        frameIndex = 0;
        isClosed = false;
    }
    
    /**
     * Returns the destination file.
     * 
     * @return the destination file
     */
    public File getDestFile() {
        return destFile;
    }
    
    /**
     * Returns the width of the video in pixels.
     * 
     * @return the width of the video in pixels
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Returns the height of the video in pixels.
     * 
     * @return the height of the video in pixels
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Returns the number of time scale units per frame.
     * 
     * @return the number of time scale units per frame
     */
    public int getTimeStep() {
        return timeStep;
    }
    
    /**
     * Returns the time scale units in fractions of a second.
     * 
     * @return the time scale units in fractions of a second
     */
    public int getTimeScale() {
        return timeScale;
    }
    
    /**
     * Returns the frame rate in frames per second.
     * <br>
     * Equivalent to: <pre>{@code getTimeScale()/(double)getTimeStep()}</pre>
     * 
     * @return the frame rate
     */
    public double getFrameRate() {
        return timeScale/(double)timeStep;
    }
    
    /**
     * Returns the current frame index. The current frame index is the index of
     * next frame to be encoded.
     * 
     * @return the current frame index
     */
    public long getFrameIndex() {
        return frameIndex;
    }
    
    /**
     * Returns the current frame time in seconds. The current frame time is the
     * time for the next frame to be encoded.
     * <br>
     * Equivalent to: <pre>{@code (getFrameIndex()*getTimeStep())/(double)getTimeScale()}</pre>
     *
     * @return the current frame time in seconds
     */
    public double getFrameTime() {
        return (frameIndex*timeStep)/(double)(timeScale);
    }
    
    /**
     * Returns the audio source file or {@code null} if no audio source has been
     * set.
     * 
     * @return the audio source file or {@code null} if no audio source has been set
     */
    public File getAudioSourceFile() {
        return audioFile;
    }    
    
    /**
     * Returns {@code true} if {@link #close()} has been invoked.
     * 
     * @return {@code true} if {@code close()} has been called
     */
    public boolean isClosed() {
        return isClosed;
    }
    
    /**
     * Sets the audio source for the encoded video and transcodes audio upto the
     * current position in the video stream.
     * <p>
     * Invoking this method after an audio source has already been set either by
     * the constructor by a previous call to this method will cause an
     * {@code IllegalStateException} to be thrown.
     * 
     * @param audioFile the audio source file
     * @param seekTime the time to seek to within the audio source in seconds
     * @throws IllegalArgumentException if {@code audioFile} is {@code null}
     * @throws IllegalArgumentException if {@code seekTime} is not not in range [0,inf)
     * @throws IllegalStateException if an audio source has already been set
     * @throws IllegalStateException if the {@code VideoEncoder} has been closed
     * @throws IOException if there was an error setting the audio source or transcoding the audio
     */
    public void setAudio(File audioFile, double seekTime) throws IOException {
        if (audioFile == null)
            throw new IllegalArgumentException("audioFile is null");
        if (!(0 <= seekTime && seekTime < Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("seekTime is not not in range [0,inf): "+seekTime);
        if (this.audioFile != null)
            throw new IllegalStateException("an audio source has already been set");
        if (isClosed)
            throw new IllegalStateException("VideoEncoder is closed");
        this.audioFile = audioFile;
        initAudio(seekTime);
    }
    
    /**
     * Appends the specified image as a new frame at the end of the video stream
     * and, if an audio source has been specified, transcodes the audio for the
     * new frame.
     * 
     * @param image the image to append to the video stream
     * @throws IllegalArgumentException if {@code image} is null
     * @throws IllegalArgumentException if the resolution of {@code image} does not match the expected resolution
     * @throws IllegalStateException if the {@code VideoEncoder} has been closed
     * @throws IOException if an error occurred while adding the frame
     */
    public void addFrame(BufferedImage image) throws IOException {
        if (image == null)
            throw new IllegalArgumentException("image is null");
        if (image.getWidth() != width || image.getHeight() != height)
            throw new IllegalArgumentException("image resolution does not match; expected: "+width+"x"+height+" found: "+image.getWidth()+"x"+image.getHeight());
        if (isClosed)
            throw new IllegalStateException("VideoEncoder is closed");
        encodeImage(image);
        if (audioFile != null)
            encodeAudio();
        frameIndex++;
    }
    
    /**
     * Closes the {@code VideoEncoder} and releases any system resources
     * associated with it.
     * 
     * @throws IOException if an error occurred wile closing the {@code VideoEncoder}
     */
    @Override
    public void close() throws IOException {
        isClosed = true;
        onClose();
    }
    
    /**
     * Initializes audio transcoding resources and transcodes audio form the
     * source to output file upto the current frame.
     * <p>
     * This method is called by {@link #setAudioSource(File, double)} so it will
     * be invoked at most once. Users of {@code VideoEncoder} may call invoke
     * this method before any images have been encoded, after some images have
     * been coded and more remain, or after all images have been encoded. It is
     * up to this method to ensure that all off the audio corresponding to 
     * frame {@code 0} through {@link #frameIndex} has been encoded.
     * <p>
     * The parameter {@code seekTime} represents the time that the audio should
     * skip to for encoding the first frame. That is, time 0 sec in the video
     * output corresponds to {@code seekTime} in the audio source time. 
     * {@code seekTime} is guaranteed to be in the range [0,inf), ie a non-negative normal number.
     * 
     * @param seekTime the time to skip
     * @throws IOException if there was an error setting the audio source or transcoding the audio
     */
    protected abstract void initAudio(double seekTime) throws IOException;
    
    /**
     * Encodes video for the next frame.
     * <p>
     * This method is called by {@link #encodeFrame(BufferedImage)} before
     * encoding audio and before incrementing the frame index.
     * 
     * @param image the image to encode
     * @throws IOException if an error occurs wile encoding the image
     */
    protected abstract void encodeImage(BufferedImage image) throws IOException;
    
    /**
     * Encodes audio for the next frame.
     * <p>
     * This method is called by {@link #encodeFrame(BufferedImage)} if
     * {@link #audioFile} is not {@code null} after encoding the image and
     * before incrementing the frame index.
     * 
     * @throws IOException if an error occurs wile encoding the audio
     */
    protected abstract void encodeAudio() throws IOException;
    
    /**
     * Closes the {@code VideoEncoder} and releases any system resources
     * associated with it.
     * <p>
     * Called by {@link #close()} after the {@link #isClosed() isClosed} is set
     * to {@code true}.
     * 
     * @throws IOException if an error occurs wile closing the {@code VideoEncoder}
     */
    protected abstract void onClose() throws IOException;
}
