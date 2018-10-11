package com.diversitii.dcapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.diversitii.dcapp.database.TopicsDao;

/**
 * Dynamically updating adapter for topic deletion AutoCompleteTextView.
 */
class AutoCompleteAdapter extends ArrayAdapter implements Filterable {
    private static final String TAG = AutoCompleteAdapter.class.getName();

    private String[] mTopics;
    private Context mContext;

    AutoCompleteAdapter(Context context, int resource) {
        super(context, resource);
        mContext = context;
    }

    @Override
    public int getCount() {
        return (mTopics != null) ? mTopics.length : 0;
    }

    @Override
    public String getItem(int position) {
        return (mTopics != null) ? mTopics[position] : "";
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    try {
                        // Get list of topics beginning with entered text
                        mTopics = new TopicsDao().getMatches(mContext, constraint.toString());
                    } catch (Exception e) {
                        Log.e(TAG, "Filter: " + e);
                    }
                    filterResults.count = (mTopics != null) ? mTopics.length : 0;
                    filterResults.values = mTopics;
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }
}
