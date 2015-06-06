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
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IAudioSamplesEvent;
import com.xuggle.xuggler.IAudioResampler;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import java.io.IOException;
import static java.util.Arrays.stream;

/**
 * {@coder AudioDecoder} uses Xuggler and FFmpeg to decode an audio file into
 * PCM samples.
 * 
 * @author Jeremiah N. Hankins
 */
public class AudioDecoder {
//    
//    /**
//     * The audio file reader and decoder.
//     */
//    private final IMediaReader reader;
//    
//    /**
//     * The number of channels.
//     */
//    private final int numChannels;
//    
//    /**
//     * The sample rate in Hz.
//     */
//    private final int sampleRate;
//    
//    /**
//     * The audio resampler.
//     */
//    private final IAudioResampler resampler;
//    
//    /**
//     * The index of the first audio stream found in the file.
//     */
//    private final int audioStreamIndex;
//    
//    /**
//     * Constructs a new {@code AudioDecoder} for the specified file.
//     * 
//     * @param audioFileName the path to the audio file
//     * @param numChannels
//     * @param sampleRate
//     * @throws IllegalArgumentException if {@code audioFileName} is {@code null} or empty
//     * @throws IllegalArgumentException if {@code numChannels} is not in range [1,inf)
//     * @throws IllegalArgumentException if {@code sampleRate} is not in range [1,inf)
//     * @throws AudioStreamNotFoundException if the specified file does not contain an audio stream
//     */
//    public AudioDecoder(String audioFileName, int numChannels, int sampleRate) throws AudioStreamNotFoundException {
//        if (audioFileName == null || audioFileName.isEmpty())
//            throw new IllegalArgumentException("fileName cannot be null or empty: "+audioFileName);
//        if (numChannels < 1)
//            throw new IllegalArgumentException("numChannels is not in range [1,inf): "+numChannels);
//        if (sampleRate < 1)
//            throw new IllegalArgumentException("sampleRate is not in range [1,inf): "+sampleRate);
//
//        // Create the media reader for the file
//        IMediaReader audioReader = ToolFactory.makeReader(audioFileName);
//        // Get the container from the reader
//        IContainer container = audioReader.getContainer();
//        // Get the audio stream from the container
//        IStream audioStream = AudioDecoder.getAudioStream(container);
//        // If no audio stream was found, throw an exception
//        if (audioStream == null)
//            throw new AudioStreamNotFoundException("file does not contain an audio stream: "+audioFileName);
//        // Get the stream's encoder
//        IStreamCoder streamCoder = audioStream.getStreamCoder();
//        // Get the number of channels in the input stream
//        int inputChannels = streamCoder.getChannels();
//        // Set the number of output channels to be the inputChannels and numChannels
//        this.numChannels = Math.min(inputChannels, numChannels);
//        // Store the output sample rate
//        this.sampleRate = sampleRate;
//        // Create the resampler
//        resampler = IAudioResampler.make(
//                numChannels,                    // Output channels
//                inputChannels,                  // Input channels
//                sampleRate,                     // Output sample rate
//                streamCoder.getSampleRate(),    // Input sample rate
//                IAudioSamples.Format.FMT_FLT,   // Output format (floats)
//                streamCoder.getSampleFormat()); // Input format
//        
//        reader.addListener(new MediaListenerAdapter() {
//            @Override
//            public void onAudioSamples(IAudioSamplesEvent audioSamplesEvent) {
//                // If its  not the right stream index, skip this event
//                if (audioSamplesEvent.getStreamIndex() != audioStreamIndex)
//                    return;
//                // Get the audio samples from the event
//                IAudioSamples inSamples = audioSamplesEvent.getAudioSamples();
//                
//                
//                resampler.getMinimumNumSamplesRequiredInOutputSamples(inSamples)
//                
//                //
//                audioSamples.
//                audioSamples.g
//            }
//        });
//    }
//    
//    /**
//     * Returns the number of channels in the output.
//     * 
//     * @return the number of channels
//     */
//    public int getNumChannels() {
//        return numChannels;
//    }
//    
//    /**
//     * Returns the sample rate of the output.
//     * 
//     * @return the sample rate in Hz
//     */
//    public int getSampleRate() {
//        return sampleRate;
//    }
//    
//    /**
//     * 
//     * @return 
//     */
//    public int getSamples(float[]  ) {
//        
//    }
//    
//    private class StreamReader extends MediaListenerAdapter {
//        private IAudioSamples outSamples;
//        
//        @Override
//        public void onAudioSamples(IAudioSamplesEvent audioSamplesEvent) {
//            // If its  not the right stream index, skip this event
//            if (audioSamplesEvent.getStreamIndex() != audioStreamIndex)
//                return;
//            // Get the audio samples from the event
//            IAudioSamples inSamples = audioSamplesEvent.getAudioSamples();
//            // Get the number of input samples
//            long numInSamples = inSamples.getNumSamples();
//            // Determine how many output samples are needed to decode the input samples
//            int numOutSamples = resampler.getMinimumNumSamplesRequiredInOutputSamples(inSamples);
//            //
//            if (outSamples == null || outSamples.getMaxSamples() < numOutSamples)
//                outSamples = IAudioSamples.make(numOutSamples, numChannels, IAudioSamples.Format.FMT_FLT);
//            
//            resampler.resample(outSamples, inSamples, numInSamples);
//            
//            audioSamples.
//            audioSamples.g
//        }
//    }
    
    
    
    /**
     * Returns the first (lowest index) {@code IStream} in the specified
     * {@code IContainer} or {@code null}.
     * 
     * @param container the media container
     * @return the first audio stream
     */
    public static IStream getAudioStream(IContainer container) {
        if (container == null)
            throw new IllegalArgumentException("container cannot be null");
        // Search the container for an audio stream
        for (int streamIndex=0; streamIndex<container.getNumStreams(); streamIndex++) {
            // Get a reference to the stream
            IStream stream = container.getStream(streamIndex);
            // Get the stream's coder
            IStreamCoder streamCoder = stream.getStreamCoder();
            // If this is an audio stream...
            if (streamCoder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                // Return the stream
                return stream;
            }
        }
        // No audio stream was found, return null
        return null;
    }
}
