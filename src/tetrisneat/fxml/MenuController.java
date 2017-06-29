
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
import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import libneat.Genome;
import libneat.Pool;
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
    public static final int THREAD_COUNT = 4;
    public NEATController best = null;
    public ArrayList<NEATController> controller = new ArrayList<>();
    public Pool pool;
    public DynamicTaskExecutor exe = new DynamicTaskExecutor();
    
    public ArrayList<NEATController> createControllers(Pool pool){
        ArrayList<NEATController> controllers = new ArrayList<>();
        for(Genome genome : pool.getPopulation()){
            controllers.add(createController(genome));
        }
        
        return controllers;
    }
    public NEATController createController(Genome genome){
        NEATController con = new NEATController();
        con.genome = genome;
        return con;
    }
    public ArrayList<Runnable> neatSimulation(ArrayList<NEATController> controller){
  
        
        ArrayList<Runnable> threads = new ArrayList<>();
        
        for(NEATController con:controller){
            
            threads.add(makeNEATControlledGame(con,LEARNING_STEP_DELAY,DISPLAY_WHILE_LEARNING));
            
        }
        return threads;
        
    }
    public NEATController makeNEATControlledGame(NEATController con,long delay,boolean visible){
        TetrisGame.GameFrame[] gm = new TetrisGame.GameFrame[1];
        Runnable r =() ->{
                try{
                    gm[0] = TetrisGame.initNewGame(visible);
                    TetrisGame game = gm[0].game;
                    con.gm = gm[0];
                    while(!game.gameOver){
                        con.makeMove();
                        long de = Math.max(delay, LEARNING_STEP_DELAY);
                        if(de>0)
                            Thread.sleep(de);
                    }
                    System.out.println("Game Over "+game.score+" "+con.genome.ID);


                }catch (InterruptedException e){
                    System.err.println("Interrupted ");
                }finally{
                    gm[0].frame.dispose();
                }
            };
        con.logic = r;
        return con;
    }
    public void learn(ArrayList<NEATController> controller){
        if(best == null)
            best = controller.get(0);
        for(NEATController con:controller){
            
            double fitness = con.evaluateFitness();
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
//        exe = Executors.newFixedThreadPool(THREAD_COUNT);
        exe.setRunnerSize(THREAD_COUNT);
        new Thread( () ->{
            while(leftToEnqueue.decrementAndGet()>=0){
                controller = createControllers(pool);
                ArrayList<Runnable> neatSimulation = neatSimulation(controller);
                leftExecuting.getAndSet(controller.size());
                CountDownLatch latch = new CountDownLatch(controller.size());
                for(Runnable r:neatSimulation){
                    exe.submit(() ->{
                            r.run();
                            latch.countDown();
                        }
                    );
                }
                
                try {
                    latch.await();
                } catch (InterruptedException ex) {
                    Logger.getLogger(MenuController.class.getName()).log(Level.SEVERE, null, ex);
                }
                learn(controller);



                System.out.println(best.game().rowsCleared+" "+best.genome.fitness+"  "+best.genome.links.size());
                pool.newGeneration();
                System.out.println("New generation: "+pool.generation);
                update();
                
                if(pool.generation%10 == 0){
                    new Thread( () ->{
                            try {
                                save("pool"+pool.generation);
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
            generationLabel.setText(pool.generation+"");
            if(best != null && best.genome != null)
                bestScoreLabel.setText(best.genome.fitness.intValue()+"");
            LEARNING_STEP_DELAY = Integer.parseInt(learningDelayText.getText());
        });
    }
    public void init(){
        
        pool = new Pool(22*10,4);
        pool.startingGeneration(Integer.parseInt(populationText.getText()));
        for(Genome genome:pool.getPopulation()){
            Log.print(genome.ID,genome.nodes.size(),genome.links.size());
        }
        
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
        exe.stopEverything(false);
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
            System.out.println("Running");
            return;
        }
        try{
            System.out.println("Before read");
        ArrayList<String> read = new ArrayList<>(LibraryLB.FileManaging.FileReader.readFromFile(this.generationText.getText()));
        pool = g.fromJson(read.get(0), Pool.class);
        System.out.println("After read");
        System.out.println(best == null);
//        update();
        System.out.println("Load done");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

