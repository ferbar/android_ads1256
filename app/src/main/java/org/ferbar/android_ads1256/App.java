package org.ferbar.android_ads1256;

import android.app.Application;

/**
 * Created by chris on 12.02.17.
 */
public class App extends Application {
    public final String TAG="Application";

    TCPClient client;

    TCPClient getClient() {
        if(this.client==null) {
            this.client=new TCPClient();
        }
        return this.client;
    }

}
