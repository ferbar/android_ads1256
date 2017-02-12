package org.ferbar.android_ads1256;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.Semaphore;

public class MainActivity extends AppCompatActivity {

    public static final String TAG="MainActivity";
    Toolbar toolbar;
    boolean running=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(this.toolbar);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.stopCapture();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GraphView graphView = (GraphView) MainActivity.this.findViewById(R.id.graph);
        graphView.invalidate();
        if(GraphView.values[GraphView.writePos] != null) {
            repaintLegende(GraphView.values[GraphView.writePos]);
        }
    }


    class CaptureThread extends Thread implements TCPClient.CommandCallback {
        boolean mRun = true;
        double lastTime=0;
        private final Semaphore semaphore = new Semaphore(1, true);

        @Override
        public void commandCallback(String result, Object extra) throws InterruptedException {

            String data[] = result.split("\\n");
            TCPClient.Entry dataList[] = new TCPClient.Entry[data.length-1];
            for (int i=0; i<data.length-1; i++) { // in der letzten zeile is nix
                // System.out.println(data[i]);
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

            MainActivity.this.dataMessageReceived(dataList);

            this.semaphore.release();
        }

        @Override
        public void run() {
            try {
                App app = (App) getApplicationContext();
                TCPClient client = app.getClient();
                while (this.mRun) {
                    this.semaphore.acquire(); // damit immer nur ein g command in der queue ist
                    client.queueCommand(this, "d " + String.format(Locale.US, "%.6f", this.lastTime), null);
                    Thread.sleep(100,0);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception: ",e);
            } finally {

            }
        }
    }

    CaptureThread captureThread=null;

    public void startCapture() {
        this.captureThread=new CaptureThread();
        this.captureThread.start();
        ActionMenuItemView m= (ActionMenuItemView) this.toolbar.findViewById(R.id.action_play_pause);
        m.setIcon(getResources().getDrawable(R.drawable.ic_pause_black_24dp));
        this.running=true;
    }

    public void stopCapture() {
        if(this.captureThread != null) {
            this.captureThread.mRun=false;
            this.captureThread=null;
        }
        ActionMenuItemView m= (ActionMenuItemView) this.toolbar.findViewById(R.id.action_play_pause);
        m.setIcon(getResources().getDrawable(R.drawable.ic_play_black_24dp));
        this.running=false;
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_play_pause:
                ActionMenuItemView m= (ActionMenuItemView) this.toolbar.findViewById(R.id.action_play_pause);
                if(this.running) {
                    stopCapture();
                } else {
                    startCapture();
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /*
    @Override
    public void helloMessageReceived(final String message) throws InterruptedException {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView) MainActivity.this.findViewById(R.id.textView1);
                tv.setText(message);
                GraphView graphView = (GraphView) MainActivity.this.findViewById(R.id.graph);
                graphView.setMax(5);
            }
        });
        Thread.sleep(100, 0);
    }
    */

    public void repaintLegende(float value[]) {
        TextView tv = (TextView) MainActivity.this.findViewById(R.id.textView1);
        tv.setText("0:" + String.format("%.3fV", value[0]));
        tv = (TextView) MainActivity.this.findViewById(R.id.textView2);
        tv.setText("1:" + String.format("%.3fV", value[1]));
        tv = (TextView) MainActivity.this.findViewById(R.id.textView3);
        tv.setText("2:" + String.format("%.3fV", value[2]));
        tv = (TextView) MainActivity.this.findViewById(R.id.textView4);
        tv.setText("3:" + String.format("%.3fV", value[3]));
    }

    public void dataMessageReceived(final TCPClient.Entry[] dataList) throws InterruptedException {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dataList.length > 0) {
                    float values[]=new float[8];
                    for(int i=0; i < dataList[dataList.length - 1].value.length; i++) {
                        values[i]=(float)MainActivity.this.valueToVolt(dataList[dataList.length - 1].value[i]);
                    }
                    MainActivity.this.repaintLegende(values);
                }
                /*
                String text="";
                for(TCPClient.Entry e : dataList) {
                    text+=e.time+": "+e.value+"\n";
                }
                tv.setText(text);
                */
                if(dataList.length > 0) {
                    GraphView graphView = (GraphView) MainActivity.this.findViewById(R.id.graph);
                    // graphView.values[0] = (float)MainActivity.this.valueToVolt(dataList[0].value);
                    for(int i=0; i < dataList.length; i++) {
                        int p=(i+GraphView.writePos) % GraphView.values.length;
                        float volts[]=new float[8];
                        for(int j=0; j< 8; j++) {
                            volts[j]=(float)MainActivity.this.valueToVolt(dataList[i].value[j]);
                        }
                        GraphView.values[p]=volts;
                        GraphView.times[p]=dataList[i].time;
                    }
                    GraphView.writePos+= dataList.length;
                    if(GraphView.writePos >= GraphView.values.length)
                        GraphView.writePos%=GraphView.values.length;
                    graphView.invalidate();
                }
            }
        });
        Thread.sleep(100, 0);
    }

    double valueToVolt(int value) {
        return (((double)value / 0x7fffff)*5);
    }
}
