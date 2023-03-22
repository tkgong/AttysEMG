package tech.glasgowneuro.attysscope2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.RectRegion;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XValueMarker;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.androidplot.xy.RectRegion;


import tech.glasgowneuro.attyscomm.AttysComm;
public class AmplitudeFragment extends Fragment {

    String TAG = "AmplitudeFragment";

    int channel = AttysComm.INDEX_Analogue_channel_1;
    final int nSampleBufferSize = 100;
    private static final String[] MAXYTXT = {
            "auto range", "1E8", "1E7", "1E6", "1E5", "1E4", "500", "50", "20", "10", "5", "2", "1", "0.5", "0.1", "0.05",
            "0.01", "0.005", "0.001", "0.0005", "0.0001"};

//    private static final String[] WINDOW_LENGTH = {"0.1 sec", "0.2 sec", "0.5 sec", "1 sec", "2 sec", "5 sec", "10 sec"};
//
//    private static final int DEFAULT_WINDOW_LENGTH = 3;
    int nEpoch = 0;
    final int maxEpoch = 50;
    int count = 50;
    private int windowLength = 100;

    private SimpleXYSeries amplitudeHistorySeries;
    private SimpleXYSeries amplitudeHistorySeries2;
    private SimpleXYSeries amplitudeFullSeries = null;
    private SimpleXYSeries amplitudeFullSeries2 = null;

    private XYPlot amplitudePlot = null;

    private TextView amplitudeReadingText = null;
    int leftMargin, rightMargin;

    private SignalAnalysis signalAnalysis = null;

    private final String[] units = new String[AttysComm.NCHANNELS];
    public void setUnits(String [] _units) {
        System.arraycopy(_units, 0, units, 0, AttysComm.NCHANNELS);
    }

    View view = null;

    int samplingRate = 500; // SR = 250HZ, need to modify it for EMG recording

    /* The EMG frequency ranges vary from 0.01 Hz to 10 kHz, depending on the type of examination (invasive or noninvasive).
    The most useful and important frequency ranges are within the range from 50 to 150 Hz */

    int step = 0;
    int step2 = 0;

    float current_stat_result = 0;
    float current_stat_result2 = 0;
    float[][] previousEpoch = new float [50][18];
    float[][] previousEpoch2 = new float [50][18];
    boolean ready = false;

    boolean acceptData = false;

    private String dataFilename = null;

    private final byte dataSeparator = AttysScope.DATA_SEPARATOR_TAB;

    public void setSamplingrate(int _samplingrate) {
        samplingRate = _samplingrate;
    }
    private boolean isStatic = false;

    public void reset() {
        ready = false;

        signalAnalysis = new SignalAnalysis(windowLength); // windowLength == 100

        step = 0;
        step2 = 0;

        int n = amplitudeHistorySeries.size();
        for (int i = 0; i < n; i++) {
            amplitudeHistorySeries.removeLast();
        }
        int m = amplitudeHistorySeries2.size();
        for (int j = 0; j < m; j ++){
            amplitudeHistorySeries2.removeLast();
        }
        isStatic = false;
        amplitudeFullSeries = new SimpleXYSeries(" ");
            amplitudeHistorySeries.setTitle(units[channel] + " pp");
        amplitudeFullSeries2 = new SimpleXYSeries(" ");
            amplitudeHistorySeries2.setTitle(units[channel] + "pp");
        amplitudePlot.setRangeLabel(units[channel]);
        amplitudePlot.setTitle(" ");

        amplitudePlot.redraw();

        ready = true;
    }
    protected static File takeScreenShot (View view, String fileNme){
        Date date = new Date();
        CharSequence format = DateFormat.format("yyyy-MM-dd_hh:mm:ss", date);
        try {
            String dirPath = Environment.getExternalStorageDirectory().toString() + "/Screenshot";
            File fileDir = new File(dirPath);
            if(!fileDir.exists()){
                boolean mkdir = fileDir.mkdir();
            }
            String path = dirPath + "/" + fileNme + "-" + format +"jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);

            File imageFile = new File(path);
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
            int imageQuality = 100;
             bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, fileOutputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
//
//        if (isStatic){
//            Toast.makeText(getContext(), "is Static is 1", Toast.LENGTH_SHORT).show();
//        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "creating Fragment");
        }

        if (container == null) {
            return null;
        }

        signalAnalysis = new SignalAnalysis(samplingRate);

        view = inflater.inflate(R.layout.amplitudefragment, container, false);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);

        //ask for file permission


        // setup the APR Levels plot:
        amplitudePlot = view.findViewById(R.id.amplitude_PlotView);
        amplitudeHistorySeries = new SimpleXYSeries(" ");
      amplitudeHistorySeries2 = new SimpleXYSeries(" ");
        //amplitudeReadingText = view.findViewById(R.id.amplitude_valueTextView);
        //amplitudeReadingText.setText(String.format(Locale.US,"%04d", 0));
//        ToggleButton toggleButtonDoRecord = view.findViewById(R.id.amplitude_doRecord);
//        toggleButtonDoRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                acceptData = isChecked;
//            }
//        });
//        toggleButtonDoRecord.setChecked(true);

        Button startButton = view.findViewById(R.id.start_recording);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
                acceptData = true;
            }
        });


//        Button screenshotButton = view.findViewById(R.id.take_screenshot);
//        screenshotButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                takeScreenShot(getActivity().getWindow().getDecorView().getRootView(), "Screenshot");
//            }
//        });
//        Button resetButton = view.findViewById(R.id.amplitude_Reset);
//        resetButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                reset();
//            }
//
//        });
        Button saveButton = view.findViewById(R.id.save_history);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEpoch(isStatic, amplitudeHistorySeries, previousEpoch, nEpoch);
                saveEpoch(isStatic, amplitudeHistorySeries2, previousEpoch2, nEpoch);
                nEpoch ++;
                count ++;
                if(nEpoch == maxEpoch) {
                    nEpoch = 0; // populate the array from the beginning
                }
            }
        });

        Button showHistoryButton = view.findViewById(R.id.show_history);
        showHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPreviousEpoch(count, channel);
            }
        });


        Button highlightButton = view.findViewById(R.id.highlight);
        highlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.highlight_dialog, null);
                builder.setView(dialogView);
                final AlertDialog dialog = builder.create();
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(dialog.getWindow().getAttributes());
                lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                dialog.show();
                dialog.getWindow().setAttributes(lp);
                EditText left_Margin = (EditText) dialogView.findViewById(R.id.left_margin);
                String userInput_left = left_Margin.getText().toString().trim();
                if (!userInput_left.isEmpty()) {
                    leftMargin = Integer.parseInt(userInput_left);
                }
                EditText right_Margin = (EditText) dialogView.findViewById(R.id.right_margin);
                String userInput_Right = right_Margin.getText().toString().trim();
                if (!userInput_Right.isEmpty()) {
                    rightMargin = Integer.parseInt(userInput_Right);
                }

                Button ok_Button = dialogView.findViewById(R.id.OK_Button);
                ok_Button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        highlightPlot(amplitudePlot, leftMargin, rightMargin);
                        dialog.dismiss();
                    }
                });
                Button cancel_Button = dialogView.findViewById(R.id.Cancel_Button);
                cancel_Button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }
        });

        Spinner spinnerChannel = view.findViewById(R.id.amplitude_channel);
//        String[] channelName = {"ADC_1", "ADC_2", "ADC_1+ADC_2"};
//        ArrayAdapter<String> adapterChannel =
//                new ArrayAdapter<>(requireContext(),
//                android.R.layout.simple_spinner_dropdown_item,
//                channelName);
        ArrayAdapter<String> adapterChannel =
                new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        AttysComm.CHANNEL_DESCRIPTION_SHORT);
        adapterChannel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChannel.setAdapter(adapterChannel);
        spinnerChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                channel = position;
                if(!isStatic) {
                    reset();
                }
                updatePlot(channel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinnerChannel.setBackgroundResource(android.R.drawable.btn_default);
        spinnerChannel.setSelection(AttysComm.INDEX_Analogue_channel_1);

        float winLen = 0.1f;
        windowLength = (int) (winLen * samplingRate);
        if (amplitudePlot != null) {
            amplitudePlot.setDomainStep(StepMode.INCREMENT_BY_VAL, 20 * winLen);
        }
//        Spinner spinnerHistory = view.findViewById(R.id.view_history);
//        ArrayAdapter<String> adapterHistory = new ArrayAdapter<>(getContext(),
//                android.R.layout.simple_spinner_dropdown_item,
//                PREVIOUS_EPOCHES);
//        adapterHistory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerHistory.setPrompt("HISTORY");
//        spinnerHistory.setAdapter(adapterHistory);
//        spinnerHistory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                String item = parent.getItemAtPosition(position).toString();
//                if(item.equals("No Previous Epoch")){
//                    Toast.makeText(parent.getContext(), "No Previous Epoch Displayed", Toast.LENGTH_SHORT).show();
//                }
//                else if (item.equals("All Previous Epoch")){
//                    Toast.makeText(parent.getContext(),"Display All Previous Data Stored", Toast.LENGTH_SHORT).show();
//                }
//                else {
//                    try {
//                        int nEpoch = Integer.parseInt(PREVIOUS_EPOCHES[position].split(" ")[0].substring(0, 2));
//                        Toast.makeText(parent.getContext(),"Display Previous" + " " + nEpoch + " " + "Epoch", Toast.LENGTH_SHORT).show();
//                    } catch (NumberFormatException e) {
//                        // Handle the exception here, for example by displaying an error message or using a default value
//                        Toast.makeText(getContext(), "The string cannot be parsed to a valid integer.", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Do nothing
//            }
//        });
//        spinnerHistory.setBackgroundResource(android.R.drawable.btn_default);
//        spinnerHistory.setSelection(0);

        setPlot();

        Spinner spinnerMaxY = view.findViewById(R.id.amplitude_maxy);
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, MAXYTXT);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMaxY.setAdapter(adapter1);
        spinnerMaxY.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    amplitudePlot.setRangeUpperBoundary(1, BoundaryMode.AUTO);
                    amplitudePlot.setRangeLowerBoundary(0, BoundaryMode.AUTO);
                } else {
                    Screensize screensize = new Screensize(getContext());
                    amplitudePlot.setRangeLowerBoundary(0, BoundaryMode.FIXED);
                    if (screensize.isTablet()) {
                        amplitudePlot.setRangeStep(StepMode.INCREMENT_BY_VAL, Float.parseFloat(MAXYTXT[position]) / 10);
                    } else {
                        amplitudePlot.setRangeStep(StepMode.INCREMENT_BY_VAL, Float.parseFloat(MAXYTXT[position]) / 10);
                    }
                    amplitudePlot.setRangeUpperBoundary(Float.parseFloat(MAXYTXT[position]), BoundaryMode.FIXED);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinnerMaxY.setBackgroundResource(android.R.drawable.btn_default);
        spinnerMaxY.setSelection(0);

        reset();

        return view;

    }
    private void setPlot() {
        Paint paint = new Paint();
        paint.setColor(Color.argb(128, 0, 255, 0));
        amplitudePlot.getGraph().setDomainGridLinePaint(paint);
        amplitudePlot.getGraph().setRangeGridLinePaint(paint);

        amplitudePlot.setDomainLabel("t/sec");
        amplitudePlot.setRangeLabel(" ");

        amplitudePlot.setRangeLowerBoundary(0, BoundaryMode.FIXED);
        amplitudePlot.setRangeUpperBoundary(1, BoundaryMode.AUTO);

        XYGraphWidget.LineLabelRenderer lineLabelRendererY = new XYGraphWidget.LineLabelRenderer() {
            @Override
            public void drawLabel(Canvas canvas,
                                  XYGraphWidget.LineLabelStyle style,
                                  Number val, float x, float y, boolean isOrigin) {
                Rect bounds = new Rect();
                style.getPaint().getTextBounds("a", 0, 1, bounds);
                drawLabel(canvas, String.format(Locale.US,"%04.5f ", val.floatValue()),
                        style.getPaint(), x + (float)bounds.width() / 2, y + bounds.height(), isOrigin);
            }
        };

        amplitudePlot.getGraph().setLineLabelRenderer(XYGraphWidget.Edge.LEFT, lineLabelRendererY);
        XYGraphWidget.LineLabelStyle lineLabelStyle = amplitudePlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT);
        Rect bounds = new Rect();
        String dummyTxt = String.format(Locale.US,"%04.5f ", 100000.000599);
        lineLabelStyle.getPaint().getTextBounds(dummyTxt, 0, dummyTxt.length(), bounds);
        amplitudePlot.getGraph().setMarginLeft(bounds.width());

        XYGraphWidget.LineLabelRenderer lineLabelRendererX = new XYGraphWidget.LineLabelRenderer() {
            @Override
            public void drawLabel(Canvas canvas,
                                  XYGraphWidget.LineLabelStyle style,
                                  Number val, float x, float y, boolean isOrigin) {
                if (!isOrigin) {
                    Rect bounds = new Rect();
                    style.getPaint().getTextBounds("a", 0, 1, bounds);
                    drawLabel(canvas, String.format(Locale.US,"%d", val.intValue()),
                            style.getPaint(), x + (float)bounds.width() / 2, y + bounds.height(), isOrigin);
                }
            }
        };

        amplitudePlot.getGraph().setLineLabelRenderer(XYGraphWidget.Edge.BOTTOM, lineLabelRendererX);
    }

    private void updatePlot(int channel) {
        if (channel == AttysComm.INDEX_Analogue_channel_1) {
            amplitudePlot.clear();
            amplitudePlot.addSeries(amplitudeHistorySeries,
                    new LineAndPointFormatter(
                            Color.rgb(100, 255, 255), null, null, null));
        } else if (channel == AttysComm.INDEX_Analogue_channel_2) {
            amplitudePlot.clear();
            amplitudePlot.addSeries(amplitudeHistorySeries2,
                    new LineAndPointFormatter(
                            Color.rgb(255, 100, 255), null, null, null));
        }
        amplitudePlot.redraw();
    }
//    private void setPlot() {
////        if(channel == AttysComm.INDEX_Analogue_channel_1){
////            Toast.makeText(getContext(), "channel is " + channel, Toast.LENGTH_SHORT).show();
//            amplitudePlot.addSeries(amplitudeHistorySeries,
//                new LineAndPointFormatter(
//                        Color.rgb(100, 255, 255), null, null, null));
////        else if (channel == AttysComm.INDEX_Analogue_channel_2){
////            Toast.makeText(getContext(), "channel is " + channel, Toast.LENGTH_SHORT).show();
////            amplitudePlot.addSeries(amplitudeHistorySeries2,
////                    new LineAndPointFormatter(
////                            Color.rgb(255, 100, 255), null, null, null));
////        }
////        else{
////            Toast.makeText(getContext(), "Are We in This Condition?" , Toast.LENGTH_SHORT).show();
////        }
//
//        Paint paint = new Paint();
//        paint.setColor(Color.argb(128, 0, 255, 0));
//        amplitudePlot.getGraph().setDomainGridLinePaint(paint);
//        amplitudePlot.getGraph().setRangeGridLinePaint(paint);
//
//        amplitudePlot.setDomainLabel("t/sec");
//        amplitudePlot.setRangeLabel(" ");
//
//        amplitudePlot.setRangeLowerBoundary(0, BoundaryMode.FIXED);
//        amplitudePlot.setRangeUpperBoundary(1, BoundaryMode.AUTO);
//
//        XYGraphWidget.LineLabelRenderer lineLabelRendererY = new XYGraphWidget.LineLabelRenderer() {
//            @Override
//            public void drawLabel(Canvas canvas,
//                                  XYGraphWidget.LineLabelStyle style,
//                                  Number val, float x, float y, boolean isOrigin) {
//                Rect bounds = new Rect();
//                style.getPaint().getTextBounds("a", 0, 1, bounds);
//                drawLabel(canvas, String.format(Locale.US,"%04.5f ", val.floatValue()),
//                        style.getPaint(), x + (float)bounds.width() / 2, y + bounds.height(), isOrigin);
//            }
//        };
//
//        amplitudePlot.getGraph().setLineLabelRenderer(XYGraphWidget.Edge.LEFT, lineLabelRendererY);
//        XYGraphWidget.LineLabelStyle lineLabelStyle = amplitudePlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT);
//        Rect bounds = new Rect();
//        String dummyTxt = String.format(Locale.US,"%04.5f ", 100000.000599);
//        lineLabelStyle.getPaint().getTextBounds(dummyTxt, 0, dummyTxt.length(), bounds);
//        amplitudePlot.getGraph().setMarginLeft(bounds.width());
//
//        XYGraphWidget.LineLabelRenderer lineLabelRendererX = new XYGraphWidget.LineLabelRenderer() {
//            @Override
//            public void drawLabel(Canvas canvas,
//                                  XYGraphWidget.LineLabelStyle style,
//                                  Number val, float x, float y, boolean isOrigin) {
//                if (!isOrigin) {
//                    Rect bounds = new Rect();
//                    style.getPaint().getTextBounds("a", 0, 1, bounds);
//                    drawLabel(canvas, String.format(Locale.US,"%d", val.intValue()),
//                            style.getPaint(), x + (float)bounds.width() / 2, y + bounds.height(), isOrigin);
//                }
//            }
//        };
//
//        amplitudePlot.getGraph().setLineLabelRenderer(XYGraphWidget.Edge.BOTTOM, lineLabelRendererX);
//
//    }

    public synchronized void addValue(final float[] sample) {
        if (!ready) return;
        if (!acceptData) return;
        if (sample[AttysComm.INDEX_GPIO0] != 0){
            for (int i = 0; i < 15; i++) { // continue to add 15 samples after the trigger
                if (signalAnalysis != null) {
                    signalAnalysis.addData(sample[AttysComm.INDEX_Analogue_channel_1]);
                    signalAnalysis.addData2(sample[AttysComm.INDEX_Analogue_channel_2]);
                    if (signalAnalysis.bufferFull()) {
                        updateStats();
                        signalAnalysis.reset();
                    }
                    if (signalAnalysis.bufferFull2()){
                        updateStats2();
                        signalAnalysis.reset2();
                    }
                }
            }
            acceptData = false; //after 15 samples, stop adding samples anymore
            isStatic = true; // set the flag to true
            updateStats();
            updateStats2();// update amplitudeHistorySeries and keep only last 18 samples using this function
        } else {
            if (signalAnalysis != null) {
                signalAnalysis.addData(sample[AttysComm.INDEX_Analogue_channel_1]);
                signalAnalysis.addData2(sample[AttysComm.INDEX_Analogue_channel_2]);
                if (signalAnalysis.bufferFull()) {
                    updateStats();
                    updateStats2();
                    signalAnalysis.reset();
                    signalAnalysis.reset2();
                }
            }
        }
    }
    private void updateStats() {

        double delta_t = (double) windowLength * (1.0 / samplingRate);

        if (signalAnalysis != null) {
            current_stat_result = signalAnalysis.getPeakToPeak(); // peak-to-peak value of the signal -- wanted
        }
        //}

        if (amplitudeHistorySeries == null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "amplitudeHistorySeries == null");
            }
            return;
        }

        // get rid the oldest sample in history:
        if (amplitudeHistorySeries.size() > nSampleBufferSize) {
            amplitudeHistorySeries.removeFirst();
        }

        int n = nSampleBufferSize - amplitudeHistorySeries.size();
        for (int i = 0; i < n; i++) {
            // add the latest history sample:
            amplitudeHistorySeries.addLast(step * delta_t, current_stat_result);
            step++;
        }

        // add the latest history sample:
        amplitudeHistorySeries.addLast(step * delta_t, current_stat_result);
        amplitudeFullSeries.addLast(step * delta_t, current_stat_result);
        step++;

        if (isStatic) {
            int nToRemove = amplitudeHistorySeries.size() - 18;
            for (int i = 0; i < nToRemove; i++) {
                amplitudeHistorySeries.removeFirst();
            }
        } // keeps last 18 samples only for static plot
        amplitudePlot.redraw();



        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (amplitudeReadingText != null) {
                        amplitudeReadingText.setText(String.format(Locale.US,"%1.05f %s pp", current_stat_result, units[AttysComm.INDEX_Analogue_channel_1]));

                    }
                }
            });
        }
    }

    private void updateStats2() {

        double delta_t = (double) windowLength * (1.0 / samplingRate);

        if (signalAnalysis != null) {
            current_stat_result2 = signalAnalysis.getPeakToPeak2(); // peak-to-peak value of the signal -- wanted
        }
        //}

        if (amplitudeHistorySeries2 == null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "amplitudeHistorySeries2 == null");
            }
            return;
        }

        // get rid the oldest sample in history:
        if (amplitudeHistorySeries2.size() > nSampleBufferSize) {
            amplitudeHistorySeries2.removeFirst();
        }

        int n = nSampleBufferSize - amplitudeHistorySeries2.size();
        for (int i = 0; i < n; i++) {
            // add the latest history sample:
            amplitudeHistorySeries2.addLast(step2 * delta_t, current_stat_result2);
            step2++;
        }

        // add the latest history sample:
        amplitudeHistorySeries2.addLast(step2 * delta_t, current_stat_result2);
        amplitudeFullSeries2.addLast(step2 * delta_t, current_stat_result2);
        step2 ++;

        if (isStatic) {
            int nToRemove = amplitudeHistorySeries2.size() - 18;
            for (int i = 0; i < nToRemove; i++) {
                amplitudeHistorySeries2.removeFirst();
            }
        } // keeps last 18 samples only for static plot

        amplitudePlot.redraw();



        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (amplitudeReadingText != null) {
                        amplitudeReadingText.setText(String.format(Locale.US,"%1.05f %s pp", current_stat_result, units[AttysComm.INDEX_Analogue_channel_2]));

                    }
                }
            });
        }
    }

    private void saveEpoch (boolean isStatic, SimpleXYSeries amplitudeHistorySeries, float[][] previousEpoch, int nEpoch){
        if (!isStatic){
            Log.v(TAG, "realtime plot cannot be saved!");
            return;
        }

        if (previousEpoch[0].length != amplitudeHistorySeries.size()) {
            throw new IllegalArgumentException("Array dimensions do not match");
        }
        else {
            for (int i = 0; i < previousEpoch[0].length; i++) {
                Number yValue = amplitudeHistorySeries.getY(i);
                previousEpoch[nEpoch][i] = yValue.floatValue();
            }
            Toast.makeText(getActivity(), "saved!", Toast.LENGTH_SHORT).show();
        }


    }
    private void highlightPlot(XYPlot plot, int leftMargin, int rightMargin, boolean isStatic) {

        if (!isStatic){
            throw new IllegalArgumentException("CANNOT ANNOTATE THE REALTIME PLOT");
        }


        // Define the colors for the gradient
        int[] colors = {Color.YELLOW, Color.YELLOW};
        // Define the positions for the gradient
        float[] positions = {(float) leftMargin / plot.getWidth(), (float) rightMargin / plot.getWidth()};

        // Create the paint object with the linear gradient
        Paint bgPaint = new Paint();
        bgPaint.setShader(new LinearGradient(0, 0, 0, plot.getHeight(), colors, positions, Shader.TileMode.CLAMP));

        // Set the background paint of the graph widget to the linear gradient paint
        plot.getGraph().setBackgroundPaint(bgPaint);
    }
    private void addPreviousEpoch(int count, int channel){
        SimpleXYSeries[] previousSeries = new SimpleXYSeries[50];
        if(count < 50){ // when we don't have 50 epoch, plot all we have
            if (channel == AttysComm.INDEX_Analogue_channel_1){ // add the arrays from channel 1

            }
            else if (channel == AttysComm.INDEX_Analogue_channel_2){ // add the arrays from channel 2

            }
        }
        else { // when count >50, plot previous 50 plots
            if (channel == AttysComm.INDEX_Analogue_channel_1){

            }
            else if (channel == AttysComm.INDEX_Analogue_channel_2){

            }

        }

    }

}
