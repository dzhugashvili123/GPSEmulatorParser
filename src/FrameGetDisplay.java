import com.fazecast.jSerialComm.SerialPort;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FrameGetDisplay {
    private static final BlockingQueue<Character> bufferQueue = new LinkedBlockingQueue<>();
    private static SerialPort serialPort;
    static ArrayList<String> gpgga, gprmc;
    //static ArrayList<String> prevgpgga, prevgprmc;
    static boolean validity;
    static String prevgpgga;


    public static void main(String[] args) {
        // Initialize the serial port
        serialPort = SerialPort.getCommPort("COM2");  // Replace with your receiver port
        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);

        if (serialPort.openPort()) {
            System.out.println("Port opened successfully.");
        } else {
            System.err.println("Error: Unable to open serial port.");
            return;
        }

        // Start the reader and display threads
        Thread readerThread = new Thread(new SerialReader());
        Thread displayThread = new Thread(new DisplayProcessor());
        Thread windowThread = new Thread(new AWTwindow());
        
        readerThread.start();
        displayThread.start();
        windowThread.start();
    }

    static class SerialReader implements Runnable {
        @Override
        public void run() {
            byte[] byteBuffer = new byte[1]; 

            while (serialPort.isOpen()) {
                if (serialPort.bytesAvailable() > 0) {
                    serialPort.readBytes(byteBuffer, 1);
                    char receivedChar = (char) byteBuffer[0];

                    try {
                        bufferQueue.put(receivedChar);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Reader thread interrupted.");
                        break;
                    }
                }
            }
        }
    }

    static class DisplayProcessor extends JFrame implements Runnable {

        public DisplayProcessor() {
            gpgga = new ArrayList<String>();
            gprmc = new ArrayList<String>();
            prevgpgga = "GPGGA,151643,2058.4884,S,14911.7889,E,1,11,1.7,548.3,M,,,";
            validity = false;
        }

        public static double calculateDistance(String nmea1, String nmea2) {
            double[] coord1 = extractCoordinates(nmea1);
            double[] coord2 = extractCoordinates(nmea2);
    
            return haversine(coord1[0], coord1[1], coord2[0], coord2[1]);
        }

        private static double haversine(double lat1, double lon1, double lat2, double lon2) {
            final int R = 6371; // Radius of the Earth in km
    
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
    
            lat1 = Math.toRadians(lat1);
            lat2 = Math.toRadians(lat2);
    
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                       Math.cos(lat1) * Math.cos(lat2) *
                       Math.sin(dLon / 2) * Math.sin(dLon / 2);
    
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            
            return R * c; // Distance in km
        }
    
        private static double[] extractCoordinates(String nmea) {
            String[] tokens = nmea.split(",");
            
            double latitude = convertDMMToDecimal(tokens[2], tokens[3]);
            double longitude = convertDMMToDecimal(tokens[4], tokens[5]);
            
            return new double[]{latitude, longitude};
        }
    
        private static double convertDMMToDecimal(String dmmString, String direction) {
            double dmm = Double.parseDouble(dmmString);
            int degrees = (int) (dmm / 100);
            double minutes = dmm - (degrees * 100);
            double decimalDegrees = degrees + (minutes / 60);
    
            // Adjust for South and West coordinates
            if (direction.equals("S") || direction.equals("W")) {
                decimalDegrees *= -1;
            }
            
            return decimalDegrees;
        }

        public void parseAndDisplay(String data) {
            String trimmed = data.trim();

            //_________________________________________________________________________________________
            int checksumIndex = trimmed.indexOf('*');
            String recievedCheckSum = trimmed.substring(checksumIndex+1, trimmed.length());
            String minusCheckSum = (checksumIndex != -1) 
            ? trimmed.substring(0, checksumIndex) 
            : trimmed;
            int cs = 0;     //Calculated checksum
            for(int i = 0; i<minusCheckSum.length(); i++){
                cs ^= minusCheckSum.charAt(i);
            }

            //_________________________________________________________________________________________

            String withoutCheckSum = (checksumIndex != -1) 
            ? trimmed.substring(0, checksumIndex) 
            : trimmed;


            if(withoutCheckSum.contains("GPGGA"))
            {
                if ( recievedCheckSum.equals(String.format("%02X", cs))) {
                    gpgga.clear();
                    double dist = calculateDistance(prevgpgga, withoutCheckSum);
                    String[] withoutCommaGPGGA = withoutCheckSum.substring(0).split(",");
                    for (String token : withoutCommaGPGGA) {
                        gpgga.add(token);
                    }
                    prevgpgga = withoutCheckSum;
                    System.out.println(withoutCheckSum + "\t" + String.format("%02X", cs) + "\t" + recievedCheckSum);
                    if(dist < 8000) validity = true;
                    else validity = false;
                }
                else{
                    System.out.println(withoutCheckSum + "\tCorrupted Data");
                }
            }
            else {
                gprmc.clear();
                if ( recievedCheckSum.equals(String.format("%02X", cs))) {
                    String[] withoutCommaGPRMC = withoutCheckSum.substring(0).split(",");
                    for (String token : withoutCommaGPRMC) {
                        gprmc.add(token);
                    }
                    System.out.println(withoutCheckSum + "\t" + String.format("%02X", cs) + "\t" + recievedCheckSum);
                }
                else{
                    System.out.println(withoutCheckSum + "\tCorrupted Data");
                }
            }
            System.out.println("-----------------------------------------");
        }

        @Override
        public void run() {
            StringBuilder sentenceBuilder = new StringBuilder();

            while(true) {
                try {
                    char currentChar = bufferQueue.take();
                    if(currentChar == '$') {    sentenceBuilder.setLength(0);}
                    else {
                        if(currentChar == '\r') parseAndDisplay(sentenceBuilder.toString());
                        else    sentenceBuilder.append(currentChar);
                    }
                } catch(InterruptedException e) { System.out.println("Display thread interrupted.");}
            }

        }
    }

    static class AWTwindow extends JFrame implements Runnable {
        private JLabel latitudeLabel, longitudeLabel;
        private JLabel utcLabel, altitudeLabel, speedLabel, courseLabel, errorLabel;
        private Timer timer;

        ArrayList<String> gpggaData, gprmcData;

        public AWTwindow() {
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
            
            // Make the frame visible
            setVisible(true);

        }

        @Override
        public void run() {
            gpggaData = new ArrayList<String>(gpgga);
            gprmcData = new ArrayList<String>(gprmc);

            timer = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Disp();
                }
            });
            timer.start();
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

        public void Disp() {
            gpggaData = new ArrayList<String>(gpgga);
            gprmcData = new ArrayList<String>(gprmc);

            latitudeLabel.setText("Latitude: " + convertDMMToDMS(gpggaData.get(2)) + "\t" + gpggaData.get(3));
            longitudeLabel.setText("Longitude: " + convertDMMToDMS(gpggaData.get(4)) + "\t" + gpggaData.get(5));
            utcLabel.setText("Time: " + convertToTimeFormat(gpggaData.get(1)));
            altitudeLabel.setText(new String("Altitude: " + gpggaData.get(9) + "\tM"));
            if(validity == true)    errorLabel.setText("Valid");
            else    errorLabel.setText("Not valid");

            speedLabel.setText("Speed : " + Float.valueOf(gprmcData.get(7))*1.852 + "\tKMPS");
            courseLabel.setText("Course : " + gprmcData.get(8)+"°");
        }
    }
}
