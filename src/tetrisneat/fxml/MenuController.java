/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tetrisneat.fxml;

import LibraryLB.FX.SceneManagement.BaseController;
import LibraryLB.Log;
import LibraryLB.Threads.DynamicTaskExecutor;
import LibraryLB.Threads.Sync.ConditionalWait;
import NEATPort.Genome;
import NEATPort.Pool;
import NEATPort.Species;
import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import tetrisneat.NEATController;
import tetrisneat.TetrisGame;

/**
 * FXML Controller class
 *
 * @author Lemmin
 */
public class MenuController extends BaseController {

    @FXML public Label enqueueLabel;
    @FXML public Label generationLabel;
    @FXML public Label bestScoreLabel;
    @FXML public TextField enqueueText;
    @FXML public TextField populationText;
    @FXML public TextField learningDelayText;
    @FXML public TextField generationText;
    public Gson g = new Gson();
    public ConditionalWait wait = new ConditionalWait();
    public AtomicInteger leftToEnqueue = new AtomicInteger(0);
    public AtomicInteger leftExecuting = new AtomicInteger(0);
    public boolean running = false;
    public boolean DISPLAY_WHILE_LEARNING = true;
    public long LEARNING_STEP_DELAY = 0;
    public static final long BEST_STEP_DELAY = 50;
    public NEATController best = null;
    public ArrayList<NEATController> controller = new ArrayList<>();
    public Pool pool;
    public DynamicTaskExecutor exe = new DynamicTaskExecutor();
//    public ExecutorService exe = Executors.newFixedThreadPool(4);
    
    public ArrayList<NEATController> createControllers(Pool pool){
        ArrayList<NEATController> controllers = new ArrayList<>();
        for(Species spec:pool.poolSpecies){
            for(Genome genome : spec.genomes){
                genome.generateNetwork();
                NEATController con = new NEATController();
                con.genome = genome;
                controllers.add(con);
            }
        }
        return controllers;
    }
    public ArrayList<Runnable> neatSimulation(ArrayList<NEATController> controller){
  
        
        ArrayList<Runnable> threads = new ArrayList<>();
        
        for(NEATController con:controller){
            
            threads.add(makeNEATControlledGame(con,LEARNING_STEP_DELAY,DISPLAY_WHILE_LEARNING));
            
        }
        return threads;
        
    }
    public NEATController makeNEATControlledGame(NEATController con,long delay,boolean visible){
        Runnable r =() ->{
            TetrisGame.GameFrame gm = TetrisGame.initNewGame(visible);
            TetrisGame game = gm.game;
            con.gm = gm;
            while(!game.gameOver){
                con.makeMove();
                try {
                    
                    Thread.sleep(Math.max(delay, LEARNING_STEP_DELAY));
                } catch (InterruptedException ex) {
//                    Logger.getLogger(main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            System.out.println("Game Over "+game.score);
            con.gm.frame.dispose();

        };
        con.logic = r;
        return con;
    }
    public void learn(Pool pool,ArrayList<NEATController> controller){
        if(best == null)
            best = controller.get(0);
        for(NEATController con:controller){
            
            double fitness = con.evaluateFitness();
            if(pool.getMaxFitness() < fitness){
                pool.setMaxFitness(fitness);
            }
            if(best.genome.fitness < fitness){
                best = con;
            }
        }
        
    }
    public void neat() throws InterruptedException{
        
        update();
        if(running){
            return;
        }
        running = true;
        exe.setRunnerSize(4);
        new Thread( () ->{
            while(leftToEnqueue.decrementAndGet()>=0){
                controller = createControllers(pool);
                ArrayList<Runnable> neatSimulation = neatSimulation(controller);
                leftExecuting.set(controller.size());
                for(Runnable r:neatSimulation){
                    exe.submit(() ->{
                            r.run();
                            leftExecuting.decrementAndGet();
                            wait.wakeUp();
                        }
                    );
                }

                while(leftExecuting.get()>0){
                    wait.requestWait();
                    wait.conditionalWait();
                }
                learn(pool,controller);



                System.out.println(best.game().rowsCleared+" "+best.game().score+"  "+best.genome.genes.size());
                pool.newGeneration();
                System.out.println("New generation: "+pool.stats.generation);
                update();
                
                if(pool.stats.generation%10 == 0){
                    new Thread( () ->{
                            try {
                                save("pool"+pool.stats.generation);
                            } catch (FileNotFoundException | UnsupportedEncodingException ex) {
                                Logger.getLogger(MenuController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    ).start();
                   
                }
            }
            stop();
            running = false;
            update();
        }
       ).start();
        
        
    }
    @Override
    public void exit() {
        leftToEnqueue.set(0);
        exe.shutdown();
        Log.close();
    }
    
    @Override
    public void update() {
        Platform.runLater(() ->{
            enqueueLabel.setText(""+leftToEnqueue.get());
            generationLabel.setText(pool.stats.generation+"");
            if(best != null)
                bestScoreLabel.setText(best.game().score+"");
            LEARNING_STEP_DELAY = Integer.parseInt(learningDelayText.getText());
        });
    }
    public void init(){
        
        pool = new Pool();
        pool.stats.POPULATION = Integer.parseInt(populationText.getText());
        pool.stats.INPUTS = 22*10;
        pool.stats.OUTPUTS = 4;
        pool.initializePool();
//        controller = createControllers(pool);
//        learn(pool,controller);
        
    }
    public void enqueue() throws InterruptedException{
        int enq = Integer.parseInt(enqueueText.getText());
        leftToEnqueue.addAndGet(enq);
        LEARNING_STEP_DELAY = Integer.parseInt(learningDelayText.getText());
        neat();
        
    }
    public void playBest(){
        new Thread(makeNEATControlledGame(best,BEST_STEP_DELAY,true)).start();
        
    }
    public void stop(){
        leftToEnqueue.set(0);
        leftExecuting.set(0);
        exe.stopEverything();
        wait.wakeUp();
        update();
    }
    
    public void save() throws FileNotFoundException, UnsupportedEncodingException{
        save(this.generationText.getText());
    }
    public void save(String where) throws FileNotFoundException, UnsupportedEncodingException{
        String toJson = g.toJson(pool);
        LibraryLB.FileManaging.FileReader.writeToFile(where, Arrays.asList(toJson));
    }
    public void load() throws FileNotFoundException, IOException{
        if(running){
            return;
        }
        ArrayList<String> read = new ArrayList<>(LibraryLB.FileManaging.FileReader.readFromFile(this.generationText.getText()));
        pool = g.fromJson(read.get(0), Pool.class);
//        controller = createControllers(pool);
//        learn(pool,controller);
        update();
    }
}

