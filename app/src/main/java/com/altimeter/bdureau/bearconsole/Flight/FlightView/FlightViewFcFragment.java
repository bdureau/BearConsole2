package com.altimeter.bdureau.bearconsole.Flight.FlightView;
/**
 * @description: This will display altimeter flight curves using the freechart library
 * @author: boris.dureau@neuf.fr
 **/
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;
import org.afree.chart.AFreeChart;
import org.afree.chart.ChartFactory;
import org.afree.chart.axis.NumberAxis;
import org.afree.chart.axis.ValueAxis;
import org.afree.chart.plot.PlotOrientation;
import org.afree.chart.plot.XYPlot;
import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.SolidColor;
import org.afree.graphics.geom.Font;



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
    static int colors[] = {Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED,
            Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW};

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.tabflight_view_fc_fragment, container, false);
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
    }
}