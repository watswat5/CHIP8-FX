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

    /**Speed in Hz*/
    int frequency;

    boolean paused;
    Scene mainScene;
    Canvas canvas;

    public CHIP8(String rom) {
        super();
        this.rom = rom;
        frequency = 1000;
    }

    public CHIP8(String rom, int frequency) {
        super();
        this.rom = rom;
        this.frequency = frequency;
    }

    public void start(Stage stage) {
        reset();
        try {
            loadRom(rom);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("File not found!");
        }
        Timeline stepTimer = new Timeline( new KeyFrame(Duration.seconds(1/frequency),(e) -> step()));
        stepTimer.setCycleCount(Timeline.INDEFINITE);
        stepTimer.play();

        //Set up numpad listener
        canvas = new Canvas(400,400);
        Pane root = new Pane(canvas);
        mainScene = new Scene(root);
        stage.setScene(mainScene);
        stage.setTitle("CHIP8");
        stage.setResizable(false);
        mainScene.setOnKeyPressed( e -> keyPressed(e) );
        mainScene.setOnKeyReleased( e -> keyReleased(e) );
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
        for (int i = 9; i < charset.length; i++) {
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
        int low = ram[pc];
        int high = ram[pc + 1];

        //Decode Instruction

        //00 high
        if (high == 0) {
            if (low == 0xEE) { //CLS
                clearDisplay();
                pc++;
                return;
            } else if (low == 0xEE) { //RET
                pc = stack[sp--];
                return;
            } else {
                throw new UnsupportedOperationException("Invalid OP-Code: " + (low + (high << 8) & 0xFFFF));
            }
        }

        //JP
        if (high >>> 4 == 1) {
            pc = low + (high << 8) & 0x0FFF;
            return;
        }

        //CALL
        if (high >>> 4 == 2) {
            sp++;
            stack[sp] = pc;
            pc = low + (high << 8) & 0x0FFF;
            return;
        }

        //Skip Equal Literal
        if (high >>> 4 == 3) {
            if (gpr[high & 0x0F] == low) {
                pc += 2;
            } else {
                pc += 1;
            }
            return;
        }

        //Skip Not Equal
        if (high >>> 4 == 4) {
            if (gpr[high & 0x0F] != low) {
                pc += 2;
            } else {
                pc += 1;
            }
            return;
        }

        //Skip Equal Register
        if (high >>> 4 == 5) {
            if (gpr[high & 0x0F] == gpr[low >>> 4]) {
                pc += 2;
            } else {
                pc += 1;
            }
            return;
        }

        //Load Literal
        if (high >>> 4 == 6) {
            gpr[high & 0x0F] = low;
            pc++;
            return;
        }

        //Add Literal
        if (high >>> 4 == 7) {
            gpr[high & 0x0F] += low;
            pc++;
            return;
        }

        //Subset Operations
        if (high >>> 4 == 8) {
            subset8(low, high);
            return;
        }

        //Skip NE
        if (high >>> 4 == 9) {
            if (gpr[high & 0x0F] != gpr[low >>> 4]) {
                pc += 2;
            } else {
                pc += 1;
            }
            return;
        }

        //Load Immediate
        if (high >>> 4 == 0xA) {
            I = (low + (high << 8)) & 0x0FFF;
            pc++;
            return;
        }

        //Jump Relative
        if (high >>> 4 == 0xB) {
            pc = gpr[0] + ((low + (high << 8)) & 0x0FFF);
            return;
        }

        //RND
        if (high >>> 4 == 0xC) {
            gpr[high & 0xF] = ((int)(Math.random() * 256) & 0xFF) & low;
            pc++;
            return;
        }

        //Draw
        if (high >>> 4 == 0xD) {
            draw(low, high);
            return;
        }

        //Keypad Skip
        if (high >>> 4 == 0xE) {
            //Skip Equal
            if (low == 0x9E) {
                if (numpad[high & 0x0F]) {
                    pc += 2;
                } else {
                    pc += 1;
                }
            }
            //Skip Not Equal
            if (low == 0x9E) {
                if (!numpad[high & 0x0F]) {
                    pc += 2;
                } else {
                    pc += 1;
                }
            }
            return;
        }

        //Subset F
        if (high >>> 4 == 0xF) {
            subsetF(low, high);
        }
    }

    public void subsetF(int low, int high) {
        switch (low) {
            case 0x07:
                gpr[high & 0x0F] = dt;
                break;
            case 0x0A://Wait for key press
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
                break;
            case 0x15:
                dt = gpr[high & 0x0F];
                break;

        }
    }

    public void draw(int low, int high) {
        int size = low & 0x0F;
        int xPos = gpr[high & 0x0F];
        int yPos = gpr[low >>> 4];

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
        pc++;
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
        pc++;
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

    public String dumpRom() {
        return "";
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
