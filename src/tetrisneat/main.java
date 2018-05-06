/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tetrisneat;

import lt.lb.commons.FX.SceneManagement.Frame;
import lt.lb.commons.FX.SceneManagement.MultiStageManager;
import lt.lb.commons.Log;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import tetrisneat.TetrisGame.GameFrame;
import tetrisneat.fxml.MenuController;

/**
 *
 * @author Lemmin
 */
public class main {
    
    public static void simpleGame(int gameCountInt){
        // Make the falling piece drop every game tick
        ArrayList<GameFrame> games = new ArrayList<>();
        
        for(int i=0;i<gameCountInt;i++){
            TetrisGame.GameFrame gm = TetrisGame.initNewGame(true, new Integer[]{0, 1, 2, 3, 4, 5, 6});
            games.add(gm);
        }
        
        
        AtomicInteger gameCount = new AtomicInteger(gameCountInt);
        for(GameFrame gm:games){
            Runnable r = () ->{
                
                while (!gm.game.gameOver) {
                    gm.game.waitTool.conditionalWait();
                    gm.game.dropDown();
                    gm.game.waitTool.requestWait();
                }
                System.out.println("Game Over son");
                gm.frame.dispose();
                if(0 == gameCount.decrementAndGet()){
                    System.exit(0);
                }
            };
            new Thread(r).start();
        }
    }
    
    public static MultiStageManager stageManager;
    
    public static void neatSetup() throws Exception{
        stageManager = new MultiStageManager();
        URL res = stageManager.getResource("/tetrisneat/fxml/menu.fxml");
        System.out.println(res);
        Frame frame = stageManager.newFrame(res, "Tetris NEAT");
        if(frame==null){
            System.out.println("FRAME IS NULL");
        }else{
            Platform.runLater(() ->{
                frame.getStage().show();
                MenuController contr = (MenuController) frame.getController();
                
            });
        }
    }
    public static void main(String[] args){
        Log.keepBuffer = false;
        try{
        neatSetup();        
        }catch(Exception e){
            e.printStackTrace();
        }
//        simpleGame(1);
        
        
    }
}
