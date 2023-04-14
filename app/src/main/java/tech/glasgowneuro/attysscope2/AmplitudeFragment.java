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
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
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

import com.androidplot.Series;
import com.androidplot.SeriesRegistry;
import com.androidplot.xy.BarFormatter;
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
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.androidplot.xy.RectRegion;
import com.androidplot.xy.XYRegionFormatter;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYSeriesRenderer;


import tech.glasgowneuro.attyscomm.AttysComm;
public class AmplitudeFragment extends Fragment {

    String TAG = "AmplitudeFragment";

    int channel = AttysComm.INDEX_Analogue_channel_1;
    final int nSampleBufferSize = 100;
    private static final String[] MAXYTXT = {
            "auto", "1E8", "1E7", "1E6", "1E5", "1E4", "500", "50", "20", "10", "5", "2", "1", "0.5", "0.1", "0.05",
            "0.01", "0.005", "0.001", "0.0005", "0.0001"};
    private static final String[] historyStatus = {
            "OFF",
            "ON"
    };

//    private static final String[] WINDOW_LENGTH = {"0.1 sec", "0.2 sec", "0.5 sec", "1 sec", "2 sec", "5 sec", "10 sec"};
//
//    private static final int DEFAULT_WINDOW_LENGTH = 3;
    int nEpoch = 0;
    final int maxEpoch = 50;
    int count = 0;
    int staticCount = 0;
    private int windowLength = 100;
    private SimpleXYSeries amplitudeHistorySeries;
    private SimpleXYSeries amplitudeHistorySeries2;
    private SimpleXYSeries amplitudeFullSeries = null;
    private SimpleXYSeries amplitudeFullSeries2 = null;
    float amplitudeHistorySeriesMaxY = 0;
    float previousEpochMaxY = 0;
    boolean isShown = false;
    int nData = 0;
    private XYPlot amplitudePlot = null;
    private SimpleXYSeries amplitudeStaticSeries;
    private SimpleXYSeries amplitudeStaticSeries2;
    private TextView amplitudeReadingText = null;
    float  leftMargin, rightMargin;

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
    int previousStep = 0;

    float current_stat_result = 0;
    float current_stat_result2 = 0;
    float[][] previousEpoch = new float [50][19];
    float[][] previousEpoch2 = new float [50][19];
    boolean ready = false;
    float[] staticData = new float[16];
    float[] staticData2 = new float [16];
    boolean acceptData = false;
    boolean isCleared = false;
    boolean isHighlighted = false;
    private String dataFilename = null;
    float[] last3Data = new float[3];
    float[] last3Data2 = new float[3];
    private final byte dataSeparator = AttysScope.DATA_SEPARATOR_TAB;

    public void setSamplingrate(int _samplingrate) {
        samplingRate = _samplingrate;
    }
    private boolean isStatic = false;

    public void reset() {
        ready = false;

        signalAnalysis = new SignalAnalysis(windowLength); // windowLength == 100
        for(int i =0; i< 16; i++){
            staticData[i] = 0;
            staticData2[i] = 0;
        }
        step = 0;
        step2 = 0;
        previousStep = 0;
        staticCount = 0;
        nData = 0;
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
            amplitudeStaticSeries = new SimpleXYSeries("");
            amplitudeStaticSeries2 = new SimpleXYSeries("");
        amplitudePlot.setRangeLabel(units[channel]);
        amplitudePlot.setTitle(" ");
        amplitudePlot.clear();
        amplitudePlot.setDomainBoundaries(null, null, BoundaryMode.AUTO);
        ready = true;
        amplitudeHistorySeriesMaxY = 0;
        previousEpochMaxY = 0;
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
            String path = dirPath + "/" + fileNme + "-" + format +".jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
            File imageFile = new File(path);
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
            int imageQuality = 100;
             bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, fileOutputStream);
        } catch (FileNotFoundException e) {
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

        Button startButton = view.findViewById(R.id.start_recording); // start button
        Spinner spinnerMaxY = view.findViewById(R.id.amplitude_maxy);
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, MAXYTXT); // zoom in/out spinner
        ToggleButton highlightToggleButton = view.findViewById(R.id.highlight_button);
        // highlight button
        Button screenshotButton = view.findViewById(R.id.take_screenshot);// take screenshot button
        Button saveButton = view.findViewById(R.id.save_history); // save button
        Button clearHistoryButton = view.findViewById(R.id.clear_history); // clear history button
        Spinner historySpinner = view.findViewById(R.id.show_history);
        ArrayAdapter<String> adapterHistoryStatus =
                new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        historyStatus);
        adapterHistoryStatus.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // set history spinner
        Spinner spinnerChannel = view.findViewById(R.id.amplitude_channel); // change channel spinner
        ArrayAdapter<String> adapterChannel =
                new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        AttysComm.CHANNEL_DESCRIPTION_SHORT);

        Paint paint = new Paint();
        paint.setColor(Color.argb(128, 0, 255, 0));
        amplitudePlot.getGraph().setDomainGridLinePaint(paint);
        amplitudePlot.getGraph().setRangeGridLinePaint(paint);

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
        amplitudePlot.setDomainLabel("t/s(ms)");
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
                acceptData = true;
                updatePlot(channel, isStatic);
                historySpinner.setSelection(0);
                highlightToggleButton.setChecked(false);
            }
        });

        highlightToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // handle state change
                if (isChecked) {
                    if (!isStatic){
                        Toast.makeText(getContext(), "Cannot highlight realtime plot", Toast.LENGTH_SHORT).show();
                    }
                    else{
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
                    EditText right_Margin = (EditText) dialogView.findViewById(R.id.right_margin);
                    Button ok_Button = dialogView.findViewById(R.id.OK_Button);
                    ok_Button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String userInput_left = left_Margin.getText().toString().trim();
                            String userInput_Right = right_Margin.getText().toString().trim();
                            if (!userInput_left.isEmpty() && !userInput_Right.isEmpty()) {
                                leftMargin = Integer.parseInt(userInput_left);
                                rightMargin = Integer.parseInt(userInput_Right);

//                            if(userInput_left.isEmpty() || userInput_Right.isEmpty()){
//                                Toast.makeText(getContext(), "Please type valid left and right margin values", Toast.LENGTH_SHORT).show();
//                            }
//                            else {
                            dialog.dismiss();
//                            highlightPlot(amplitudePlot,leftMargin,rightMargin,isHighlighted);
                                for (int i = 0; i < amplitudeHistorySeries.size(); i++) {
                                    double yValue = amplitudeHistorySeries.getY(i).doubleValue();
                                    if (yValue > amplitudeHistorySeriesMaxY) {
                                        amplitudeHistorySeriesMaxY = (float) yValue;
                                    }
                                }
                                for (int i = 0; i < 50; i++) {
                                    for (int j = 0; j < 19; j++) {
                                        double val = previousEpoch[i][j];
                                        if (val > previousEpochMaxY) {
                                            previousEpochMaxY = (float) val;
                                        }
                                    }
                                }
                                if (previousEpochMaxY >+ amplitudeHistorySeriesMaxY){
                                    amplitudeHistorySeriesMaxY = previousEpochMaxY;
                                }
                            SimpleXYSeries verticalLineSeries = new SimpleXYSeries(
                                    Arrays.asList(leftMargin, leftMargin),
                                    Arrays.asList(0, amplitudeHistorySeriesMaxY),
                                    "H_L"
                            );
                            SimpleXYSeries verticalLineSeries2 = new SimpleXYSeries(
                                        Arrays.asList(rightMargin, rightMargin),
                                        Arrays.asList(0, amplitudeHistorySeriesMaxY),
                                        "H_R"
                                );
                            amplitudePlot.addSeries(verticalLineSeries, new LineAndPointFormatter(Color.argb(128, 255, 255, 0), null, null, null));
                            amplitudePlot.addSeries(verticalLineSeries2, new LineAndPointFormatter(Color.argb(128, 255, 255, 0), null, null, null));

                            amplitudePlot.redraw();
                            isHighlighted = true;}
                            else{
                                Toast.makeText(getContext(), "Input valid left and right margin", Toast.LENGTH_SHORT).show();
                            }
                            }
                        //}
                    });
                    Button cancel_Button = dialogView.findViewById(R.id.Cancel_Button);
                    cancel_Button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                            }
                    }
                else {
                    if (isHighlighted){
                        SeriesRegistry seriesRegistry = amplitudePlot.getRegistry();
                        List<XYSeries> seriesList = seriesRegistry.getSeriesList();
                        for (XYSeries series : seriesList) {
                            String seriesTitle = series.getTitle();
                            if (seriesTitle != null && (seriesTitle.equals("H_L") || seriesTitle.equals("H_R"))) {
                                amplitudePlot.removeSeries(series);
                            }
                        }
                        amplitudePlot.redraw();

                    }
                    isHighlighted = false;
                }
            }
        });


        screenshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeScreenShot(getActivity().getWindow().getDecorView().getRootView(), "Screenshot");
                Toast.makeText(getContext(), "Screenshot taken!", Toast.LENGTH_SHORT).show();
            }
        });


        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEpoch(isStatic, amplitudeStaticSeries, previousEpoch, nEpoch);
                saveEpoch(isStatic, amplitudeStaticSeries2, previousEpoch2, nEpoch);
                nEpoch ++;
                count ++;
                isCleared = false;
                if(nEpoch == maxEpoch) {
                    nEpoch = 0; // populate the array from the beginning
                }
                historySpinner.setSelection(0);
            }
        });



        historySpinner.setAdapter(adapterHistoryStatus);

        historySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position == 0) {
                    if (isShown) {
                        amplitudePlot.clear();
                        updatePlot(channel, isStatic);
                        isShown = false;
                    }
                } else if (position == 1) {
                    addPreviousEpoch(count, channel,nEpoch, isCleared);// show history
                    isShown = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        historySpinner.setBackgroundResource(android.R.drawable.btn_default);
        historySpinner.setSelection(0); // By default, set it to OFF




        clearHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearPreviousEpoch();
                historySpinner.setSelection(0);
            }
        });


//        highlightButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//                LayoutInflater inflater = getActivity().getLayoutInflater();
//                View dialogView = inflater.inflate(R.layout.highlight_dialog, null);
//                builder.setView(dialogView);
//                final AlertDialog dialog = builder.create();
//                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
//                lp.copyFrom(dialog.getWindow().getAttributes());
//                lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
//                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//                dialog.show();
//                dialog.getWindow().setAttributes(lp);
//                EditText left_Margin = (EditText) dialogView.findViewById(R.id.left_margin);
//                String userInput_left = left_Margin.getText().toString().trim();
//                if (!userInput_left.isEmpty()) {
//                    leftMargin = Integer.parseInt(userInput_left);
//                }
//                EditText right_Margin = (EditText) dialogView.findViewById(R.id.right_margin);
//                String userInput_Right = right_Margin.getText().toString().trim();
//                if (!userInput_Right.isEmpty()) {
//                    rightMargin = Integer.parseInt(userInput_Right);
//                }
//

//            }
//        });

        

        adapterChannel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChannel.setAdapter(adapterChannel);
        spinnerChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                channel = position;
                historySpinner.setSelection(0);
                if(!isStatic) {
                    reset();
                }
                updatePlot(channel, isStatic);
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



        return view;

    }

    private void updatePlot(int channel, boolean isStatic) {
        if (channel == AttysComm.INDEX_Analogue_channel_1) {
            boolean alreadyAdded = ((AttysScope) requireActivity()).getAlreadyAdded();
            amplitudePlot.clear();
            if(isStatic){
                SimpleXYSeries ySeries = new SimpleXYSeries(new ArrayList<Number>(), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "");
                for (int i = 0; i < 19; i++) {
                    float xValue = -6 + i * (30.0f - (-6)) / 18; // Correspond X-axis to Y-axis
                    ySeries.addLast(xValue, amplitudeStaticSeries.getY(i));
                }
                amplitudePlot.addSeries(ySeries, new LineAndPointFormatter(
                        Color.GREEN, null, null, null));
            }
            else {
                amplitudePlot.addSeries(amplitudeHistorySeries,
                        new LineAndPointFormatter(
                                Color.rgb(100, 255, 255), null, null, null));
            }
        }
        else if (channel == AttysComm.INDEX_Analogue_channel_2) {
            amplitudePlot.clear();
            if(isStatic){
                SimpleXYSeries ySeries2 = new SimpleXYSeries(new ArrayList<Number>(), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "");
                for (int i = 0; i < 19; i++) {
                    float xValue = -6 + i * (30.0f - (-6)) / 18; // Correspond X-axis to Y-axis
                    ySeries2.addLast(xValue, amplitudeStaticSeries2.getY(i));
                }
                amplitudePlot.addSeries(ySeries2, new LineAndPointFormatter(
                        Color.RED, null, null, null));
            }
            else {
                amplitudePlot.addSeries(amplitudeHistorySeries2,
                        new LineAndPointFormatter(
                                Color.rgb(100, 255, 255), null, null, null));
            }
        }
        amplitudePlot.redraw();
    }

    public synchronized void addValue(final float[] sample) {
        if (!ready) return;
        if (!acceptData) return;
        if (sample[AttysComm.INDEX_GPIO0] != 0 && staticCount == 0){
            last3Data = signalAnalysis.getLast3Data();
            last3Data2 = signalAnalysis.getLast3Data2();
            isStatic =true;
                if (signalAnalysis != null) {
                    signalAnalysis.addData(sample[AttysComm.INDEX_Analogue_channel_1] * 200);
                    signalAnalysis.addData2(sample[AttysComm.INDEX_Analogue_channel_2] * 200);
                    staticData[staticCount] = signalAnalysis.getData();
                    staticData2[staticCount] = signalAnalysis.getData2();
                }
            staticCount ++;
        }
        if(isStatic){
            if(staticCount == 1){
                signalAnalysis.reset();
            }
            if (signalAnalysis != null){
            signalAnalysis.addData(sample[AttysComm.INDEX_Analogue_channel_1] * 200);
            signalAnalysis.addData2(sample[AttysComm.INDEX_Analogue_channel_2] * 200);
            staticData[staticCount] = signalAnalysis.getData();
            staticData2[staticCount] = signalAnalysis.getData2();
            staticCount ++;
            }
        }
        if(isStatic && staticCount == 15){
            if (signalAnalysis != null){
            signalAnalysis.addData(sample[AttysComm.INDEX_Analogue_channel_1] * 200);
            signalAnalysis.addData2(sample[AttysComm.INDEX_Analogue_channel_2] * 200);
            staticData[staticCount] = signalAnalysis.getData();
            staticData2[staticCount] = signalAnalysis.getData2();
            acceptData = false;
            }
            updateRawValue(channel, last3Data);
            return;
        }
        else {
            if (signalAnalysis != null) {
                signalAnalysis.addData(sample[AttysComm.INDEX_Analogue_channel_1] * 200);
                signalAnalysis.addData2(sample[AttysComm.INDEX_Analogue_channel_2] * 200);
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

    private void updateRawValue(int channel, float[] last3Data){
        if (isStatic) {
            amplitudePlot.clear();
            amplitudePlot.setDomainBoundaries(-6, 30, BoundaryMode.FIXED);
            int stepSize = 2;
            amplitudePlot.setDomainStep(StepMode.INCREMENT_BY_VAL, stepSize);
            SimpleXYSeries ySeries = new SimpleXYSeries(new ArrayList<Number>(), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "");
            SimpleXYSeries ySeries2 = new SimpleXYSeries(new ArrayList<Number>(), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,"");
            if(ySeries.size() > 0){
                for (int j = 0; j < ySeries.size(); j++){
                    ySeries.removeFirst();
                }
            }
            if (ySeries2.size() > 0){
                for (int i = 0; i < ySeries2.size(); i++){
                    ySeries2.removeFirst();
                }
            }
            for (int i = -6; i < 0; i = i + 2) {
                ySeries.addLast(i, last3Data[(i + 6) / 2]);
                ySeries2.addLast(i, last3Data[(i+6) / 2]);
            }
            for (int i = 0; i < 32; i = i + 2){
                ySeries.addLast(i, staticData[i / 2]);
                ySeries2.addLast(i, staticData2[i / 2]);
            }

            if(amplitudeStaticSeries != null && amplitudeStaticSeries.size() != 0 ){
                for (int i = 0; i < amplitudeStaticSeries.size(); i++){
                    amplitudeStaticSeries.removeFirst();
                }
            }
            if (amplitudeStaticSeries2 != null && amplitudeStaticSeries2.size() != 0){
                for (int i = 0; i < amplitudeStaticSeries2.size(); i++){
                    amplitudeStaticSeries2.removeFirst();
                }
            }

            for(int i = 0; i < ySeries.size(); i++){
                Number x = ySeries.getX(i);
                Number y = ySeries.getY(i);
                Number x2 = ySeries2.getX(i);
                Number y2 = ySeries2.getY(i);
                amplitudeStaticSeries.addLast(x, y);
                amplitudeStaticSeries2.addLast(x2,y2);
            }
            if (channel == AttysComm.INDEX_Analogue_channel_1){
                LineAndPointFormatter seriesFormat = new LineAndPointFormatter(Color.GREEN, null, null, null);
                amplitudePlot.addSeries(ySeries, seriesFormat);
                amplitudePlot.redraw();
            }
            else if (channel == AttysComm.INDEX_Analogue_channel_2){
                LineAndPointFormatter seriesFormat = new LineAndPointFormatter(Color.RED, null, null, null);
                amplitudePlot.addSeries(ySeries2, seriesFormat);
                amplitudePlot.redraw();
            }
        }
    }
    private void saveEpoch (boolean isStatic, SimpleXYSeries amplitudeHistorySeries, float[][] previousEpoch, int nEpoch){
        if (!isStatic){
            Log.v(TAG, "realtime plot cannot be saved!");
            return;
        }
        if (previousEpoch[0].length != amplitudeStaticSeries.size()) {
            throw new IllegalArgumentException("Array dimensions do not match");
        }
        else {
            for (int i = 0; i < previousEpoch[0].length; i++) {
                Number yValue = amplitudeStaticSeries.getY(i);
                previousEpoch[nEpoch][i] = yValue.floatValue();
            }
            Toast.makeText(getActivity(), "saved!", Toast.LENGTH_SHORT).show();
        }
    }
    private void addPreviousEpoch(int count, int channel, int nEpoch, boolean isCleared){
        SimpleXYSeries[] previousEpochSeries = new SimpleXYSeries[maxEpoch];
        SimpleXYSeries[] previousEpochSeries2 = new SimpleXYSeries[maxEpoch];
        int alpha = 128;// adjust the transparency of the plot
        if (isCleared){
            Toast.makeText(getContext(), "Previous data are cleared!", Toast.LENGTH_SHORT).show();
        }
        else{
            if(count < 50){ // when we don't have 50 epoch, plot all we have
                // if channel is ADC 1..
                if (channel == AttysComm.INDEX_Analogue_channel_1){
                    for (int i = 0; i < nEpoch; i++) {
                        // Convert the float array to a list of Float objects
                        List<Float> yValues = new ArrayList<Float>();
                        for (int j = 0; j < previousEpoch[i].length; j++) {
                            yValues.add(previousEpoch[i][j]);
                        }
                        // Create a new series with a length of 18
                        SimpleXYSeries series = new SimpleXYSeries(
                                Arrays.asList(-6, -4, -2, 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30), // x values
                                yValues, // y values
                                " " // series title
                        );
                        // Add the series to the array
                        if (series.size() == 19) {
                            previousEpochSeries[i] = series;
                            amplitudePlot.addSeries(previousEpochSeries[i],
                                    new LineAndPointFormatter(
                                            Color.argb(alpha, 0, 255, 0), null, null, null
                                    )
                            );
                            amplitudePlot.redraw();
                        }
                    }

                }
                // if channel is ADC2 then....
                else if (channel == AttysComm.INDEX_Analogue_channel_2){
                    for (int m = 0; m < nEpoch; m++) {
                        // Convert the float array to a list of Float objects
                        List<Float> yValues2 = new ArrayList<Float>();
                        for (int n = 0; n < previousEpoch2[m].length; n++) {
                            yValues2.add(previousEpoch2[m][n]);
                        }

                        // Create a new series with a length of 18
                        SimpleXYSeries series2 = new SimpleXYSeries(
                                Arrays.asList(-6, -4, -2, 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30), // x values
                                yValues2, // y values
                                " " // series title
                        );
                        // Add the series to the array
                        if (series2.size() == 19) {
                            previousEpochSeries2[m] = series2;
                            amplitudePlot.addSeries(previousEpochSeries2[m],
                                    new LineAndPointFormatter(
                                            Color.argb(alpha, 255, 0, 0), null, null, null
                                    )
                            );
                            amplitudePlot.redraw();
                        }
                    }

                }
            }
            else { // when count >50, plot previous 50 plots
                if (channel == AttysComm.INDEX_Analogue_channel_1){
                    for (int i = 0; i < maxEpoch; i++) {
                        // Convert the float array to a list of Float objects
                        List<Float> yValues = new ArrayList<Float>();
                        for (int j = 0; j < previousEpoch[i].length; j++) {
                            yValues.add(previousEpoch[i][j]);
                        }
                        // Create a new series with a length of 18
                        SimpleXYSeries series = new SimpleXYSeries(
                                Arrays.asList(-6, -4, -2, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30), // x values
                                yValues, // y values
                                " " // series title
                        );
                        // Add the series to the array
                        if (series.size() == 19) {
                            previousEpochSeries[i] = series;
                            amplitudePlot.addSeries(previousEpochSeries[i],
                                    new LineAndPointFormatter(
                                            Color.argb(alpha, 0, 255, 0), null, null, null
                                    )
                            );
                            amplitudePlot.redraw();
                        }
                    }
                }
                else if (channel == AttysComm.INDEX_Analogue_channel_2){
                    for (int m = 0; m < maxEpoch; m++) {
                        // Convert the float array to a list of Float objects
                        List<Float> yValues2 = new ArrayList<Float>();
                        for (int n = 0; n < previousEpoch2[m].length; n++) {
                            yValues2.add(previousEpoch2[m][n]);
                        }
                        // Create a new series with a length of 18
                        SimpleXYSeries series2 = new SimpleXYSeries(
                                Arrays.asList(-6, -4, -2, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30), // x values
                                yValues2, // y values
                                " " // series title
                        );
                        // Add the series to the array
                        if (series2.size() == 19) {
                            previousEpochSeries2[m] = series2;
                            amplitudePlot.addSeries(previousEpochSeries2[m],
                                    new LineAndPointFormatter(
                                            Color.argb(alpha, 255, 0, 0), null, null, null
                                    )
                            );
                            amplitudePlot.redraw();
                        }
                    }
                }

            }
        }
    }
    private void clearPreviousEpoch(){
        count = 0;
        nEpoch = 0;
        for (int i = 0; i < maxEpoch; i++ ){
            for(int j =0; j < 19; j ++){
                previousEpoch[i][j] = 0; // setting all the array element to 0
            }
        }
        isCleared = true;
    }
    public boolean isStatic(){
        return isStatic;
    }
}
