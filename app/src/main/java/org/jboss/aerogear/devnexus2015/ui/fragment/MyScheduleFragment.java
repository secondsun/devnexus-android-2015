package org.jboss.aerogear.devnexus2015.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.devnexus.util.GsonUtils;
import org.devnexus.vo.Presentation;
import org.devnexus.vo.ScheduleItem;
import org.devnexus.vo.UserCalendar;
import org.devnexus.vo.contract.UserCalendarContract;
import org.jboss.aerogear.devnexus2015.MainActivity;
import org.jboss.aerogear.devnexus2015.R;
import org.jboss.aerogear.devnexus2015.ui.adapter.MyScheduleViewAdapter;
import org.jboss.aerogear.devnexus2015.util.AddSessionClickListener;
import org.jboss.aerogear.devnexus2015.util.SessionClickListener;
import org.jboss.aerogear.devnexus2015.util.SessionPickerReceiver;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.devnexus.vo.contract.UserCalendarContract.DATE;

/**
 * Created by summers on 1/7/15.
 */
public class MyScheduleFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SessionClickListener, AddSessionClickListener, SessionPickerReceiver {

    private static final String TAG = MyScheduleFragment.class.getSimpleName();
    private static final int SCHEDULE_LOADER = 0x0100;
    private static final String DATE_KEY = "Schedule.dateKey";
    private RecyclerView recycler;
    private ContentResolver resolver;
    private Toolbar toolbar;
    private Spinner spinner;

    private ContentObserver userCalendarObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {

            Bundle args = new Bundle();
            args.putInt(DATE_KEY, spinner.getSelectedItemPosition());
            getLoaderManager().restartLoader(SCHEDULE_LOADER, args, MyScheduleFragment.this);
        }
    };
    private SessionPickerFragment pickerFragment;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_schedule, null);
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle("");
        ((MainActivity) getActivity()).attachToolbar(toolbar);
        recycler = (RecyclerView) view.findViewById(R.id.my_recycler_view);
        recycler.setLayoutManager(new GridLayoutManager(getActivity(), 1));
        resolver = getActivity().getContentResolver();
        recycler.setAdapter(new MyScheduleViewAdapter(new ArrayList<UserCalendar>(1), getActivity(), this, this));

        spinner = (Spinner) toolbar.findViewById(R.id.spinner_nav);
        loadSpinnerNav(spinner);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getContentResolver().registerContentObserver(UserCalendarContract.URI, true, userCalendarObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(userCalendarObserver);
    }

    private void loadSpinnerNav(final Spinner spinner) {
        spinner.setAdapter(new CalendarDateAdapter(getActivity()));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                
                Bundle args = new Bundle();
                args.putInt(DATE_KEY, position);
                getLoaderManager().restartLoader(SCHEDULE_LOADER, args, MyScheduleFragment.this);
                

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (args == null) {
            args = Bundle.EMPTY;
        }
        int dateIndex = args.getInt(DATE_KEY, 0);

        return new CursorLoader(getActivity(), UserCalendarContract.URI, null, DATE, new String[]{"" +dateIndex}, null);
        
        
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if ( data.getCount() == 0 ) {
            
        } else {
            List<UserCalendar> calendarItems = new ArrayList<>(data.getCount());
            while (data.moveToNext()) {
                UserCalendar calendarItem = GsonUtils.GSON.fromJson(data.getString(0), UserCalendar.class);
                calendarItems.add(calendarItem);
            }
            Collections.sort(calendarItems);
            refreshData(calendarItems);
        }
    }

    private void refreshData(List<UserCalendar> calendarItems) {
        GridLayoutManager gridLayoutManager = (GridLayoutManager) recycler.getLayoutManager();
        int index = gridLayoutManager.findFirstVisibleItemPosition(); 
        View v = gridLayoutManager.getChildAt(0); 
        
        recycler.setAdapter(new MyScheduleViewAdapter(new ArrayList<UserCalendar>(calendarItems), getActivity(), this, this));
        gridLayoutManager.scrollToPosition(index);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void loadSession(Presentation presentation) {
        Fragment sessionDetailFragment = SessionDetailFragment.newInstance(presentation.title, presentation.id);
        ((MainActivity)getActivity()).switchFragment(sessionDetailFragment, MainActivity.BackStackOperation.ADD, "SessionDetailFragment");
    }

    @Override
    public void showPicker(UserCalendar userCalendar) {

        pickerFragment = SessionPickerFragment.newInstance(userCalendar);
        pickerFragment.setReceiver(this);
        pickerFragment.show(getFragmentManager(), "SessionPicker");
    }

    @Override
    public void receiveSessionItem(UserCalendar calendarItem, ScheduleItem session) {
        calendarItem.item = session;
        resolver.update(UserCalendarContract.URI, UserCalendarContract.valueize(calendarItem, true), UserCalendarContract.ID, new String[]{calendarItem.getId() + ""} );
        if (pickerFragment != null) {
            pickerFragment.dismiss();
            pickerFragment = null;
        }
    }

    private class CalendarDateAdapter extends BaseAdapter implements SpinnerAdapter {

        

        private final DateFormat FORMAT = new SimpleDateFormat("MMMM dd");

        public CalendarDateAdapter(Activity activity) {
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Object getItem(int position) {
            return UserCalendarContract.DATES.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.textview_dropdown, null);
            }

            ((TextView) convertView.findViewById(R.id.header_label)).setText(FORMAT.format((Date) getItem(position)));

            return convertView;        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.textview, null);
            }

            ((TextView) convertView.findViewById(R.id.header_label)).setText(FORMAT.format((Date) getItem(position)));

            return convertView;
        }
    }


}
