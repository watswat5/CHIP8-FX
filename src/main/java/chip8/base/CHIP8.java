package chip8.base;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import java.io.FileNotFoundException;
import java.io.File;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.io.InputStream;
import java.io.FileInputStream;

/**
*   Class representation of a CHIP8 computer.
*/
public class CHIP8 extends Application{

    public static boolean DEBUG = true;
    /**General purpose RAM.*/
    int[] ram;

    /** General Purpose Registers Vx.*/
    int[] gpr;

    /** Delay timer.*/
    int dt;

    /**Sound timer.*/
    int st;

    /** Program Counter. */
    int pc;

    /**Stack pointer. */
    int sp;

    /**Memory Address Register. */
    int I;

    /**Stack array.*/
    int[] stack;

    /**Display area.*/
    boolean[][] display;

    /** Numpad */
    boolean[] numpad;

    /**Numpad Mapping*/
    String[] mapping;

    /**ROM path.*/
    String rom;

    /**Timer variable.*/
    private long lastTime;

    /**Step timer.*/
    Timeline stepTimer;

    /**Speed in Hz*/
    double frequency;

    /**Used to pause execution for debugging.*/
    boolean paused;

    Scene mainScene;
    Canvas canvas;

    public void start(Stage stage) {
        Parameters params = getParameters();
        rom = params.getNamed().get("rom");//"3-corax+.ch8";
        frequency = Double.parseDouble(params.getNamed().get("clock"));//Clock speed

        //Enable/disable debug
        if (params.getNamed().containsKey("debug")) {
            String dbg = params.getNamed().get("debug");
            DEBUG = dbg.toUpperCase().compareTo("TRUE") == 0;
        } else {
            DEBUG = false;
        }

        reset();
        stepTimer = new Timeline( new KeyFrame(Duration.seconds(1.0/frequency),(e) -> step()));
        stepTimer.setCycleCount(Timeline.INDEFINITE);

        //Set up numpad listener
        canvas = new Canvas(640,320);
        Pane root = new Pane(canvas);
        mainScene = new Scene(root);
        stage.setScene(mainScene);
        stage.setTitle("CHIP8");
        stage.setResizable(false);
        mainScene.setOnKeyPressed( e -> keyPressed(e) );
        mainScene.setOnKeyReleased( e -> keyReleased(e) );
        stage.show();
        stepTimer.play();
    }

    public void reset() {
        //Init all variables
        paused = true;
        ram = new int[4096];
        gpr = new int[16];
        dt = 0;
        st = 0;
        //draw_timer = 60;
        stack = new int[16];
        sp = 0;
        pc = 0x200;
        display = new boolean[64][32];
        I = 0;
        numpad = new boolean[16];
        mapping = new String[]{"x","1","2","3","q","w","e","a","s","d","z","c","4","r","f","v"};

        //Load charset into RAM
        int[] charset = {0xF0,0x90,0x90,0x90,0xF0,0x20,0x60,0x20,0x20,0x70,0xF0,0x10,0xF0,0x80,0xF0,0xF0,0x10,0xF0,0x10,0xF0,0x90,0x90,0xF0,0x10
                        ,0x10,0xF0,0x80,0xF0,0x10,0xF0,0xF0,0x80,0xF0,0x90,0xF0,0xF0,0x10,0x20,0x40,0x40,0xF0,0x90,0xF0,0x90,0xF0,0xF0,0x90,0xF0
                        ,0x10,0xF0,0xF0,0x90,0xF0,0x90,0x90,0xE0,0x90,0xE0,0x90,0xE0,0xF0,0x80,0x80,0x80,0xF0,0xE0,0x90,0x90,0x90,0xE0,0xF0,0x80
                        ,0xF0,0x80,0xF0,0xF0,0x80,0xF0,0x80,0x80};
        for (int i = 0; i < charset.length; i++) {
            ram[i] = (charset[i] & 0xFF);
        }

        try {
            loadRom(rom);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("File not found!");
        }
        //Init timer variable
        lastTime = System.currentTimeMillis();
        paused = false;
    }

    public void step() {
        if (paused){
            //debug("Paused");
            return;
        }

        //Display update timer
        if (System.currentTimeMillis() - lastTime > 16) {
            dt = dt > 0 ? (dt - 1) : 0;
            st = st > 0 ? (st - 1) : 0;
            updateDisplay();
            lastTime = System.currentTimeMillis();
        }

        //Fetch instruction
        int high = ram[pc];
        int low = ram[pc + 1];

        //Decode Instruction
        String lower = String.format("%02X", low);
        String upper = String.format("%02X", high);
        debug(pc + ": " + upper.substring(upper.length() - 2, upper.length()) + " " + lower.substring(lower.length() - 2, lower.length()) + "\n");

        //00 high
        if ((high & 0xFF) == 0) {
            if ((low & 0xFF) == 0xE0) { //CLS
                debug("Clear Display");
                clearDisplay();
                pc+=2;
                return;
            } else if ((low & 0xFF) == 0xEE) { //RET
                debug("Return");
                pc = stack[sp--] + 2;
                return;
            }
        }

        //JP
        if (((high & 0xF0)) == 0x10) {
            debug("jmp: " + (low + (high << 8) & 0x0FFF));
            pc = (((high & 0x0F) << 8) + (low & 0xFF));
            return;
        }

        //CALL
        if ((high  & 0xF0) == 0x20) {
            sp++;
            stack[sp] = pc;
            int addr = (((high & 0x0F) << 8) + (low & 0xFF));
            debug("CALL: " + addr);
            pc = addr;
            return;
        }

        //Skip Equal Literal
        if ((high  & 0xF0) == 0x30) {
            if (gpr[high & 0x0F] == (low & 0xFF)) {
                debug("Skipped " + (pc + 2));
                pc += 4;
            } else {
                pc += 2;
            }
            return;
        }

        //Skip Not Equal
        if ((high  & 0xFF) >>> 4 == 4) {
            if (gpr[high & 0x0F] != (low & 0xFF)) {
                debug("Skipped " + (pc + 2));
                pc += 4;
            } else {
                pc += 2;
            }
            return;
        }

        //Skip Equal Register
        if ((high  & 0xFF) >>> 4 == 5) {
            if (gpr[high & 0x0F] == gpr[(low >>> 4) & 0x0F]) {
                debug("Skipped " + (pc + 2));
                pc += 4;
            } else {
                pc += 2;
            }
            return;
        }

        //Load Literal
        if ((high  & 0xFF) >>> 4 == 6) {
            debug("Loading " + (low & 0xFF) + " into GPR" + (high & 0x0F));
            gpr[high & 0x0F] = low & 0xFF;
            pc+=2;
            return;
        }

        //Add Literal
        if ((high  & 0xFF) >>> 4 == 7) {
            debug("Adding " + (low & 0xFF) + " into GPR" + (high & 0x0F));
            gpr[high & 0x0F] += (low & 0xFF);
            while (gpr[high & 0x0f]  > 255) {
                gpr[high & 0x0f] -= 256;
            }
            pc+=2;
            //paused = true;
            return;
        }

        //Subset Operations
        if ((high  & 0xFF) >>> 4 == 8) {
            subset8(low, (high  & 0xFF));
            return;
        }

        //Skip NE
        if ((high  & 0xFF) >>> 4 == 9) {
            if (gpr[high & 0x0F] != gpr[(low >>> 4) & 0x0F]) {
                pc += 4;
            } else {
                pc += 2;
            }
            return;
        }

        //Load Immediate into I
        if ((high & 0xFF) >>> 4 == 0x0A) {
            debug("Setting I:" + (((high & 0x0F) << 8) + (low & 0xFF)));
            I = (((high & 0x0F) << 8) + (low & 0xFF));
            pc+=2;
            return;
        }

        //Jump Relative
        if ((high  & 0xFF) >>> 4 == 0xB) {
            pc = gpr[0] + ((low + (high << 8)) & 0x0FFF);
            return;
        }

        //RND
        if ((high  & 0xFF) >>> 4 == 0xC) {
            gpr[high & 0xF] = ((int)(Math.random() * 256) & 0xFF) & low;
            pc+=2;
            return;
        }

        //Draw
        if ((high  & 0xFF) >>> 4 == 0xD) {
            draw(low, high);
            return;
        }

        //Keypad Skip
        if ((high  & 0xFF) >>> 4 == 0xE) {
            for(boolean key : numpad) {
                System.out.print("" + key + "\t");
            }
            System.out.println("\t Looking for " + gpr[(high & 0x0F)]);
            //Skip Equal
            if ((low & 0xFF) == 0x9E) {
                if (numpad[gpr[high & 0x0F]]) {
                    pc += 4;
                } else {
                    pc += 2;
                }
            }
            //Skip Not Equal
            if ((low & 0xFF) == 0xA1) {
                if (!numpad[gpr[high & 0x0F]]) {
                    pc += 4;
                } else {
                    pc += 2;
                }
            }
            return;
        }

        //Subset F
        if ((high  & 0xFF) >>> 4 == 0xF) {
            subsetF(low, (high  & 0xFF));
            return;
        }

        //Unknown OPcode, pause
        paused = true;
        return;
    }

    public void subsetF(int low, int high) {
        switch (low) {
            case 0x07://Read delay timer
                gpr[high & 0x0F] = dt & 0xFF;
                pc += 2;
                break;
            case 0x0A://Wait for key press
                int key = -1;
                for (int i = 0; i < 16; i++) {
                    if (numpad[i]) {
                        key = i;
                        gpr[high & 0x0F] = key & 0xFF;
                        pc+=2;
                        break;
                    }
                }
                break;
            case 0x15://Set delay timer
                dt = gpr[high & 0x0F] & 0xFF;
                pc+=2;
                break;
            case 0x18://Set sound timer
                st = gpr[high & 0x0F] & 0xFF;
                pc+=2;
                break;
            case 0x1E://Add value to I
                I += gpr[high & 0x0F];
                pc+=2;
                break;
            case 0x29://Set I to digit address
                I = 5 * gpr[high & 0x0F];
                pc+=2;
                break;
            case 0x33://BCD
                int val = gpr[high & 0x0F];
                ram[I + 2] = (val % 10) & 0xFF;
                ram[I + 1] = ((val / 10) % 10) & 0xFF;
                ram[I] = ((val / 100) % 10) & 0xFF;
                pc+=2;
                break;
            case 0x55:
                for (int i = 0; i <= (high & 0x0F); i++) {
                    ram[I + i] = gpr[i] & 0xFF;
                }
                I += (high & 0x0F) + 1;
                pc+=2;
                break;
            case 0x65:
                debug("" + (high & 0x0F));
                for (int i = 0; i <= (high & 0x0F); i++) {
                    debug("GPR"+ i + ": " + (ram[I + i] & 0x0F));
                    gpr[i] = ram[I + i] & 0xFF;
                }
                //I++;
                I += (high & 0x0F) + 1;
                //paused = true;
                pc+=2;
                break;
        }
        return;
    }

    public void debugDrawSprite(int address, int size) {
        if (DEBUG) System.out.println("Address: " + gpr[3]);
        for (int i = 0; i < size; i++) {
            if (DEBUG) System.out.println(String.format("%8s", Integer.toBinaryString(ram[address + i])).replace(' ', '0'));
            //if (DEBUG) System.out.println(ram[address + i]);
        }
        if (DEBUG) System.out.println();
    }

    private void updateDisplay() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        for (int row = 0; row < 32; row++) {
            for (int column = 0; column < 64; column++) {
                if (display[column][row]) {
                    gc.setFill(Color.WHITE);
                    gc.fillRect(column * 10, row * 10, 10, 10);
                } else {
                    gc.setFill(Color.BLACK);
                    gc.fillRect(column * 10, row * 10, 10, 10);
                }
            }
        }
    }

    public void draw(int low, int high) {
        //if (DEBUG) System.out.println((high & 0x0F) + ", " + ((low >>> 4) & 0x0F));
        int size = low & 0x0F;
        int xPos = gpr[high & 0x0F] & 0xFF;
        int yPos = gpr[((low >>> 4) & 0x0F)] & 0xFF;
        //debugDrawSprite(I, size);
        gpr[0xF] = 0x0;
        //Lines, starting at address in I
        for (int line = 0; line < size; line++) {
            //Wrap y coord
            int newY = yPos + line;
            while (newY > 31) {
                newY = newY - 32;
            }
            int bits = ram[I + line];
            //Columns
            for (int column = 0; column < 8; column++) {
                //Wrap x coord
                int newX = xPos + column;
                while (newX > 63) {
                    newX -= 64;
                }
                //Top left of sprite is at xPos, yPos and is MSB of line 0
                boolean pixel = display[newX][newY];
                //Extract the new pixel.
                //AND bits with 1 shifted over by 7 (starting on left)
                //Set boolean if result is > 0
                //Subtract column number from shift every loop
                boolean newPixel = (bits & (0x01 << (7 - column))) > 0;

                //XOR both
                boolean result = pixel ^ newPixel;
                //Check to see if pixel was erased
                if (pixel && !result) {
                    gpr[0xF] = 1;
                } //else {
                    //gpr[0xF] = 0;
                //}
                //Update dsiplay
                display[newX][newY] = result;
                //updateDisplay();
            }
        }
        //updateDisplay(); //Now done on a timer
        pc+=2;
        return;
    }

    public void subset8(int low, int high) {
        debug("Subset 8: " + (low & 0x0F));
        switch (low & 0x0F) {
            case 0: //Load
                gpr[high & 0x0F] = gpr[(low >>> 4) & 0x0F];
                break;
            case 1: //OR
                gpr[high & 0x0F] |= gpr[(low >>> 4) & 0x0F];
                gpr[0xF] = 0x0;
                break;
            case 2: //AND
                gpr[high & 0x0F] &= gpr[(low >>> 4) & 0x0F];
                gpr[0xF] = 0x0;
                break;
            case 3: //XOR
                gpr[high & 0x0F] ^= gpr[(low >>> 4) & 0x0F];
                gpr[0xF] = 0x0;
                break;
            case 4: //Add
                int sum = (gpr[high & 0x0F] + gpr[(low >>> 4) & 0x0F]);
                boolean overflow = false;
                while (sum > 255) {
                    overflow = true;
                    sum -= 256;
                }
                gpr[high & 0x0F] = sum & 0xFF;
                if (overflow) {
                    gpr[0xF] = 0x01;
                } else {
                    gpr[0xF] = 0x00;
                }
                break;
            case 5: //SUB
                debug(gpr[high & 0x0F] + ", " +  gpr[(low >>> 4) & 0x0F]);
                int xval = gpr[high & 0x0F] & 0xFF;
                int yval = gpr[(low >>> 4) & 0x0F] & 0xFF;
                gpr[high & 0x0F] = (xval - yval) & 0xFF; // Perform subtraction
                if (xval >= yval) {
                    gpr[0xF] = 0x01;
                } else {
                    gpr[0xF] = 0x00;
                }
                //paused = true;
                break;
            case 6: //SHR
                gpr[high & 0xF] = gpr[(low >>> 4) & 0xF];
                int set = gpr[high & 0x0F] & 0x01;
                gpr[high & 0x0F] = gpr[high & 0x0F] >>> 1;
                gpr[0x0F] = set & 0xFF;
                break;
            case 7: //SUBN
                if (gpr[high & 0x0F] <= gpr[(low >>> 4) & 0x0F]) {
                    gpr[high & 0x0F] = (gpr[(low >>> 4) & 0x0F] - gpr[high & 0x0F]);
                    gpr[0xF] = 0x01;
                } else {
                    gpr[high & 0x0F] = 0xFF - (gpr[high & 0x0F] - gpr[(low >>> 4) & 0x0F] - 1);
                    gpr[0xF] = 0x00;
                }
                break;
            case 0xE:
                //debug("8E: " + Integer.toBinaryString(gpr[high & 0x0F]));
                //paused = true;
                boolean bs = false;
                gpr[high & 0xF] = gpr[(low >>> 4) & 0xF];
                if ((gpr[high & 0x0F] & 0x80) > 0) {
                    bs = true;
                } else {
                    bs = false;
                }
                //debug(Integer.toBinaryString(gpr[high & 0x0F]));
                gpr[high & 0x0F] = ((gpr[high & 0x0F] & 0xFF) << 1) & 0xFF;
                if (bs) {
                    gpr[0xF] = 0x01;
                } else {
                    gpr[0xF] = 0x00;
                }
                //debug(Integer.toBinaryString(gpr[high & 0x0F]));
                break;
            default:
                debug("Unkown opcode???");
                break;
        }
        pc+=2;
        return;
    }

    //Throws FileNotFoundException
    public boolean loadRom(String filename) throws FileNotFoundException {
        try (
            InputStream inputStream = new FileInputStream(filename);
        ) {
            //Read file into array
            long fileSize = new File(filename).length();
            byte[] allBytes = new byte[(int) fileSize];
            inputStream.read(allBytes);
            if (allBytes.length > 4096 - 0x200) {
                throw new IllegalArgumentException("ROM too big!");
            }
            //Copy array into RAM
            for (int i = 0; i < allBytes.length; i++) {
                ram[0x200 + i] = allBytes[i];
            }
        } catch (Exception ex) {
            throw new FileNotFoundException("ROM file not found!");
        }
        return true;
    }

    public void pokeRAM(int addr, int[] data) {
        for (int i = 0; i < data.length; i++) {
            ram[addr + i] = data[i];
        }
    }

    public String dumpRom(int length) {
        String t_string = "";
        for(int i = 0; i < length; i+=2) {
            String lower = String.format("%02X", ram[i + 0x200]);
            String upper = String.format("%02X", ram[i + 1 + 0x200]);
            t_string += lower.substring(lower.length() - 2, lower.length()) + " " + upper.substring(upper.length() - 2, upper.length()) + "\t";
            if (i % 20 == 0) {
                t_string += "\n";
            }
            //if (DEBUG) System.out.println(Integer.toHexString(ram[i]) + " " + Integer.toHexString(ram[i + 1]));
        }
        return t_string;
    }

    public String toString() {
        return "";
    }

    private void clearDisplay() {
        for (int row = 0; row < 32; row++) {
            for (int column = 0; column < 64; column++) {
                display[column][row] = false;
            }
        }
        //updateDisplay();
    }



    private void debug(String s) {
        if (DEBUG) System.out.println(s);
    }

    private void keyReleased(KeyEvent evt){
        String ch = evt.getText();
        for (int i = 0; i < mapping.length; i++)  {
            if (mapping[i].compareTo(ch) == 0) {
                debug("Released " + ch);
                //Key released
                numpad[i] = false;
            }
        }
    }

    private void keyPressed(KeyEvent evt){
        String ch = evt.getText();
        if (ch.compareTo(" ") == 0) {
            reset();
        }
        //paused=true;
        for (int i = 0; i < mapping.length; i++)  {
            if (mapping[i].compareTo(ch) == 0) {
                debug("Pressed " + ch);
                //Key pressed
                numpad[i] = true;
            }
        }

        if (ch.compareTo("p") == 0) {
            paused = !paused;
        }
    }
}
