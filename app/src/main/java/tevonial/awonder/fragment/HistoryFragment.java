package tevonial.awonder.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;

import tevonial.awonder.MainActivity;
import tevonial.awonder.R;

public class HistoryFragment extends Fragment {
    private static SharedPreferences sPrefs;
    private static Gson sGson;
    private static Type sType;
    private static ArrayList<HistoryItem> sHistory;

    private HistoryAdapter mHistoryAdapter;
    private ListView mListView;

    private static String sHistoryKey = MainActivity.sContext.getString(R.string.pref_history_key);

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, null);
        MainActivity.sCurrentView = MainActivity.FRAGMENT_HISTORY;
        getActivity().setTitle(getString(R.string.history_fragment_title));

        mHistoryAdapter = new HistoryAdapter(getContext(), sHistory);
        mListView = (ListView) view.findViewById(R.id.history_list);

        mListView.setAdapter(mHistoryAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ResultsFragment.setPointer(position);
                MainActivity.switchView(-3);
            }
        });

        registerForContextMenu(mListView);

        return view;
    }

    public static void init() {
        sPrefs = MainActivity.sContext.getSharedPreferences(sHistoryKey, Context.MODE_PRIVATE);
        sType = new TypeToken<ArrayList<HistoryItem>>(){}.getType();
        sGson = new Gson();
        sHistory = getHistory();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        TextView header = new TextView(getContext());
        header.setTextSize(20);
        header.setPadding(25,25,25,25);
        header.setTextColor(Color.WHITE);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        String title = sHistory.get(((AdapterView.AdapterContextMenuInfo) menuInfo).position).getPoll();
        if (title.length() > 30) {
            header.setText("\"" + title.substring(0, 27) + "...\"");
        } else {
            header.setText("\"" + title + "\"");
        }

        menu.setHeaderView(header);
        menu.add(Menu.NONE, 0, Menu.NONE, "View");
        menu.add(Menu.NONE, 1, Menu.NONE, "Delete Item");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
        switch (item.getItemId()) {
            case 0:
                ResultsFragment.setPointer(position);
                MainActivity.switchView(-3);
                break;
            case 1:
                sHistory.remove(position);
                saveHistory();
                mHistoryAdapter.notifyDataSetChanged();
                Log.d("EW", "removed: " + position);
        }
        return true;
    }

    public static ArrayList<HistoryItem> getHistory() {
        if (sPrefs.contains(sHistoryKey)) {
            String json = sPrefs.getString(sHistoryKey, null);
            return sGson.fromJson(json, sType);
        } else {
            return new ArrayList<>();
        }
    }

    public static void saveHistory() {
        String json = sGson.toJson(sHistory, sType);
        SharedPreferences.Editor editor = sPrefs.edit();
        editor.putString(sHistoryKey, json);
        editor.apply();
    }

    public static HistoryItem getHistoryItem(int i) {
        return sHistory.get(i);
    }

    public static int addItem(HistoryItem item) {
        sHistory.add(item);
        saveHistory();
        return sHistory.size()-1;
    }

    public static class HistoryItem {
        private int mode;
        private String poll;
        private int sums[];
        private double avg;
        private int total;

        public HistoryItem(int mode, String poll, int sums[], double avg, int total) {
            this.mode = mode;
            this.poll = poll;
            this.sums = sums;
            this.avg = avg;
            this.total = total;
        }

        public int getMode() {
            return mode;
        }

        public String getPoll() {
            return poll;
        }

        public int[] getSums() {
            return sums;
        }

        public double getAvg() {
            return avg;
        }

        public int getTotal() {
            return total;
        }
    }

    public class HistoryAdapter extends ArrayAdapter<HistoryItem> {
        private Context context;
        private ArrayList<HistoryItem> items;
        private int num_icons[] = {R.drawable.numeric_0_box, R.drawable.numeric_1_box, R.drawable.numeric_2_box, R.drawable.numeric_3_box, R.drawable.numeric_4_box, R.drawable.numeric_5_box, R.drawable.numeric_6_box, R.drawable.numeric_7_box, R.drawable.numeric_8_box, R.drawable.numeric_9_box, R.drawable.numeric_9_box};
        private int delta_icons[] = {R.drawable.delta_down, R.drawable.delta};

        public HistoryAdapter(Context context, ArrayList<HistoryItem> items) {
            super(context, R.layout.row_history, items);
            this.context = context; this.items = items;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.row_history, parent, false);
            HistoryItem item = items.get(position);
            int mode = item.getMode();

            ((TextView) rowView.findViewById(R.id.text1)).setText(item.getPoll());

            if (mode == 1) {
                ((TextView) rowView.findViewById(R.id.text2)).setText((new DecimalFormat("#0.00")).format(item.getAvg()) + "/10");
                ((ImageView) rowView.findViewById(R.id.icon)).setImageResource(num_icons[(int)Math.round(item.getAvg())]);
            } else if (mode == 2) {
                ((TextView) rowView.findViewById(R.id.text2)).setText((new DecimalFormat("#0.0")).format(item.getAvg() * 100.0) + "%");
                int i = (int)(item.getAvg()+0.5); if (i > 1) {i = 1;}
                ((ImageView) rowView.findViewById(R.id.icon)).setImageResource(delta_icons[i]);
            }

            ((TextView) rowView.findViewById(R.id.text3)).setText(item.getTotal() + " responses");

            return rowView;
        }
    }
}
