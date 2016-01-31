package com.mbodyreader.bluetooth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import org.apache.log4j.LogManager;

/**
 *
 * @author ciszek
 */
public class BluetoothServices {

    private static final int ATTR_ID = 0x0100;
    
    public static ServiceRecord findService( RemoteDevice remoteDevice, UUID serviceType ) {
        
        List<RemoteDevice> remoteDevices = new ArrayList<>();
        remoteDevices.add(remoteDevice);
        List<ServiceRecord> foundServices = findServices( remoteDevices, serviceType );
        
        if ( foundServices.size() > 0 ) {
            return foundServices.get(0);
        }
        return null;
    }
    
    public static List<ServiceRecord> findServices( List<RemoteDevice> remoteDevices, UUID serviceType) {

        List<ServiceRecord> serviceList = new ArrayList<>();
        
        final Object serviceSearchCompletedEvent = new Object();

        DiscoveryListener listener = new DiscoveryListener() {

            @Override
            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
            }

            @Override
            public void inquiryCompleted(int discType) {
            }

            @Override
            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
                for (int i = 0; i < servRecord.length; i++) {
                    String url = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                    if (url == null) {
                        continue;
                    }
                    serviceList.add(servRecord[i]);
                }
            }

            @Override
            public void serviceSearchCompleted(int transID, int respCode) {
                synchronized(serviceSearchCompletedEvent){
                    serviceSearchCompletedEvent.notifyAll();
                }
            }

        };

        UUID[] searchUuidSet = new UUID[] { serviceType };
        int[] attrIDs =  new int[] {
                ATTR_ID
        };

        remoteDevices.stream().forEach((bluetoothDevice) -> {
            synchronized(serviceSearchCompletedEvent) {
                try {
                    LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(attrIDs, searchUuidSet, bluetoothDevice, listener);
                    serviceSearchCompletedEvent.wait();               
                } catch (IOException | InterruptedException ex) {
                LogManager.getLogger(BluetoothServices.class).debug(ex);
                }
            }
        });
        return serviceList;
    }
 
}
