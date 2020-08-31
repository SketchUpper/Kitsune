package org.xtimms.kitsune.core.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.xtimms.kitsune.R;
import org.xtimms.kitsune.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;

public class DirAdapter extends BaseAdapter {

    private final Context mContext;
    private final ArrayList<File> mFiles;
    private final Drawable[] mIcons;
    private File mCurrentDir;

    public DirAdapter(Context context, File dir) {
        mContext = context;
        mFiles = new ArrayList<>();
        mIcons = ThemeUtils.getThemedIcons(
                context,
                R.drawable.ic_directory_dark,
                R.drawable.ic_directory_null_dark
        );
        setCurrentDir(dir);
    }

    @NonNull
    public File getCurrentDir() {
        return mCurrentDir;
    }

    public void setCurrentDir(@NonNull File dir) {
        mCurrentDir = dir;
        mFiles.clear();
        File[] list = dir.listFiles();
        if (list != null) {
            for (File o : list) {
                if (o.isDirectory()) {
                    mFiles.add(o);
                }
            }
        }
    }

    @Override
    public int getCount() {
        return mFiles.size();
    }

    @Override
    public File getItem(int position) {
        return mFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mFiles.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView) (convertView == null ? View.inflate(mContext, R.layout.item_dir, null) : convertView);
        File f = getItem(position);
        textView.setText(f.getName());
        textView.setCompoundDrawablesWithIntrinsicBounds(f.canWrite() ? mIcons[0] : mIcons[1], null, null, null);
        return textView;
    }
}
