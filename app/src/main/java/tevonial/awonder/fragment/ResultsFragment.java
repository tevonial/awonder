package tevonial.awonder.fragment;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.DecimalFormat;
import java.util.ArrayList;

import tevonial.awonder.MainActivity;
import tevonial.awonder.R;
import tevonial.awonder.fragment.HistoryFragment.HistoryItem;

public class ResultsFragment extends Fragment {
    private TextView mPollView;
    private RelativeLayout mPieView;
    
    private PieChart mPieChart;
    private ArrayList<Entry> mEntries;
    private ArrayList<String> mLabels;

    private double mAvg; private int mTotal, mMode;
    private String mPoll;

    private static int sPtr = 0;
    private static boolean sDoAnimate = true;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_results, null);
        MainActivity.sCurrentView = MainActivity.FRAGMENT_RESULTS;
        getActivity().setTitle(getString(R.string.results_fragment_title));

        mPieChart = (PieChart) view.findViewById(R.id.chart);
        mPollView = (TextView) view.findViewById(R.id.poll);
        mPieView = (RelativeLayout) view.findViewById(R.id.pie_container);

        mEntries = new ArrayList<>();
        mLabels = new ArrayList<>();

        (new PrepareChartTask()).execute();

        if (sDoAnimate) {
            mPieChart.animateY(1000);
        }

        return view;
    }

    public static void setPointer(int ptr) {
        ResultsFragment.sPtr = ptr;
        ResultsFragment.sDoAnimate = true;
    }

    private class PrepareChartTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... v) {
            HistoryItem item = HistoryFragment.getHistoryItem(sPtr);
            sDoAnimate = false;

            int sums[] = item.getSums();
            mMode = item.getMode();
            mAvg = item.getAvg();
            mTotal = item.getTotal();
            mPoll = item.getPoll();

            if (mMode == 1) {
                for (int i = 0; i < sums.length; i++) {
                    if (sums[i] > 0) {
                        mEntries.add(new Entry((float) sums[i], i));
                        mLabels.add(String.valueOf(i));
                    }
                }
            } else if (mMode == 2) {
                mEntries.add(new Entry((float) sums[0], 0));
                if (sums[0] > 0) {
                    mLabels.add(getString(R.string.no));
                } else {
                    mLabels.add("");
                }
                mEntries.add(new Entry((float) sums[1], 1));
                if (sums[1] > 0) {
                    mLabels.add(getString(R.string.yes));
                } else {
                    mLabels.add("");
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            MainActivity.sLoading.setVisibility(View.INVISIBLE);

            PieDataSet dataset = new PieDataSet(mEntries, "");
            dataset.setColors(ColorTemplate.PASTEL_COLORS);
            dataset.setDrawValues(false);
            PieData data = new PieData(mLabels, dataset);
            data.setValueTextSize(18);
            data.setValueTypeface(Typeface.DEFAULT_BOLD);

            if (mMode == 1) {
                mPieChart.setCenterText((new DecimalFormat("#0.00")).format(mAvg) + "/10");
            } else if (mMode == 2) {
                mPieChart.setCenterText((new DecimalFormat("#0.0")).format(mAvg * 100.0) + "%");
            }
            mPieChart.setCenterTextSize(30);
            mPieChart.setDescription(String.valueOf(mTotal) + " Responses");
            mPieChart.setDescriptionTextSize(15);
            mPieChart.getLegend().setEnabled(false);
            mPieChart.setData(data);

            mPollView.setText(mPoll);
            mPieView.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPreExecute() {
            MainActivity.sLoading.setVisibility(ProgressBar.VISIBLE);
            mAvg = 0;
        }

        @Override
        protected void onProgressUpdate(Void... v) {}
    }

}
