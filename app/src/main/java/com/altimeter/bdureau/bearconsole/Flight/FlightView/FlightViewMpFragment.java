package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Flight Computer Management
 * Class: FlightViewMpFragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Fragment displaying rocket telemetry curves (altitude, acceleration,
 *  velocity, battery voltage, etc.) using the MPAndroidChart library.
 *  Optimized to handle high-frequency flight datasets efficiently by:
 *    - Pre-allocating entry collections to minimize GC pauses and memory churn.
 *    - Disabling expensive per-point circle and value rendering during display.
 *    - Handling adaptive time units (ms, seconds, minutes) with time scaling factors.
 *    - Providing a zero-argument default constructor for safe Fragment recreation.
 *  The flight events (burnout, apogee, main deployment, landing) detected on the
 *  altitude curve (see FlightEvents) are drawn as labelled vertical dashed lines, and as
 *  colored dots on the altitude curve when it is displayed. They can be hidden from the
 *  menu of the flight screen (setShowEvents()).
 * ============================================================================
 */

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightViewTabActivity;
import com.altimeter.bdureau.bearconsole.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.altimeter.bdureau.bearconsole.config.GlobalConfig;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.List;

public class FlightViewMpFragment extends Fragment {
    private LineChart mChart;
    public XYSeriesCollection allFlightData;
    private ConsoleApplication myBT;

    private int graphBackColor;
    private int fontSize;
    private int axisColor;
    private int labelColor;
    private int nbrColor;

    private String[] units = null;
    private String[] curvesNames = null;
    private boolean[] checkedItems = null;

    // Flight events drawn on the curves (computed once per flight, see FlightEvents)
    private boolean showEvents = true;
    private List<FlightEvents.Event> events = null;
    private XYSeriesCollection eventsSource = null;

    // Palette used for styling distinct telemetry curves
    private static final int[] COLORS = {
            Color.RED, Color.BLUE, Color.BLACK, Color.GREEN, Color.CYAN, Color.GRAY,
            Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK, Color.GREEN,
            Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE,
            Color.BLACK, Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW
    };

    /**
     * Default constructor required by the Android framework for state restoration.
     */
    public FlightViewMpFragment() {
        super();
    }

    /**
     * Parameterized constructor initializing datasets and user preferences.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // After a re-creation by the framework the dependencies were not given by a
        // constructor: the application object is always available from the context
        if (myBT == null) {
            myBT = (ConsoleApplication) context.getApplicationContext();
        }
    }

    /**
     * After a re-creation by the framework the flight data were not given by a constructor:
     * they are taken again from the host FlightViewTabActivity. Called from onCreateView(),
     * when the activity has finished its own onCreate().
     */
    private void restoreFromHost() {
        if (!(getActivity() instanceof FlightViewTabActivity)) return;
        FlightViewTabActivity host = (FlightViewTabActivity) getActivity();
        if (allFlightData == null) allFlightData = host.getAllFlightData();
        if (curvesNames == null) curvesNames = host.getCurvesNames();
        if (checkedItems == null) checkedItems = host.getCheckedItems();
        if (units == null) units = host.getUnits();
    }

    public FlightViewMpFragment(XYSeriesCollection pAllFlightData,
                                ConsoleApplication pBT,
                                String[] pCurvesNames,
                                boolean[] pCheckedItems,
                                String[] pUnits) {
        this.allFlightData = pAllFlightData;
        this.myBT = pBT;
        this.curvesNames = pCurvesNames;
        this.checkedItems = pCheckedItems;
        this.units = pUnits;
    }

    /**
     * Updates which curves should be rendered.
     */
    public void setCheckedItems(boolean[] checkedItems) {
        this.checkedItems = checkedItems;
    }

    /**
     * Shows or hides the flight events. The caller redraws the curves with drawAllCurves().
     */
    public void setShowEvents(boolean show) {
        this.showEvents = show;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        restoreFromHost();
        View view = inflater.inflate(R.layout.fragment_flight_view_mp, container, false);
        mChart = view.findViewById(R.id.linechart);

        // Hardware accelerated gestures and interaction settings
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setPinchZoom(true);
        mChart.setDrawGridBackground(false);

        // Apply visual configuration and render initial series
        drawGraph();
        if (allFlightData != null) {
            drawAllCurves(allFlightData);
        }

        return view;
    }

    /**
     * Reads application preferences and applies visual styling to chart axes and legends.
     */
    public void drawGraph() {
        if (myBT == null || myBT.getAppConf() == null) {
            return;
        }

        graphBackColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphBackColor());
        fontSize = myBT.getAppConf().ConvertFont(myBT.getAppConf().getFontSize());
        axisColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphColor());
        labelColor = Color.BLACK;
        nbrColor = Color.BLACK;

        if (mChart != null) {
            mChart.setBackgroundColor(graphBackColor);

            // Configure X Axis styling
            XAxis xAxis = mChart.getXAxis();
            xAxis.setTextColor(nbrColor);
            xAxis.setAxisLineColor(axisColor);
            xAxis.setTextSize(Math.max(8f, fontSize - 2f));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            // Configure Left Y Axis styling
            YAxis leftAxis = mChart.getAxisLeft();
            leftAxis.setTextColor(nbrColor);
            leftAxis.setAxisLineColor(axisColor);
            leftAxis.setTextSize(Math.max(8f, fontSize - 2f));

            // Disable Right Y Axis to save rendering overhead
            YAxis rightAxis = mChart.getAxisRight();
            rightAxis.setEnabled(false);

            // Configure Legend styling
            Legend legend = mChart.getLegend();
            legend.setTextColor(labelColor);
            legend.setTextSize(Math.max(8f, fontSize - 2f));
            legend.setWordWrapEnabled(true);
        }
    }

    /**
     * Filters active flight curves, adjusts time scaling, and plots points.
     */
    public void drawAllCurves(XYSeriesCollection allFlightData) {
        if (mChart == null || allFlightData == null || allFlightData.getSeriesCount() == 0 || curvesNames == null) {
            return;
        }

        String graphTimeUnits = getString(R.string.unit_time);
        XYSeries primarySeries = allFlightData.getSeries(0);
        int totalFirstItems = primarySeries.getItemCount();

        // Calculate appropriate time unit scaling based on total flight duration
        float timeFactor = 1.0f;
        if (totalFirstItems > 0) {
            float maxTime = primarySeries.getX(totalFirstItems - 1).floatValue();
            if (maxTime > 600000) {
                graphTimeUnits = getString(R.string.unit_time_minutes);
                timeFactor = 0.001f / 60.0f;
            } else if (maxTime > 10000) {
                graphTimeUnits = getString(R.string.unit_time_seconde);
                timeFactor = 0.001f;
            }
        }

        // Pre-allocate container for selected datasets
        ArrayList<ILineDataSet> dataSets = new ArrayList<>(curvesNames.length);

        for (int i = 0; i < curvesNames.length; i++) {
            if (checkedItems != null && i < checkedItems.length && checkedItems[i]) {
                XYSeries series = allFlightData.getSeries(i);
                int itemCount = series.getItemCount();

                // Exact pre-allocation prevents repeated ArrayList array resizing
                ArrayList<Entry> yValues = new ArrayList<>(itemCount);

                for (int k = 0; k < itemCount; k++) {
                    float xVal = series.getX(k).floatValue() * timeFactor;
                    float yVal = series.getY(k).floatValue();
                    yValues.add(new Entry(xVal, yVal));
                }

                String curveLabel = curvesNames[i] + (units != null && i < units.length ? " " + units[i] : "");
                LineDataSet dataSet = new LineDataSet(yValues, curveLabel);

                int colorIndex = i % COLORS.length;
                dataSet.setColor(COLORS[colorIndex]);
                dataSet.setLineWidth(1.8f);

                // Disable expensive markers and text to maintain high rendering framerates
                dataSet.setDrawCircles(false);
                dataSet.setDrawValues(false);
                dataSet.setDrawIcons(false);

                dataSets.add(dataSet);
            }
        }

        // Flight events: vertical lines on the time axis, dots on the altitude curve
        addFlightEvents(allFlightData, dataSets, timeFactor);

        // Bind data or clear if no curves are selected
        if (!dataSets.isEmpty()) {
            LineData data = new LineData(dataSets);
            mChart.setData(data);
        } else {
            mChart.clear();
        }

        // Update chart description label
        Description desc = new Description();
        desc.setText(graphTimeUnits);
        desc.setTextColor(labelColor);
        mChart.setDescription(desc);

        // Request direct view invalidation/redraw
        mChart.invalidate();
    }

    /**
     * Draws the flight events: one labelled dashed vertical line per event (limit lines of the
     * X axis, drawn behind the curves) and, when the altitude curve (series 0) is displayed,
     * a dot of the event color on that curve. The labels alternate between the top and the
     * bottom of the chart so that close events (burnout and apogee of a short flight) do not overlap.
     */
    private void addFlightEvents(XYSeriesCollection flightData, ArrayList<ILineDataSet> dataSets,
                                 float timeFactor) {
        XAxis xAxis = mChart.getXAxis();
        xAxis.removeAllLimitLines();
        if (!showEvents || myBT == null || myBT.getAppConf() == null) {
            return;
        }
        boolean isMeters = myBT.getAppConf().getUnits() == GlobalConfig.AltitudeUnit.METERS;
        if (events == null || eventsSource != flightData) {
            events = FlightEvents.compute(flightData.getSeries(0), isMeters);
            eventsSource = flightData;
        }
        if (events.isEmpty()) {
            return;
        }

        String altUnit = isMeters ? "m" : "ft";
        float textSize = Math.max(8f, fontSize - 4f);
        ArrayList<Entry> dots = new ArrayList<>(events.size());
        ArrayList<Integer> dotColors = new ArrayList<>(events.size());
        for (int i = 0; i < events.size(); i++) {
            FlightEvents.Event ev = events.get(i);
            float x = (float) (ev.timeMs * timeFactor);
            LimitLine line = new LimitLine(x, ev.label(requireContext(), altUnit));
            line.setLineColor(ev.type.color);
            line.setLineWidth(1.2f);
            line.enableDashedLine(12f, 8f, 0f);
            line.setTextColor(ev.type.color);
            line.setTextSize(textSize);
            line.setLabelPosition((i % 2 == 0) ? LimitLine.LimitLabelPosition.RIGHT_TOP
                    : LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
            xAxis.addLimitLine(line);

            dots.add(new Entry(x, (float) ev.altitude));
            dotColors.add(ev.type.color);
        }
        xAxis.setDrawLimitLinesBehindData(true);

        // Dots only make sense on the altitude curve: same Y scale
        boolean altitudeShown = checkedItems != null && checkedItems.length > 0 && checkedItems[0];
        if (altitudeShown) {
            LineDataSet eventSet = new LineDataSet(dots, "");
            eventSet.setColor(Color.TRANSPARENT);
            eventSet.setLineWidth(0f);
            eventSet.setCircleColors(dotColors);
            eventSet.setCircleRadius(5f);
            eventSet.setCircleHoleRadius(2.5f);
            eventSet.setCircleHoleColor(Color.WHITE);
            eventSet.setDrawCircleHole(true);
            eventSet.setDrawValues(false);
            eventSet.setHighlightEnabled(false);
            // No legend entry for the dots: the lines carry the event names
            eventSet.setForm(Legend.LegendForm.NONE);
            dataSets.add(eventSet);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Release chart references to prevent memory leaks
        if (mChart != null) {
            mChart.clear();
            mChart = null;
        }
    }
}