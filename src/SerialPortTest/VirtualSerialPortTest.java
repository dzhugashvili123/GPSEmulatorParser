package SerialPortTest;
import com.fazecast.jSerialComm.SerialPort;

public class VirtualSerialPortTest {

    public static void main(String[] args) {
        // Open the first virtual serial port (e.g., COM3 for sending data)
        SerialPort sendPort = SerialPort.getCommPort("COM1");
        sendPort.setBaudRate(9600);
        sendPort.setNumDataBits(8);
        sendPort.setNumStopBits(1);
        sendPort.setParity(SerialPort.NO_PARITY);

        // Open the second virtual serial port (e.g., COM4 for receiving data)
        SerialPort receivePort = SerialPort.getCommPort("COM2");
        receivePort.setBaudRate(9600);
        receivePort.setNumDataBits(8);
        receivePort.setNumStopBits(1);
        receivePort.setParity(SerialPort.NO_PARITY);

        try {
            // Open both ports
            if (sendPort.openPort() && receivePort.openPort()) {
                System.out.println("Ports opened successfully");

                // Send data from the sendPort
                String dataToSend = "Hello from COM3!";
                sendPort.getOutputStream().write(dataToSend.getBytes());
                System.out.println("Data sent: " + dataToSend);

                // Read data from the receivePort
                byte[] buffer = new byte[1024];
                int bytesRead = receivePort.getInputStream().read(buffer);
                if (bytesRead > 0) {
                    String receivedData = new String(buffer, 0, bytesRead);
                    System.out.println("Data received on COM4: " + receivedData);
                }

                // Close the ports after communication
                sendPort.closePort();
                receivePort.closePort();
            } else {
                System.out.println("Failed to open the ports.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
