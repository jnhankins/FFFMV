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

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IAudioSamplesEvent;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * {@code VideoEncoder} converts a sequence of images into video.
 * <p>
 * Resultant output files uses the H264 video codec and MP3 audio codec.
 * <p>
 * The methods contained in this class are not synchronized.
 * 
 * @author Jeremiah N. Hankins
 */
public class VideoEncoder {
    /**
     * The video stream index.
     */
    private static final int videoStreamIndex = 0;
    
    /**
     * The audio stream index.
     */
    private static final int audioStreamIndex = 1;
    
    /**
     * The FFMPEG encoder wrapper.
     */
    private final IMediaWriter writer;
    
    /**
     * The image buffer which the correct resolution and pixel format
     */
    private final BufferedImage imageBuffer;
    
    /**
     * The graphics context for the {@link #imageBuffer}.
     */
    private final Graphics imageBufferGraphics;
    
    /**
     * The frame rate.
     */
    private final double framesPerSecond;
    
    /**
     * The index of the next frame to encode. The first index is 0.
     */
    private int frameIndex;
    
    /**
     * The audio stream transcoder. If {@code null}, then audio will not be
     * transcoded into the output file.
     */
    private AudioStreamTranscoder audioTranscoder;
    
    
    /**
     * Constructs a new {@code VideoEncoder} without audio.
     * 
     * @param outputFileName the output file name
     * @param width the video width in pixels
     * @param height the video height in pixels
     * @param framesPerSecond the video frame rate
     * @throws IllegalArgumentException if {@code fileName} is {@code null} or empty
     * @throws IllegalArgumentException if {@code width} is not in range [1,inf)
     * @throws IllegalArgumentException if {@code height} is not in range [1,inf)
     * @throws IllegalArgumentException if {@code framesPerSecond} is not in range (0,inf)
     */
    public VideoEncoder(String outputFileName, int width, int height, double framesPerSecond) {
        if (outputFileName == null || outputFileName.isEmpty())
            throw new IllegalArgumentException("fileName is null or empty: "+outputFileName);
        if (width < 1)
            throw new IllegalArgumentException("width is not in range [1,inf): "+width);
        if (height < 1)
            throw new IllegalArgumentException("height is not in range [1,inf): "+height);
        if (!(0 < framesPerSecond && framesPerSecond < Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("framesPerSecond is not in range (0,inf): "+height);
        // Store the frame rate
        this.framesPerSecond = framesPerSecond;
        // Create the output file
        final File outputFile = new File(outputFileName);
        // If the output file exists delete it
        if (outputFile.exists())
            outputFile.delete();
        // Create the media writer
        writer = ToolFactory.makeWriter(outputFileName);
        // Add a video stream
        writer.addVideoStream(
                videoStreamIndex,                // Writer's video stream index
                0,                               // Codec stream id
                ICodec.ID.CODEC_ID_H264,      // H264 Codec
                IRational.make(framesPerSecond), // Frame Rate
                width,                           // Image width
                height);                         // Image Height
//        // Get the writer's container
//        final IContainer container = writer.getContainer();
//        // Get the video stream
//        final IStream videoStream = container.getStream(videoStreamIndex);
//        // Get the video encoder
//        final IStreamCoder videoEncoder = videoStream.getStreamCoder();
//        // Set the bit rate to adjust quality
//        videoEncoder.setBitRate(5000*1000);
        // Create the image buffer with the correct resolution and pixel format
        imageBuffer = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        // Create a graphics context for the image buffer
        imageBufferGraphics = imageBuffer.createGraphics();
        // Set the inital frame index
        frameIndex = 0;
    }
    
    /**
     * Constructs a new {@code VideoEncoder} with audio.
     * 
     * @param outputFileName the output file name
     * @param width the video width in pixels
     * @param height the video height in pixels
     * @param framesPerSecond the video frame rate
     * @param audioFileName the audio source file
     * @param audioSeekTime the time to seek to in the audio stream in microseconds
     * @throws IllegalArgumentException if {@code fileName} is {@code null} or empty
     * @throws IllegalArgumentException if {@code width} is not in range [1,inf)
     * @throws IllegalArgumentException if {@code height} is not in range [1,inf)
     * @throws IllegalArgumentException if {@code framesPerSecond} is not in range (0,inf)
     * @thrwos IllegalArgumentException if {@code audioFileName} is {@code null} or empty
     * @throws IllegalArgumentException if {2code audioSeekTime} is not in range [0,inf)
     * @throws IOException if there was an error opening or reading the audio file
     * @throws AudioStreamNotFoundException if the file specified does not contain an audio stream
     * @throws XugglerErrorException if an error occurred while seeking to the specified time
     */
    public VideoEncoder(String outputFileName, int width, int height, double framesPerSecond, String audioFileName, long audioSeekTime) 
            throws AudioStreamNotFoundException, XugglerErrorException, IOException {
        this(outputFileName, width, height, framesPerSecond);
        // Create the audio transcoder
        audioTranscoder = new AudioStreamTranscoder(audioFileName, audioSeekTime);
    }
    
    /**
     * Sets the audio source for the encoded video and transcodes audio upto the
     * current position in the video stream.
     * <p>
     * Invoking this method after an audio source has already been set either by
     * the constructor by a previous call to this method will cause an
     * {@code IllegalStateException} to be thrown.
     * 
     * @param audioFileName the audio source file
     * @param audioSeekTime the time to seek to in the audio stream in microseconds
     * @throws IllegalArgumentException if {@code audioFileName} is {@code null} or empty
     * @throws IllegalArgumentException if {2code audioSeekTime} is not in range [0,inf)
     * @throws IllegalStateException if an audio source has already been set
     * @throws IllegalStateException if {@code close()} has been previously invoked
     * @throws IOException if there was an error opening or reading the file
     * @throws AudioStreamNotFoundException if the file specified does not contain an audio stream
     * @throws XugglerErrorException if an error occurred while seeking to the specified time
     */
    public void setAudioSource(String audioFileName, long audioSeekTime) 
            throws AudioStreamNotFoundException, XugglerErrorException, IOException {
        if (audioTranscoder != null)
            throw new IllegalStateException("an audio source has already been set");
        if (!writer.isOpen())
            throw new IllegalStateException("the video encoder has been closed");
        // Create the audio transcoder
        audioTranscoder = new AudioStreamTranscoder(audioFileName, audioSeekTime);
        // Transcode audio up to the current frame
        audioTranscoder.transcode(getTimeMicro());
    }
    
    /**
     * Appends the specified image as a new frame at the end of the video stream
     * and, if an audio source has been specified, transcodes the audio for the
     * new frame.
     * 
     * @param image the image to append to the video stream
     * @throws IllegalArgumentException if {@code image} is null
     * @throws IllegalArgumentException if the resolution of {@code image} does not match the expected resolution
     * @throws IllegalStateException if {@code close()} has been previously invoked
     * @throws XugglerErrorException if an error occurred wile reading the audio source file
     */
    public void encodeFrame(BufferedImage image) throws XugglerErrorException {
        if (image == null)
            throw new IllegalArgumentException("image cannot be null: "+image);
        if (image.getWidth() != imageBuffer.getWidth() || image.getHeight() != imageBuffer.getHeight())
            throw new IllegalArgumentException("image resolution does not match; expected: "+imageBuffer.getWidth()+"x"+imageBuffer.getHeight()+" found: "+image.getWidth()+"x"+image.getHeight());
        if (!writer.isOpen())
            throw new IllegalStateException("the video encoder has been closed");
        // Copy the image into the buffer (image buffer is TYPE_3BYTE_BGR)
        imageBufferGraphics.drawImage(image, 0, 0, null);
        // Calculate the time in microseconds for the current frame
        long time = getTimeMicro();
        // Encode the video frame
        writer.encodeVideo(videoStreamIndex, imageBuffer, time, TimeUnit.MICROSECONDS);
        // Encode the audio for the frame
        if (audioTranscoder != null)
            audioTranscoder.transcode(time);
        // Increment the frame index
        frameIndex++;
    }
    
    /**
     * Closes I/O stream attached to this {@code VideoEncoder}.
     * <p>
     * After invoking this method, any subsequent invocation of 
     * {@link #encodeFrame(java.awt.image.BufferedImage) encodeFrame()} or
     * {@link #setAudioSource(java.lang.String, long) setAudioSource()} will
     * cause an {@code IllegalStateException} to be thrown.
     * <p>
     * Invoking this method more than once has no additional effects.
     */
    public void close() {
        // If the audioReader is not null and is open, close it
        if (audioTranscoder != null && audioTranscoder.audioReader.isOpen())
            audioTranscoder.audioReader.close();
        // If the writer is open, close it
        if (writer.isOpen())
            writer.close();
    }
    
    /**
     * Returns the current length of the encoded video stream in microseconds.
     * 
     * @return the length of the encoded video microseconds
     */
    public long getTimeMicro() {
        return (long)((frameIndex/framesPerSecond)*1e6);
    }
    
    private class AudioStreamTranscoder extends MediaListenerAdapter {
        /**
         * The audio file name.
         */
        private final String audioFileName;
        
        /**
         * The audio media reader.
         */
        private final IMediaReader audioReader;
    
        /**
         * The index of the first audio stream found in the file.
         */
        private final int inputStreamIndex;
        
        /**
         * The time for audio stream in microseconds.
         */
        private long audioTime;
        
        /**
         * Constructs a new {@code AudioStreamTranscoder}.
         * 
         * @param audioFileName the path to the audio file
         * @param skipToTime the time to seek to in the audio file in microseconds
         * @throws IllegalArgumentException if {@code audioFileName} is {@code null} or empty
         * @throws IllegalArgumentException if {@code audioSeekTime} is not in the range [0,inf)
         * @throws IOException if there was an error opening or reading the file
         * @throws AudioStreamNotFoundException if the specified file does not contain an audio stream
         * @throws XugglerErrorException if an error occurs while attempting to seek to the correct time
         */
        AudioStreamTranscoder(String audioFileName, long audioSeekTime) 
                throws AudioStreamNotFoundException, XugglerErrorException, IOException {
            if (audioFileName == null || audioFileName.isEmpty())
                throw new IllegalArgumentException("audioFileName cannot be null or empty: "+audioFileName);
            if (audioSeekTime < 0)
                throw new IllegalArgumentException("audioSeekTime is not in range [0,inf): "+audioSeekTime);
            // Store the audio file name
            this.audioFileName = audioFileName;
            // Create a container to hold the input media file
            IContainer container = IContainer.make();
            // Open the input file for reading and store the returned error number
            int errorNo = container.open(audioFileName, IContainer.Type.READ, null);
            // If there was an error, throw an exception
            if (errorNo < 0)
                throw new IOException("failed to open file: "+IError.errorNumberToType(errorNo).name()+" \""+audioFileName+"\"");
            // Create the media reader for the file
            audioReader = ToolFactory.makeReader(container);
            // Add this AustioStreamTranscoder as a listener to the reader
            audioReader.addListener(this);
            // Get the audio stream from the container
            IStream audioStream = AudioDecoder.getAudioStream(container);
            // If no audio stream was found, throw an exception
            if (audioStream == null)
                throw new AudioStreamNotFoundException("file does not contain an audio stream: "+audioFileName);
            // Store the index of the audio stream
            inputStreamIndex = audioStream.getIndex();
            // Get the input stream coder
            IStreamCoder audioCoder = audioStream.getStreamCoder();
            // Use atleast 1 channel and atmost 2
            int channels = audioCoder.getChannels();
            if (channels < 1) channels = 1;
            if (channels > 2) channels = 2;
            // Use the same sample rate as the input stream, or 48khz if the
            // input sample rate is unknown
            int sampleRate = audioCoder.getSampleRate();
            if (sampleRate < 0) sampleRate = 48000;
            // Add the audio stream to the writer
            writer.addAudioStream(
                    audioStreamIndex,       // Audio stream index
                    0,                      // Codec stream id
                    ICodec.ID.CODEC_ID_MP3, // AAC (lossless) codec
                    channels,               // Output num channels
                    sampleRate);            // Output sample rate
            // Set the initial time
            audioTime = 0; // 0 microseconds
            // While the audio time has not caught up to the seek time...
            while (audioTime < audioSeekTime) {
                // Read the next packet and catch the return code
                IError error = audioReader.readPacket();
                // If there was an error...
                if (error != null) {
                    // If it was the EOF singal, break
                    if (error.getType() == IError.Type.ERROR_EOF) {
                        break;
                    }
                    // Otherwise it was a real error, throw an exception
                    throw new XugglerErrorException(error, "Error encountered while decoding audio file: "+audioFileName);
                }
            }
        }
        
        @Override
        public void onAudioSamples(IAudioSamplesEvent event) {
            // If the event is for the correct audio stream...
            if (event.getStreamIndex() == inputStreamIndex) {
                // Get the time for the event
                audioTime = event.getTimeStamp();
                System.out.println("audioTime: "+audioTime);
                // Get the audio samples from the event
                IAudioSamples audioSamples = event.getAudioSamples();
                // Write the audio samples to the output file
                writer.encodeAudio(audioStreamIndex, audioSamples);
            }
        }
        
        /**
         * Transcodes audio from the audio source file to the output file from
         * the current audio stream time to up to the specified time.
         * 
         * @param videoTime the time that the audio stream needs to catch up to
         * @throws XugglerErrorException if an error occurs while transcoding the audio
         */
        public void transcode(long videoTime) throws XugglerErrorException {
            // While the audio time has not caught up to the video time...
            while (audioTime < videoTime) {
                // Read the next packet and catch the return code
                IError error = audioReader.readPacket();
                // If there was an error...
                if (error != null) {
                    // If it was the EOF singal, break
                    if (error.getType() == IError.Type.ERROR_EOF) {
                        break;
                    }
                    // Otherwise it was a real error, throw an exception
                    throw new XugglerErrorException(error, "Error encountered while decoding audio file: "+audioFileName);
                }
            }
        }
    }
}
