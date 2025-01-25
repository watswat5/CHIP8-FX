# CHIP-8 Interpreter in Java

A **CHIP-8 interpreter** written in Java, using **JavaFX** as the graphical frontend.

⚠ **Sound is not currently working.**

## 📦 Setup
To compile the project, run:
```sh
mvn compile
```

You can also package the JAR file with:
```sh
mvn package
```

## ▶ Running
To run the project, use the following command:
```sh
mvn exec:java -Dexec.mainClass=chip8.base.Driver -Dexec.args="--rom=$ROM_NAME_HERE --clock=$FREQUENCY --debug=$TRUE/FALSE"
```
- **`--rom=$ROM_NAME_HERE`** → Specify the ROM file to load.
- **`--clock=$FREQUENCY`** → Set the emulation clock speed.
- **`--debug=$TRUE/FALSE`** → Enable or disable debugging mode.

## 🎮 Controls
This interpreter uses the following standard CHIP-8 layout:

| CHIP-8 Key | Keyboard |
|------------|----------|
| 1 | 1 |
| 2 | 2 |
| 3 | 3 |
| C | 4 |
| 4 | Q |
| 5 | W |
| 6 | E |
| D | R |
| 7 | A |
| 8 | S |
| 9 | D |
| E | F |
| A | Z |
| 0 | X |
| B | C |
| F | V |

**Note:** The CHIP-8 keys are arranged in a 4×4 grid corresponding to hexadecimal digits 0x0–0xF, which we've mapped to the above PC keyboard layout.

## ⚠ Status
This project is **still a Work in Progress (WIP)**—expect bugs! 🚧
