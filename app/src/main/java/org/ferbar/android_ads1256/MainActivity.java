package org.ferbar.android_ads1256;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements TCPClient.OnMessageReceived {

    TCPClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.client.stopClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.client=new TCPClient(this);
        this.client.start();
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

            /*
            case R.id.action_favorite:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                return true;
*/
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

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

   @Override
    public void dataMessageReceived(final TCPClient.Entry[] dataList) throws InterruptedException {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dataList.length > 0) {
                    TextView tv = (TextView) MainActivity.this.findViewById(R.id.textView1);
                    tv.setText("0:" + String.format("%.3fV", MainActivity.this.valueToVolt(dataList[dataList.length - 1].value[0])));
                    tv = (TextView) MainActivity.this.findViewById(R.id.textView2);
                    tv.setText("1:" + String.format("%.3fV", MainActivity.this.valueToVolt(dataList[dataList.length - 1].value[1])));
                    tv = (TextView) MainActivity.this.findViewById(R.id.textView3);
                    tv.setText("2:" + String.format("%.3fV", MainActivity.this.valueToVolt(dataList[dataList.length - 1].value[2])));
                    tv = (TextView) MainActivity.this.findViewById(R.id.textView4);
                    tv.setText("3:" + String.format("%.3fV", MainActivity.this.valueToVolt(dataList[dataList.length - 1].value[3])));
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
                        int p=(i+graphView.writePos) % graphView.values.length;
                        float volts[]=new float[8];
                        for(int j=0; j< 8; j++) {
                            volts[j]=(float)MainActivity.this.valueToVolt(dataList[i].value[j]);
                        }
                        graphView.values[p]=volts;
                        graphView.times[p]=dataList[i].time;
                    }
                    graphView.writePos+= dataList.length;
                    if(graphView.writePos >= graphView.values.length)
                        graphView.writePos%=graphView.values.length;
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
