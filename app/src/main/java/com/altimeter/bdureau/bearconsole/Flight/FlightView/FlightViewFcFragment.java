package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: FlightViewFcFragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  First tab of the flight view when the AFreeChart library is used (Android 7): draws the
 *  curves selected by the user (altitude, temperature, pressure, speed, acceleration, battery,
 *  GPS...) of the displayed flight, each with its own axis and unit.
 *  The flight events (burnout, apogee, main deployment, landing) detected on the
 *  altitude curve (see FlightEvents) are drawn as labelled vertical dashed markers on the
 *  time axis. They can be hidden from the menu of the flight screen (setShowEvents()).
 * ============================================================================
 */

import androidx.annotation.NonNull;
import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightViewTabActivity;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.GlobalConfig;
import org.afree.chart.AFreeChart;
import org.afree.chart.ChartFactory;
import org.afree.chart.axis.NumberAxis;
import org.afree.chart.axis.ValueAxis;
import org.afree.chart.plot.PlotOrientation;
import org.afree.chart.plot.ValueMarker;
import org.afree.chart.plot.XYPlot;
import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.SolidColor;
import org.afree.graphics.geom.Font;
import org.afree.ui.Layer;
import org.afree.ui.RectangleAnchor;
import org.afree.ui.TextAnchor;

import java.util.List;


public class FlightViewFcFragment extends Fragment {
    private ChartView chartView;
    private AFreeChart mChart = null;
    private XYPlot plot;
    public XYSeriesCollection allFlightData;
    private ConsoleApplication myBT;
    int graphBackColor, fontSize, axisColor, labelColor, nbrColor;

    private String units[] = null;
    String curvesNames[];
    boolean checkedItems[];

    // Flight events drawn on the curves (computed once per flight, see FlightEvents)
    private boolean showEvents = true;
    private List<FlightEvents.Event> events = null;
    private XYSeriesCollection eventsSource = null;
    static int colors[] = {Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED,
            Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW};

    /** Required by the framework to re-create the fragment (configuration change, process restore). */
    public FlightViewFcFragment() {
        super();
    }

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

    public FlightViewFcFragment(XYSeriesCollection pAllFlightData,
                                ConsoleApplication pBT,
                                String pCurvesNames[],
                                boolean pCheckedItems[],
                                String pUnits[]) {
        this.allFlightData = pAllFlightData;
        this.myBT = pBT;
        this.curvesNames = pCurvesNames;
        this.checkedItems = pCheckedItems;
        this.units = pUnits;

    }

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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        restoreFromHost();
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_flight_view_fc, container, false);
        chartView = (ChartView) view.findViewById(R.id.chartView1);

        mChart = ChartFactory.createXYLineChart(
                "",
                getResources().getString(R.string.Time_fv),
                "",
                null,
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips?
                false                     // URLs?
        );
        chartView.setChart(mChart);
        drawGraph();
        drawAllCurves(allFlightData);

        return view;
    }

    public void drawGraph() {
        graphBackColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphBackColor());
        fontSize = myBT.getAppConf().ConvertFont(myBT.getAppConf().getFontSize());
        axisColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphColor());
        labelColor = Color.BLACK;
        nbrColor = Color.BLACK;
        //font
        Font font = new Font("Dialog", Typeface.NORMAL,fontSize);
        mChart.getTitle().setFont(font);
        // set the background color for the chart...
        mChart.setBackgroundPaintType(new SolidColor(graphBackColor));
        // get a reference to the plot for further customisation...
        plot = mChart.getXYPlot();
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setBackgroundPaintType(new SolidColor(graphBackColor));
        plot.setOutlinePaintType(new SolidColor(Color.YELLOW));
        plot.setDomainZeroBaselinePaintType(new SolidColor(Color.GREEN));
        plot.setRangeZeroBaselinePaintType(new SolidColor(Color.MAGENTA));
        final ValueAxis Xaxis = plot.getDomainAxis();
        Xaxis.setAutoRange(true);
        Xaxis.setAxisLinePaintType(new SolidColor(axisColor));

        final ValueAxis YAxis = plot.getRangeAxis();
        YAxis.setAxisLinePaintType(new SolidColor(axisColor));

        Xaxis.setTickLabelFont(font);
        Xaxis.setLabelFont(font);

        YAxis.setTickLabelFont(font);
        YAxis.setLabelFont(font);

        //X axis label color
        Xaxis.setLabelPaintType(new SolidColor(labelColor));
        Xaxis.setTickMarkPaintType(new SolidColor(axisColor));
        Xaxis.setTickLabelPaintType(new SolidColor(nbrColor));
        //Y axis label color
        YAxis.setLabelPaintType(new SolidColor(labelColor));
        YAxis.setTickLabelPaintType(new SolidColor(nbrColor));
        final NumberAxis rangeAxis2 = new NumberAxis("Range Axis 2");
        rangeAxis2.setAutoRangeIncludesZero(false);
    }

    public void drawAllCurves(XYSeriesCollection allFlightData) {
        XYSeriesCollection flightData;
        flightData = new XYSeriesCollection();
        String graphTimeUnits = getResources().getString(R.string.unit_time);
        int nbrOfItem = allFlightData.getSeries(0).getItemCount();
        float maxTime = allFlightData.getSeries(0).getX(nbrOfItem-1).floatValue();

        float timeFactor = 1;
        if(maxTime > 10000){
            graphTimeUnits = getString(R.string.unit_time_seconde);
            timeFactor =0.001f;
        }

        if(maxTime > 600000){
            graphTimeUnits = getString(R.string.unit_time_minutes);
            timeFactor =0.001f/60;
        }

        for (int i = 0; i < curvesNames.length; i++) {
            Log.d("drawAllCurves", "i:" + i);
            Log.d("drawAllCurves", "curvesNames:" + curvesNames[i]);

            if (checkedItems[i]) {
                Log.d("drawAllCurves", "curve checked" + curvesNames[i]);
                flightData.addSeries(allFlightData.getSeries(i));
            }
        }
        plot.setDataset(0, flightData);
        addFlightEvents(allFlightData);
    }

    /**
     * Draws the flight events as dashed vertical markers of the domain (time) axis, in the
     * foreground so that the labels stay readable. The X values of the series are the raw
     * milliseconds of the recording, so the markers are placed at the event time in ms.
     * The labels alternate between two heights so that close events do not overlap.
     */
    private void addFlightEvents(XYSeriesCollection flightData) {
        plot.clearDomainMarkers();
        if (!showEvents || flightData == null || flightData.getSeriesCount() == 0) {
            return;
        }
        boolean isMeters = myBT.getAppConf().getUnits() == GlobalConfig.AltitudeUnit.METERS;
        if (events == null || eventsSource != flightData) {
            events = FlightEvents.compute(flightData.getSeries(0), isMeters);
            eventsSource = flightData;
        }
        String altUnit = isMeters ? "m" : "ft";
        Font labelFont = new Font("Dialog", Typeface.NORMAL, Math.max(10, fontSize - 4));
        // Labels of the events in the right part of the chart are written on the left of
        // their line, otherwise they would be cut by the edge of the chart
        XYSeries alt = flightData.getSeries(0);
        double xMiddle = alt.getMinX() + 0.6 * (alt.getMaxX() - alt.getMinX());
        for (int i = 0; i < events.size(); i++) {
            FlightEvents.Event ev = events.get(i);
            ValueMarker marker = new ValueMarker(ev.timeMs);
            marker.setPaintType(new SolidColor(ev.type.color));
            marker.setStroke(2f);
            marker.setEffect(new DashPathEffect(new float[]{12f, 8f}, 0f));
            marker.setLabel(ev.label(requireContext(), altUnit));
            marker.setLabelFont(labelFont);
            marker.setLabelPaintType(new SolidColor(ev.type.color));
            boolean onLeft = ev.timeMs > xMiddle;
            boolean top = (i % 2 == 0);
            if (top) {
                marker.setLabelAnchor(onLeft ? RectangleAnchor.TOP_LEFT : RectangleAnchor.TOP_RIGHT);
                marker.setLabelTextAnchor(onLeft ? TextAnchor.TOP_RIGHT : TextAnchor.TOP_LEFT);
            } else {
                marker.setLabelAnchor(onLeft ? RectangleAnchor.BOTTOM_LEFT : RectangleAnchor.BOTTOM_RIGHT);
                marker.setLabelTextAnchor(onLeft ? TextAnchor.BOTTOM_RIGHT : TextAnchor.BOTTOM_LEFT);
            }
            plot.addDomainMarker(marker, Layer.FOREGROUND);
        }
    }
}