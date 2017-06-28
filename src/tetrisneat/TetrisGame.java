/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tetrisneat;

import LibraryLB.Threads.Sync.ConditionalWait;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class TetrisGame extends JPanel {
    public static long winScore = 500000;
    public static int spawnFluctuation = 4; // max 4
    public static boolean DETERMINISTIC = false;
//    public static Integer[] usablePieces = {0, 1, 2, 3, 4, 5, 6};
    
//    public static Integer[] usablePieces = {0};//LINE OK
//    public static Integer[] usablePieces = {1};//DNF >440
//    public static Integer[] usablePieces = {2};//DNF >100
    public static Integer[] usablePieces = {3};//BLOCK OK
//    public static Integer[] usablePieces = {4};//DNF >100
//    public static Integer[] usablePieces = {5};//DNF >100
//    public static Integer[] usablePieces = {6};//DNF >100
//    public static Integer[] usablePieces = {0,3};// LINE + BLOCK
    
    public boolean visible;
    public int rowsCleared;
    public boolean gameOver;
    public ConditionalWait waitTool = new ConditionalWait();
    
    private static final long serialVersionUID = -8715353373678321308L;

    private final Point[][][] Tetraminos = {
        // I-Piece
        {
                { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(3, 1) },
                { new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(1, 3) },
                { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(3, 1) },
                { new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(1, 3) }
        },

        // J-Piece
        {
                { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(2, 0) },
                { new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(2, 2) },
                { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(0, 2) },
                { new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(0, 0) }
        },

        // L-Piece
        {
                { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(2, 2) },
                { new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(0, 2) },
                { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(0, 0) },
                { new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(2, 0) }
        },

        // O-Piece
        {
                { new Point(0, 0), new Point(0, 1), new Point(1, 0), new Point(1, 1) },
                { new Point(0, 0), new Point(0, 1), new Point(1, 0), new Point(1, 1) },
                { new Point(0, 0), new Point(0, 1), new Point(1, 0), new Point(1, 1) },
                { new Point(0, 0), new Point(0, 1), new Point(1, 0), new Point(1, 1) }
        },

        // S-Piece
        {
                { new Point(1, 0), new Point(2, 0), new Point(0, 1), new Point(1, 1) },
                { new Point(0, 0), new Point(0, 1), new Point(1, 1), new Point(1, 2) },
                { new Point(1, 0), new Point(2, 0), new Point(0, 1), new Point(1, 1) },
                { new Point(0, 0), new Point(0, 1), new Point(1, 1), new Point(1, 2) }
        },

        // T-Piece
        {
                { new Point(1, 0), new Point(0, 1), new Point(1, 1), new Point(2, 1) },
                { new Point(1, 0), new Point(0, 1), new Point(1, 1), new Point(1, 2) },
                { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(1, 2) },
                { new Point(1, 0), new Point(1, 1), new Point(2, 1), new Point(1, 2) }
        },

        // Z-Piece
        {
                { new Point(0, 0), new Point(1, 0), new Point(1, 1), new Point(2, 1) },
                { new Point(1, 0), new Point(0, 1), new Point(1, 1), new Point(0, 2) },
                { new Point(0, 0), new Point(1, 0), new Point(1, 1), new Point(2, 1) },
                { new Point(1, 0), new Point(0, 1), new Point(1, 1), new Point(0, 2) }
        }
    };

    private final Color[] tetraminoColors = {
            Color.cyan, Color.blue, Color.orange, Color.yellow, Color.green, Color.pink, Color.red
    };

    private Point pieceOrigin;
    private int currentPiece;
    private int rotation;
    private ArrayList<Integer> nextPieces = new ArrayList<Integer>();

    public long score;
    private Color[][] well;

    // Creates a border around the well and initializes the dropping piece
    private void init() {
            int height = 23;
            int width = 12;
            well = new Color[width][height];
            for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                            if (i == 0 || i == 11 || j == 22) {
                                    well[i][j] = Color.GRAY;
                                    
                            } else {
                                    well[i][j] = Color.BLACK;
                            }
                    }
            }
            newPiece();
    }
    private Random r = new Random();
    // Put a new, random piece into the dropping position
    public void newPiece() {
            if(winScore<=score){
                gameOver = true;
                return;
            }
            if(DETERMINISTIC){
                pieceOrigin = new Point(5,2);
            }else{
                int right = 5 - spawnFluctuation;
                pieceOrigin = new Point(right+r.nextInt(spawnFluctuation), 2);
            }
                            
            rotation = 0;
            if (collidesAt(pieceOrigin.x, pieceOrigin.y + 1, rotation)) {
                gameOver = true;
                return;
            }
            
            if (nextPieces.isEmpty()) {
                    
                    Collections.addAll(nextPieces,usablePieces);
                    if(!DETERMINISTIC)
                    Collections.shuffle(nextPieces);
            }
            currentPiece = nextPieces.remove(0);
//            nextPieces.remove(0);
    }

    // Collision test for the dropping piece
    private boolean collidesAt(int x, int y, int rotation) {
            for (Point p : Tetraminos[currentPiece][rotation]) {
                    if (well[p.x + x][p.y + y] != Color.BLACK) {
                            return true;
                    }
            }
            return false;
    }

    // Rotate the piece clockwise or counterclockwise
    public void rotate(int i) {
            int newRotation = (rotation + i) % 4;
            if (newRotation < 0) {
                    newRotation = 3;
            }
            if (!collidesAt(pieceOrigin.x, pieceOrigin.y, newRotation)) {
                    rotation = newRotation;
            }
            if(visible)
                repaint();
    }

    // Move the piece left or right
    public void move(int i) {
            if (!collidesAt(pieceOrigin.x + i, pieceOrigin.y, rotation)) {
                    pieceOrigin.x += i;	
            }
            if(visible)
                repaint();
    }

    // Drops the piece one line or fixes it to the well if it can't drop
    public void dropDown() {
            if (!collidesAt(pieceOrigin.x, pieceOrigin.y + 1, rotation)) {
                    pieceOrigin.y += 1;
            } else {
                    fixToWell();
            }	
            if(visible)
                repaint();
    }

    // Make the dropping piece part of the well, so it is available for
    // collision detection.
    public void fixToWell() {
            for (Point p : Tetraminos[currentPiece][rotation]) {
                    well[pieceOrigin.x + p.x][pieceOrigin.y + p.y] = tetraminoColors[currentPiece];
            }
            clearRows();
            newPiece();
    }

    public void deleteRow(int row) {
            for (int j = row-1; j > 0; j--) {
                    for (int i = 1; i < 11; i++) {
                            well[i][j+1] = well[i][j];
                    }
            }
    }

    // Clear completed rows from the field and award score according to
    // the number of simultaneously cleared rows.
    public void clearRows() {
            boolean gap;
            int numClears = 0;

            for (int j = 21; j > 0; j--) {
                    gap = false;
                    for (int i = 1; i < 11; i++) {
                            if (well[i][j] == Color.BLACK) {
                                    gap = true;
                                    break;
                            }
                    }
                    if (!gap) {
                            deleteRow(j);
                            j += 1;
                            numClears += 1;
                    }
            }
            this.rowsCleared+=numClears;
            switch (numClears) {
            case 1:
                    score += 1000;
                    break;
            case 2:
                    score += 3000;
                    break;
            case 3:
                    score += 5000;
                    break;
            case 4:
                    score += 8000;
                    break;
            }
    }

    // Draw the falling piece
    private void drawPiece(Graphics g) {		
            g.setColor(tetraminoColors[currentPiece]);
            for (Point p : Tetraminos[currentPiece][rotation]) {
                    g.fillRect((p.x + pieceOrigin.x) * 26, 
                                       (p.y + pieceOrigin.y) * 26, 
                                       25, 25);
            }
    }
    
    
    
    @Override 
    public void paintComponent(Graphics g){
        if(!visible){
            return;
        }
        // Paint the well
        g.fillRect(0, 0, 26*12, 26*23);
        for (int i = 0; i < 12; i++) {
                for (int j = 0; j < 23; j++) {
                        g.setColor(well[i][j]);
                        g.fillRect(26*i, 26*j, 25, 25);
                }
        }

        // Display the score
        g.setColor(Color.WHITE);
        g.drawString("" + score, 19*12, 25);

        // Draw the currently falling piece
        drawPiece(g);
    }


    
    public Integer[][] getBoard(){
        Integer[][] output = new Integer[well[0].length-1][well.length-2];
        for(int i=1; i<well.length-1;i++){
            for(int j=0; j<well[i].length-1;j++){

                int rgb =-1;
                if(null!=well[i][j]){
                    if(well[i][j] == Color.BLACK){
                        rgb = 0;
                    }
                    else if(well[i][j] == Color.GRAY){
                        rgb = -1;
                    }
                    else{
                        rgb = 1;
                    }
                }
                output[j][i-1] = rgb;
            }
        }
        int x = pieceOrigin.x;
        int y = pieceOrigin.y;
        Point[] name = this.Tetraminos[this.currentPiece][rotation];
        for(Point p:name){
            output[y+p.y][x+p.x-1] = 1;
        }
//        System.out.println(output.length+" "+output[0].length);
        return output;
    }
    
    public Integer[] getHighestColumns(){
        Integer[][] output = new Integer[well[0].length][well.length];
        for(int i=0; i<well.length;i++){
            for(int j=0; j<well[i].length;j++){

                int rgb =-1;
                if(null!=well[i][j]){
                    if(well[i][j] == Color.BLACK){
                        rgb = 0;
                    }
                    else if(well[i][j] == Color.GRAY){
                        rgb = -1;
                    }
                    else{
                        rgb = 1;
                    }
                }
                output[j][i] = rgb;
            }
        }
        Integer[] max = new Integer[output[0].length-2];//frame
        for(int i=0; i<max.length; i++){
            max[i] = 0;
        }
        for(int i=0; i<output.length-1; i++){
            for(int j=0; j<max.length; j++){
                if(output[i][j+1] == 1){
                    max[j] = Math.max(max[j], output.length-1 - i);
                }
            }
        }
        return max;
    }
    public static TetrisGame inittNewGame(){
        return new TetrisGame();
    }
    public static GameFrame initNewGame(boolean visible){
        JFrame f = new JFrame("Tetris");
        final TetrisGame game = new TetrisGame();
        game.visible = visible;
        game.init();
        f.add(game);
//        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(12*26+10, 26*23+25);
        f.setVisible(visible);
        
        if(visible)
            f.repaint();
        
        

        
        f.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
                game.gameOver = true;
                game.waitTool.wakeUp();
            }
        });
        // Keyboard controls
        f.addKeyListener(new KeyListener() {
                public void keyTyped(KeyEvent e) {
                }

                public void keyPressed(KeyEvent e) {
                        switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP:
                                game.rotate(-1);
                                break;
                        case KeyEvent.VK_DOWN:
                                game.rotate(+1);
                                break;
                        case KeyEvent.VK_LEFT:
                                game.move(-1);
                                break;
                        case KeyEvent.VK_RIGHT:
                                game.move(+1);
                                break;
                        case KeyEvent.VK_SPACE:
                                game.dropDown();
                                game.score += 1;
                                break;
                        case KeyEvent.VK_ESCAPE:
                            f.dispatchEvent(new WindowEvent(f,WindowEvent.WINDOW_CLOSING));
                            game.gameOver = true;
                            break;
                        
                        case KeyEvent.VK_I:
                            String out = "";
                            Color[][] well = game.well;
                            Integer[][] output = new Integer[game.well[0].length][game.well.length];
                            for(int i=0; i<well.length;i++){
                                for(int j=0; j<well[i].length;j++){

                                    int rgb =-1;
                                    if(null!=well[i][j]){
                                        if(well[i][j] == Color.BLACK){
                                            rgb = 0;
                                        }
                                        else if(well[i][j] == Color.GRAY){
                                            rgb = -1;
                                        }
                                        else{
                                            rgb = 1;
                                        }
                                    }
                                    output[j][i] = rgb;
                                }
                            }
                            for(Integer[] array: game.getBoard()){
                                out+= Arrays.asList(array).toString()+"\n";

                            }
                            
                            System.out.println(out);
                            break;
                            
                        case KeyEvent.VK_O:
                            System.out.println(Arrays.asList(game.getHighestColumns()).toString());
                            break;
                        
                        case KeyEvent.VK_F:
                            System.out.println(Arrays.toString(formatBoard(game.getBoard())));
                        }
                        
                        game.waitTool.wakeUp();
                }

                public void keyReleased(KeyEvent e) {
                }
        });
        
        GameFrame gm = new GameFrame();
        gm.frame = f;
        gm.game = game;
        return gm;

    }
    
    public static class GameFrame{
        public JFrame frame;
        public TetrisGame game;
    }
    public static double[] formatBoard(Integer[][] board){
        double[] res = new double[board.length*board[0].length];
        for(int i=0;i<board.length;i++){
            for(int j=0; j<board[i].length;j++){
                res[i*board[i].length+j] = board[i][j];
            }
        }
        return res;
    }
    
}
