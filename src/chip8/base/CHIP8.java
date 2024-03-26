package chip8.base;

/**
*   Class representation of a CHIP8 computer.
*/
public class CHIP8 {

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

    private long lastTime;

    public CHIP8() {
        //Init all variables
        ram = new int[4096];
        gpr = new int[16];
        dt = 0;
        st = 0;
        stack = new int[16];
        sp = 0;
        pc = 0x200;
        display = new boolean[64][32];
        I = 0;

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


    }

    private void clearDisplay() {
        for (boolean[] line : display) {
            for (boolean pixel : line) {
                pixel = false;
            }
        }
    }
}
