package chip8.base;

import javafx.application.Application;

public class Driver {
    public static void main(String[] args) {
        for(String s : args) {
            System.out.println(s);
        }
        try {
            Application.launch(CHIP8.class, args);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        } // try
    } // main
}
