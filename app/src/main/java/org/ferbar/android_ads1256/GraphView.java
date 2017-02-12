package org.ferbar.android_ads1256;

/**
 * Created by chris on 08.02.17.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


public class GraphView extends View {

    public final static String TAG="GraphView";

    public static boolean BAR = true;
    public static boolean LINE = false;

    private Paint paint;
    public static float values[][] = new float[1000][];
    public static double times[] = new double[1000];
    /*
    private String[] horlabels;
    private String[] verlabels;
    private String title="";
    */
    private boolean type;
    private float max=5;
    public static int writePos=0;
    private int[] graphColors = {Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.DKGRAY, Color.CYAN, Color.BLACK, Color.MAGENTA};

    public GraphView(Context context, AttributeSet attrs) /*, float[] values, String title, String[] horlabels, String[] verlabels, boolean type) */ {
        super(context, attrs);

        /*
        if (title == null)
            title = "test";
        else
            this.title = title;
        */
        /*
        if (horlabels == null) {
            // this.horlabels = new String[0];
            this.horlabels = new String[]{"today", "tomorrow", "next week", "next month"};
        } else {
            this.horlabels = horlabels;
        }
        if (verlabels == null) {
            // this.verlabels = new String[0];
            this.verlabels = new String[] { "great", "ok", "bad" };
        } else {
            this.verlabels = verlabels;
        }
        */
        this.type = LINE;
        paint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float border = 20;
        float horstart = border * 2;
        float height = getHeight();
        float width = getWidth() - 1;
        float max = getMax();
        float min = getMin();
        /*
        double xmin=this.getMinTime();
        double xmax=this.getMaxTime();
        */
        float diff = max - min;
        float graphheight = height - (2 * border);
        float graphwidth = width - (2 * border);

        paint.setTextAlign(Align.LEFT);
        /*
        int vers = verlabels.length - 1;
        for (int i = 0; i < verlabels.length; i++) {
            paint.setColor(Color.DKGRAY);
            float y = ((graphheight / vers) * i) + border;
            canvas.drawLine(horstart, y, width, y, paint);
            paint.setColor(Color.BLACK);
            canvas.drawText(verlabels[i], 0, y, paint);
        }*/

        paint.setColor(Color.DKGRAY);
        float y = graphheight + border;
        canvas.drawLine(horstart, y, width, y, paint);
        paint.setColor(Color.BLACK);
        canvas.drawText(String.format("%.1f", min), 0, y, paint);
        y = 0 + border;
        paint.setColor(Color.DKGRAY);
        canvas.drawLine(horstart, y, width, y, paint);
        paint.setColor(Color.BLACK);
        canvas.drawText(String.format("%.1fV", max), 0, y, paint);


        paint.setColor(Color.DKGRAY);
        float x = 0 + horstart;
        canvas.drawLine(x, height - border, x, border, paint);
        paint.setTextAlign(Align.LEFT);
        paint.setColor(Color.BLACK);
        canvas.drawText("0", x, height - 4, paint);

        paint.setColor(Color.DKGRAY);
        x = graphwidth + horstart;
        canvas.drawLine(x, height - border, x, border, paint);
        paint.setTextAlign(Align.RIGHT);
        paint.setColor(Color.BLACK);
        double duration=GraphView.times[(this.writePos+GraphView.times.length-1)%GraphView.times.length] - GraphView.times[(this.writePos+GraphView.times.length)%GraphView.times.length];
        // Log.d(TAG, "last:" + String.format("%.2f",this.times[this.times.length - 1]) + " first:" + String.format("%.2f", this.times[0]));
        canvas.drawText(String.format("%.1fs", duration), x, height - 4, paint);

        /*
        int hors = horlabels.length - 1;
        for (int i = 0; i < horlabels.length; i++) {
            paint.setColor(Color.DKGRAY);
            float x = ((graphwidth / hors) * i) + horstart;
            canvas.drawLine(x, height - border, x, border, paint);
            paint.setTextAlign(Align.CENTER);
            if (i == horlabels.length - 1)
                paint.setTextAlign(Align.RIGHT);
            if (i == 0)
                paint.setTextAlign(Align.LEFT);
            paint.setColor(Color.BLACK);
            canvas.drawText(horlabels[i], x, height - 4, paint);
        }
        */

        paint.setColor(Color.RED);
        x = ((graphwidth / GraphView.values.length) * this.writePos) + horstart;
        canvas.drawLine(x, height - border, x, border, paint);

        /*
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Align.CENTER);
        canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);
        */

        if (max != min) {
            paint.setColor(Color.LTGRAY);
            if (type == BAR) {
                float datalength = values.length;
                float colwidth = (width - (2 * border)) / datalength;
                for (int i = 0; i < values.length; i++) {
                    float val = values[i][0] - min;
                    float rat = val / diff;
                    float h = graphheight * rat;
                    canvas.drawRect((i * colwidth) + horstart, (border - h) + graphheight, ((i * colwidth) + horstart) + (colwidth - 1), height - (border - 1), paint);
                }
            } else {
                float datalength = values.length;
                float colwidth = (width - (2 * border)) / datalength;
                float halfcol = colwidth / 2;
                float lasth[] = new float[8];
                for (int i = 0; i < values.length; i++) {
                    if(values[i]==null) continue;
                    for(int j=0; j < 8; j++) {
                        float val = values[i][j] - min;
                        float rat = val / diff;
                        float h = graphheight * rat;
                        if (i > 0) {
                            paint.setColor(this.graphColors[j]);
                            canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasth[j]) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - h) + graphheight, paint);
                        }
                        lasth[j] = h;
                    }
                }
            }
        }
    }

    public void setMax(float max) {
        this.max=max;
    }
    
    private float getMax() {
        return this.max;
        /*
        float largest = Integer.MIN_VALUE;
        for (int i = 0; i < values.length; i++)
            if (values[i] > largest)
                largest = values[i];
        return largest;
        */
    }

    private float getMin() {
        return 0;
        /*
        float smallest = Integer.MAX_VALUE;
        for (int i = 0; i < values.length; i++)
            if (values[i] < smallest)
                smallest = values[i];
        return smallest;
        /*
        double log=Math.log10(smallest);
        int precision=(int)Math.floor(log)*-1;
        double exp=Math.pow(10, precision);
        return (float) (Math.floor(smallest*exp)/exp);
        */
    }

    private double getMaxTime() {
        double largest = Integer.MIN_VALUE;
        for (int i = 0; i < GraphView.times.length; i++)
            if (GraphView.times[i] > largest)
                largest = GraphView.times[i];
        return largest;

    }

    private double getMinTime() {
        double smallest = Integer.MAX_VALUE;
        for (int i = 0; i < GraphView.times.length; i++)
            if (GraphView.times[i] < smallest)
                smallest = GraphView.times[i];
        return smallest;
    }

}