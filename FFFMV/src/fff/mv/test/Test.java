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
import fff.render.BasicCallback;
import fff.render.FlameRenderer;
import fff.render.RendererCallback;
import fff.render.RendererTask;
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
        project.setFrameRate(25); // 25 fps
        project.setStartSec(0);   //  0 sec
        project.setLengthSec(20); // 20 sec
        
        // Create the callback function
        RendererCallback callback = new BasicCallback("test", 4);
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
    }
}
