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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * This class draws spectrograms for {@code AudioData}.
 * 
 * 
 * @author Jeremiah N. Hankins
 */
public class AudioSpectrogram {
    /**
     * The number of seconds of audio represented by each spectrogram subpanel.
     */
    private static final double imageTimeWidth = 10;
    
    /**
     * A percentage of overhead added to the value used to scale the FFT values
     * into a reasonable range. If as more audio data is processed, the maximum
     * value increases only slightly, it would be wasteful to invalidate the
     * entire cache just because a few values would fall slightly out of range
     * after scaling. So, we just make sure the scaling value leaves a little
     * wiggle room.
     */
    private static final float maxValueOverhead = 1.1f;
    
    /**
     * The {@code AudioData} used to build the current cache.
     */
    private AudioData audioData;
    
    /**
     * The maximum frequency to display.
     */
    private double maxFrequency;
    
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
     * A list containing containing the width in pixels of the completed portion
     * of each image contained in the {@code images} array.
     */
    private final ArrayList<Integer> imageWidths = new ArrayList(); 
    
    /**
     * Clears the cache if the cache was not built for the specified audio data.
     * 
     * @param audioData the new audio data for the cache
     * @param maxFrequency the maximum audio frequency to display
     * @param fftMax the maximum FFT value provided by the audio data so far
     */
    private void checkCache(AudioData audioData, double maxFrequency, float fftMax) {
        
        if (this.audioData != audioData || this.maxFrequency != maxFrequency) {
            // If the audio data or maximum frequency have changed...
            
            // Store the new audio data
            this.audioData = audioData;
            
            // Store the new maximum frequency
            this.maxFrequency = maxFrequency;
            
            // Clear the cached image panels
            images.clear();
            imageWidths.clear();
            
            // Caclulate the y-value for each frequency bin
            initializeBinYs();
            
            // Store the new maximum value, but add some overhead to reduce the
            // chances of the cache becoming invalid if the maximum value in the
            // FFT data increases a small amount
            fftValueMax = fftMax*maxValueOverhead;
            
        } else if (fftValueMax < fftMax) {
            // If the maximum fft value has exceeded the maximum value we had
            // preveiously been using for building the current cache...
            
            // Clear the cached image panels
            images.clear();
            imageWidths.clear();
            
            // Store the new maximum value, but add some overhead to reduce the
            // chances of the cache becoming invalid if the maximum value in the
            // FFT data increases a small amount
            fftValueMax = fftMax*maxValueOverhead;
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
                (int)(maxFrequency/fftResolution),
                audioData.getFFTBinCount());

        // The bin for the highest frequency will be the shortest bin (in
        // terms of pixel height) and we want to ensure that when it's drawn
        // it's atleast 1 pixel tall. The following caluclates a scaling
        // factor which achieves the desired results.
        double scale = 
                  yScale(fftResolution*numBinsToUse) 
                - yScale(fftResolution*(numBinsToUse-1));
        scale = scale > 1 ? 1 : 1/scale;

        // Allocate an array that will hold the maximum y-value (the top) 
        // for each frequency bin being used
        binYs = new int[numBinsToUse];

        // Get the minimum y-value after scaling
        double yMin = yScale(0)*scale;

        // Fill the array with the y-values for each bin
        for (int bin=0; bin < numBinsToUse; bin++) {
            binYs[bin] = (int)(yScale(fftResolution*(bin+1))*scale - yMin);
        }
    }
    
    /**
     * Converts the specified frequency specified in Hz into a vertical height
     * using a logarithmic scaling function.
     * 
     * @param frequency the frequency to convert specified in Hz 
     * @return the height for the frequency
     */
    private double yScale(double frequency) {
        return Math.log(1 + frequency);
    }
    
//    /**
//     * Sets the approximate maximum time in milliseconds that the 
//     * {@code drawSpectrogram} method will spend working on the internally 
//     * cached spectrogram image before 
//     * 
//     * spectrogramdrawSpectrogram image cached inside this {@code AudioSpectrogram}
//     * 
//     * panels to the cache before drawing the 
//     * 
//     * @param maxTimeMS 
//     */
//    public void setMaxTime(long maxTimeMS)

    
    
    public void drawSpectrogram(
            Graphics g, 
            AudioData audioData, 
            int x0, int y0, 
            int dx, int dy,
            double t0, double t1, 
            double maxFrequency) {
        
        // Since the AudioData object might still be decoding audio and 
        // processing frames concurrently in annother thread while this function
        // is running, to simplify things, lock in the maximum number of frames
        // and the maximum fft value that this function will consider right at
        // the start.
        int maxFrame = audioData.getFrameCount();
        float fftMax = audioData.getFFTMax();
        
        // Clear the cache if the cache was not built for this audio data
        checkCache(audioData, maxFrequency, fftMax);
        
        
        // Get the audio frame rate (audio frames per second)
        double rate = audioData.getFrameRate();
        
        // Get the frames indexes containing the maximum and minimum times
        int minVisibleFrame = (int)Math.max(t0*rate, 0);
        int maxVisibleFrame = (int)Math.min(t1*rate, maxFrame);
        
        // Get the start times for those frame indexes
        double minTime = minVisibleFrame/rate;
        double maxTime = maxVisibleFrame/rate;
        
        // Get the panel indexes for those start start times
        int minImg = (int)(minTime/imageTimeWidth);
        int maxImg = (int)(maxTime/imageTimeWidth);
        
        // Ensure that the size of the cache is large enough that we can work
        // without worrying about index out of bounds acceptions or needless
        // array copies
        ensureSize(images, maxImg+1);
        ensureSize(imageWidths, maxImg+1);
        
        // For each visible panel...
        for (int imgNo = minImg; imgNo <= maxImg; imgNo++) {
            
            // Attempt to retrive the panel from the cache
            BufferedImage image = images.get(imgNo);
            Integer completedWidth = imageWidths.get(imgNo);
            
            // If the image panel is complete, then skip this image
            if (image != null && completedWidth == image.getWidth())
                continue;
            
            // Get the maximum and minimum frame indicies contained in the
            // image panel. Note: Each image pixel is one frame and the 
            // panel might not have been completed.
            int frameMin = (int)(imgNo*imageTimeWidth*rate);
            int frameMax = (int)((imgNo+1)*imageTimeWidth*rate); // exclusive
            
            // Calculate the width of the entire panel
            int fullWidth = frameMax-frameMin;
            
            // Calculate the width of the visible portion of the panel
            if (frameMax > maxVisibleFrame)
                frameMax = maxVisibleFrame;
            int visibleWidth = frameMax-frameMin;
            
            // Get the height of the panel
            int h = binYs[numBinsToUse-1];
            
            // If the panel's width is too small (because the frame data is not 
            // available yet), then skip this panel
            if (visibleWidth < 1) continue;
            
            // If the image is not in the cache...
            if (image == null) {
                
                // Construct a new image large enough to contain the panel
                image = new BufferedImage(fullWidth, h, BufferedImage.TYPE_INT_ARGB);
                
                // And add the panel to the cache
                images.add(imgNo, image);
                imageWidths.add(imgNo, visibleWidth);
                
                // Set th image's completed width to 0
                completedWidth = 0;
            }
            
            // Create a graphics context for the image
            Graphics imgG = image.getGraphics();
            
            // For each remaining uncompleted verticle stip, each of wich 
            // represents one frame of audio data...
            for (int x = completedWidth; x < visibleWidth; x++) {
                // Get the frame index for this verticle strip
                int frame = frameMin+x;
                // For each FFT bin in use...
                for (int bin = 0; bin < numBinsToUse; bin++) {
                    // Get the FFT bin's value
                    float fftValue = audioData.getFFTFrameBin(frame, bin);
                    // Convert the value into a color
                    imgG.setColor(fftValueToColor(fftValue));
                    // Draw a verticle line representing the bin
                    int binY0 = h-(bin == 0? 0 : binYs[bin-1]);
                    int binY1 = h-binYs[bin];
                    imgG.drawLine(x, binY0, x, binY1);
                }
            }
        }
        
        // Calculate the "pixels per second"
        double dxdt = dx/(t1-t0);
        
        // Calculate the x-axis offset
        int X = x0-(int)(t0*dxdt);
        
        // For each visible panel...
        for (int imgNo = minImg; imgNo <= maxImg; imgNo++) {
            
            // If the panel is in the cache...
            if (imgNo < images.size() && images.get(imgNo) != null) {
                
                // Get the panel image
                BufferedImage image = images.get(imgNo);
                
                // Get the maximum and minimum frame indicies contained in the
                // image panel. Note: Each image pixel is one frame and the 
                // panel might not have been completed.
                int frameMin = (int)(imageTimeWidth*rate*imgNo);
                int frameMax = (int)(imageTimeWidth*rate*(imgNo+1));
                if (frameMax > frameMin + image.getWidth())
                    frameMax = frameMin + image.getWidth();
                
                // Get the time bounds for the for the image
                double T0 = imgNo*imageTimeWidth;
                double T1 = Math.min((imgNo+1)*imageTimeWidth, frameMax/rate);
                
                // Get the minimum and maximum x values (without the offset)
                int X0 = (int)(T0*dxdt);
                int X1 = (int)(T1*dxdt);
                
                // Draw the image panel after applying applying the x offfset,
                // y offset, and y scaling
                g.drawImage(image, X+X0, y0, X1-X0, dy, null);
            }
        }
    }

    /**
     * Converts the specified FFT value into a color value.
     * 
     * @param fftValue the FFT value to convert
     * @return the color assigned to the FFT value
     */
    private Color fftValueToColor(float fftValue) {
        // Use the normalize the value as a hue
        float hue = fftValue/fftValueMax;
        // Modify the hue so that is goes from blue to red
        hue = (1.6666666f-hue)%1.0f;
        // Convert the hue into a completly saturated bright color
        int rgb = Color.HSBtoRGB(hue, 1, 1);
        // Return the color
        return new Color(rgb);
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
    private void ensureSize(ArrayList<?> list, int size) {
        list.ensureCapacity(size);
        while (list.size() < size) {
            list.add(null);
        }
    }
    
    
    public static void main(String[] args) throws InterruptedException {
        //File file = new File("E:\\Music\\Essential Mix\\Essential Mix (2011-04-16) Alex Metric.mp3");
        File file = new File("E:\\Music\\Nero\\[2011] Welcome Reality\\Nero - Welcome Reality - 07 - Innocence.flac");
        AudioDataTask task = new AudioDataTask(file, 44100, 2048, 0);
        task.setExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable thrown) {
                System.err.println(thrown);
            }
        });
        
        AudioData data = task.getData();
        
        AudioSpectrogram spectrogram = new AudioSpectrogram();
        
        JFrame frame = new JFrame();
        frame.setTitle(file.toString());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.add(new JPanel() {
            public void paint(Graphics g) {
                g.setColor(Color.yellow);
                g.fillRect(0, 0, getWidth(), getHeight());
                spectrogram.drawSpectrogram(
                        g, 
                        data,
                        0, 0, 
                        getWidth(), getHeight(), 
                        0, data.getFrameCount()/data.getFrameRate(),
                        4000);
            }
        });
        frame.setVisible(true);
        
        Timer repaintTimer = new Timer(40, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                frame.repaint();
            }
        });
        repaintTimer.start();
        
        task.start();
        task.awaitTermination();
        System.out.println("done");
    }
}
