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
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;

/**
 * This class draws spectrograms for {@code AudioData}.
 * 
 * @author Jeremiah N. Hankins
 */
public class AudioSpectrogram {
    /**
     * The number of seconds of audio represented by each spectrogram subpanel.
     */
    private static final double IMAGE_TIME_WIDTH = 10.0;
    
    /**
     * A percentage of overhead added to the value used to scale the FFT values
     * into a reasonable range. If as more audio data is processed, the maximum
     * value increases only slightly, it would be wasteful to invalidate the
     * entire cache just because a few values would fall slightly out of range
     * after scaling. So, we just make sure the scaling value leaves a little
     * wiggle room.
     */
    private static final float MAX_VALUE_OVERHEAD = 1.1f;
    
    /**
     * The {@code AudioData} used to build the current cache.
     */
    private AudioData audioData;
    
    /**
     * The number of FFT bins that will be used to generate the spectrogram 
     * images.
     */
    private int numBinsToUse;
    
    /**
     * An array containing the maximum y-value (the top) for each frequency bin
     * that is currently in use.
     */
    private int[] binYs;
    
    /**
     * A value slightly larger than the maximum value of any FFT bin value 
     * encountered so far. Used to scale the FFT results.
     */
    private float fftValueMax;
    
    /**
     * A list containing {@code BufferedImage}, each of which contains a
     * cached portion of the spectrogram image.
     */
    private final ArrayList<BufferedImage> images = new ArrayList();
    
    /**
     * A list containing the width in pixels of the completed portion of each
     * image contained in the {@code images} array.
     */
    private final ArrayList<Integer> imageWidths = new ArrayList();
    
    /**
     * A list containing cached {@code BufferedImage} panels scaled to the
     * correct width and height for the current dxdt and dy parameters.
     */
    private final ArrayList<BufferedImage> scaledImages = new ArrayList();
    
    /**
     * The dy parameter value used to construct the scaled image cache.
     */
    private int scaleDY;
    
    /**
     * The dxdt parameter value used to construct the scaled image cache.
     */
    private double scaleDXDT;
    
    /**
     * The maximum frequency to display.
     */
    private double maxFrequency = 4000.0;
    
    /**
     * Controls the ratio of the mix of linear and logarithmic scaling of the
     * frequency bin heights.
     */
    private double linLogRatio = 0.25;
    
    /**
     * Returns the maximum frequency to display.
     * 
     * @return the maximum frequency to display
     */
    public double getMaxFrequency() {
        return maxFrequency;
    }
    
    /**
     * Returns the ratio of linear to logarithmic scaling of the heights of the
     * frequency bins such that a returned value of {@code 0} indicates 100%
     * linear and a returned value of {@code 1} indicates 100% logarithmic.
     *
     * @return the ratio of linear to logarithmic scaling
     */
    public double getLinLogRatio() {
        return linLogRatio;
    }
    
    /**
     * Sets the upper maximum frequency to display specified in Hz.
     * 
     * @param maxFrequency the maximum frequency to display specified in Hz
     * @throws IllegalArgumentException {@code maxFrequency} is not in range (0,inf)
     */
    public void setMaxFrequency(double maxFrequency) {
        if (!(0 < maxFrequency && maxFrequency < Double.MAX_VALUE))
            throw new IllegalArgumentException("maxFrequency is not in range (0,inf): "+maxFrequency);
        if (this.maxFrequency != maxFrequency) {
            this.maxFrequency = maxFrequency;
            audioData = null;
        }
    }
    
    /**
     * Sets the ratio of linear to logarithmic scaling of the heights of the
     * frequency bins such that if a specified value of {@code 0} indicates 100%
     * linear and a specified value of {@code 1} indicates 100% logarithmic.
     * 
     * @param ratio the ratio of linear to logarithmic scaling
     * @throws IllegalArgumentException {@code ratio} is not in range [0,1]
     */
    public void setLinLogRatio(double ratio) {
        if (!(0 <= maxFrequency && maxFrequency <= 1))
            throw new IllegalArgumentException("ratio is not in range [0,1]: "+ratio);
        if (linLogRatio != ratio) {
            linLogRatio = ratio;
            audioData = null;
        }
    }
    
    /**
     * Returns the number of FFT bins that were used to generate the image drawn
     * by the most recent call to {@code #drawSpectrogram}. 
     * <p>
     * The value returned by this method is undefined if
     * {@code #drawSpectrogram} has never been called or if
     * {@code setMaxFrequency} was called more recently than
     * {@code #drawSpectrogram}.
     * 
     * @return the number of FFT bins used to draw the most recently drawn
     * spectrogram
     */
    public int getBinsUsed() {
        return numBinsToUse;
    }
    
    /**
     * Returns the "height" of the bin with the specified index that was used to
     * generate the image drawn by the most recent call to
     * {@code #drawSpectrogram}. The height is in the range [0, 1] and is an
     * absolute height, not a relative height such that the heights of the bins
     * is monotonically increasing and {@code getBinHeight(getBinsUsed()-1)} is
     * always {@code 1.0}.
     * <p>
     * The behavior of this method is undefined if
     * {@code #drawSpectrogram} has never been called or if
     * {@code setMaxFrequency} was called more recently than
     * {@code #drawSpectrogram}.
     * 
     * @param binIndex the index of the bin
     * @return the height of the bin with the specified index
     * @throws IllegalArgumentException if {@code binIndex} is not in range [0, {@code getBinsUsed()})
     */
    public double getBinHeight(int binIndex) {
        if (!(0 <= binIndex && binIndex < numBinsToUse))
            throw new IllegalArgumentException("binIndex is not in range [0, "+numBinsToUse+"): "+binIndex);
        return binYs[binIndex]/(double)binYs[numBinsToUse-1];
    }
    
    /**
     * Clears the cache if the cache was not built for the specified audio data.
     * 
     * @param audioData the new audio data for the cache
     */
    private void checkCache(AudioData audioData, int dy, double dxdt) {
        // Get the maximum FFT value provided by the audio data so far
        float fftMax = audioData.fftMax;
        
        if (this.audioData != audioData) {
            // If the audio data source has changed...
            
            // Store the audio data
            this.audioData = audioData;
            
            // Initialize the bin
            initializeBinYs();
            
            // Clear the cache
            images.clear();
            imageWidths.clear();
            scaledImages.clear();
            
            // Store the new maximum value, but add some overhead to reduce the
            // chances of the cache becoming invalid if the maximum value in the
            // FFT data increases a small amount
            fftValueMax = fftMax*MAX_VALUE_OVERHEAD;
            
            // Store the scaling parameters
            scaleDY = dy;
            scaleDXDT = dxdt;
            
        } else if (fftValueMax < fftMax) {
            // If the maximum fft value has exceeded the maximum value we had
            // preveiously been using for building the current cache...
            
            // Clear the cache
            images.clear();
            imageWidths.clear();
            scaledImages.clear();
            
            // Store the new maximum value, but add some overhead to reduce the
            // chances of the cache becoming invalid if the maximum value in the
            // FFT data increases a small amount
            fftValueMax = fftMax*MAX_VALUE_OVERHEAD;
            
            // Store the scaling parameters
            scaleDY = dy;
            scaleDXDT = dxdt;
            
        } else if (scaleDY != dy || scaleDXDT != dxdt) {
            // If the scaling paramteres change...
            
            // Clear the scaled image cache
            scaledImages.clear();
            
            // Store the scaling parameters
            scaleDY = dy;
            scaleDXDT = dxdt;
        }
    }
    
    /**
     * Initializes the array containing the maximum y-values for each frequency
     * bin currently in use.
     */
    private void initializeBinYs() {
        // Get the resolution of the FFT bins
        double fftResolution = audioData.getFFTBinFrequency(1);

        // Determine how many FFT bins will be used
        numBinsToUse = Math.min(
                (int)Math.ceil(maxFrequency/fftResolution)+1,
                audioData.getFFTBinCount());
        
        // The bin for the highest frequency will be the shortest bin (in
        // terms of pixel height) and we want to ensure that when it's drawn
        // it's atleast 1 pixel tall. The following caluclates a scaling
        // factor which achieves the desired results.
        double scale = 
                  yScale(fftResolution, numBinsToUse) 
                - yScale(fftResolution, numBinsToUse-1);
        scale = scale > 1 ? 1 : 1/scale;

        // Allocate an array that will hold the maximum y-value (the top) 
        // for each frequency bin being used
        binYs = new int[numBinsToUse];

        // Get the minimum y-value after scaling
        double yMin = yScale(fftResolution, 0)*scale;
        
        // Fill the array with the y-values for each bin
        for (int bin = 0; bin < numBinsToUse; bin++) {
            binYs[bin] = (int)(yScale(fftResolution, bin+1)*scale - yMin);
        }
    }
    
    /**
     * Converts the specified frequency specified in Hz into a vertical height
     * using a logarithmic scaling function.
     * 
     * @param frequency the frequency to convert specified in Hz 
     * @return the height for the frequency
     */
    private double yScale(double fftResolution, int bin) {
        double maxFreq = fftResolution*numBinsToUse;
        double frequency = fftResolution*(bin+1);
        double log = Math.log(1 + frequency) / Math.log(1 + maxFreq);
        double lin = (bin + 1.0) / numBinsToUse;
        return lin +  (log - lin) * linLogRatio;
    }
    
    /**
     * Renders panels and stores them in the cache.
     * 
     * @param maxVisFrame the minimum visible frame index
     * @param minVisPanel the minimum visible panel index
     * @param maxVisPanel the maximum visible panel index
     */
    private void buildImageCache(int maxVisFrame, int minVisPanel, int maxVisPanel) {
        
        // Get the audio frame rate (audio frames per second)
        double rate = audioData.getFrameRate();
            
        // Get the height of the panels
        int h = binYs[numBinsToUse-1];
        
        // Get a direct reference to the fft data
        List<float[]> fftData = audioData.fftData;
        
        // For each visible panel...
        for (int panelIdx = minVisPanel; panelIdx <= maxVisPanel; panelIdx++) {
            
            // Attempt to retrive the panel from the cache
            BufferedImage image = images.get(panelIdx);
            Integer completedWidth = imageWidths.get(panelIdx);
            
            // If the image panel is complete, then skip this image
            if (image != null && completedWidth == image.getWidth())
                continue;
            
            // Get the maximum and minimum frame indicies contained in the
            // image panel. Note: Each image pixel is one frame and the 
            // panel might not have been completed.
            int minPanelFrame = (int)(    panelIdx*(rate*IMAGE_TIME_WIDTH));
            int maxPanelFrame = (int)((panelIdx+1)*(rate*IMAGE_TIME_WIDTH));
            
            // Calculate the width of the entire panel
            int fullWidth = maxPanelFrame-minPanelFrame;
            
            // Calculate the width of the visible portion of the panel
            int visWidth = Math.min(maxPanelFrame, maxVisFrame) - minPanelFrame;
            
            // If the panel isn't actually visible, skip this panel
            if (visWidth < 1) continue;
            
            // If the image is not in the cache, or the images is not tall
            // enough...
            if (image == null) {
                
                // Construct a new image large enough to contain the panel
                image = new BufferedImage(fullWidth, h, BufferedImage.TYPE_INT_ARGB);
                
                // And add the panel to the cache
                images.set(panelIdx, image);
                
                // Set th image's completed width to 0
                completedWidth = 0;
            }
            
            // IMPORTANT NOTE:
            // 
            // Accelerated images have good display performance but modifying 
            // them via a graphics context exhibits poor performance. Good 
            // modification performance can be achieved by directly accessing 
            // the pixel array buffer that backs the image, but doing so
            // de-accelerates the images and hinders future display performance. 
            //
            // Through testing, it was determined that for this class the best 
            // results were achieved by using a hybrid approach: 
            //
            // An unaccelerated image is modified via pixel-buffer so that it 
            // can be generated quickly, then, once the image is complete, the 
            // unaccelerated is  copied via graphics context into new accelerated
            // image so it can be subsequently displayed quickly.
            // 
            // Testing revealed that the hybrid aproach is between 2 and 4 times
            // faster on average then either using either a single accelerated
            // image or a single unaccelerated image alone.
            
            // Get direct access to the pixel array backing this image so that
            // we can modify it efficiently.
            int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
            
            // For each remaining uncompleted verticle stip, each of wich 
            // represents one frame of audio data...
            for (int x = completedWidth; x < visWidth; x++) {
                
                // Get the frame index for this verticle strip
                int frame = minPanelFrame+x;
                
                // Get the fft bin data for the frame
                float[] binData = fftData.get(frame);
                        
                // For each FFT bin in use...
                for (int bin = 0; bin < numBinsToUse; bin++) {
                    
                    // Get the FFT bin's value
                    float fftValue = binData[bin];
                    
                    // Convert the value into a color
                    int rgb = fftValueToColor(fftValue);
                    
                    // Draw a verticle line representing the bin
                    int binY0 = h-(bin == 0? 0 : binYs[bin-1]);
                    int binY1 = h-binYs[bin];
                    int pixIdx = binY1 * fullWidth + x;
                    for (int y=binY1; y<binY0; y++) {
                        pixels[pixIdx] = rgb;
                        pixIdx += fullWidth;
                    }
                }
            }
            
            // If the image has been completed...
            if (visWidth == fullWidth) {
                // ...then the image will not be modified in the future and does
                // not requires direct pixel acces anymore, so we'll create an 
                // accelreated version of the image to achieve better display 
                // performance as per the note above.
                
                // Construct a new acclerated image
                BufferedImage acceleratedImage = new BufferedImage(fullWidth, h, BufferedImage.TYPE_INT_ARGB);
                
                // Get the graphics context for the image
                Graphics imgG = acceleratedImage.createGraphics();
                
                // Copy the unaccelerated image into the accerlated image
                imgG.drawImage(image, 0, 0, null);
                
                // Replace the unaccelerated image with the accelrated image
                // in the cache
                images.set(panelIdx, acceleratedImage);
            }
            
            // Store the new completed width for the panel
            imageWidths.set(panelIdx, visWidth);
        }
    }
    
    /**
     * Converts the specified FFT value into a color value.
     * 
     * @param fftValue the FFT value to convert
     * @return the color assigned to the FFT value
     */
    private int fftValueToColor(float fftValue) {
        // Use the normalize the value as a hue
        float hue = fftValue/fftValueMax;
        // Modify the hue so that is goes from blue to red
        hue = (1.6666666f-hue)%1.0f;
        // Convert the hue into a completly saturated bright color
        return Color.HSBtoRGB(hue, 1, 1);
    }
    
    /**
     * Draws the spectrogram representing the specified {@code AudioData} on the
     * specified graphics context.
     * 
     * @param g the graphics context on which to draw the spectrogram
     * @param audioData the data source for the spectrogram
     * @param x0 the x-coordinate of the upper left corner of the spectrogram
     * @param y0 the y-coordinate of the upper left corner of the spectrogram
     * @param dx the width of the spectrogram in pixels
     * @param dy the height of the spectrogram in pixels
     * @param t0 the minimum visible time (the time at {@code x0})
     * @param t1 the maximum visible time (the time at {@code x0+dx})
     * 
     * @throws NullPointerException if {@code g} or {@code audioData} is {@code null}
     * @throws IllegalArgumentException if either {@code dx} or {@code dy} is not positive
     * @throws IllegalArgumentException if either {@code t0} or {@code t1} is not finite or {@code t1} is greater than {@code t0}
     */
    public void drawSpectrogram(
            Graphics g, 
            AudioData audioData, 
            int x0, int y0, 
            int dx, int dy,
            double t0, double t1
        ) {
        if (g == null)
            throw new NullPointerException("g");
        if (audioData == null)
            throw new NullPointerException("audioData");
        if (dx < 1 || dy < 1)
            throw new IllegalArgumentException("dx and dy must be positive: dx="+dx+" dy="+dy);
        if (!(Double.NEGATIVE_INFINITY < t0 && t0 <= t1 && t1 < Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("t0 and t1 must be finite and t0 must be less than or equal to t1: t0="+t0+" t1="+t1);
        
        // Do nothing if there is no audio data available
        if (audioData.getFrameCount() < 1)
            return;
        
        // Translate and clip the graphics context
        g = g.create(x0, y0, dx, dy);
        
        // Since the AudioData object might still be decoding audio and 
        // processing frames concurrently in annother thread while this function
        // is running, to simplify things, lock in the maximum number of frames.
        int maxFrame = audioData.getFrameCount();
        
        // Calculate the pixels-per-second and pixels-per-panel
        double dxdt = dx/(t1-t0);
        double dxdp = dxdt*IMAGE_TIME_WIDTH;
        
        // If the images scalars have not changed since the last invocation of
        // this method, then cache the scaled images because:
        // 
        // 1) If we're drawing the same image panels over and over using the
        // same scaling coefficients, then it is MUCH more effient to cache the
        // scaled images panels and reuse them rather than performing the 
        // eact same image scaling operations over and over.
        // 
        // 2) If the scalar coefficients are constaly changing, then building a
        // cache of the scaled images is a HUGE waste of resources, since the
        // cache will be invalid before ever being used if the scalar 
        // coefficients change on the next invocation.
        boolean cacheScaledImages = (scaleDY == dy && scaleDXDT == dxdt);
        
        // Clear the cache if the cache was not built for this audio data
        checkCache(audioData, dy, dxdt);
        
        // Get the audio frame rate (audio frames per second)
        double rate = audioData.getFrameRate();
        
        // Get the maximum and minimum visible frame indicies
        int minVisFrame = Math.max((int)(t0*rate), 0);
        int maxVisFrame = Math.min((int)(t1*rate), maxFrame);
        
        // Get the maximum and minimum visible panel indicies
        int minVisPanel = (int)(minVisFrame/(rate*IMAGE_TIME_WIDTH));
        int maxVisPanel = (int)(maxVisFrame/(rate*IMAGE_TIME_WIDTH));
        
        // Ensure that the size of the cache is large enough that we can work
        // without worrying about index out of bounds acceptions or needless
        // array copies
        ensureSize(images,       maxVisPanel+1);
        ensureSize(imageWidths,  maxVisPanel+1);
        if (cacheScaledImages) ensureSize(scaledImages, maxVisPanel+1);
        
        // Build the image cache for the frames that are needed
        buildImageCache(maxVisFrame, minVisPanel, maxVisPanel);
        
        // Calculate the x-axis offset
        int xOffset = -(int)(t0*dxdt);
        
        // For each visible panel...
        for (int panelIdx = minVisPanel; panelIdx <= maxVisPanel; panelIdx++) {
            
            // Get the panel image
            BufferedImage image = images.get(panelIdx);
            
            // If the image panel is not in cache, then skip it
            if (image == null) continue;
            
            // Get the min and max x-values for the panel and its width
            int pX0 = (int)(panelIdx*dxdp);
            int pX1 = (int)((panelIdx+1)*dxdp);
            int pDx = pX1-pX0;
            
            if (!cacheScaledImages || image.getWidth() != imageWidths.get(panelIdx)) {
                // IF we are not using the scaled image cache
                // OR the image has not been completed, THEN draw it directly to
                // the graphics context without using the scaled image cache...
                g.drawImage(image, xOffset+pX0, y0, pDx, dy, null);
                
            } else {
                // If we are using the scaled image cache 
                // AND the image has been completed...
                
                // Attempt to get the scaled image from the cache
                BufferedImage scaledImage = scaledImages.get(panelIdx);
                
                if (scaledImage == null) {
                    // If the scaled image was not in the cache...

                    // Construct a new BufferedImage large enough to contain the 
                    // scaled image
                    scaledImage = new BufferedImage(pDx, dy, BufferedImage.TYPE_INT_ARGB);
                    
                    // Draw the scaled image into the buffer
                    Graphics scaleG = scaledImage.getGraphics();
                    scaleG.drawImage(image, 0, 0, pDx, dy, null);

                    // Store the scaled image buffer in the cache
                    scaledImages.set(panelIdx, scaledImage);
                }
                
                // Draw the scaled image into the graphics context
                g.drawImage(scaledImage, xOffset+pX0, y0, null);
            }
        }
    }
    
    /**
     * Ensures that the specified {@code ArrayList} has at least the specified
     * size. If before this method call, the {@code ArrayList} was less than the
     * specified size, then it is {@code null} padded until it reaches the 
     * desired size.
     * 
     * @param list the list whose size needs to be ensured
     * @param size the minimum desired size of the list
     */
    private static void ensureSize(ArrayList<?> list, int size) {
        list.ensureCapacity(size);
        while (list.size() < size) {
            list.add(null);
        }
    }
}
