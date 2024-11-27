import com.fazecast.jSerialComm.SerialPort;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class GPSEmulator {
    private SerialPort serialPort;
    private Random random = new Random();

    public GPSEmulator(String portName, int baudRate) {
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);
    }

    public boolean openPort() {
        if (serialPort.openPort()) {
            System.out.println("Port " + serialPort.getSystemPortName() + " opened successfully.");
            return true;
        } else {
            System.out.println("Failed to open port " + serialPort.getSystemPortName());
            return false;
        }
    }

    public void closePort() {
        if (serialPort.closePort()) {
            System.out.println("Port closed successfully.");
        }
    }

    public void startSendingData() {
        Thread sendingThread = new Thread(() -> {
            while (true) {
                String gpgga = generateGPGGA();
                String gprmc = generateGPRMC();
                String data = gpgga + "\r\n" + gprmc + "\r\n";

                serialPort.writeBytes(data.getBytes(), data.length());
                System.out.println("Sent:\n" + data);

                try {
                    Thread.sleep(1000); // Send data every second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        sendingThread.start();
    }

    private String generateGPGGA() {
        String utcTime = new SimpleDateFormat("HHmmss").format(new Date());
        String latitude = String.format("%02d%07.4f", random.nextInt(90), random.nextDouble() * 60);
        String nsIndicator = random.nextBoolean() ? "N" : "S";
        String longitude = String.format("%03d%07.4f", random.nextInt(180), random.nextDouble() * 60);
        String ewIndicator = random.nextBoolean() ? "E" : "W";
        int fixQuality = 1;
        int numSatellites = 8 + random.nextInt(5);
        double hdop = 0.9 + random.nextDouble();
        double altitude = 500 + random.nextDouble() * 100; // Altitude in meters

        return String.format("$GPGGA,%s,%s,%s,%s,%s,%d,%02d,%.1f,%.1f,M,,,*%02X",
                utcTime, latitude, nsIndicator, longitude, ewIndicator, fixQuality,
                numSatellites, hdop, altitude, calculateChecksum(
                String.format("GPGGA,%s,%s,%s,%s,%s,%d,%02d,%.1f,%.1f,M,,,",
                        utcTime, latitude, nsIndicator, longitude, ewIndicator, fixQuality, numSatellites, hdop, altitude)
        ));
    }

    private String generateGPRMC() {
        String utcTime = new SimpleDateFormat("HHmmss").format(new Date());
        String status = "A";
        String latitude = String.format("%02d%07.4f", random.nextInt(90), random.nextDouble() * 60);
        String nsIndicator = random.nextBoolean() ? "N" : "S";
        String longitude = String.format("%03d%07.4f", random.nextInt(180), random.nextDouble() * 60);
        String ewIndicator = random.nextBoolean() ? "E" : "W";
        double speed = random.nextDouble() * 20;  // Speed in knots
        double course = random.nextDouble() * 360; // Course in degrees
        String date = new SimpleDateFormat("ddMMyy").format(new Date());
        
        return String.format("$GPRMC,%s,%s,%s,%s,%s,%s,%.1f,%.1f,%s,,*%02X",
                utcTime, status, latitude, nsIndicator, longitude, ewIndicator, speed, course, date,
                calculateChecksum(
                String.format("GPRMC,%s,%s,%s,%s,%s,%s,%.1f,%.1f,%s,,",
                        utcTime, status, latitude, nsIndicator, longitude, ewIndicator, speed, course, date)
        ));
    }

    private int calculateChecksum(String sentence) {
        int checksum = 0;
        for (int i = 0; i < sentence.length(); i++) {
            checksum ^= sentence.charAt(i);
        }
        return checksum;
    }

    public static void main(String[] args) {
        String portName = "COM1"; // Replace with your virtual COM port
        int baudRate = 9600;

        GPSEmulator emulator = new GPSEmulator(portName, baudRate);

        if (emulator.openPort()) {
            emulator.startSendingData();
        } else {
            System.out.println("Unable to open port.");
        }
    }
}
