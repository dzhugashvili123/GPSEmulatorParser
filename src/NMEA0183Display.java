import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;


public class NMEA0183Display extends JFrame {
    private JLabel latitudeLabel, longitudeLabel;
    private JLabel utcLabel, altitudeLabel, speedLabel, courseLabel, errorLabel;
    private SerialPort serialPort;
    private Timer timer;

    public NMEA0183Display() {
        setTitle("NMEA 0183 GPS Data Display");
        setSize(400, 300);
        setLayout(new GridLayout(10, 1));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        latitudeLabel = new JLabel("Latitude: ");
        longitudeLabel = new JLabel("Longitude: ");
        utcLabel = new JLabel("UTC: ");
        altitudeLabel = new JLabel("Altitude: ");
        speedLabel = new JLabel("Speed (SOG): ");
        courseLabel = new JLabel("Course (COG): ");
        errorLabel = new JLabel("");
        errorLabel.setForeground(Color.RED);

        add(latitudeLabel);
        add(longitudeLabel);
        add(utcLabel);
        add(altitudeLabel);
        add(speedLabel);
        add(courseLabel);
        add(errorLabel);

        initializeSerialPort();

        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (serialPort != null && serialPort.isOpen()) {
                    readAndDisplayNMEAData();
                } else {
                    errorLabel.setText("Error: Serial port not available or not open.");
                }
            }
        });
        timer.start();
    }

    private void initializeSerialPort() {
        serialPort = SerialPort.getCommPort("COM2"); // Replace with your port name
        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);

        if (serialPort.openPort()) {
            System.out.println("Port opened successfully.");
        } else {
            errorLabel.setText("Error: Unable to open serial port.");
        }
    }

    private void readAndDisplayNMEAData() {
        byte[] buffer = new byte[1024];
        int numRead = serialPort.readBytes(buffer, buffer.length);

        if (numRead > 0) {
            String data = new String(buffer, 0, numRead);
            List<String>[] gpsData = parseNMEA(data);
            parseAndDisplayNMEAData(gpsData);
        } else {
            errorLabel.setText("Error: No data available.");
        }
    }

    private void parseAndDisplayNMEAData(List<String>[] gpsData) {
            List<String> gpggaData = gpsData[0];
            List<String> gprmcData = gpsData[0];

            // Display fields
            latitudeLabel.setText("Latitude: " + convertDMMToDMS(gpggaData.get(2)) + "\t" + gpggaData.get(3));
            longitudeLabel.setText("Longitude: " + convertDMMToDMS(gpggaData.get(4)) + "\t" + gprmcData.get(5));
            utcLabel.setText("Time: " + convertToTimeFormat(gpggaData.get(1)));
            altitudeLabel.setText(new String("Altitude: " + gpggaData.get(9) + "\tM"));
            errorLabel.setText("");  // Clear error if successful

            speedLabel.setText("Speed : " + Float.valueOf(gprmcData.get(7))*1.852 + "\tKMPS");
            courseLabel.setText("Course : " + gprmcData.get(8)+"°");
            errorLabel.setText("");  // Clear error if successful
    }

    public static List<String>[] parseNMEA(String nmeaSentence) {
        @SuppressWarnings("unchecked")
        List<String>[] arrayOfLists = new List[2];
        arrayOfLists[0] = new ArrayList<>();
        arrayOfLists[1] = new ArrayList<>();
        String[] twoSentences = nmeaSentence.substring(0).split("\n");
        // System.out.println(twoSentences[0]+"\n______________________\n"+twoSentences[1]);

        // Remove the checksum part if it exists
        int checksumIndex = twoSentences[0].indexOf('*');
        String sentenceGPGGA = (checksumIndex != -1) 
            ? twoSentences[0].substring(0, checksumIndex) 
            : twoSentences[0];
        String sentenceGPRMC = (checksumIndex != -1) 
            ? twoSentences[1].substring(0, checksumIndex) 
            : twoSentences[1];

        // Split the sentence into tokens by comma
        String[] withoutCommaGPGGA = sentenceGPGGA.substring(1).split(",");
        String[] withoutCommaGPRMC = sentenceGPRMC.substring(1).split(",");


        // Add each token to the list
        for (String token : withoutCommaGPGGA) {
            arrayOfLists[0].add(token);
        }
        for (String token : withoutCommaGPRMC) {
            arrayOfLists[1].add(token);
        }

        return arrayOfLists;
    }

    public static String convertToTimeFormat(String hhmmss) {
        // Check if the input is valid
        if (hhmmss == null || hhmmss.length() != 6) {
            throw new IllegalArgumentException("Input must be a 6-character string in HHMMSS format.");
        }

        // Extract hours, minutes, and seconds
        String hours = hhmmss.substring(0, 2);
        String minutes = hhmmss.substring(2, 4);
        String seconds = hhmmss.substring(4, 6);

        // Format and return in HH:MM:SS
        return hours + ":" + minutes + ":" + seconds;
    }
  
    public static String convertDMMToDMS(String dmmString) {
        // Convert the string to a double
        double dmm = Double.parseDouble(dmmString);

        // Extract the degrees part (first two digits for degrees)
        int degrees = (int) (dmm / 100);  // Dividing by 100 to extract the first two digits

        // Extract the minutes part (fractional part of DMM)
        double minutesDecimal = (dmm - degrees * 100);  // Subtract degrees multiplied by 100

        // Extract minutes and seconds
        int minutes = (int) minutesDecimal;
        double seconds = (minutesDecimal - minutes) * 60;

        // Round seconds to 2 decimal places
        seconds = Math.round(seconds * 100.0) / 100.0;

        // Return the result in DMS format as a string
        return String.format("%d° %d' %.2f\"", degrees, minutes, seconds);
    }
        
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NMEA0183Display frame = new NMEA0183Display();
            frame.setVisible(true);
        });
    }
}
