package lt.lb.mavencopydeploy;

import lt.lb.commons.javafx.FXDefs;
import lt.lb.commons.javafx.fxrows.FXDrows;
import lt.lb.commons.javafx.scenemanagement.MultiStageManager;

/**
 *
 * @author Lemmin
 */
public class JavaFXLaunch {
    
    static MultiStageManager  sceneManager;
    
    public static void main(String[] args) throws Exception {
        sceneManager = new MultiStageManager(JavaFXLaunch.class.getClassLoader());
        
        FXDrows fxrows = FXDefs.fxrows();
        
        fxrows.getNew().addLabel("Hello").display();
        
       sceneManager.newFxrowsFrame("Title", fxrows).get().show();
    }
    
}
