package frc.robot.subsystems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.wpi.first.wpilibj.AddressableLED;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;

public class LED extends SubsystemBase {
    // state pattern bindings
    public enum LEDState {
        WHITE, BLACK, 
        RED, GREEN, BLUE, 
        TURQUOISE, PURPLE, YELLOW,
        RAINBOW,
    }

    private Map<LEDState, Command> commands = new HashMap<LEDState, Command>();
    {
        commands.put(LEDState.WHITE, new InstantCommand(() -> setLED(75, 75, 75), this));
        commands.put(LEDState.BLACK, new InstantCommand(() -> setLED(0, 0, 0), this));
        commands.put(LEDState.RED, new InstantCommand(() -> setLED(255, 0, 0), this));
        commands.put(LEDState.GREEN, new InstantCommand(() -> setLED(0, 255, 0), this));
        commands.put(LEDState.BLUE, new InstantCommand(() -> setLED(0, 0, 255), this));
        commands.put(LEDState.TURQUOISE, new InstantCommand(() -> setLED(50, 210, 200), this));
        commands.put(LEDState.PURPLE, new InstantCommand(() -> setLED(70, 0, 100), this));
        commands.put(LEDState.YELLOW, new InstantCommand(() -> setLED(150, 75, 0), this));
        commands.put(LEDState.RAINBOW, new ChromaLED(this, (double i) -> Color.fromHSV((int)Math.floor(i * 180), 255, 255)));
    };

    // addressable led
    private AddressableLED strip;
    private AddressableLEDBuffer buffer;

    // blinking
    private boolean blinking;
    private Command blinkCommand = new Blink(this);

    // led state
    private LEDState state;
    private ArrayList<LEDState> activeStateList;

    public LED() { 
        strip = new AddressableLED(3);
        buffer = new AddressableLEDBuffer(50);
        strip.setLength(buffer.getLength());
        blinking = false;
        activeStateList = new ArrayList<LEDState>();
        state = LEDState.BLACK;
        startLED();
    }

    // bind buttons
    public void bindButtonToState(JoystickButton button, LEDState state) {
        button.onTrue(new InstantCommand(() -> activateState(state), this));
        button.onFalse(new InstantCommand(() -> deactivateState(state), this));
    }

    public void bindButtonToBlink(JoystickButton button) {
        button.onTrue(new InstantCommand(() -> setBlink(true), this));
        button.onFalse(new InstantCommand(() -> setBlink(false), this));
    }

    // led state
    public void activateState(LEDState state) {
        if (activeStateList.contains(state)) return;
        activeStateList.add(state);
        setState();
    }

    public void deactivateState(LEDState state) {
        if (!activeStateList.contains(state)) return;
        activeStateList.remove(activeStateList.indexOf(state));
        setState();
    }

    public void setState() {
        getStateCommand().cancel();
        int size = activeStateList.size();
        boolean on = (size > 0) && !blinking;
        state = on ? activeStateList.get(size - 1) : LEDState.BLACK;
        startLED();
    }

    public boolean isActive() {
        return activeStateList.size() > 0;
    }

    private Command getStateCommand() {
        return commands.get(state);
    }


    // led strip and buffer
    private void startLED() {
        strip.start();
        getStateCommand().schedule();
    }

    private void setLED(int r, int g, int b) {
        for (int i = 0; i < buffer.getLength(); i++) {
            buffer.setRGB(i, r, g, b);
        }
        strip.setData(buffer);
    }

    // led blink
    public void setBlink(boolean blinkEnable) {
        if (blinkCommand.isScheduled()) blinkCommand.cancel();
        if (blinkEnable) blinkCommand.schedule();
        setState();
    }

    private static class ChromaLED extends Command {
        private LED led;
        private LEDColorSupplier supplier;

        private ChromaLED(LED led, LEDColorSupplier supplier) {
            this.led = led;
            this.supplier = supplier;
            addRequirements(led);
        }

        @Override
        public void execute() {
            int len = led.buffer.getLength();
            int offset = (int) Math.floor((System.currentTimeMillis() / 10) % len);
            for (int i = 0; i < len; i++) {
                int index = (i + offset) % len;
                Color color = supplier.get((double) i / len);
                led.buffer.setLED(index, color);
            }
            led.strip.setData(led.buffer);
        }

        public static interface LEDColorSupplier {
            public Color get(double progress);
        }
    }

    private static class Blink extends Command {
        private LED led;
        private long blinkTime;

        private Blink(LED led) {
            this.led = led;
            blinkTime = -1L;
            //addRequirements(led); // this one line killed me
        } 

        @Override
        public void initialize() {
            blinkTime = -1L;
        }

        @Override
        public void execute() {
            long time = System.currentTimeMillis();
            if (!led.isActive()) {
                blinkTime = time; 
                led.blinking = false;
                return;
            }
            if (time - blinkTime < 500) return;
            // toggle blink on and off
            blinkTime = time;
            led.blinking = !led.blinking;
            led.setState();
        }

        @Override
        public void end(boolean interrupted) {
            led.blinking = false;
        }
    }
}