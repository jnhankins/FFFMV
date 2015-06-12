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

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IRational;
import fff.mv.io.XugglerAudioInputStream.Encoding;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;

/**
 * {@code XugglerVideoEncoder} converts a sequence of images into video.
 * <p>
 * Resultant output files are encoded using H.264 (X264) for video and AAC
 * (libvo_aacenc) audio encoding. The encoding is performed by the Xuggler
 * library which is a Java wrapper for FFMPEG.
 * <p>
 * The methods contained in this class are not synchronized.
 * 
 * @author Jeremiah N. Hankins
 */
public class XugglerVideoEncoder extends VideoEncoder {
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
     * The audio input stream. May be {@code null} if no audio is set.
     */
    private XugglerAudioInputStream audioInputStream = null;

    /**
     * The sample rate of the audio input stream.
     */
    private double audioSampleRate;
    
    /**
     * The number of bytes per frame in the audio input stream.
     */
    private int audioFrameSize;
    
    /**
     * A buffer for samples read from the audio input stream.
     */
    private byte[] audioBuffer;
    
    /**
     * A little-endian short buffer wrapping {@link #audioBuffer}.
     */
    private ShortBuffer shortBuffer;
    
    /**
     * A a buffer for holding samples that have been converted into shorts by
     * the {@link #shortBuffer}.
     */
    private short[] audioShortsBuffer;
    
    /**
     * The number of bytes read from the audio input stream so far (excluding
     * the initial seek).
     */
    private long audioStreamPos;
    
    /**
     * Constructs a new {@code XugglerVideoEncoder}.
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
    public XugglerVideoEncoder(File destFile, int width, int height, int timeStep, int timeScale) {
        super(destFile, width, height, timeStep, timeScale);
        
        // If the output file exists delete it
        if (destFile.exists())
            destFile.delete();
        // Create the media writer
        writer = ToolFactory.makeWriter(destFile.getPath());
        // Add a video stream
        writer.addVideoStream(
                videoStreamIndex,                   // Writer's video stream index
                0,                                  // Codec stream id
                ICodec.ID.CODEC_ID_H264,            // H264 Codec
                IRational.make(timeScale/timeStep), // Frame Rate
                width,                              // Image width
                height);                            // Image Height
        
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
    }
    
    @Override
    public void encodeImage(BufferedImage image) throws XugglerErrorException {
        // Copy the image into the buffer (image buffer is TYPE_3BYTE_BGR)
        imageBufferGraphics.drawImage(image, 0, 0, null);
        // Calculate the time in microseconds for the current frame
        long time = (getFrameIndex()*getTimeStep()*1000000L)/getTimeScale();
        // Encode the video frame
        writer.encodeVideo(videoStreamIndex, imageBuffer, time, TimeUnit.MICROSECONDS);
    }
    
    @Override
    protected void initAudio(double seekTime) throws IOException {
        // Crea the audio input stream
        audioInputStream = XugglerAudioInputStream.open(
                getAudioSourceFile().getPath(), null, null, Encoding.S16);
        // Seek to the correct time
        audioInputStream.seek(seekTime);
        // Get the audio format
        AudioFormat format = audioInputStream.getFormat();
        // Store the sample rate
        audioSampleRate = format.getFrameRate();
        // Store the number of bytes in a frame of audio
        audioFrameSize = format.getFrameSize();
        // Make a buffer for audio data that is larege enough to hold atleast
        // one video-frame's worth of audio samples
        int samplesPerFrame = (int)Math.ceil((audioSampleRate*getTimeScale())/getTimeStep()) + 1;
        audioBuffer = new byte[samplesPerFrame*audioFrameSize];
        // Wrap the audio buffer in a short buffer for fast conversion to shrots
        shortBuffer = ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        // Get the AAC encoding codec
        ICodec codec = ICodec.findEncodingCodecByName("libvo_aacenc");
        // Add the audio stream to the writer
        writer.addAudioStream(
                audioStreamIndex,             // Audio stream index
                0,                            // Codec stream id
                codec,                        // AAC codec
                format.getChannels(),         // Output num channels
                (int)format.getSampleRate()); // Output sample rate
        // Initialize the audiop stream position
        audioStreamPos = 0;
        
        // Encode audio up to the current video frame index
        for (int frameIndex=0; frameIndex < getFrameIndex()-1; frameIndex++)
            if (!encodeAudio(frameIndex))
                break;
    }
    
    @Override
    protected void encodeAudio() throws IOException {
        encodeAudio(getFrameIndex());
    }
    
    /**
     * Reads the next second of audio from the audio input stream and writes it
     * to the appends it
     * to the encodes
     * it of the audio from the audio stream between
     * {@link #audioStreamIndex} and calculated position for where the stream
     * should be at the end of the specified video frame index.
     * 
     * @param frameIndex the video frame index
     * @return {@code false} if EOF was hit and no audio was encoded
     * @throws IOException if an error occurred while reading from the stream
     */
    protected boolean encodeAudio(long frameIndex) throws IOException {
        // Calculate what the audio stream position should be a the end of this
        // operation, that is, the position in the audio stream that corresponds
        // with the end end of the frame
        long frameEndPos = audioFrameSize*(long)(audioSampleRate*(frameIndex+1)/getFrameRate());
        // Calculate the number of bytes that need to be read
        int bytesToRead = (int)(frameEndPos-audioStreamPos);
        // Fill the buffer.
        // Note: The XugglerAudioInputStream.read() method will return exactly 
        // the number of bytes requested unless it hits EOF, so we dont need to 
        // loop to ensure we fill the audio buffer
        int bytesRead = audioInputStream.read(audioBuffer, 0, bytesToRead);
        // If EOF return false
        if (bytesRead == -1)
            return false;
        if (bytesRead != bytesToRead)
            System.err.println("bytesRead != bytesToRead");
        // Update thee audio stream position
        audioStreamPos += bytesRead;
        // Make sure the shrots buffer is the correct size;
        if (audioShortsBuffer == null || audioShortsBuffer.length != bytesRead/2)
            audioShortsBuffer = new short[bytesRead/2];
        // Convert the samples from an array of bytes to an array of shorts
        shortBuffer.rewind();
        shortBuffer.get(audioShortsBuffer);
        // Encode the shorts
        writer.encodeAudio(audioStreamIndex, audioShortsBuffer, (long)(getFrameTime()*1e6), TimeUnit.MICROSECONDS);
        // Return true to indicate that we're not EOF
        return true;
    }
    
    @Override
    public void onClose() throws IOException {
        // If the audioTranscoder is not null and is open, close it
        if (audioInputStream != null)
            audioInputStream.close();
        // If the writer is open, close it
        if (writer.isOpen())
            writer.close();
    }
}
