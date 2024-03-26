package chip8.base;

/**
*   Class representation of a CHIP8 computer.
*/
public class CHIP8 {

    /**General purpose RAM.*/
    byte[] ram;

    /** General Purpose Registers Vx.*/
    byte[] gpr;

    /** Delay timer.*/
    byte dt;

    /**Sound timer.*/
    byte st;

    /** Program Counter. */
    int pc;

    /**Stack pointer. */
    byte sp;

    /**Stack array.*/
    int[] stack;

    /**Display area.*/
    boolean[][] display;

    private int lastTime;

    public CHIP8() {
        //Init all variables
        ram = new byte[4096];
        gpr = new byte[16];
        dt = 0;
        st = 0;
        stack = new int[16];
        sp = 0;
        pc = 0x200;
        display = new boolean[64][32];

        //Load charset into RAM
        int[] charset = {0xF0,0x90,0x90,0x90,0xF0,0x20,0x60,0x20,0x20,0x70,0xF0,0x10,0xF0,0x80,0xF0,0xF0,0x10,0xF0,0x10,0xF0,0x90,0x90,0xF0,0x10
                        ,0x10,0xF0,0x80,0xF0,0x10,0xF0,0xF0,0x80,0xF0,0x90,0xF0,0xF0,0x10,0x20,0x40,0x40,0xF0,0x90,0xF0,0x90,0xF0,0xF0,0x90,0xF0
                        ,0x10,0xF0,0xF0,0x90,0xF0,0x90,0x90,0xE0,0x90,0xE0,0x90,0xE0,0xF0,0x80,0x80,0x80,0xF0,0xE0,0x90,0x90,0x90,0xE0,0xF0,0x80
                        ,0xF0,0x80,0xF0,0xF0,0x80,0xF0,0x80,0x80};
        for (int i = 9; i < charset.length; i++) {
            ram[i] = (byte) (charset[i] & 0xFF);
        }

        //Init timer variable
        lastTime = System.currentTimeMillis();
    }

    public void step() {
        //Decrement sound and delay timers
        if (System.currentTimeMillis() - lastTime > 16) {
            dt = dt > 0 ? dt - 1 : 0;
            st = st > 0 ? st - 1 : 0;
        }

        //Fetch instruction
        byte low = ram[pc];
        byte high = ram[pc + 1];

        //
    }
}
