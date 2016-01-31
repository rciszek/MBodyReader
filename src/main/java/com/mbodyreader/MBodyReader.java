
package com.mbodyreader;

import com.mbodyreader.bluetooth.BluetoothServices;
import com.mbodyreader.bluetooth.BluetoothDevices;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import org.apache.log4j.LogManager;

/**
 *
 * @author ciszek
 */
public class MBodyReader {
    
    private static final long RFCOMM = 0x0003;
    private static final String ONLINE_START = "om4\r";
    private static final String ONLINE_STOP = "omn\r";

    //Header reading was bugged and was removed from version 0.2. Therefore
    //the amount of channels is always assumed to be 4 and values to start after
    //303 bytes.
    private static final int HEADER_BYTES = 303;
    private static final int CHANNEL_COUNT = 4;
            
    private static final int CHANNEL_INFO_BYTES = 70;
    private static final int SIGNAL_TYPE_BYTES = 20;
    private static final int SIGNAL_SOURCE_BYTES = 40;
    private static final int SIGNAL_UNITS_BYTES = 10;  
    
    private static final byte MARKER_MIN = 0x7F;
    private static final byte SYNC_CONSTANT = 0x50;
    
    private int[] values = new int[CHANNEL_COUNT];
    
    private Thread readerThread;
    private State state = State.STOPPED;

    public State getState() {
        return state;
    }
    
    public enum State {
        RUNNING,
        STOPPED,
        DEVICE_NOT_FOUND,
        SERVICE_NOT_FOUND
    }
    
    private String url; 
    
    public void start( String deviceName) {
             
        RemoteDevice mBodyDevice = BluetoothDevices.findDevice(deviceName);
        if ( mBodyDevice == null ) {
            state = State.DEVICE_NOT_FOUND;
            return;
        }
        ServiceRecord serviceRecord = BluetoothServices.findService(mBodyDevice, new UUID(RFCOMM));

        if ( serviceRecord == null ) {
            state = State.SERVICE_NOT_FOUND;
            return;
        }
        
        url = serviceRecord.getConnectionURL( ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
        
        readerThread = new Thread()
        {
             public void run()
             {
                readData(url);
             }
        };
        state = State.RUNNING;
        readerThread.start();

    }
    
    public void stop() {
        state = State.STOPPED;
        sendMessage(url, ONLINE_STOP);
    }
    
    private void sendMessage( String url, String message ) {
             
        try {
            StreamConnection connection = (StreamConnection) Connector.open(url);    
            OutputStream outputStream = connection.openOutputStream();
            outputStream.write(message.getBytes(StandardCharsets.US_ASCII));
            connection.close();
        } catch(IOException exception) {
            LogManager.getLogger(MBodyReader.class).debug(exception);
        }        
    }
    
    private void readData( String url ) {
        try {
            StreamConnection connection = (StreamConnection) Connector.open(url);    
            
            InputStream inputStream = connection.openInputStream();
            OutputStream outputStream = connection.openOutputStream();
            outputStream.write(ONLINE_START.getBytes(StandardCharsets.US_ASCII));
            
            readHeader(inputStream);
            readSamples(inputStream, CHANNEL_COUNT);
            
            connection.close();
        } catch(IOException exception) {
            stop();
            LogManager.getLogger(MBodyReader.class).debug(exception);
        }        
    } 
        
    private void readHeader( InputStream inputStream ) throws IOException {
        byte[] header = new byte[HEADER_BYTES];
        inputStream.read(header);
    }      
    
    private void readSamples( InputStream inputStream, int channelCount ) throws IOException {

  
        byte[] buffer = new byte[8];
        
        while ( inputStream.read(buffer)  != -1 && getState() == State.RUNNING ) {
                   
            if ( buffer[0] >= MARKER_MIN || buffer[1] < SYNC_CONSTANT) {
                continue;
            }
            
            
             buffer[1] = (byte) (buffer[1] - SYNC_CONSTANT);
            
             ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
             byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
             
             int latest[] = new int[channelCount]; 
 
             for ( int i = 0; i < channelCount; i++ ) {
                 latest[i] = byteBuffer.getShort();
             }
                                
            values = latest;
        }
        
    }
    
    public int[] getValues() {
        return values;
    }
   
}
