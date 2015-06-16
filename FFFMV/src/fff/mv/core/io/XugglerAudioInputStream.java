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

package fff.mv.core.io;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioResampler;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * {@code XugglerAudioInputStream} provides the means to stream audio file as an
 * {@link AudioInputStream}. Internally, this class relies on the Xuggler
 * library which is a Java wrapper for FFMPEG. It attempts to open a file for
 * reading as a media file and locate an audio stream within it. If the file
 * contains more than one audio stream, then the audio stream with the lowest
 * index is used.
 * <p>
 * {@code XugglerAudioInputStream} objects are instantiated using the static
 * {@code open()} methods. In addition to taking a file,
 * {@code XugglerAudioInputStream} also optionally takes the number of channels,
 * the sample rate, and the sample encoding format to use for the resulting
 * stream as parameters. If any of these parameters is left {@code null}, then
 * default values will are chosen.
 * 
 * @author Jeremiah N. Hankins
 */
public class XugglerAudioInputStream extends AudioInputStream {
    
    /**
     * The input stream backing this object.
     */
    private final InnerInputStream inStream;
    
    /**
     * Constructs a new {@code XugglerAudioInputStream}.
     * 
     * @param inStream the {@code InputStream} backing this object
     * @param format the {@code AudioFormat} of the stream
     * @param length the number of bytes in the stream
     */
    XugglerAudioInputStream(InnerInputStream inStream, AudioFormat format, long length) {
        super(inStream, format, length);
        this.inStream = inStream;
    }
    
    /**
     * Returns the {@link Encoding} of the stream.
     * 
     * @return the {@code Encoding} of the stream
     */
    public Encoding getEncoding() {
        return inStream.encoding;
    }
    
    /**
     * Sets the position of the audio stream to the specified time.
     * 
     * @param time the time to seek to in seconds
     */
    public void seek(double time) {
        inStream.seek(time);
    }
    
    /**
     * Constructs a new {@code XugglerAudioInputStream} for the specified file.
     * <br>
     * <pre>This method is equivalent to calling:{@code
     *   open(file, null, null, null)}</pre>
     * 
     * @param file the file to open and stream
     * @return the audio stream
     * @throws IllegalArgumentException if {@code file} is {@code null}
     * @throws XugglerErrorException if there was an error opening the file
     * @see #open(File, Integer, Integer, XugglerAudioInputStream.Encoding) 
     */
    public static XugglerAudioInputStream open(File file) throws XugglerErrorException {
        return open(file, null, null, null);
    }
    
    /**
     * Constructs a new {@code XugglerAudioInputStream} for the specified file
     * using the specified number of channels, sample rate, and sampler encoding
     * format and will skip to the specified time. If {@code channels},
     * {@code sampleRate}, or {@code encoding} is {@code null}, then default
     * values will be chosen for the {@code null} value.
     * 
     * @param file the input file
     * @param channels the number of channels for the output or {@code null} to use the default value
     * @param sampleRate the sample rate in Hz for the output or {@code null} to use the default value
     * @param encoding the sample encoding format for the output or {@code null} to use the default value
     * @return the audio stream
     * @throws IllegalArgumentException if {@code file} is {@code null}
     * @throws IllegalArgumentException if {@code channels} is not {@code null} or positive
     * @throws IllegalArgumentException if {@code sampleRate} is not {@code null} or positive
     * @throws XugglerErrorException if there was an error opening the file
     */
    public static XugglerAudioInputStream open(File file, Integer channels, Integer sampleRate, Encoding encoding) throws XugglerErrorException {
        // Create the input stream
        InnerInputStream inStream = new InnerInputStream(file, channels, sampleRate, encoding);
        // Create and return the XugglerAudioInputStream
        return new XugglerAudioInputStream(inStream, inStream.format, inStream.frameLength);
    }
    
    /**
     * {@code Encoding} enumerates available samples encoding formats available
     * to {@code XugglerAudioInputStream}.
     */
    public static enum Encoding {
        /**
         * 4 byte signed floating point
         */
        FLT(IAudioSamples.Format.FMT_FLT, AudioFormat.Encoding.PCM_FLOAT,    4, "Float"),
        
        /**
         * 8 byte signed floating point
         */
        DBL(IAudioSamples.Format.FMT_DBL, AudioFormat.Encoding.PCM_FLOAT,    8, "Double"),
        
        /**
         * 8-bit unsigned PCM
         */
        U8 (IAudioSamples.Format.FMT_U8,  AudioFormat.Encoding.PCM_UNSIGNED, 1, "Unsigned 8-bit"),
        
        /**
         * 16-bit signed PCM
         */
        S16(IAudioSamples.Format.FMT_S16, AudioFormat.Encoding.PCM_SIGNED,   2, "Signed 16-bit"),
        
        /**
         * 32-bit signed PCM
         */
        S32(IAudioSamples.Format.FMT_S32, AudioFormat.Encoding.PCM_SIGNED,   4, "Signed 32-bit");
        
        private final IAudioSamples.Format xFormat;
        private final AudioFormat.Encoding jEncoding;
        private final int bytes;
        private final String desc;
        
        private Encoding(
                IAudioSamples.Format xFormat, 
                AudioFormat.Encoding jEncoding, 
                int bytes, 
                String desc) {
            this.xFormat = xFormat;
            this.jEncoding = jEncoding;
            this.bytes = bytes;
            this.desc = desc;
        }
        
        /**
         * Returns the {@link IAudioSamples.Format} for this {@code Encoding}.
         * 
         * @return the {@code IAudioSamples.Format} for this {@code Encoding}
         */
        public IAudioSamples.Format getXugglerFormat() {
            return xFormat;
        }
        
        /**
         * Returns the {@link AudioFormat.Encoding} for this {@code Encoding}.
         * 
         * @return the {@code AudioFormat.Encoding} for this {@code Encoding}
         */
        public AudioFormat.Encoding getJavaEncoding() {
            return jEncoding;
        }
        
        /**
         * Returns the number of bytes (sets of 8 bits) per sample for this 
         * {@code Encoding}.
         * 
         * @return the number of bytes per sample for this {@code Encoding}
         */
        public int getBytes() {
            return bytes;
        }
        
        /**
         * Returns a {@code String} description of this {@code Encoding}
         * 
         * @return a {@code String} description of this {@code Encoding}
         */
        public String getDescription() {
            return desc;
        }
        
        /**
         * Returns the {@code Encoding} for the specified 
         * {@link IAudioSamples.Format} or {@code null} if there is no 
         * {@code Encoding} for the specified {@code IAudioSamples.Format}.
         * 
         * @param format the {@code IAudioSamples.Format}
         * @return the {@code Encoding} or {@code null} if no suitable {@code Encoding} is found
         */
        public static Encoding getEncoding(IAudioSamples.Format format) {
            switch (format) {
                case FMT_FLT: return FLT;
                case FMT_DBL: return DBL;
                case FMT_U8:  return U8;
                case FMT_S16: return S16;
                case FMT_S32: return S32;
            }
            return null;
        }
    }
    
    /**
     * The {@code InputStream} backing a {@code XugglerAudioInputStream}.
     */
    static class InnerInputStream extends InputStream {
        /**
         * The input file.
         */
        final File file;
        
        /**
         * The container for the media in the file.
         */
        final IContainer container;
        
        /**
         * The index for the first audio stream found in the container.
         */
        final int streamIndex;
        
        /**
         * The decoder for the audio stream.
         */
        final IStreamCoder audioCoder;
        
        /**
         * The audio resampler. If {@code null} resampling is not needed.
         */
        final IAudioResampler resampler;
        
        /**
         * The {@link Encoding} of the stream.
         */
        final Encoding encoding;
        
        /**
         * The {@link AudioFormat} of the stream.
         */
        final AudioFormat format;
        
        /**
         * The number of frames in the stream or {@code -1} if the duration of
         * the stream is unknown.
         */
        final long frameLength;
        
        /**
         * The last packet read from the container.
         */
        final IPacket packet;
        
        /**
         * The current byte offset within the packet.
         */
        int packetOffset;
        
        /**
         * The last set of decoded samples from the stream. These samples have
         * not been resampled. This is an alias of {@link #samples} if 
         * {@link #resampler} is {@code null}.
         */
        final IAudioSamples inSamples;
        
        /**
         * The last set of decoded samples from the stream. These samples have
         * been resampled. This is an alias of {@link #inSamples} if 
         * {@link #resampler} is {@code null}.
         */
        IAudioSamples samples;
                
        /**
         * The last set of decoded samples from the stream.
         */
        IBuffer sampleBuffer;
        
        /**
         * The number of bytes read from the current set of decoded samples.
         */
        int sampleBufferOffset;
        
        /**
         * The EOF flag.
         */
        boolean eof;
        
        /**
         * Constructs a new {@code InnerInputStream} which uses Xuggler and 
         * FFMPEG to decode the specified file into a byte stream with the
         * requested number of channels, sample rate, and sample encoding.
         * 
         * @param file the input file
         * @param channels the number of channels for the output or {@code null} to use the default value
         * @param sampleRate the sample rate in Hz for the output or {@code null} to use the default value
         * @param encoding the sample encoding format for the output or {@code null} to use the default value
         * @throws IllegalArgumentException if {@code file} is {@code null}
         * @throws IllegalArgumentException if {@code channels} is not positive
         * @throws IllegalArgumentException if {@code sampleRate} is not positive
         * @throws XugglerErrorException if there was an error opening the file
         * @throws XugglerStreamNotFoundException if an audio stream could not be located within the file
         */
        InnerInputStream(File file, Integer channels, Integer sampleRate, Encoding encoding) throws XugglerErrorException {
            if (file == null)
                throw new IllegalArgumentException("file cannot be null");
            if (channels != null && channels < 1)
                throw new IllegalArgumentException("channels is out of range [1,inf): "+channels);
            if (sampleRate != null && sampleRate < 1)
                throw new IllegalArgumentException("sampleRate is out of range [1,inf): "+sampleRate);
            
            // Store the file name
            this.file = file;
            
            // Create the container that will hold the input media file
            container = IContainer.make();
            
            // Open the input file for reading
            int errorNo = container.open(file.getPath(), IContainer.Type.READ, null);
            if (errorNo < 0)
                throw new XugglerErrorException(errorNo, "Error opening file: "+file.getPath());
            
            // Get the index of the audio
            IStream audioStream = getAudioStream(container);
            if (audioStream == null)
                throw new XugglerErrorException(IError.make(IError.Type.ERROR_UNKNOWN), "Could not an find audio stream in file: "+file.getPath());
            
            // Store the stream index and coder
            streamIndex = audioStream.getIndex();
            audioCoder  = audioStream.getStreamCoder();
            
            // Open the audio coder
            errorNo = audioCoder.open(null, null);
            if (errorNo < 0)
                throw new XugglerErrorException(errorNo, "Error opening file: "+file.getPath());
            
            // Get the number of channels, sample rate, and format for the input
            int inChannels   = audioCoder.getChannels();
            int inSampleRate = audioCoder.getSampleRate();
            IAudioSamples.Format inType = audioCoder.getSampleFormat();
            
            // Determine the number of channels, sample rate, and format for the
            // output. By default the 
            int outChannels = inChannels;
            if (channels != null) outChannels = channels;
            int outSampleRate = inSampleRate;
            if (sampleRate != null) outSampleRate = sampleRate;
            if (encoding == null) encoding = Encoding.getEncoding(inType);
            if (encoding == null) encoding = Encoding.S16;
            this.encoding = encoding;
            IAudioSamples.Format outType = encoding.getXugglerFormat();
            
            // Create the resampler if one is needed
            resampler = (inChannels != outChannels || inSampleRate != outSampleRate || inType != outType)?
                    IAudioResampler.make(outChannels, inChannels, outSampleRate, inSampleRate, outType, inType) :
                    null;
                    
            // Create the AudioFormat for the stream
            format = new AudioFormat(
                    encoding.getJavaEncoding(),      // encoding
                    outSampleRate,                   // sampleRate
                    encoding.getBytes()*8,           // sampleSizeInBits
                    outChannels,                     // channels
                    outChannels*encoding.getBytes(), // frameSize
                    outSampleRate,                   // frameRate
                    false);                          // bigEndian
            
            // Try to estimate the length of the stream in bytes
            long duration = audioStream.getDuration();
            if (duration == Global.NO_PTS) {
                frameLength = AudioSystem.NOT_SPECIFIED;
            } else {
                double base = audioStream.getTimeBase().getDouble();
                frameLength = (long)Math.ceil(duration*base*outSampleRate);
            }
            
            // Initialize the packet
            packet = IPacket.make();
            packetOffset = Integer.MAX_VALUE;
            
            // Initialize the sample buffers
            samples = IAudioSamples.make(1024, audioCoder.getChannels());
            sampleBuffer = null;
            sampleBufferOffset = Integer.MAX_VALUE;
            if (resampler == null) {
                inSamples = samples;
            } else {
                inSamples = IAudioSamples.make(1024, outChannels);
            }
            
            // Set the EOF flag to false
            eof = false;
        }
        
        /**
         * If the current sample buffer is empty or all of its data has been
         * consumed, then this method will read from the stream, decode the
         * audio and fill the sample buffer. Returns {@code true} if the 
         * operation is successful and {@code false} if EOF is reached.
         * 
         * @return {@code false} if EOF is reached, otherwise {@code true}
         * @throws IOException if there was an error reading the stream
         */
        boolean getSamples() throws IOException {
            // If we've already hit EOF, return false to indicate EOF
            if (eof)
                return false;
            // If the sample buffer is null or we have consumed all of the
            // samples in the buffer...
            if (sampleBuffer == null || sampleBufferOffset >= samples.getSize()) {
                // Keep looping until we have some samples or reach EOF
                mainLoop: while (true) {
                    // If we have consumed the entire packet...
                    if (packetOffset >= packet.getSize()) {
                        do {
                            // Read the next packet from the stream. If the 
                            // return values is negative, then we're at EOF.
                            if (container.readNextPacket(packet) < 0) {
                                // Set the EOF flag
                                eof = true;
                                // Return false to indicate EOF
                                return false;
                            }
                            // If the packet was not from the correct stream, 
                            // loop back and decode annother packet untill we 
                            // find one from the correct stream or reach EOF
                        } while (packet.getStreamIndex() != streamIndex);
                        // At this point, we have a fresh packet from the 
                        // correct stram and are not EOF, so reset the packet
                        // offset
                        packetOffset = 0;
                    }
                    // Because the  packet may contain multiple sets of samples 
                    // (frames) and also because the decoder might consume some
                    // packet data without decoding enough samples, we need to
                    // keep looping until either we have decoded some samples or
                    // we have consumed the entire packet.
                    while (packetOffset < packet.getSize()) {
                        // Decode the next chunk of the packet and throw an
                        // exception if an error occurs
                        int bytesDecoded = audioCoder.decodeAudio(inSamples, packet, packetOffset);
                        if (bytesDecoded < 0) {
                            System.err.println("Error encountered while decoding audio in file: "+file.getPath());
                            packetOffset = packet.getSize();
                            break;
//                            throw new IOException("Error encountered while decoding audio in file: "+file.getPath());
                        }
                        packetOffset += bytesDecoded;
                        // If the input samples buffer is full...
                        if (inSamples.isComplete()) {
                            // If we're using a resampler...
                            // Note: if we're not using a resampler then 
                            // 'inSamples' is an alias for 'samples', so
                            // nothing more needs to be done
                            if (resampler != null) {
                                // Figure out how many samples will be generated
                                long numSamples = resampler.getMinimumNumSamplesRequiredInOutputSamples(inSamples);
                                // Ensure the output buffer is large enough
                                if(samples.getMaxSamples() < numSamples)
                                    samples = IAudioSamples.make(numSamples, resampler.getOutputChannels(), resampler.getOutputFormat());
                                // Resample 
                                resampler.resample(samples, inSamples, numSamples);
                            }
                            // Get the underlying buffer for the samples
                            sampleBuffer = samples.getData();
                            // Set the sample buffer offset to 0
                            sampleBufferOffset = 0;
                            // We're done!
                            break mainLoop;
                        }
                    }
                    // If we get to this point, we've just finished decoding
                    // the end of a packet, but did not fill the sample buffer,
                    // so loop back arround and get a new packet and try again
                }
            }
            // Return the sample buffer
            return true;
        }

        @Override
        public int available() throws IOException {
            if (frameLength != AudioSystem.NOT_SPECIFIED) {
                long bytes = frameLength*format.getFrameSize();
                return (int)Math.min(bytes, Integer.MAX_VALUE);
            }
            if (!getSamples())
                return -1;
            return samples.getSize() - sampleBufferOffset;
        }
        
        @Override
        public int read() throws IOException {
            // If there are no samples, we've hit EOF
            if (!getSamples())
                return -1;
            // Create a 1 byte array to hold our result
            byte[] ret = new byte[1];
            // Read out the next byte
            sampleBuffer.get(sampleBufferOffset, ret, 0, 1);
            // Increment the sample offset
            sampleBufferOffset++;
            // Return the sample
            return ret[0];
        }
        
        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            if (bytes == null)
                throw new NullPointerException("bytes");
            if (off < 0 || len < 0 || len > bytes.length - off)
                throw new IndexOutOfBoundsException();
            if (eof)
                return -1;
            if (len == 0)
                return 0;
            // Keep track of how many bytes we've read so far
            int read = 0;
            // While we have not yea read the requested number of bytes and EOF
            // has not been reached....
            while (read < len && getSamples()) {
                // Cacluate the number of bytes to read from the buffer by 
                // taking minimum value of the number of unread bytes in the
                // buffer and the number of bytes that still need to be read
                // to complete this operation.
                int size = Math.min(samples.getSize()-sampleBufferOffset, len-read);
                // Copy the bytes from the buffer
                sampleBuffer.get(sampleBufferOffset, bytes, off+read, size);
                // Update the number of bytes that we've read and the number of
                // unread bytes in the sample buffer
                read += size;
                sampleBufferOffset += size;
            }
            // If we did not read any bytes, return -1 to singal EOF
            if (read == 0)
                return -1;
            // Otherwise return the number of bytes read
            return read;
        }
        
        @Override
        public long skip(long n) throws IOException {
            if (eof || n <= 0)
                return 0;
            // Keep track of the number of bytes skipped
            long skipped = 0;
            // While we havn't skipped enough bytes and we're not EOF
            while (skipped < n && getSamples()) {
                // Skip as many bytes as we need still need to skip, or the rest
                // of the current buffer, which ever is smaller.
                long skip = Math.min(samples.getSize()-sampleBufferOffset, n-skipped);
                // Update the number of bytes we've skipped and the number of
                // unused bytes in the sample buffer
                skipped += skip;
                sampleBufferOffset += skip;
            }
            // Return the number of bytes skipped
            return skipped;
        }
        
        @Override
        public void close() throws IOException {
            audioCoder.close();
            container.close();
        }
        
        /**
         * Seeks to the first "key frame" in the audio stream <i>after</i> the
         * specified time.
         * <p>
         * This method assumes the audio stream starts at timestamp 0 and will
         * not work properly if the stream begins with some other timestamp.
         * 
         * @param time the absolute time ti seek to in seconds
         */
        void seek(double time) {
            // Get the audio stream
            IStream audioStream = container.getStream(streamIndex);
            // Get the stream's time base
            IRational timebase = audioStream.getTimeBase();
            // Convert the time in seconds into a timestamp
            long timestamp = (long)(time*timebase.getDouble());
            // Seek to that timestamp
            container.seekKeyFrame(streamIndex, timestamp, 0);      
        }
        
        /**
         * Returns the {@code IStream} in the specified {@code IContainer} which has
         * the lowest index and uses an audio codec, or {@code null} if the
         * container does not contain an audio stream.
         * 
         * @param container the media container
         * @return the first audio stream
         */
        static IStream getAudioStream(IContainer container) {
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
    
    public static void main(String[] args) throws XugglerErrorException, LineUnavailableException, IOException {
//        File file = new File("E:\\Music\\Nero\\[2011] Welcome Reality\\Nero - Welcome Reality - 07 - Innocence.flac");
//        File file = new File("E:\\Music\\Essential Mix\\Essential Mix (2008-04-12) Pete Tong & Martin Doorly.mp3");
        File file = new File("E:\\Music\\Essential Mix\\Essential Mix (2011-04-16) Alex Metric.mp3");
        
        AudioInputStream stream = XugglerAudioInputStream.open(file);
        AudioFormat format = stream.getFormat();
        
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine mLine = (SourceDataLine)AudioSystem.getLine(info);
        mLine.open(format);
        mLine.start();
        int len;
        byte[] buffer = new byte[1024*4];
        while ((len = stream.read(buffer)) >= 0)
            mLine.write(buffer, 0, len);
        mLine.drain();
    }
}