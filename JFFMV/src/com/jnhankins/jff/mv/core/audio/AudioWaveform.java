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

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

/**
 * This class draws waveforms for {@code AudioData} using RMS and FFT data. The
 * generated image is not a waveform in the truest sense because it does not use
 * individual PCM data points from the original audio signal. Instead the a
 * waveform image is generated using the relative height for each frame, which
 * indicates the relative volumes of each frame. To further accentuate the
 * relative difference in volume, only top half of the waveform is displayed.
 * Additionally, a color is assigned to each to each frame based on the FFT
 * results to provide some indication of the overall tone or pitch contained in
 * the portion of the audio signal contained in the frame.
 * 
 * @author Jeremiah N. Hankins
 */
public class AudioWaveform {
    /**
     * Approximate maximum number of bytes to use for the entire pixel-cache 
     * before attempting to reduce the size of the pixel cache by discarding old
     * data using a crude least-recently-used (LRU) caching algorithm.
     * This value applies only to the pixel-cache and does not apply to the
     * frame-cache or color-cache.
     */
    private final long maxPixelCacheSize = 100*1024*1024; // 100 MB
    
    /**
     * The maximum tolerance used when retrieving data from the pixel cache
     * based on the required scale. If the pixel cache contains data such that
     * the absolute difference between 
     */
    private final double maxScaleError = 1e-6; // one ten thousandth of 1%
    
    /**
     * The {@code AudioData} used go generate the cache.
     */
    private AudioData audioData;
    
    /**
     * An array containing the color assigned to each frequency bin. Colors are
     * represented as a float array of length 3 containing the red, green, and
     * blue color values .
     */
    private float[][] binColorCache;
    
    /**
     * An array containing the weight of each frequency bin. Used to calculate
     * the color of a frame by taking the weighted average of the FFT bin colors
     * using the FFT bin's magnitude for the frame multiplied by the bin's
     * weight.
     */
    private float[] binWeight;
    
    /**
     * A vector containing the cached frame data.
     */
    private final Vector frameDataCache;
    
    /**
     * A list of vectors containing cached pixel data for various scales. This
     * list is ordered in the natural that arrises as vectors are added and
     * removed and is otherwise unordered. Vectors are always added to the end
     * of this list. The scale of each cache in this list is stored in
     * {@link #pixelDataArrayScales}, such that the scale for
     * {@code pixelDataArrays.get(i)} is given by
     * {@code pixelDataArrayScales.get(i)}.
     */
    private final List<Vector> pixelDataArrays;
    
    /**
     * A list containing the scales of the pixel caches stored in
     * {@link #pixelDataArrays}.
     */
    private final List<Double> pixelDataArrayScales;
    
    /**
     * A sorted list containing the same vector pixel caches found in 
     * {@code pixelDataArrays}. The pixel caches are sorted such that the most
     * recently used cache appears at the end of this list, and the cache that
     * has not been used for the greatest period of time appears at the 
     * beginning of this list. Its used to implement a crude least-recently-used
     * (LRU) caching algorithm for pixel data.
     */
    private final List<Vector> pixelDataArrayUseOrder;
    
    /**
     * A vector containing the pixel cache that is currently in use.
     */
    private Vector pixelDataArray;
    
    /**
     * An estimate of the number of bytes currently being used by the pixel 
     * cache.
     */
    private int pixelCacheSize;
    
    /**
     * A small buffer used by {@link #calcPixelData(double, int)} to convert the
     * pixel color from RGB into HSB and back. This is done so that saturation
     * and brightness can be increased while in the HSB format while maintaining
     * a constant hue.
     */
    private float[] hsbBuffer;
    
    /**
     * The following variables are used for debugging the caches.
     */
    private final boolean DEBUG_PRINT = false;
    private long frameDataArrayCacheHit  = 0;
    private long frameDataArrayCacheMiss = 0;
    private long frameDataCacheHit  = 0;
    private long frameDataCacheMiss = 0;
    private long pixelDataArrayCacheHit  = 0;
    private long pixelDataArrayCacheMiss = 0;
    private long pixelDataCacheHit  = 0;
    private long pixelDataCacheMiss = 0;
    
    /**
     * Constructs a new {@code AudioWaveform}.
     */
    public AudioWaveform() {
        frameDataCache = new Vector();
        pixelDataArrays = new ArrayList();
        pixelDataArrayScales = new ArrayList();
        pixelDataArrayUseOrder = new ArrayList();
        pixelCacheSize = 0;
    }
    
    public void drawWaveform(Graphics g, AudioData audioData, int x0, int y0, int dx, int dy, double t0, double t1) {
        if (audioData == null || audioData.getFrameCount() < 1)
            return;
        
        // Calcualte the total elapsed time
        double dt = t1 - t0;
        // Calculate the time per x-axis pixel
        double dtdx = (float)(dt/dx);
        
        // Initialize the constant bin color cache
        initBinColors(audioData);
        // Initialize the frame data cache
        initFrameData(audioData);
        // Initialize the pixel data cache and get the scale that will be used
        dtdx = initPixelData(audioData, dtdx);
        
        // Store the audio data was used to initialize the caches
        this.audioData = audioData;
        // Store whether or not the audio is done processing
        // Store the number of frames
        int frameCount = audioData.getFrameCount();
        // Store the maximum RMS value
        float rmsMax = audioData.getRMSMax();
        // Store the frame rate
        double frameRate = audioData.getFrameRate();
        
        // Get the minimum pixel index (inclusive)
        int minPx = (int)(t0/dtdx);
        // Get the maximum pixel index (inclusive)
        int maxPx = (int)(frameCount/(frameRate*dtdx));
        // Get the maximum frame index in the maximum pixel index
        int maxFrame = (int)Math.ceil(frameRate*dtdx*(maxPx+1));
        // If the maximum frame index is too large...
        if (maxFrame > frameCount) {
            // ... then reduece the maximum pixel index
            maxPx--;
            // and recalculate the maximum frame index
            maxFrame = (int)Math.ceil(frameRate*dtdx*(maxPx+1));
        }
        
        // Ensure the frame data cache is large enough
        sizeFrameData(maxFrame+1);
        // Ensure the pixel data cache is large enough
        sizePixelData(maxPx);
        
        // Precalculate the y value at the bottom of the waveform
        int y = y0+dy/2;
        
        // Keep track of the pixel index
        int px = minPx;
        // Scan along each image pixel on the x-axis
        for (int x=0; x<dx; x++) {
            // If the pixel is visible...
            if (0 <= px && px <= maxPx) {
                // If the required frame data is not in the cache
                // calculate the frame data and cache it now
                readyPixelData(dtdx, x);
                // Get the height from the pixel data
                float height = pixelDataArray.getHeight(px);
                // Cacluate the height of the line in pixels
                int h = (int)(Math.pow(height/rmsMax, 2)*dy)/2;
                // Get the color
                int color = pixelDataArray.getColor(px);
                // Set the color of the line
                g.setColor(new Color(color));
                // Draw the line
                g.drawLine(x0+x, y+h, x0+x, y-h);
            }
            // Increment the pixel index
            px++;
        }
        
        if (DEBUG_PRINT) {
            System.out.println("frameDataArrayCacheHit:  " + frameDataArrayCacheHit);
            System.out.println("frameDataArrayCacheMiss: " + frameDataArrayCacheMiss);
            System.out.println("frameDataArrayCache %:   " + (frameDataArrayCacheHit/(double)(frameDataArrayCacheHit+frameDataArrayCacheMiss)));
            System.out.println("frameDataCacheHit:  " + frameDataCacheHit);
            System.out.println("frameDataCacheMiss: " + frameDataCacheMiss);
            System.out.println("frameDataCache %:   " + (frameDataCacheHit/(double)(frameDataCacheHit+frameDataCacheMiss)));
            System.out.println("pixelDataArrayCacheHit:  " + pixelDataArrayCacheHit);
            System.out.println("pixelDataArrayCacheMiss: " + pixelDataArrayCacheMiss);
            System.out.println("pixelDataArrayCache %:   " + (pixelDataArrayCacheHit/(double)(pixelDataArrayCacheHit+pixelDataArrayCacheMiss)));
            System.out.println("pixelDataCacheHit:  " + pixelDataCacheHit);
            System.out.println("pixelDataCacheMiss: " + pixelDataCacheMiss);
            System.out.println("pixelDataCache %:   " + (pixelDataCacheHit/(double)(pixelDataCacheHit+pixelDataCacheMiss)));
            System.out.println();
        }
    }
    
    private void initBinColors(AudioData audioData) {
        // IF the bin color cache has not been initialized OR the audio data
        // has changed THEN initialize the color cache
        if (binColorCache == null || this.audioData != audioData) {
            // Then generate a new colors cache for the correct number of bins
            binColorCache = calcBinColors(audioData);
        }
    }
    private float[][] calcBinColors(AudioData audioData) {
        // Get the number of bins
        int numBins = audioData.getFFTBinCount();
        // Get the resolution of the bins
        double freqRes = audioData.getFFTBinResolution();
        // Allocate space for the colors
        binColorCache = new float[numBins][];
        binWeight = new float[numBins];
        // Generate the colors for each bin
        double totWidth = 0;
        for (int bin=0; bin<numBins; bin++) {
            binColorCache[bin] = calcBinColor(audioData, bin);
            float freq = audioData.getFFTBinFrequency(bin);
            binWeight[bin] = freqLogScale(freq + freqRes) - freqLogScale(freq);
            totWidth += binWeight[bin];
        }
        // Normalize the bin width vector
        for (int bin=0; bin<numBins; bin++)
            binWeight[bin] /= totWidth;
        // Return the colors
        return binColorCache;
    }
    private float[] calcBinColor(AudioData audioData, int bin) {
        // Allocate space for the color
        float[] color = new float[3];
        // Convert the frequency into a hue using magic numbers discoverd heuristically
        float freq = audioData.getFFTBinFrequency(bin);
        float binLog = (float)Math.min(Math.max((Math.log10(1+freq)-1.75), 0), 1);
        float binHue = (float)((binLog*2/3.0) % 1.0);
        // Convert the hue into RGB
        int binRGB = Color.HSBtoRGB(binHue, 1.0f, 1.0f);
        // Extract and store the seperate R, G, and B values as doubles
        float R = ((binRGB>>16) & 0xFF) / 255.0f;
        float G = ((binRGB>> 8) & 0xFF) / 255.0f;
        float B = ((binRGB    ) & 0xFF) / 255.0f;
        // Normalize the RGB color vector (just because the output looks better)
        double T = R+G+B;
        R /= T;
        G /= T;
        B /= T;
        // Store the color
        color[0] = R;
        color[1] = G;
        color[2] = B;
        // Return the color
        return color;
    }
    private float freqLogScale(double freq) {
        return (float)((Math.log10(Math.min(Math.max(freq, 20), 20000)*0.5)-1)/3);
//        return (float)(((Math.log(freq*2)/Math.log(20))-1)/3);
//        double max = audioData.getFreqBinFrequency(audioData.getFreqBinCount()-1);
//        double min = audioData.getFreqBinFrequency(0);
//        double a = 10/(4*min);
//        double b = Math.log10(a*max/10);
//        return (float)((Math.log10(Math.min(Math.max(freq, min), max)*a)-1.0)/b);
    }
    
    private double initPixelData(AudioData newAudioData, double dtdx) {
        // If the audio data is not the same as the audio data that was used the
        // last time a waveform was drawn, then clear the pixel data cache
        if (audioData != newAudioData) {
            pixelDataArrayUseOrder.clear();
            pixelDataArrayScales.clear();
            pixelDataArrays.clear();
            pixelCacheSize = 0;
        }
        // Search the cache for an array with a scale aproximatly equal to the
        // requested scale
        for (int s=pixelDataArrayScales.size()-1; s>=0; s--) {
            // Get the scale for the cached array
            double scale = pixelDataArrayScales.get(s);
            // If the scale for the cached array is aproximatly equal to the
            // requested scale...
            if (Math.abs(dtdx-scale)/dtdx < maxScaleError) {
                // Then use the cached array
                pixelDataArray = pixelDataArrays.get(s);
                // Move the array it to the end of the use order
                // Note: Useing a loop to find the index because 
                //  List.remove(Vector) invokes Vector.equals(Vector) which does 
                //  element-wise comparisons...
                for (int idx=pixelDataArrayUseOrder.size()-1; ; idx--) {
                    if (pixelDataArrayUseOrder.get(idx) == pixelDataArray) {
                        // If it's already at the end of the list, do nothing
                        if (idx != pixelDataArray.size()-1) {
                            pixelDataArrayUseOrder.remove(idx);
                            pixelDataArrayUseOrder.add(pixelDataArray);
                        }
                        break;
                    }
                }
                // And return the scale being used
                pixelDataArrayCacheHit++;
                return scale;
            }
        }
        // There are no cached arrays with scales aproximatly equal to the
        // requested scale, so create a new array to use
        pixelDataArray = new Vector();
        // Store the array and its scale in the cache
        pixelDataArrays.add(pixelDataArray);
        pixelDataArrayScales.add(dtdx);
        // Add the array to the end of the use order
        pixelDataArrayUseOrder.add(pixelDataArray);
        // Update the size of the cache
        pixelCacheSize += pixelDataArray.size()*8;
        // If the cache has become too large, then reduce its size
        reducePixelDataCacheSize();
        // Return the scale
        pixelDataArrayCacheMiss++;
        return dtdx;
    }
    private void sizePixelData(int maxPxIndex) {
        // Get the size of the current pixel data array
        int size = pixelDataArray.size();
        // If the array is not large enough
        if (size <= maxPxIndex) {
            // Get current capacity of the array
            int oldCapacity = pixelDataArray.size();
            // Enlarge the array
            pixelDataArray.setSize(maxPxIndex+1);
            // Get the new capacity of the array
            int newCapacity = pixelDataArray.size();
            // Update the total pixel data cache size
            pixelCacheSize += (newCapacity-oldCapacity)*8;
            // If the cache has become too large, then reduce its size
            reducePixelDataCacheSize();
        }
    }
    private void readyPixelData(double dtdx, int pxIndex){
        // If the pixel data is not in the cache...
        // Calculate the pixel data and cache it now
        if (!pixelDataArray.has(pxIndex)) {
            calcPixelData(dtdx, pxIndex);
            pixelDataCacheMiss++;
        } else {
            pixelDataCacheHit++;
        }
    }
    private void calcPixelData(double dtdx, int pxIndex) {
        // Get the frame rate
        double frameRate = audioData.getFrameRate();
        // Get the boundary times for the x-axis pixel
        double xt0 = dtdx*pxIndex;
        double xt1 = dtdx*(pxIndex+1);
        // Get the boundary frame numbers for the x-axis pixel
        int xf0 = (int)(frameRate*xt0); // inclusive
        int xf1 = (int)Math.ceil(frameRate*xt1); // exclusive
        // Keep track of the maximum heights
        float height = 0;
        // Keep track of the average frame color
        int colorR = 0;
        int colorG = 0;
        int colorB = 0;
        // Scan through the frames for this x-axis pixel
        for (int xf=xf0; xf<xf1; xf++) {
            // Get the frame data
            readyFrameData(xf);
            // Keep track of the maximum heights
            float frameHeight = frameDataCache.getHeight(xf);
            height = Math.max(frameHeight, height);
            // Keep track of the average frame color
            int color = frameDataCache.getColor(xf);
            colorR += (color >> 16) & 0xFF;
            colorG += (color >>  8) & 0xFF;
            colorB +=  color        & 0xFF;
        }
        // Determine the number of frames in this x-axis pixel
        int numFrames = xf1-xf0;
        // Keep track of the average frame color
        colorR /= numFrames;
        colorG /= numFrames;
        colorB /= numFrames;
        // Convert the RGB color into HSB, ensure the saturation and
        // brightness are atleast 0.75, convert from HSB back to RGB
        hsbBuffer = Color.RGBtoHSB(colorR, colorG, colorB, hsbBuffer);
        if (hsbBuffer[1] < 0.75f) hsbBuffer[1] = 0.75f;
        if (hsbBuffer[2] < 0.75f) hsbBuffer[2] = 0.75f;
        int rgb = Color.HSBtoRGB(hsbBuffer[0], hsbBuffer[1], hsbBuffer[2]);
        // Cache the pixel data
        pixelDataArray.set(pxIndex, rgb, height);
    }
    private void reducePixelDataCacheSize() {
        boolean print = DEBUG_PRINT && pixelCacheSize > maxPixelCacheSize;
        if (print) {
            System.out.println("maxSize: "+maxPixelCacheSize);
            System.out.println("oldSize: "+pixelCacheSize);
            System.out.println("old#Arr: "+pixelDataArrays.size());
        }
        // While the cache is too large and there is more than one cache entry
        while (pixelCacheSize > maxPixelCacheSize && pixelDataArrays.size() > 1) {
            // Get the array that has not been used for the greatest period of time
            Vector arrayToRemove = pixelDataArrayUseOrder.get(0);
            // Remove the array from the cache
            // Use a loop to find the index because List.remove(Vector) invokes
            // Vector.equals(Vector) which does element-wise comparisons...
            for (int idx=0; ; idx++) {
                if (pixelDataArrays.get(idx) == arrayToRemove) {
                    pixelDataArrays.remove(idx);
                    pixelDataArrayScales.remove(idx);
                    break;
                }
            }
            pixelDataArrayUseOrder.remove(0);
            // Update the size of the cache
            pixelCacheSize -= pixelDataArray.size()*8;
        }
        if (print) {
            System.out.println("newSize: "+pixelCacheSize);
            System.out.println("new#Arr: "+pixelDataArrays.size());
            System.out.println();
        }
    }
    
    private void initFrameData(AudioData newAudioData) {
        // If the audio data is not the same as the audio data that was used the
        // last time a waveform was drawn, then clear the frame data cache
        if (audioData != newAudioData) {
            frameDataCache.clear();
            frameDataArrayCacheMiss++;
        }
        else frameDataArrayCacheHit++;
    }
    private void sizeFrameData(int maxFrameIndex) {        
        // Make sure the cache is large enough to contain the requested frame
        if (frameDataCache.size() <= maxFrameIndex) {
            // Get current capacity of the array
            int oldCapacity = frameDataCache.size();
            // Enlarge the array
            frameDataCache.setSize(maxFrameIndex+1);
            // Get the new capacity of the array
            int newCapacity = frameDataCache.size();
            // Update the total pixel data cache size
            pixelCacheSize += (newCapacity-oldCapacity)*8;
            // If the cache has become too large, then reduce its size
            reducePixelDataCacheSize();
        }
    }
    private void readyFrameData(int frameIndex) {
        // If the frame data is not in the cache...
        // Calculate the frame data and cache it now
        if (!frameDataCache.has(frameIndex)) {
            calcFrameData(frameIndex);
            frameDataCacheMiss++;
        } else {
            frameDataCacheHit++;
        }
    }
    private void calcFrameData(int frameIndex) {
        // Use the rms as the height of the frame
        float height = audioData.getRMSFrame(frameIndex);
        // Keep track of the frame's color (weighted average)
        float totMag = 0;
        float colorR = 0;
        float colorG = 0;
        float colorB = 0;
        // Get the frame's constant-q magnitudes
        float[] binMagBuffer = audioData.fftData.get(frameIndex);
        // Get the colors for the constant-q bins for the frame
        for (int bin=0; bin<binMagBuffer.length; bin++) {
            // Get the bin's magnitude
            float mag = binMagBuffer[bin]*binMagBuffer[bin] * binWeight[bin];
            // Update the total magnitude
            totMag += mag;
            // Get the bin's color
            float[] color = binColorCache[bin];
            // Update the frame's color
            colorR += mag*color[0];
            colorG += mag*color[1];
            colorB += mag*color[2];
        }
        // Keep track of the frame's color (weighted average)
        colorR /= totMag;
        colorG /= totMag;
        colorB /= totMag;
        // Pack the frame data
        int color = (int)(colorR*255) << 16 |
                    (int)(colorG*255) <<  8 |
                    (int)(colorB*255);
        // Cache the frame data
        frameDataCache.set(frameIndex, color, height);
    }
    
    private class Vector {
        /**
         * The initial size of the vector.
         */
        private static final int SIZE_INIT = 1024;
        
        /**
         * The minimum mount that the vector will increase in size when
         */
        private static final int SIZE_INCR = 1024;
        private int colors[];
        private float heights[];
        
        Vector() {
            colors = new int[SIZE_INIT];
            heights = new float[SIZE_INIT];
            for (int i=0; i<SIZE_INIT; i++) {
                colors[i] = -1;
            }
        }
        
        void set(int index, int color, float height) {
            colors[index] = color;
            heights[index] = height;
        }
        
        int size() {
            return colors.length;
        }
        
        void setSize(int size) {
            int oldSize = size();
            if (size > oldSize) {
                int newSize = Math.max(oldSize + SIZE_INCR, size);
                
                int[] newColors = new int[newSize];
                System.arraycopy(colors, 0, newColors, 0, oldSize);
                for (int i=oldSize; i<newSize; i++)
                    newColors[i] = -1;
                colors = newColors;
                
                float[] newHeights = new float[newSize];
                System.arraycopy(heights, 0, newHeights, 0, oldSize);
                heights = newHeights;
            }
        }
        
        boolean has(int index) {
            return colors[index] > 0;
        }
        
        int getColor(int index){
            return colors[index];
        }
        
        float getHeight(int index) {
            return heights[index];
        }
        
        void clear() {
            int size = size();
            for (int i=0; i<size; i++) {
                colors[i] = -1;
            }
        }
    }
}
