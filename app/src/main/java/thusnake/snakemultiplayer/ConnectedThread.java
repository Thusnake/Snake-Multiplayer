package thusnake.snakemultiplayer;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Thusnake on 01-Aug-16.
 */
public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private int state = 1;
    private final int STATE_NOT_CONNECTED = 1;
    private final int STATE_CONNECTED = 2;

    public ConnectedThread(BluetoothSocket socket) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
            state = 2;
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        byte[] filler = {69};
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        // TODO REMOVE THIS WHEN YOU'RE READY TO START DOING BLUETOOTH SHIT
        /*
        while (true) {
            try {
                //if (mmInStream.available() > 0) {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                //} // Whoever goes through run() crashes horribly.
                if (MyGLRenderer.bluetoothBytes == null) {
                    byte[] concat = new byte[bytes];
                    System.arraycopy(buffer,0,concat,0,bytes);
                    MyGLRenderer.bluetoothBytes = concat;
                } else {
                    byte[] a = MyGLRenderer.bluetoothBytes;
                    byte[] concat = new byte[a.length + bytes];
                    System.arraycopy(a, 0, concat, 0, a.length);

                    //System.out.print("First: ");
                    //for (int i=0; i<concat.length; i++) System.out.print(concat[i] + " ");
                    //System.out.println(" ");

                    //System.arraycopy(filler, 0, concat, a.length, filler.length);

                    //System.out.print("Second: ");
                    //for (int i=0; i<concat.length; i++) System.out.print(concat[i] + " ");
                    //System.out.println(" ");

                    System.arraycopy(buffer, 0, concat, a.length, bytes);

                    //System.out.print("Third: ");
                    //for (int i=0; i<concat.length; i++) System.out.print(concat[i] + " ");
                    //System.out.println(" ");

                    MyGLRenderer.bluetoothBytes = concat;
                }

            } catch (IOException e) {
                break;
            }
        }
        */
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }

    public int getCurrentState() {
        return state;
    }
}
