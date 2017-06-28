/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tetrisneat;

import LibraryLB.FX.SceneManagement.Frame;
import LibraryLB.FX.SceneManagement.MultiStageManager;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import tetrisneat.TetrisGame.GameFrame;

/**
 *
 * @author Lemmin
 */
public class main {
    
    public static void simpleGame(int gameCountInt){
        // Make the falling piece drop every game tick
        ArrayList<GameFrame> games = new ArrayList<>();
        
        for(int i=0;i<gameCountInt;i++){
            TetrisGame.GameFrame gm = TetrisGame.initNewGame(true);
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
            });
        }
    }
    public static void main(String[] args){
        try{
        neatSetup();        
        }catch(Exception e){
            e.printStackTrace();
        }
//        simpleGame(1);
        
        
    }
}
