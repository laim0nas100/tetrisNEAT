/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tetrisneat;

import NEATPort.Genome;
import tetrisneat.TetrisGame.GameFrame;
import static tetrisneat.TetrisGame.formatBoard;
/**
 *
 * @author Lemmin
 */
public class NEATController implements Runnable {
    public double fitness;
    public Genome genome;
    public GameFrame gm;
    public long movesMade;
    public Runnable logic;
    public TetrisGame game(){
        return gm.game;
    }
    public void makeMove(){
        
        TetrisGame game = gm.game;
        double[] move = genome.evaluateNetwork(formatBoard(game.getBoard()));
        int i = 0;
        int max = 0;
        for(;i<4;i++){
            if(move[max]<move[i]){
                max = i;
            }
        }
        switch(max){
            case 0:
                game.move(1);
                break;
            case 1:
                game.move(-1);
                break;
            case 2:
                game.dropDown();
                game.score+=2;
                break;
            case 3:
                game.rotate(1);
                break;
        }
        game.score+=1;
        movesMade++;
        gm.game.dropDown();
    }
    public double evaluateFitness(){
        if(gm == null){
            return 0;
        }
        fitness = Math.pow(game().score,2);
        genome.fitness = fitness;
        return fitness;
    }
    @Override
    public void run() {
        logic.run();
    }
}
