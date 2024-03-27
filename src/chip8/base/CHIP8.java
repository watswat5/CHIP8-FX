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

    boolean paused;
    Scene mainScene;
    Canvas canvas;

    public void start(Stage stage) {
        reset();
        rom = "1-chip8-logo.ch8";
        frequency = 15;
        try {
            loadRom(rom);
            //System.out.println(dumpRom(1024));
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("File not found!");
        }
        stepTimer = new Timeline( new KeyFrame(Duration.seconds(1.0/frequency),(e) -> step()));
        stepTimer.setCycleCount(Timeline.INDEFINITE);

        //Set up numpad listener
        canvas = new Canvas(400,400);
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
        stack = new int[16];
        sp = 0;
        pc = 0x200;
        display = new boolean[64][32];
        I = 0;
        numpad = new boolean[16];
        mapping = new String[]{"1","2","3","4","q","w","e","r","a","s","d","f","z","x","c","v"};

        //Load charset into RAM
        int[] charset = {0xF0,0x90,0x90,0x90,0xF0,0x20,0x60,0x20,0x20,0x70,0xF0,0x10,0xF0,0x80,0xF0,0xF0,0x10,0xF0,0x10,0xF0,0x90,0x90,0xF0,0x10
                        ,0x10,0xF0,0x80,0xF0,0x10,0xF0,0xF0,0x80,0xF0,0x90,0xF0,0xF0,0x10,0x20,0x40,0x40,0xF0,0x90,0xF0,0x90,0xF0,0xF0,0x90,0xF0
                        ,0x10,0xF0,0xF0,0x90,0xF0,0x90,0x90,0xE0,0x90,0xE0,0x90,0xE0,0xF0,0x80,0x80,0x80,0xF0,0xE0,0x90,0x90,0x90,0xE0,0xF0,0x80
                        ,0xF0,0x80,0xF0,0xF0,0x80,0xF0,0x80,0x80};
        for (int i = 0; i < charset.length; i++) {
            ram[i] = (charset[i] & 0xFF);
        }

        //Init timer variable
        lastTime = System.currentTimeMillis();
        paused = false;
    }

    public void step() {
        //Decrement sound and delay timers
        if (System.currentTimeMillis() - lastTime > 16) {
            dt = dt > 0 ? (dt - 1) : 0;
            st = st > 0 ? (st - 1) : 0;
        }

        //Fetch instruction
        int high = ram[pc];
        int low = ram[pc + 1];

        //Decode Instruction
        String lower = String.format("%02X", low);
        String upper = String.format("%02X", high);
        //System.out.println(pc + ": " + upper.substring(upper.length() - 2, upper.length()) + " " + lower.substring(lower.length() - 2, lower.length()) + "\n");

        //00 high
        if (high == 0) {
            if ((low & 0xFF) == 0xE0) { //CLS
                clearDisplay();
                pc+=2;
                return;
            } else if ((low & 0xFF) == 0xEE) { //RET
                pc = stack[sp--];
                return;
            } else {
                throw new UnsupportedOperationException("Invalid OP-Code: " + (low + (high << 8) & 0xFFFF));
            }
        }

        //JP
        if ((high & 0xF0) == 0x0010) {
            //System.out.println("jmp: " + (low + (high << 8) & 0x0FFF));
            pc = low + (high << 8) & 0x0FFF;
            return;
        }

        //CALL
        if ((high  & 0xFF) >>> 4 == 2) {
            sp++;
            stack[sp] = pc;
            pc = low + (high << 8) & 0x0FFF;
            return;
        }

        //Skip Equal Literal
        if ((high  & 0xFF) >>> 4 == 3) {
            if (gpr[high & 0x0F] == low) {
                pc += 4;
            } else {
                pc += 2;
            }
            return;
        }

        //Skip Not Equal
        if ((high  & 0xFF) >>> 4 == 4) {
            if (gpr[high & 0x0F] != low) {
                pc += 4;
            } else {
                pc += 2;
            }
            return;
        }

        //Skip Equal Register
        if ((high  & 0xFF) >>> 4 == 5) {
            if (gpr[high & 0x0F] == gpr[low >>> 4]) {
                pc += 4;
            } else {
                pc += 2;
            }
            return;
        }

        //Load Literal
        if ((high  & 0xFF) >>> 4 == 6) {
            gpr[high & 0x0F] = low;
            pc+=2;
            return;
        }

        //Add Literal
        if ((high  & 0xFF) >>> 4 == 7) {
            gpr[high & 0x0F] += low;
            pc+=2;
            return;
        }

        //Subset Operations
        if ((high  & 0xFF) >>> 4 == 8) {
            subset8(low, (high  & 0xFF));
            return;
        }

        //Skip NE
        if ((high  & 0xFF) >>> 4 == 9) {
            if (gpr[high & 0x0F] != gpr[low >>> 4]) {
                pc += 4;
            } else {
                pc += 2;
            }
            return;
        }

        //Load Immediate
        if ((high & 0xFF) >>> 4 == 0x0A) {
            I = (((low & 0xFF) & ((high << 8) | 0xFFF))) & 0x0FFF;
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
            //Skip Equal
            if (low == 0x9E) {
                if (numpad[high & 0x0F]) {
                    pc += 4;
                } else {
                    pc += 2;
                }
            }
            //Skip Not Equal
            if (low == 0xA1) {
                if (!numpad[high & 0x0F]) {
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
    }

    public void subsetF(int low, int high) {
        switch (low) {
            case 0x07://Read delay timer
                gpr[high & 0x0F] = dt;
                break;
            case 0x0A://Wait for key press
                stepTimer.pause();
                boolean pressed = false;
                int key = -1;
                while (!pressed) {
                    for (int i = 0; i < 16; i++) {
                        if (numpad[i]) {
                            key = i;
                            pressed = true;
                            break;
                        }
                    }
                }
                gpr[high & 0x0F] = key & 0xFF;
                stepTimer.play();
                break;
            case 0x15://Set delay timer
                dt = gpr[high & 0x0F];
                break;
            case 0x18://Set sound timer
                st = gpr[high & 0x0F];
                break;
            case 0x1E://Add value to I
                I += gpr[high & 0x0F];
                break;
            case 0x29://Set I to digit address
                I = 5 * gpr[high & 0x0F];
                break;
            case 0x33://BCD
                int val = gpr[high & 0x0F];
                ram[I + 2] = (val % 10) & 0xFF;
                ram[I + 1] = ((val / 10) % 10) & 0xFF;
                ram[I] = ((val / 100) % 10) & 0xFF;
                break;
            case 0x55:
                for (int i = 0; i < (high & 0x0F); i++) {
                    ram[I + i] = gpr[i];
                }
                break;
            case 0x65:
                for (int i = 0; i < (high & 0x0F); i++) {
                    gpr[i] = ram[I + i];
                }
                break;
        }
        pc+=2;
        return;
    }

    public void debugDrawSprite(int address, int size) {
        System.out.println("Address: " + I);
        for (int i = 0; i < size; i++) {
            //System.out.println(String.format("%8s", Integer.toBinaryString(ram[address + i])).replace(' ', '0'));
            System.out.println(ram[address + i]);
        }
        System.out.println();
    }

    public void draw(int low, int high) {
        int size = low & 0x0F;
        int xPos = gpr[high & 0x0F];
        int yPos = gpr[low >>> 4];
        debugDrawSprite(I, size);
        //Lines, starting at address in I
        for (int line = 0; line < size; line++) {
            //Wrap y coord
            int newY = yPos + line;
            if (newY > 31) {
                newY = newY - 32;
            }
            int bits = ram[I + line];
            //Columns
            for (int column = 0; column < 8; column++) {
                //Wrap x coord
                int newX = xPos + column;
                if (newX > 63) {
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
                } else {
                    gpr[0xF] = 0;
                }
                //Update dsiplay
                display[newX][newY] = result;
            }
        }
        pc+=2;
        return;
    }

    public void subset8(int low, int high) {
        switch (low & 0x0F) {
            case 0: //Load
                gpr[high & 0x0F] = gpr[low >>> 4];
                break;
            case 1: //OR
                gpr[high & 0x0F] |= gpr[low >>> 4];
                break;
            case 2: //AND
                gpr[high & 0x0F] &= gpr[low >>> 4];
                break;
            case 3: //XOR
                gpr[high & 0x0F] ^= gpr[low >>> 4];
                break;
            case 4: //Add
                int sum = (gpr[high & 0x0F] + gpr[low >>> 4]);
                if (sum > 255) {
                    gpr[0xF] = 1 & 0xFF;
                } else {
                    gpr[0xF] = 0 & 0x00;
                }
                gpr[high & 0x0F] = sum & 0xFF;
                break;
            case 5: //SUB
                if (gpr[high & 0x0F] > gpr[low >>> 4]) {
                    gpr[0xF] = 1 & 0xFF;
                    gpr[high & 0x0F] = (gpr[high & 0x0F] - gpr[low >>> 4]) & 0xFF;
                } else {
                    gpr[0xF] = 0 & 0x00;
                    gpr[high & 0x0F] = 0 & 0x00;
                }
                break;
            case 6: //SHR
                gpr[0x0F] = gpr[high & 0x0F] & 0x01;
                gpr[high & 0x0F] = gpr[high & 0x0F] >>> 1;
                break;
            case 7: //SUBN
                if (gpr[high & 0x0F] < gpr[low >>> 4]) {
                    gpr[0xF] = 1 & 0xFF;
                    gpr[high & 0x0F] = (gpr[low >>> 4] - gpr[high & 0x0F]) & 0xFF;
                } else {
                    gpr[0xF] = 0 & 0x00;
                    gpr[high & 0x0F] = 0 & 0x00;
                }
                break;
            case 0xE:
                if (gpr[high & 0x0F] >>> 7 > 0) {
                    gpr[0xF] = 1 & 0xFF;
                } else {
                    gpr[0xF] = 0 & 0x00;
                }
                gpr[high & 0x0F] = gpr[high & 0x0F] << 1;
                break;
            default:
                System.out.println("Unkown opcode???");
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
                ram[i + 0x200] = allBytes[i];
            }
        } catch (Exception ex) {
            throw new FileNotFoundException("ROM file not found!");
        }
        return true;
    }

    public String dumpRom(int length) {
        if (length < 0x200) {
            throw new IllegalArgumentException("Must dump ROM after 0x1FF!");
        }
        String t_string = "";
        for(int i = 0x200; i < length; i+=2) {
            String lower = String.format("%02X", ram[i]);
            String upper = String.format("%02X", ram[i + 1]);
            t_string += lower.substring(lower.length() - 2, lower.length()) + " " + upper.substring(upper.length() - 2, upper.length()) + "\n";
            //System.out.println(Integer.toHexString(ram[i]) + " " + Integer.toHexString(ram[i + 1]));
        }
        return t_string;
    }

    public String toString() {
        return "";
    }

    private void clearDisplay() {
        for (boolean[] line : display) {
            for (boolean pixel : line) {
                pixel = false;
            }
        }
    }

    private void keyPressed(KeyEvent evt){
        String ch = evt.getCharacter();

        for (int i = 0; i < mapping.length; i++)  {
            if (mapping[i] == ch + "") {
                //Key pressed
                numpad[i] = true;
            }
        }
    }

    private void keyReleased(KeyEvent evt){
        String ch = evt.getCharacter();

        for (int i = 0; i < mapping.length; i++)  {
            if (mapping[i] == ch + "") {
                //Key pressed
                numpad[i] = false;
            }
        }
    }
}
