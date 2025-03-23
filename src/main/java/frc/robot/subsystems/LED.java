package frc.robot.subsystems;

import edu.wpi.first.wpilibj.AddressableLED;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class LED extends SubsystemBase {
    public static class LEDStrip {
        AddressableLED led;
        AddressableLEDBuffer buffer;
    
        public LEDStrip(int ledPort, int bufferLength) {
            this.led = new AddressableLED(ledPort);
            this.buffer = new AddressableLEDBuffer(bufferLength);
            this.led.setLength(bufferLength);
        }
    }
    public interface LEDColorSupplier {
        public Color get(LEDStrip strip, int i, double time);
    }

    private LEDStrip[] stripList;
    private LEDColorSupplier colorSupplier;
    private double previousTime;
    
    public LED(LEDStrip... stripList) {
        this.stripList = stripList;
        this.previousTime = -1;
        setLight(Color.kBlack);

        // initialize led
        for (LEDStrip strip : stripList) {
            strip.led.start();
        }
    }

    @Override
    public void periodic() {
        double time = getTime() - previousTime;
        for (LEDStrip strip : stripList) {
            for (int i = 0; i < strip.buffer.getLength(); i++) {
                strip.buffer.setLED(i, colorSupplier.get(strip, i, time));
            }
            strip.led.setData(strip.buffer);
        }
    }

    public void setColorSupplier(LEDColorSupplier colorSupplier) {
        this.previousTime = getTime();
        this.colorSupplier = colorSupplier;
    }

    public void setLight(Color color) {
        setColorSupplier((LEDStrip strip, int i, double time) -> {
            return color;
        });
    }

    public void setRainbow() {
        setColorSupplier((LEDStrip strip, int i, double time) -> {
            double indexOffset = 360 * ((double)i / (double)strip.buffer.getLength());
            double timeOffset = -time * 100;
            double progress = ((indexOffset + timeOffset) % 360 + 360) % 360;
            return Color.fromHSV((int)Math.floor(progress), 255, 255);
        });
    }

    public void setBlinkLight(Color color, double interval) {
        setColorSupplier((LEDStrip strip, int i, double time) -> {
            if ((time / interval) % 2 > 1) return Color.kBlack;
            return color;
        });
    }

    private double getTime() {
        return (double)(System.currentTimeMillis()) / 1000;
    }
}