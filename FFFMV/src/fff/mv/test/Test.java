/**
 * FastFlameFractalMusicVideo (FFF)
 * An application for creating music videos using flame fractals.
 * Copyright (C) 2015  Jeremiah N. Hankins
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package fff.mv.test;

import fff.flame.Flame;
import fff.mv.core.KeyFlameList;
import fff.mv.core.Project;
import fff.mv.core.ProjectFlame;
import fff.mv.core.ProjectFlameSet;
import fff.mv.render.ProjectRendererTask;
import fff.render.FlameRenderer;
import fff.render.FlameRendererCallback;
import fff.render.FlameRendererTask;
import fff.render.ocl.FlameRendererOpenCL;
import fff.render.ocl.FlameRendererOpenCL.DeviceType;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * For testing the fff.mv.core classes.
 * 
 * @author Jeremiah N. Hankins
 */
public class Test {
    public static void main(String args[]) {
        // Create a new empty project
        Project project = new Project();
        
        // Set the name of the project
        project.setName("Test Project");
        
        // Get the project's flame set
        ProjectFlameSet pflameSet = project.getProjectFlameSet();
        
        // Add a normal Sierpinski triangle to the project
        Flame flame0 = Flame.newSierpinski();
        ProjectFlame pflame0 = pflameSet.add(flame0, "Sierpinski");
        
        // Add an upsidedown Sierpinski triangle to the project
        Flame flame1 = Flame.newSierpinski();
        flame1.getView().setRotation(180);
        ProjectFlame pflame1 = pflameSet.add(flame1, "Sierpinski (+180)");
        
        // Create an animation (flame0 -> flame1 -> flame0)
        KeyFlameList keyFlameList = project.getKeyFlameList();
        keyFlameList.add(pflame0,   0); //  0 sec
        keyFlameList.add(pflame1,  10); // 10 sec
        keyFlameList.add(pflame0,  20); // 20 sec
        
        // Set timing info: 25 fps, 20 sec long
        project.setFrameRate(25); // 25 fps
        project.setStartSec(0);   //  0 sec
        project.setLengthSec(20); // 20 sec
        
        // Create the callback function
        FlameRendererCallback callback = new PNGWriterCallback();
        // Create the renderer task
        FlameRendererTask task = new ProjectRendererTask(project, callback);
        // Create the renderer
        FlameRenderer renderer = new FlameRendererOpenCL(DeviceType.ALL);
        // Enqueue the task
        renderer.getQueue().add(task);
        // Start the renderer
        renderer.start();
        // Shutdown when complete
        renderer.shutdown();
    }
    
    static class PNGWriterCallback implements FlameRendererCallback {
        @Override
        public void flameRendererCallback(
                FlameRendererTask task, // task that generated the callback
                Flame flame,            // flame being worked on
                BufferedImage image,    // current flame image
                double quality,         // current image quality
                double points,          // number of points plotted
                double elapsedTime,     // time spent on image
                boolean isFinished)     // true if image is complete
        {
            // Display progress updates
            System.out.println(String.format("Drawn %.2fM dots in %.2f sec at %.2fM dots/sec for quality of %.2f.", points/1e7, elapsedTime, points/(1e7*elapsedTime), quality));
            // If the image is completed...
            if (isFinished) {
                // Get the frame number
                int frameNo = ((ProjectRendererTask)task).getPrevFrameIndex();
                // Create the file name
                String fileName = String.format("test%04d.png", frameNo);
                // Try to write the image as a PNG file
                File file = new File(fileName);
                try {
                    System.out.println("Writing PNG image file: "+file.getCanonicalPath());
                    ImageIO.write(image, "png", file);
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                    System.exit(0);
                }
                System.out.println();
            }
        }
    }
}
