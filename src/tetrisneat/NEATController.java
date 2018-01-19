/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tetrisneat;


import Evoliution.NEAT.Genome;
import tetrisneat.TetrisGame.GameFrame;

/**
 *
 * @author Lemmin
 */
public class NEATController implements Runnable {
    public double fitness = 0d;
    public Genome genome;
    public GameFrame gm;
    public double movesMade;
    public Runnable logic;
    public TetrisGame game(){
        return gm.game;
    }
    public double[] formatBoard(Integer[][] board){
        double[] res = new double[board.length*board[0].length];
        for(int i=0;i<board.length;i++){
            for(int j=0; j<board[i].length;j++){
                res[i*board[i].length+j] = board[i][j];
            }
        }
        return res;
    }
    
    public void makeMove(){
        
        TetrisGame game = gm.game;
        Integer[][] board;
        board = game.getBoardNew();
        double[] formatBoard = formatBoard(board);
        double[] move = genome.evaluate(formatBoard);
        int regionSize = move.length / 4;
//        Log.print("In controller" +Arrays.toString(move));
        int i = 0;
        int max = 0;
        double[] finalMove = new double[4];
        for(;i<4;i++){
            for(int j = regionSize * i; j < regionSize * (i+1); j++){
                finalMove[i]+= move[j];
            }
            finalMove[i] = finalMove[i] / regionSize;
            if(finalMove[max]<finalMove[i]){
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
                game.score+=10;
                break;
            case 3:
                game.rotate(1);
                break;
        }
        game.dropDown();
        game.score+=1;
        movesMade++;
        
    }
    public double evaluateFitness(){
        if(gm == null){
            return 0;
        }
        fitness = Math.max(fitness, game().score);
//        fitness /= Math.sqrt(movesMade);
        genome.fitness = (float) fitness;
        return fitness;
    }
    @Override
    public void run() {
        logic.run();
    }
}
