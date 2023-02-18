package com.altimeter.bdureau.bearconsole.Flight.FlightView;
/**
 * @description: This will display altimeter flight curves
 * @author: boris.dureau@neuf.fr
 **/
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;


public class FlightViewMpFragment extends Fragment {
    private LineChart mChart;
    public XYSeriesCollection allFlightData;
    private ConsoleApplication myBT;
    int graphBackColor, fontSize, axisColor, labelColor, nbrColor;
    private ArrayList<ILineDataSet> dataSets;

    private String units[] = null;
    String curvesNames[];
    boolean checkedItems[];
    static int colors[] = {Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED,
            Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW};

    public FlightViewMpFragment(XYSeriesCollection pAllFlightData,
                                ConsoleApplication pBT,
                                String pCurvesNames[],
                                boolean pCheckedItems[],
                                String pUnits[]) {
        this.allFlightData = pAllFlightData;
        this.myBT = pBT;
        this.curvesNames = pCurvesNames;
        this.checkedItems = pCheckedItems;
        this.units = pUnits;
        //this.dataSets = pDataSets;
    }

    public void setCheckedItems(boolean[] checkedItems) {
        this.checkedItems = checkedItems;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dataSets = new ArrayList<>();

        View view = inflater.inflate(R.layout.fragment_flight_view_mp, container, false);

        mChart = (LineChart) view.findViewById(R.id.linechart);

        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
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
    }

    public void drawAllCurves(XYSeriesCollection allFlightData) {
        dataSets = new ArrayList<>();
        dataSets.clear();
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

                int nbrData = allFlightData.getSeries(i).getItemCount();

                ArrayList<Entry> yValues = new ArrayList<>();

                for (int k = 0; k < nbrData; k++) {
                    yValues.add(new Entry(allFlightData.getSeries(i).getX(k).floatValue()*timeFactor, allFlightData.getSeries(i).getY(k).floatValue()));
                }

                LineDataSet set1 = new LineDataSet(yValues, getResources().getString(R.string.flight_time));
                set1.setColor(colors[i]);

                set1.setDrawValues(false);
                set1.setDrawCircles(false);
                set1.setLabel(curvesNames[i] + " " + units[i]);
                set1.setValueTextColor(labelColor);

                set1.setValueTextSize(fontSize);
                dataSets.add(set1);
            }
        }

        LineData data = new LineData(dataSets);

        mChart.clear();

        mChart.setData(data);
        mChart.setBackgroundColor(graphBackColor);
        Description desc = new Description();
        //time (ms)
        desc.setText(graphTimeUnits);
        mChart.setDescription(desc);
    }
}