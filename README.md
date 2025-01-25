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

## ⚠ Status
This project is **still a Work in Progress (WIP)**—expect bugs! 🚧
