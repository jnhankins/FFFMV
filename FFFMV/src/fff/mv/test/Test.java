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

package fff.mv.test;

import fff.flame.Flame;
import fff.flame.FlameFactory;
import fff.mv.core.KeyFlameList;
import fff.mv.core.Project;
import fff.mv.core.ProjectFlame;
import fff.mv.core.ProjectFlameSet;
import fff.mv.core.ProjectRendererTask;
import fff.mv.io.VideoEncoder;
import fff.mv.io.XugglerVideoEncoder;
import fff.render.FlameRenderer;
import fff.render.RendererCallback;
import fff.render.RendererSettings;
import fff.render.RendererTask;
import fff.render.RendererUpdate;
import fff.render.ocl.FlameRendererOpenCL;
import fff.render.ocl.FlameRendererOpenCL.DeviceType;
import java.io.File;
import java.io.IOException;

/**
 * For testing the fff.mv.core classes.
 * 
 * @author Jeremiah N. Hankins
 */
public class Test {
    public static String audioFileName = "../samples/audio/Kevin MacLeod - Exit the Premises.mp3";
    public static String outputFile = "TestOutput.mp4";
    
    public static void main(String[] args) {
        try {
            // Create a new empty project
            Project project = new Project();

            // Set the name of the project
            project.setName("Test Project");

            // Get the project's flame set
            ProjectFlameSet pflameSet = project.getProjectFlameSet();

            // Add a normal Sierpinski Triangle to the project
            Flame flame0 = FlameFactory.newSierpinskiTriangle();
            ProjectFlame pflame0 = pflameSet.add(flame0, "Sierpinski Triangle");

            // Add a Golden Dragon Curve to the project
            Flame flame1 = FlameFactory.newGoldenDragonCurve();
            flame1.getView().setRotation(180);
            ProjectFlame pflame1 = pflameSet.add(flame1, "Golden Dragon Curve");

            // Create an animation (flame0 -> flame1 -> flame0)
            KeyFlameList keyFlameList = project.getKeyFlameList();
            keyFlameList.add(pflame0,   0); //  0 sec
            keyFlameList.add(pflame1,  10); // 10 sec
            keyFlameList.add(pflame0,  20); // 20 sec

            // Set timing info: 25 fps, 20 sec long
            project.setFrameRate(1, 25); // 25 fps
            project.setStartSec(0);      //  0 sec
            project.setLengthSec(20);    // 20 sec
            
            // Set some optimization flags
            RendererSettings settings = project.getRendererSettings().getSettings();
            settings.setUsePostAffines(false);
            settings.setUseFinalTransform(false);
            settings.setUseVariations(false);
            settings.setUseJitter(false);
            
            // Create the video encoder
            VideoEncoder encoder = new XugglerVideoEncoder(
                    new File(outputFile),
                    project.getRendererSettings().getSettings().getWidth(),
                    project.getRendererSettings().getSettings().getHeight(),
                    project.getTimeStep(),
                    project.getTimeScale());
            
            // Set an audio track
            encoder.setAudio(new File(audioFileName), 0);
            
            // Create the callback function
            RendererCallback callback = new RendererCallback() {
                @Override
                public void rendererCallback(RendererUpdate update) {
                    // Print an update
                    System.out.println(update.toString());
                    // If the image is finished...
                    if (update.isFinished()) {
                        // Get the project renderer task
                        ProjectRendererTask ptask = (ProjectRendererTask)update.getTask();
                        // Get the total number of frames
                        int total = ptask.getFrameCount();
                        // Get the frame index
                        int index = ptask.getPrevFrameIndex();
                        // Get the taks's time for the frame
                        double tTimeSec = ptask.getPrevFrameTime();
                        // Get the encoder's time for the frame
                        double eTimeSec = encoder.getFrameTime();
                        // Print an update abount encoding
                        System.out.println("Encoding frame "+index+"/"+total+" @"+formatTime(tTimeSec)+" @"+formatTime(eTimeSec)+"\n");
                        try {
                            // Append the image to the end of the video
                            encoder.addFrame(update.getImage());
                            // If an error occured decoding the audio source file...
                        } catch (IOException ex) {
                            // Print a stack trace for the error
                            ex.printStackTrace(System.err);
                            // ...and trigger shutdown hooks
                            System.exit(0);
                        }
                    }
                }
            };
            
            // Create the renderer task
            RendererTask task = new ProjectRendererTask(project, callback);
            // Create the renderer
            FlameRenderer renderer = new FlameRendererOpenCL(DeviceType.ALL);
            // Enqueue the task
            renderer.getQueue().add(task);
            // Start the renderer
            renderer.start();
            // Shutdown when complete
            renderer.shutdown();
            // Wait for shutdown
            renderer.awaitTermination(Long.MAX_VALUE);
            // Close the encoder (not strictly necessary)
            encoder.close();
            // All Done!
            System.out.println("Done");
            // If any errors occur...
        } catch (Exception ex) {
            // Print a stack trace for the error
            ex.printStackTrace(System.err);
            // And trigger shutdown hooks
            System.exit(0);
        }
    }
    
    /**
     * Converts time in seconds into a string in MM:SS:LLL format.
     * 
     * @param seconds the time in seconds to convert
     * @return the time in MM:SS:LLL format
     */
    public static String formatTime(double seconds) {
        return String.format("%02d:%02d:%03d", 
                (int)(seconds / 60),
                (int)(seconds)%60,
                (int)(seconds*1000)%1000);
    }
}
