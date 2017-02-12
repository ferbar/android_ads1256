package org.ferbar.android_ads1256;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

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
    final int TIMEOUT=5000;
    String helloMessage=null;

    static Queue<CommandEntry> commandQueue=new LinkedList<CommandEntry>();

    static class Entry {
        double time;
        int value[];
        public Entry(double time, int value[]) {
            this.time=time;
            this.value=value;
        }
    }

    static class CommandEntry {
        CommandCallback callback;
        String command;
        Object data;
        CommandEntry(CommandCallback callback, String command, Object data) {
            this.callback=callback; this.command=command; this.data=data;
        }
    }

    interface CommandCallback {
        public void commandCallback(String result, Object data) throws InterruptedException;
    }

    /**
     *  Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPClient() {

    }

    public void stopClient(){
        mRun = false;
    }

    /**
     * Sends the message entered by client to the server
     * @param message text entered by client
     *
     */
    public void sendMessage(String message) throws IOException {
        if (out != null ) {
            Log.i(TAG, "C: sending >>>"+message+"<<<");
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
        if(serverMessage.length() < 100)
            Log.i(TAG,"S: received [[["+serverMessage+"]]]");
        else
            Log.i(TAG,"S: received [[["+serverMessage.substring(0,100)+"...]]]");
        return serverMessage;
    }

    public void queueCommand(CommandCallback callback, String message, Object data) {
        synchronized (this) {
            if(!this.isAlive() && this.mRun==true) {
                this.start();
            }
            TCPClient.commandQueue.add(new CommandEntry(callback, message, data));
            this.notify();
        }
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
                    this.helloMessage=serverMessage;

                    Log.e(TAG, "C: Sent.");

                    Log.e(TAG, "C: Done.");


                    //in this while the client listens for the messages sent by the server
                    while (mRun) {
                        synchronized (this) {
                            if (this.commandQueue.isEmpty()) {
                                this.wait(TIMEOUT); // keine möglichkeit um zwischen timeout und notify zu unterscheiden - super is das :-D
                            }
                        }
                        CommandEntry command = this.commandQueue.poll();
                        if(command != null) {
                            this.sendMessage(command.command);
                            serverMessage=this.readMessage();
                            command.callback.commandCallback(serverMessage, command.data);
                            continue;
                        } else {
                            this.sendMessage("p"); // ping senden
                            serverMessage=this.readMessage();
                        }
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
                Log.e(TAG,"sleep exception ",e);
                break;
            }
        }
    }
}
