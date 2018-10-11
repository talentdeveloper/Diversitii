package com.diversitii.dcapp;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * An array adapter for list items.
 */
public class ListItemArrayAdapter extends BaseAdapter {
    private Context mContext;
    private final LayoutInflater mInflater;
    private final String[] mRows;
    private final boolean[] mIsEnabled;
    private final boolean mIsMembership;

    ListItemArrayAdapter(Context context, LayoutInflater inflater, String[] rows, boolean[] isEnabled, boolean isMembership) {
        mContext = context;
        mInflater = inflater;
        mRows = rows;
        mIsEnabled = isEnabled;
        mIsMembership = isMembership;
    }

    @Override
    public
    @NonNull
    View getView(int position, View convertView, @NonNull ViewGroup parent) {
        String name = (mRows[position] != null && !mRows[position].isEmpty()) ?
                mRows[position] : mContext.getString(R.string.random_pack);

        if (mIsEnabled != null) { // on/off category row
            convertView = mInflater.inflate(R.layout.cat_list_view_item, parent, false);

            // Set category name text
            ((TextView) convertView.findViewById(R.id.tv_item)).setText(name);

            final ImageView btnOn = convertView.findViewById(R.id.btn_on);
            final ImageView btnOff = convertView.findViewById(R.id.btn_off);
            if (mIsEnabled[position]) {
                btnOn.setImageResource(R.drawable.oval_green);
                btnOff.setImageResource(R.drawable.oval_dark_red);
            } else {
                btnOn.setImageResource(R.drawable.oval_dark_green);
                btnOff.setImageResource(R.drawable.oval_red);
            }

            final int pos = position;
            final ListView listView = parent.findViewById(R.id.lv_cats);
            btnOn.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            listView.performItemClick(view, pos, 0);
                        }
                    }
            );
            btnOff.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            listView.performItemClick(view, pos, 0);
                        }
                    }
            );
        } else { // purchasable category or membership row
            convertView = mInflater.inflate(R.layout.membership_list_view_item, parent, false);

            // Set text and background color
            Resources rsrcs = mContext.getResources();
            if (mIsMembership || (mRows[position] != null && mRows[position].equals(mContext.getString(R.string.membership_options)))) { // membership row
                if (mRows[position].equals(mContext.getString(R.string.membership_options))) {
                    // Portrait "NEW TOPICS" rows are to be thicker than "CATEGORY CONTROLLER" rows
                    int dpAsPixels = (int) (rsrcs.getDimension(R.dimen.div_s) * rsrcs.getDisplayMetrics().density - 0.5f);
                    int dpAsPixelsSmall = (int) (rsrcs.getDimension(R.dimen.div_xs) * rsrcs.getDisplayMetrics().density);
                    convertView.findViewById(R.id.item_bg).setPadding(dpAsPixelsSmall, dpAsPixels, dpAsPixelsSmall, dpAsPixels);
                }
                TextView tv = convertView.findViewById(R.id.tv_item);
                tv.setText(name);
                tv.setTextColor(rsrcs.getColor(R.color.blue));

                convertView.findViewById(R.id.item_bg).setBackgroundResource(R.drawable.yellow_less_rounded_rectangle);
            } else { // category row
                ((TextView) convertView.findViewById(R.id.tv_item)).setText(name);

                // Portrait "NEW TOPICS" rows are to be thicker than "CATEGORY CONTROLLER" rows
                int dpAsPixels = (int) (rsrcs.getDimension(R.dimen.div_s) * rsrcs.getDisplayMetrics().density - 0.5f);
                int dpAsPixelsSmall = (int) (rsrcs.getDimension(R.dimen.div_xs) * rsrcs.getDisplayMetrics().density);
                convertView.findViewById(R.id.item_bg).setPadding(dpAsPixelsSmall, dpAsPixels, dpAsPixelsSmall, dpAsPixels);
            }
        }
        return convertView;
    }

    @Override
    public int getCount() {
        return mRows.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
