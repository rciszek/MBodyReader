package com.mbodyreader.bluetooth;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.bluetooth.*;
import org.apache.log4j.LogManager;

/**
 *
 * @author ciszek
 */
public class BluetoothDevices {

    public static List<RemoteDevice> findDevices()  {

        List<RemoteDevice> bluetoothDevices = new ArrayList<>();
        final Object inquiryCompletedEvent = new Object();

        DiscoveryListener listener = new DiscoveryListener() {

            @Override
            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                bluetoothDevices.add(btDevice);
            }

            @Override
            public void inquiryCompleted(int discType) {
                synchronized(inquiryCompletedEvent){
                    inquiryCompletedEvent.notifyAll();
                }
            }

            @Override
            public void serviceSearchCompleted(int transID, int respCode) {
            }

            @Override
            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
            }
        };

        synchronized(inquiryCompletedEvent) {
            try {
                boolean started = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, listener);
                if (started) {
                    inquiryCompletedEvent.wait();
                }
            } catch (BluetoothStateException | InterruptedException ex) {
                LogManager.getLogger(BluetoothDevices.class).debug(ex);
            }
        }
        return bluetoothDevices;
    }
    
    public static RemoteDevice findDevice( String name ) {
        
        RemoteDevice remoteDevice = null;
        List<RemoteDevice> availableDevices = findDevices();
        
        for ( RemoteDevice device: availableDevices ) {
            try {
                if ( device.getFriendlyName(true).equals(name)) {
                    remoteDevice = device;
                }
            } catch (IOException ex) {
                Logger.getLogger(BluetoothDevices.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return remoteDevice;
    }

}