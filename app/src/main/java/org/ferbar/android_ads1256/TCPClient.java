package org.ferbar.android_ads1256;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Locale;

/**
 * Created by chris on 05.02.17.
 */
class TCPClient extends Thread {
    static final String TAG="TCPClient";
    public static final String SERVERIP = "192.168.178.45";
    public static final int SERVERPORT = 3030;
    OutputStream out;
    InputStream in;
    private boolean mRun = true;
    private OnMessageReceived mMessageListener = null;

    double lastTime=0;

    class Entry {
        double time;
        int value[];
        public Entry(double time, int value[]) {
            this.time=time;
            this.value=value;
        }
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    interface OnMessageReceived {
        public void helloMessageReceived(String message) throws InterruptedException;
        public void dataMessageReceived(Entry[] dataList) throws InterruptedException;
    }

    /**
     *  Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPClient(OnMessageReceived listener) {
        mMessageListener = listener;
    }

    public void stopClient(){
        mRun = false;
    }

    /**
     * Sends the message entered by client to the server
     * @param message text entered by client
     */
    public void sendMessage(String message) throws IOException {
        if (out != null ) {
            byte[] m=message.getBytes();
            byte buffer[] = new byte[4];
            buffer[0]=(byte)(m.length & 0xff);
            buffer[1]=(byte)(m.length >> 8 & 0xff);
            buffer[2]=(byte)(m.length >> 16 & 0xff);
            buffer[3]=(byte)(m.length >> 24 & 0xff);
            out.write(buffer);
            out.write(m);
            out.flush();
        }
    }

    public String readMessage() throws Exception {
        int size0 = in.read();
        if (size0 < 0) throw new Exception("error reading size0 ("+size0+")");
        int size1 = in.read();
        if (size1 < 0) throw new Exception("error reading size1");
        int size2 = in.read();
        if (size2 < 0) throw new Exception("error reading size2");
        int size3 = in.read();
        if (size3 < 0) throw new Exception("error reading size3");
        int size = size0 | (size1 << 8) | (size2 << 16) | (size3 << 24);
        if (size < 0 || size > 100000)
            throw new Exception("error: invalid size (" + size + ")");
        byte buffer[] = new byte[size];
        int readBytes=0;
        while(readBytes != size) {
            int rc=in.read(buffer,readBytes,size-readBytes);
            if(rc < 0) {
                throw new Exception("error: remote site closed connection");
            }
            if(rc == 0) {
                throw new Exception("error: 0 bytes read!?!?");
            }
            readBytes+=rc;
        }
        /*
        if (rc != size)
            throw new Exception("error: reading " + size + "bytes (read:"+rc+"bytes)");
            */
        String serverMessage = new String(buffer);
        return serverMessage;
    }

    @Override
    public void run() {
        while(mRun) {
            try {
                //here you must put your computer's IP address.
                InetAddress serverAddr = InetAddress.getByName(SERVERIP);

                Log.e(TAG, "C: Connecting...");

                //create a socket to make the connection with the server
                Socket socket = new Socket(serverAddr, SERVERPORT);

                try {

                    //send the message to the server
                    out = socket.getOutputStream();

                    //receive the message which the server sends back
                    in = socket.getInputStream();

                    String serverMessage=this.readMessage();
                    Log.e(TAG, "HELLO MESSAGE FROM SERVER S: Received Message: '" + serverMessage + "'");
                    if (mMessageListener != null) {
                        mMessageListener.helloMessageReceived(serverMessage);
                    }

                    Log.e(TAG, "C: Sent.");

                    Log.e(TAG, "C: Done.");


                    //in this while the client listens for the messages sent by the server
                    while (mRun) {
                        this.sendMessage("g "+String.format(Locale.US, "%.6f", this.lastTime));
                        serverMessage=this.readMessage();

                        Log.e(TAG, "RESPONSE FROM SERVER S: Received Message: '" + serverMessage + "'");

                        String data[] = serverMessage.split("\\n");
                        Entry dataList[] = new Entry[data.length-1];
                        for (int i=0; i<data.length-1; i++) { // in der letzten zeile is nix
                            System.out.println(data[i]);
                            String e[] = data[i].split(" ");
                            double time=Double.parseDouble(e[0]);
                            int values[]=new int[8];
                            for(int j=0; j < 8; j++)
                                values[j]=Integer.parseInt(e[j+1]);
                            dataList[i]=new TCPClient.Entry(time, values);
                            if(time > this.lastTime) {
                                this.lastTime = time;
                            }
                        }

                        if (mMessageListener != null) {
                            //call the method messageReceived from MyActivity class
                            mMessageListener.dataMessageReceived(dataList);
                        }
                        serverMessage = null;
                    }


                } catch (Exception e) {

                    Log.e(TAG, "S: Error", e);

                } finally {
                    //the socket must be closed. It is not possible to reconnect to this socket
                    // after it is closed, which means a new socket instance has to be created.
                    socket.close();
                }

            } catch (Exception e) {

                Log.e(TAG, "C: Error", e);

            }
            try {
                if(mRun) {
                    Log.e(TAG, "Sleeping & reconnecting");
                    Thread.sleep(5000, 0);
                } else {
                    Log.e(TAG, "thread exit");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
