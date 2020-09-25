/**
 * <pre>
 * ##########################################################################################################
 * ######                                                                                              ######
 * ######                   The code has been altered by Stein J. Modalsli                             ######
 * ######                       and is based on the code from:                                         ######
 * ######                                                                                              ######
 * ##########################################################################################################
 * ##########################################################################################################
 * ######                            This file is part of Java FSUIPC SDK                              ######
 * ######                                        Version: 1.0                                          ######
 * ######         Based upon 64 bit Java SDK by Paul Henty who amended 32 bit SDK by Mark Burton       ######
 * ######                                   ©2020, Radek Henys                                         ######
 * ######                         All rights .... well, this will be LGPL or so                        ######
 * ######                                   http:\\mouseviator.com                                     ######
 * ##########################################################################################################
 * </pre>
 */
package com.mouseviator.fsuipc.example;

import com.mouseviator.fsuipc.FSUIPC;
import com.mouseviator.fsuipc.FSUIPCWrapper;
import com.mouseviator.fsuipc.IFSUIPCListener;
import com.mouseviator.fsuipc.datarequest.IDataRequest;
import com.mouseviator.fsuipc.datarequest.advanced.FSControlRequest;
import com.mouseviator.fsuipc.datarequest.primitives.DoubleRequest;
import com.mouseviator.fsuipc.datarequest.primitives.FloatRequest;
import com.mouseviator.fsuipc.datarequest.primitives.IntRequest;
import com.mouseviator.fsuipc.datarequest.primitives.ShortRequest;
import com.mouseviator.fsuipc.datarequest.primitives.LongRequest;
import com.mouseviator.fsuipc.helpers.aircraft.AircraftHelper;
import com.mouseviator.fsuipc.helpers.aircraft.Engine1Helper;
import com.mouseviator.fsuipc.helpers.aircraft.Engine2Helper;
import com.mouseviator.fsuipc.helpers.aircraft.GearHelper;
import com.mouseviator.fsuipc.helpers.avionics.GPSHelper;
import com.mouseviator.fsuipc.helpers.SimHelper;
import com.mouseviator.fsuipc.helpers.avionics.COM1Helper;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalTime;
import java.util.AbstractQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import javax.swing.SwingUtilities;
import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;

import com.fazecast.jSerialComm.SerialPort;
import java.lang.reflect.InvocationTargetException;


/**
 * This is more complex example on using {@link FSUIPC} calls, data requests, {@link IFSUIPCListener} for continual data request processing,
 * and displaying data in Swing GUI application.
 * 
 * @author Murdock
 */
public class FSUIPCSimMonitor extends javax.swing.JFrame {

    private final int MINIMUMVALUE = 0;
    private final int MAXIMUMVALUE = 16383;
    /**
     * Default zoom
     */
    private final byte DEFAULT_ZOOM = 7;
    /**
     * Default location to center map onto
     */
    public static final GeoPosition DEFAULT_LOCATION = new GeoPosition(48.652032, -122.585922);

    /**
     * JXmapViewer kit
     */
    private final JXMapKit jXMapKit = new JXMapKit();

    /**
     * FSUIPC Instance
     */
    private final FSUIPC fsuipc = FSUIPC.getInstance();
    /**
     * FSUIPC listener
     */
    private IFSUIPCListener fsuipcListener;
    /**
     * logger
     */
    private static final Logger logger = Logger.getLogger(FSUIPCSimMonitor.class.getName());

    private final AircraftHelper aircraftHelper = new AircraftHelper();
    private final SimHelper simHelper = new SimHelper();
    private final GPSHelper gpsHelper = new GPSHelper();
    private final Engine1Helper engine1Helper = new Engine1Helper();
    private final Engine2Helper engine2Helper = new Engine2Helper();
    private final COM1Helper com1Helper = new COM1Helper();
    private final GearHelper gearHelper = new GearHelper();
    
    
    private FloatRequest aircraftHeading;
    private IDataRequest<Float> aircraftMagVar;
    private FloatRequest aircraftBank;
    private FloatRequest aircraftPitch;
    private IDataRequest<Double> gpsAltitude;
    private DoubleRequest aircraftLatitude;
    private DoubleRequest aircraftLongitude;    
    private FloatRequest aircraftIAS;
    private FloatRequest aircraftTAS;
    private FloatRequest aircraftVS;
    private IDataRequest<Short> pauseIndicator;
    private IntRequest simLocalTime;
    private ShortRequest eng1ThrLever;
    private ShortRequest eng1MixLever;
    private ShortRequest eng1PropLever;
    private FloatRequest eng1OilTemp;
    private FloatRequest eng1OilQuantity;
    private FloatRequest eng1OilPressure;
    private DoubleRequest eng1FuelFlow;
    private ShortRequest eng2ThrLever;
    private ShortRequest eng2MixLever;
    private ShortRequest eng2PropLever;
    private FloatRequest eng2OilTemp;
    private FloatRequest eng2OilQuantity;
    private FloatRequest eng2OilPressure;
    private DoubleRequest eng2FuelFlow;
    private FloatRequest com1Frequency;
    private FloatRequest com1Standby;
    private IntRequest gearPositionNose;
    private IntRequest gearPositionLeft;
    private IntRequest gearPositionRight;
    
    private IntRequest com1FrequencyInt;
    private IntRequest com1StandbyInt;
    
    private IDataRequest<Float> simFrameRate;
    private ShortRequest slewMode;
    private FSControlRequest slewControl = new FSControlRequest(65557);   //fs slew toggle control

    private final DecimalFormat decimalFormat1 = new DecimalFormat("#.#");
    
    private final DecimalFormat decimalFormat3 = new DecimalFormat("#.###");
    
    private boolean libFileLoggingEnabled = true;
    
    private SerialPort sp = SerialPort.getCommPort("COM5");


    /**
     * Creates new form FSUIPCAircraftMonitor
     */
    public FSUIPCSimMonitor() {
        //Init decimal formatters, will need that for "nice" data output
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
        dfs.setDecimalSeparator('.');
        this.decimalFormat1.setDecimalFormatSymbols(dfs);
        this.decimalFormat3.setDecimalFormatSymbols(dfs);
        
        //set finer level for debugging
        final Logger logger = Logger.getLogger("com.mouseviator");
        //disable system handlers for us, or the FINER level will not apply
        logger.setUseParentHandlers(false);
        //custom console handler
        final Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINER);
        logger.addHandler(consoleHandler);
        logger.setLevel(Level.FINER);
        

        initComponents();

        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                //disconnect fsuipc, no nned to cancel processing tasks, the diconnect method will do it for us
                fsuipc.disconnect();
            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });

        //initMapViewer();
        fsuipcStatusChanged(false);
        setTitle(false);
        startFSUIPC();        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblSituationFileCap = new javax.swing.JLabel();
        lblFSVersion = new javax.swing.JLabel();
        lblFSUIPCVersion = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("FSUIPC Aircraft Monitor");

        lblSituationFileCap.setText("Situation file:");

        lblFSVersion.setText("Version");

        lblFSUIPCVersion.setText("FSUIPCVersion");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblFSVersion)
                    .addComponent(lblFSUIPCVersion)
                    .addComponent(lblSituationFileCap, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 281, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(lblFSVersion)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblFSUIPCVersion)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblSituationFileCap)
                .addGap(45, 45, 45))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(FSUIPCSimMonitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(FSUIPCSimMonitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(FSUIPCSimMonitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(FSUIPCSimMonitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FSUIPCSimMonitor().setVisible(true);
            }
        });
    }

    private String adjustSize(String inputString, int length){
        if(inputString.length() < length){
            inputString = padRightZeros(inputString, length);
        }else if(inputString.length() > length){
            inputString = inputString.substring(0, length);
        }
        return inputString;
    }
    
    private String padRightZeros(String inputString, int length) {
    while (inputString.length() < length) {
        inputString = inputString + "0";
    }
    return inputString;
}
    
    private void sendToArduino(String outputValue1, String outputValue2, String headingText, int gearPos, int gearPosR, int gearPosL){
        //logger.fine("Sending: " + outputValue1 + ", " + outputValue2 + ", " + headingText);

       String stringToWrite = outputValue1  + "#" +  outputValue2 + "#" +  headingText + "#";
       if(gearPos == MINIMUMVALUE)
       {
           stringToWrite = stringToWrite + "On#";
       }else if (gearPos == MAXIMUMVALUE){
           stringToWrite = stringToWrite + "Off#";           
       }else{
           stringToWrite = stringToWrite + "Moving#";
       }
       if(gearPosR == MINIMUMVALUE)
       {
           stringToWrite = stringToWrite + "On#";
       }else if (gearPosR == MAXIMUMVALUE){
           stringToWrite = stringToWrite + "Off#";           
       }else{
           stringToWrite = stringToWrite + "Moving#";
       }
       if(gearPosL == MINIMUMVALUE)
       {
           stringToWrite = stringToWrite + "On#";
       }else if (gearPosL == MAXIMUMVALUE){
           stringToWrite = stringToWrite + "Off#";           
       }else{
           stringToWrite = stringToWrite + "Moving#";
       }
        logger.fine("***** Value: " + stringToWrite);
        //
        try {
            byte[] comBytes = String.valueOf(stringToWrite).getBytes();
            sp.getOutputStream().write(comBytes);
            sp.getOutputStream().flush();
        } catch (IOException ex) {
            Logger.getLogger(FSUIPCSimMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
        //
    }

    private void startFSUIPC() {
        // First of all, load the native library. The default load function will try to determine if we are running under 32 or 64 bit JVM
        // and load 32/64 bit native library respectively
        byte result = FSUIPC.load();
        if (result != FSUIPC.LIB_LOAD_RESULT_OK) {
            //lblInfo.setText("<html><p style=\"background-color: red; color: white; font-weight: bold\">Failed to load native library. NO JOY! This is all folks!!!</p></html>");                        
            return;
        }
        
        toggleLibFileLogging();    
        logger.info("Trying to connect....");
        
        fsuipcListener = new IFSUIPCListener() {
            @Override
            public void onConnected() {
                logger.info("FSUIPC connected!");

                sp.setComPortParameters(9600, 8, 1, 0); // default connection settings for Arduino
                sp.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0); // block until bytes can be written

                try {
                    if (sp.openPort()) {
                        System.out.println("Port is open :)");
                        Thread.sleep(4000);
                    } else {
                        System.out.println("Failed to open port :(");
                        return;
                    }	
                }catch(InterruptedException ex){
                        System.out.println("Failed to open port :(");
                }
                
                //register one time requests
                IDataRequest<String> situationFile = (IDataRequest<String>) fsuipc.addOneTimeRequest(simHelper.getSituationFile());
                IDataRequest<String> fsxp3dVersion = (IDataRequest<String>) fsuipc.addOneTimeRequest(simHelper.getFSXP3DVersion());
                fsuipc.processRequestsOnce();

                //clear all previous continual requests, it will also stop processing thread
                fsuipc.clearContinualRequests();

                //register continual requests                
                aircraftHeading = (FloatRequest) fsuipc.addContinualRequest(aircraftHelper.getHeading());
                aircraftMagVar = (IDataRequest<Float>) fsuipc.addContinualRequest(aircraftHelper.getMagneticVariation());
                aircraftBank = (FloatRequest) fsuipc.addContinualRequest(aircraftHelper.getBank());
                aircraftPitch = (FloatRequest) fsuipc.addContinualRequest(aircraftHelper.getPitch());
                gpsAltitude = (IDataRequest<Double>) fsuipc.addContinualRequest(gpsHelper.getAltitude(true));                
                aircraftLatitude = (DoubleRequest) fsuipc.addContinualRequest(aircraftHelper.getLatitude());
                aircraftLongitude = (DoubleRequest) fsuipc.addContinualRequest(aircraftHelper.getLongitude());
                aircraftVS = (FloatRequest) fsuipc.addContinualRequest(aircraftHelper.getVerticalSpeed(true));
                aircraftIAS = (FloatRequest) fsuipc.addContinualRequest(aircraftHelper.getIAS());
                aircraftTAS = (FloatRequest) fsuipc.addContinualRequest(aircraftHelper.getTAS());

                pauseIndicator = (IDataRequest<Short>) fsuipc.addContinualRequest(simHelper.getPauseIndicator());
                simLocalTime = (IntRequest) fsuipc.addContinualRequest(simHelper.getLocalTime());
                simFrameRate = (IDataRequest<Float>) fsuipc.addContinualRequest(simHelper.getFrameRate());
                
                eng1ThrLever = (ShortRequest) fsuipc.addContinualRequest(engine1Helper.getThrottleLever());
                eng1MixLever = (ShortRequest) fsuipc.addContinualRequest(engine1Helper.getMixtureLever());
                eng1PropLever = (ShortRequest) fsuipc.addContinualRequest(engine1Helper.getPropellerLever());
                eng1OilQuantity = (FloatRequest) fsuipc.addContinualRequest(engine1Helper.getOilQuantity());
                eng1OilTemp = (FloatRequest) fsuipc.addContinualRequest(engine1Helper.getOilTemperature());
                eng1OilPressure = (FloatRequest) fsuipc.addContinualRequest(engine1Helper.getOilPressure());
                eng1FuelFlow = (DoubleRequest) fsuipc.addContinualRequest(engine2Helper.getFuelFlow());
                
                eng2ThrLever = (ShortRequest) fsuipc.addContinualRequest(engine2Helper.getThrottleLever());
                eng2MixLever = (ShortRequest) fsuipc.addContinualRequest(engine2Helper.getMixtureLever());
                eng2PropLever = (ShortRequest) fsuipc.addContinualRequest(engine2Helper.getPropellerLever());
                eng2OilQuantity = (FloatRequest) fsuipc.addContinualRequest(engine2Helper.getOilQuantity());
                eng2OilTemp = (FloatRequest) fsuipc.addContinualRequest(engine2Helper.getOilTemperature());
                eng2OilPressure = (FloatRequest) fsuipc.addContinualRequest(engine2Helper.getOilPressure());
                eng2FuelFlow = (DoubleRequest) fsuipc.addContinualRequest(engine2Helper.getFuelFlow());
            
                //Only support 2 decimals
                com1Frequency = (FloatRequest) fsuipc.addContinualRequest(com1Helper.getFrequency());
                com1Standby = (FloatRequest) fsuipc.addContinualRequest(com1Helper.getStandByFrequency());
                //Support all decimals
                com1FrequencyInt = (IntRequest) fsuipc.addContinualRequest(new IntRequest(0x05C4));
                com1StandbyInt = (IntRequest) fsuipc.addContinualRequest(new IntRequest(0x05CC));
                
                gearPositionRight = (IntRequest) fsuipc.addContinualRequest(gearHelper.getRightPosition());
                gearPositionLeft = (IntRequest) fsuipc.addContinualRequest(gearHelper.getLefttPosition());
                gearPositionNose = (IntRequest) fsuipc.addContinualRequest(gearHelper.getNosePosition());
           
                
                //slew mode indicator request
                slewMode = (ShortRequest) fsuipc.addContinualRequest(new ShortRequest(0x05DC));
                
                
                //start continual request processing at the rate of 250ms
                fsuipc.processRequests(250, true);

                //GUI updates should be done at EDT thread
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        fsuipcStatusChanged(true);
                        //lblSituationFile.setText(situationFile.getValue().trim());
                        lblFSVersion.setText(fsuipc.getFSVersion() + " (" + fsxp3dVersion.getValue() + ")");
                        
                        //cmdPause.setEnabled(true);
                        //set app title - connected
                        setTitle(true);
                    }
                });
            }

            @Override
            public void onDisconnected() {
                logger.info("FSUIPC disconnected!");

                //cancel continual request processing
                //not needed anymore, the fsuipc class will do it while it discovers that FSUIPC disconnected, before the listener is called
                //fsuipc.cancelRequestsProcessing();

                if (sp.closePort()) {
                    System.out.println("Port is closed :)");
                } else {
                    System.out.println("Failed to close port :(");
                    return;
                }
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        fsuipcStatusChanged(false);
                        //set app title - not connected
                        setTitle(false);
                    }
                });

            }

            @Override
            public void onProcess(AbstractQueue<IDataRequest> arRequests) {
                logger.fine("FSUIPC continual request processing callback!");
                
                try {
                    //set map center
                    //jXMapKit.setAddressLocation(new GeoPosition(aircraftLatitude.getValue(), aircraftLongitude.getValue()));
                    
                    //GUI updates on EDT thread
                    //SwingUtilities.invokeLater(new Runnable() {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            //set the labels from values
                            //lblHeading.setText(String.format("%d Mag (%d TRUE)", (int) Math.round(aircraftHeading.getValue() - aircraftMagVar.getValue()), (int) Math.round(aircraftHeading.getValue())));
                            DecimalFormat df = new DecimalFormat("0.000");
                            float COM1frequency = com1FrequencyInt.getValue()/1000;
                            float STDBY1frequency = com1StandbyInt.getValue()/1000;
                            String com1FrequencyString = df.format(COM1frequency/1000);
                            String com1StandbyString = df.format(STDBY1frequency/1000);
                           
                            //logger.fine("COM1: " + Integer.toString(com1FrequencyInt.getValue()) + ", " + Integer.toString(com1StandbyInt.getValue()));
                            
                            int gearPosN = gearPositionNose.getValue().intValue();
                            int gearPosR = gearPositionRight.getValue().intValue();
                            int gearPosL = gearPositionLeft.getValue().intValue();
                            int heading = (int) Math.round(aircraftHeading.getValue() - aircraftMagVar.getValue());
                            if(heading > 359){
                                heading = heading - 360;
                            }
                            String headingText = Integer.toString(heading);
                            if(headingText.length() == 2){
                                headingText = "0" + headingText;
                            }else if(headingText.length() == 1){
                                headingText = "00" + headingText;
                            }

                            
                            //Sending values to Arduino
                            sendToArduino(com1FrequencyString, com1StandbyString, headingText, gearPosN, gearPosL, gearPosR);
                            
                        }
                    });
                } catch (InterruptedException ex) {
                    Logger.getLogger(FSUIPCSimMonitor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(FSUIPCSimMonitor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void onFail(int lastResult) {
                logger.log(Level.INFO, "Last FSUIPC function call ended with error code: {0}, message: {1}",
                        new Object[]{lastResult,
                            FSUIPC.FSUIPC_ERROR_MESSAGES.get(FSUIPCWrapper.FSUIPCResult.get(lastResult))});
            }
        };

        //add the listener to fsuipc
        fsuipc.addListener(fsuipcListener);

        //start the thread that will wait for successful fsuipc connection, will try every 5 seconds
        fsuipc.waitForConnection(FSUIPCWrapper.FSUIPCSimVersion.SIM_ANY, 5);
    }

    private void toggleLibFileLogging() {
        //enable fsuipc library file logging
        if (libFileLoggingEnabled) {                   
            FSUIPCWrapper.setupLogging(true, "fsuipc_java.log", FSUIPCWrapper.LogSeverity.DEBUG.getValue(), 20 * 1024 * 1024);
            //cmdToggleLibFileLogging.setText("Disable Lib File Log");
            libFileLoggingEnabled = false;
        } else {
            FSUIPCWrapper.setupLogging(false, "fsuipc_java.log", FSUIPCWrapper.LogSeverity.DEBUG.getValue(), 20 * 1024 * 1024);
            //cmdToggleLibFileLogging.setText("Enable Lib File Log");
            libFileLoggingEnabled = true;
        }
    }

    private byte bcdToDec(byte val)
    {
        //return( (val/16*10) + (val%16) );
        return 0;
    }
    
    private void setTitle(boolean connected) {
        String title = "FSUIPC Aircraft Monitor";
        
        try {
            String arch = System.getProperty("sun.arch.data.model");
            if (arch.equals("32")) {
                title += " (32 bit) ";
            } else if (arch.equals("64")) {
                title += " (64 bit) ";
            } else {
                title += " (Unknown architecture) ";
            }
        } catch (Exception ex) {
            logger.severe("Failed to determine system architecture!");
        }
        
        if (connected) {
            title += " - Sim CONNECTED!";
        }
        setTitle(title);
    }

    private void fsuipcStatusChanged(boolean connected) {
        if (connected) {
            //lblStatus1.setText("<html><p style=\"background-color: green; color: white; font-weight: bold\">CONNECTED</p></html>");
            lblFSVersion.setText(fsuipc.getFSVersion());
            lblFSUIPCVersion.setText(fsuipc.getVersion());
            //lblFSUIPCLibVersion.setText(fsuipc.getLibVersion());
        } else {
            //lblStatus1.setText("<html><p style=\"background-color: red; color: white; font-weight: bold\">DISCONNECTED</p></html>");
            lblFSVersion.setText("N/A");
            lblFSUIPCVersion.setText("N/A");
            //lblFSUIPCLibVersion.setText("N/A");
        }

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblFSUIPCVersion;
    private javax.swing.JLabel lblFSVersion;
    private javax.swing.JLabel lblSituationFileCap;
    // End of variables declaration//GEN-END:variables
}
