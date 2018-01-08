package thusnake.snakemultiplayer;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Thusnake on 01-Aug-16.
 */
public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    public ConnectThread(BluetoothDevice device) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        mmDevice = device;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            UUID uuid = UUID.fromString("55199d92-8a72-4fb7-807e-f482efeff3d6");
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) { }
        mmSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        OpenGLES20Activity.mBluetoothAdapter.cancelDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            System.out.println("Guest: Attempting to connect");
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                mmSocket.close();
            } catch (IOException closeException) { }
            return;
        }

        // Do work to manage the connection (in a separate thread)
        System.out.println("Guest: Socket Connected Completely");
        manageConnectedSocket(mmSocket);
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        OpenGLES20Activity.cnctdThread = new ConnectedThread(socket);
        OpenGLES20Activity.cnctdThread.start();
        byte players = 0;
        /*for (int i = 0; i < 4; i++) {
            if (MyGLRenderer.playercontroltype[i+1] != 0) {
                players++;
            }
        }*/
        byte bytes[] = {2,1,players};
        OpenGLES20Activity.cnctdThread.write(bytes);
    }

    BluetoothDevice getCurrentDevice() {
        return mmDevice;
    }
}
