
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tetrisneat.fxml;

import Evoliution.NEAT.Genome;
import Evoliution.NEAT.interfaces.GenomeMaker;
import Evoliution.NEAT.HyperNEAT.HyperGenome;
import Evoliution.NEAT.HyperNEAT.HyperNEATSpace;
import Evoliution.NEAT.Pool;
import Evoliution.NEAT.imp.DefaultHyperNEATMutator;
import Evoliution.NEAT.interfaces.GenomeMutator;
import LibraryLB.DelayedLog;
import LibraryLB.FX.SceneManagement.BaseController;
import LibraryLB.Log;
import LibraryLB.Threads.DynamicTaskExecutor;
import Misc.F;

import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
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
    @FXML public Label speciesLabel;
    @FXML public Label bestScoreLabel;
    @FXML public Label bestGenerationScoreLabel;
    @FXML public Label progressStagnationLabel;

    @FXML public TextField enqueueText;
    @FXML public TextField populationText;
    @FXML public TextField learningDelayText;
    @FXML public TextField generationText;
    @FXML public TextField resetFromBestAfterText;
    @FXML public TextField seedText;
    @FXML public TextField genomeLoggingText;
    @FXML public TextField hyperLayers;
    
    @FXML public TextField confMaxGen;
    @FXML public TextField confTimes;
    
    @FXML public CheckBox useHyperNEAT;
    @FXML public ComboBox usablePieces; 
    
    
    public Gson g = new Gson();
    public int[] dim = new int[]{22,10,2};
    public HyperNEATSpace space = new HyperNEATSpace(dim);
    public AtomicInteger leftToEnqueue = new AtomicInteger(0);
    public AtomicInteger leftExecuting = new AtomicInteger(0);
    public boolean running = false;
    public boolean DISPLAY_WHILE_LEARNING = true;
    public long LEARNING_STEP_DELAY = 0;
    public static final long BEST_STEP_DELAY = 50;
    public static final int THREAD_COUNT = 4;
    public static int resetFromBestAfter = 10;
    public static int progressStagnation = -1;
    
    public String genomeLoggingFilePrefix;
    public GenomeMutator mutator = new DefaultHyperNEATMutator();
    public NEATController best = null;
    public NEATController generationBest = null;
    public ArrayList<NEATController> controllers = new ArrayList<>();
    public Pool pool;
    public DynamicTaskExecutor exe = new DynamicTaskExecutor();
    
    public DelayedLog dLog = new DelayedLog();
    public ArrayList<NEATController> createControllers(Pool pool){
        ArrayList<NEATController> cont = new ArrayList<>();
        for(Genome genome : pool.getPopulation()){
            cont.add(createController(genome));
        }
        
        return cont;
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
    
    @Override
    public void initialize(){
        ObservableList list = FXCollections.observableArrayList();
        list.add("I");
        list.add("L");
        list.add("J");
        list.add("O");
        list.add("S");
        list.add("T");
        list.add("Z");
        list.add("All");
        this.usablePieces.setItems(list);
            this.usablePieces.getSelectionModel().selectFirst();
    }
    
    public void reset(){
        stop();
        pool = null;
        best = null;
        generationBest = null;
        controllers = new ArrayList<>();
        progressStagnation = 0;
        
    }
    
    public Integer[] getUsablePieces(){
        int selectedIndex = this.usablePieces.getSelectionModel().getSelectedIndex();
        switch(selectedIndex){
            case (0): return new Integer[]{0};
            case (1): return new Integer[]{1};
            case (2): return new Integer[]{2};
            case (3): return new Integer[]{3};
            case (4): return new Integer[]{4};
            case (5): return new Integer[]{5};
            case (6): return new Integer[]{6};
            default: return new Integer[]{0,1,2,3,4,5,6};
                
        }
        
    }
    public NEATController makeNEATControlledGame(NEATController con,long delay,boolean visible){
        TetrisGame.GameFrame[] gm = new TetrisGame.GameFrame[1];
        Runnable r =() ->{
                try{
                    gm[0] = TetrisGame.initNewGame(visible,getUsablePieces());
                    TetrisGame game = gm[0].game;
                    con.gm = gm[0];
                    while(!game.gameOver){
                        con.makeMove();
                        long de = Math.max(delay, LEARNING_STEP_DELAY);
                        if(de>0)
                            Thread.sleep(de);
                    }
                    Log.print("Game Over "+game.score);


                }catch (Exception e){
                    e.printStackTrace();
                }finally{
                    gm[0].frame.dispose();
                }
            };
        con.logic = r;
        return con;
    }
    
    
    public void learn(ArrayList<NEATController> controller){
        
        
        Log.print("Learn init ",controller.size());
        generationBest = controller.get(0);
        for(NEATController con:controller){          
            double fitness = con.evaluateFitness();
            if(generationBest.evaluateFitness() < fitness){
                generationBest = con;
            }
        }
        progressStagnation++;
        if(best == null || best.evaluateFitness() < generationBest.evaluateFitness()){
            best = generationBest;
            progressStagnation = 0;
            
            Log.print("Assign best");
        }
        if(resetFromBestAfter <=0){
            return;
        }else{
            if(progressStagnation >=resetFromBestAfter){
                progressStagnation = 0;
                this.resetFromBestNoStop();
            }
        }
    }
    public void neat1(){
        Log.print("NEAT1");
        controllers = createControllers(pool);
        ArrayList<Runnable> neatSimulation = neatSimulation(controllers);
        int size = neatSimulation.size();
        leftExecuting.set(size);
        AtomicInteger finished = new AtomicInteger(0);
        for(Runnable r:neatSimulation){
            exe.submit(() ->{
                    r.run();
                    leftExecuting.decrementAndGet();
                    finished.incrementAndGet();
                }
            );
        }

        while(leftExecuting.get()>0){
//                    Log.print(leftExecuting.get());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        Log.print("Stopped executing");
        if(finished.get() != size ) {
                stop();
                running = false;
                update();
                Log.println("STOPPED");
                return;
        }
        learn(controllers);
//        this.logGenomeFiteness();

        if(best.gm == null){
            throw new Error("Game is null");
        }
        Log.print(best.game().rowsCleared+" "+best.genome.fitness+"  "+best.genome.genes.size());
        pool.newGeneration();
        Log.print("New generation: "+pool.generation);
        update();

//        if(pool.generation%10 == 0){
//            try {
//
//                save("pool"+pool.generation+".json");
//            } catch (FileNotFoundException | UnsupportedEncodingException x) {
//                x.printStackTrace();
//            }  
//        }
    }
    
    public void neat() throws InterruptedException{
        if(pool == null){
            return;
        }
        update();
        if(running){
            return;
        }
        running = true;
//        exe = Executors.newFixedThreadPool(THREAD_COUNT);
        exe.setRunnerSize(THREAD_COUNT);
        new Thread( () ->{
            while(leftToEnqueue.decrementAndGet()>=0){
                neat1();
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
        System.exit(0);
//        F.executor.shutdown();
    }
    
    public void log(String str, String file){
        this.dLog.log(file, str);
    }
    
    
    public void runConfiguration(){
        int maxGen = Integer.parseInt(this.confMaxGen.getText());
        int times = Integer.parseInt(this.confTimes.getText());
        this.runConfiguration(maxGen, times);
    }
    public void runConfiguration(int maxGen, int times){
        exe.setRunnerSize(THREAD_COUNT);

        Runnable r = () ->{
            String name = this.genomeLoggingText.getText();
            String file = name + "Log.txt";
            for(int i = 0; i < times; i++){                
                this.reset();
                this.init();
                long seed = System.currentTimeMillis();
                F.RND.setSeed(seed);
                int iter = 0;
                double fitness = 0d;
                do{
                    iter++;
                    neat1();
                    fitness = best.evaluateFitness();
                }while(fitness<TetrisGame.winScore && this.pool.generation < maxGen);
                
                
                String str = iter+"\t\t\t"+fitness;
                dLog.log(file, str);
            }
        };
        new Thread(r).start();
    }
    public void logGenomeFiteness(){
        
        
        this.genomeLoggingFilePrefix = genomeLoggingText.getText();
        if(this.genomeLoggingFilePrefix == null || this.genomeLoggingFilePrefix.isEmpty()){
            return;
        }
        String str = "";
        ArrayList<Double> list = new ArrayList<>(this.controllers.size());
        for(NEATController contr:this.controllers){
            list.add(contr.evaluateFitness());
        }
        Collections.sort(list,Collections.reverseOrder() );
        
        int maxString = 4;
        String text = pool.generation+"";
        while(text.length() < maxString){
            text = "0"+text;
        }
        
        String fileName = genomeLoggingFilePrefix + text+".txt";
        
        for(Double d:list){
            
            dLog.log(fileName, d.toString());
        }
        
        
    }
    @Override
    public void update() {
        Platform.runLater(() ->{
            this.genomeLoggingFilePrefix = genomeLoggingText.getText();
            enqueueLabel.setText(""+leftToEnqueue.get());
            if(pool==null){
                generationLabel.setText("0");
                speciesLabel.setText("0");
            }else{
                generationLabel.setText(pool.generation+"");
                speciesLabel.setText(pool.species.size()+"");
            }
            if(best != null && best.genome != null){
                bestScoreLabel.setText((int)best.genome.fitness+"");
            }else{
                bestScoreLabel.setText("0");
            }
            if(generationBest != null && generationBest.genome != null){
                bestGenerationScoreLabel.setText((int)generationBest.genome.fitness+"");
            }else{
                bestGenerationScoreLabel.setText("0");
            }
            LEARNING_STEP_DELAY = Integer.parseInt(learningDelayText.getText());
            resetFromBestAfter = Integer.parseInt(resetFromBestAfterText.getText());
            if(progressStagnation >= 0){
                progressStagnationLabel.setText(progressStagnation + "");
            }
        });
    }
    public void init(){
        int seed = 0;
        try{
            seed = Integer.parseInt(this.seedText.getText());
        }catch(Exception e){
            e.printStackTrace();
        }
        F.RND.setSeed(seed);
        
        final int generationSize = Integer.parseInt(populationText.getText());
        int layers = 0;
        try{
            layers = Integer.parseInt(this.hyperLayers.getText());
        }catch(Exception e){
            e.printStackTrace();
        }
        if(layers<2){
            layers = 2;
        }
        final int fLayers = layers;
        GenomeMaker hyperNeatMaker = new GenomeMaker() {
            @Override
            public Collection<Genome> initializeGeneration() {
                ArrayList<Genome> genomes = new ArrayList<>();
                dim = new int[]{dim[0],dim[1],fLayers};
                space = new HyperNEATSpace(dim);
                for(int i = 0; i < generationSize; i++){
                    HyperGenome genome = new HyperGenome(dim);
                    genome.space = space;
                    genomes.add(genome);
                }
                return genomes;
            }
        };
        
        GenomeMaker neatMaker = new GenomeMaker() {
            @Override
            public Collection<Genome> initializeGeneration() {
                
                ArrayList<Genome> genomes = new ArrayList<>();
                for(int i = 0; i < generationSize; i++){
                    Genome g = new Genome(22*10,4);
                    genomes.add(g);
                }
                return genomes;
            }
        };
       
//        mutator = new DefaultNEATMutator();
//        mutator = new DefaultHyperNEATMutator();
        GenomeMaker maker;
        if(useHyperNEAT.isSelected()){
            maker = hyperNeatMaker;
        }else{
            maker = neatMaker;
        }
        
        pool = new Pool(maker, mutator);
//        pool = new Pool(22*10, 4, Integer.parseInt(populationText.getText()));

//        pool.stats.POPULATION = Integer.parseInt(populationText.getText());
//        pool.stats.INPUTS = 22*10;
//        pool.stats.OUTPUTS = 4;
//        pool.initializePool();
//        controllers = createControllers(pool);
//        learn(controllers);
        Log.print("INIT DONE");
        update();
        
        
    }
    public void enqueue() throws InterruptedException{
        int enq = Integer.parseInt(enqueueText.getText());
        this.enqueue(enq);
        
    }
    public void enqueue(int enq) throws InterruptedException{
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
        update();
    }
    
    public void resetFromBest(){
        stop();
//        pool.newGeneration(pool.allTimeBest);
        resetFromBestNoStop();
    }
    
    public void resetFromBestNoStop(){
        pool.newGeneration((Genome) pool.allTimeBest.clone());
    }
    
    
    public void save() throws FileNotFoundException, UnsupportedEncodingException{
        save(this.generationText.getText());
    }
    public void save(String where) throws FileNotFoundException, UnsupportedEncodingException{
//        pool.prepareToSerialize();
//        for(Genome genome:pool.getPopulation()){
//            genome.generateNetwork();
//        }
        Log.print("All time best:"+pool.allTimeBest.fitness);
        String toJson = g.toJson(pool);
        LibraryLB.FileManaging.FileReader.writeToFile(where, Arrays.asList(toJson));
        Log.print("Saved as:"+where);
//        pool.restoreAfterSerialize();
    }
    public void load() throws FileNotFoundException, IOException{
        if(running){
            Log.print("Running");
            return;
        }
        try{
            Log.print("Before read");
            ArrayList<String> read = new ArrayList<>(LibraryLB.FileManaging.FileReader.readFromFile(this.generationText.getText()));
            pool = g.fromJson(read.get(0), Pool.class);
            Log.print("After read");
            pool.mutator = mutator;
//            if(useHyperNEAT.isSelected()){
//                for(Genome genome:pool.getPopulation()){
//                    if(genome instanceof HyperGenome){
//                        ((HyperGenome) genome).space = space;
//                    }
////                    genome.needUpdate = true;
//                }
//            }
//            pool.afterDeserialization();
            best = createController(pool.allTimeBest);
            
    //        best.genome.generateNetwork();
            Log.print(best == null);
    //        update();
            Log.print("Load done");
        }catch (Exception e){
            e.printStackTrace();
        }
        
    }
}

