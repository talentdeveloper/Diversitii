package com.diversitii.dcapp;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.diversitii.dcapp.database.CategoryDao;
import com.diversitii.dcapp.database.PacksDao;

import java.io.File;


/**
 * An array adapter for a GridView of pack items.
 */
class PacksArrayAdapter extends BaseAdapter {
    private Context mContext;
    private final LayoutInflater mInflater;
    private final boolean[] mIsPackUnlocked;
    private final boolean mIsSinglePack;

    PacksArrayAdapter(Context context, LayoutInflater inflater, boolean[] isUnlocked, boolean isSinglePack) {
        mContext = context;
        mInflater = inflater;
        mIsPackUnlocked = isUnlocked;
        mIsSinglePack = isSinglePack;
    }

    @Override
    public
    @NonNull
    View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Inflate new view (don't recycle, want to update viewed state on returning to this screen)
        if (mIsSinglePack) {
            convertView = mInflater.inflate(R.layout.pack_grid_view_item, parent, false);

            if (position % 4 == 0 || (position - 1) % 4 == 0) { // alternate red and yellow rows (2 columns)
                if (position % 4 == 0) {
                    convertView.setBackgroundResource(R.drawable.panel_bg_left_yellow);
                } else {
                    convertView.setBackgroundResource(R.drawable.panel_bg_right_yellow);
                }
                ((TextView) convertView.findViewById(R.id.tv_unlock_pack)).setTextColor(mContext.getResources().getColor(R.color.blue));
                convertView.findViewById(R.id.layout_bg).setBackgroundResource(R.drawable.red_rectangle_rounded_corners);
            } else if (position % 9 == 0 || (position % 2 != 0 && (position - 1) % 9 == 0)) { // every 9th row is yellow
                if (position % 9 == 0) {
                    convertView.setBackgroundResource(R.drawable.panel_bg_left_red);
                } else {
                    convertView.setBackgroundResource(R.drawable.panel_bg_right_red);
                }
                ((TextView) convertView.findViewById(R.id.tv_pack_name)).setTextColor(mContext.getResources().getColor(android.R.color.black));
                ((TextView) convertView.findViewById(R.id.tv_unlock_pack)).setTextColor(mContext.getResources().getColor(R.color.blue));
                convertView.findViewById(R.id.layout_bg).setBackgroundResource(R.drawable.yellow_rectangle_rounded_corners);
            } else { // red
                if (position % 2 == 0) {
                    convertView.setBackgroundResource(R.drawable.panel_bg_left_red);
                } else {
                    convertView.setBackgroundResource(R.drawable.panel_bg_right_red);
                }
            }
            // Set pack text
            if (Utils.isPortrait(mContext)) {
                ((TextView) convertView.findViewById(R.id.tv_pack_name)).setText(mContext.getString(R.string.title_topic_pack_port, position + 1));
            } else {
                ((TextView) convertView.findViewById(R.id.tv_pack_name)).setText(mContext.getString(R.string.title_topic_pack, position + 1));
            }

            int catId = new PacksDao().getCategoryId(mContext, position + 1);
            if (catId != Constants.DEFAULT_CAT_ID) { // set category text
                ((TextView) convertView.findViewById(R.id.tv_cat_name)).setText(new CategoryDao().getCategoryText(mContext, catId));
            }

            // Show message about pack's purchase state
            if (!mIsPackUnlocked[position]) {
                if (catId != Constants.DEFAULT_CAT_ID) { // show category icon
                    File file = new File(mContext.getFilesDir(), Constants.getCategoryIconFileName(catId));
                    ((ImageView) convertView.findViewById(R.id.iv_icon)).setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
                }
                convertView.findViewById(R.id.tv_unlock_pack).setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.tv_owned_pack).setVisibility(View.INVISIBLE);
            } else {
                convertView.findViewById(R.id.tv_unlock_pack).setVisibility(View.INVISIBLE);
                convertView.findViewById(R.id.tv_owned_pack).setVisibility(View.VISIBLE);
            }
        } else { // multipack
            convertView = mInflater.inflate(R.layout.multipack_grid_view_item, parent, false);

            // Set pack text
            ((TextView) convertView.findViewById(R.id.tv_multipack)).setText(
                    mContext.getString(R.string.format_packs_text,
                            position * Constants.PACKS_PER_MULTIPACK + 1,
                            ((position + 1) * Constants.PACKS_PER_MULTIPACK)));
        }
        return convertView;
    }

    @Override
    public int getCount() {
        return mIsPackUnlocked.length;
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
