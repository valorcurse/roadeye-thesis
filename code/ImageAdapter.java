package com.cgi.roadeye.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by valorcurse on 15/04/14.
 */

public class ImageAdapter extends ArrayAdapter<Plate> {
    private Context context;
//    private ArrayList<Plate> images;

    public ImageAdapter(Context context, int resourceId, ArrayList<Plate> images) {
        super(context, resourceId, images);
        this.context = context;
//        this.images = images;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View listView;

        // get layout from mobile.xml
        listView = inflater.inflate(R.layout.image_list, null);

        Plate plate = getItem(position);

        TextView text = (TextView) listView.findViewById(R.id.textView);
        String runTime = Long.toString(TimeUnit.MILLISECONDS.toSeconds(plate.getEndTime().getTime() - plate.getStartTime().getTime()));
        text.setText(plate.getInformation() + " (" + runTime + " s)");
//        text.setText(runTime);

        // set image based on selected text
        ImageView imageView = (ImageView) listView
                .findViewById(R.id.list_item_image);
        Bitmap image = Utilities.convertMatToBmp(plate.getPlateImage());

//        double ratio = image.getWidth() / image.getHeight();
//        int scaledWidth = (int) (90 * ratio);

        image = Bitmap.createScaledBitmap(image, 180, 90, false);
        imageView.setImageBitmap(image);

        return listView;
    }

//    @Override
//    public int getCount() {
//        return images.size();
//    }

//    @Override
//    public Object getItem(int position) {
//        return null;
//    }

//    @Override
//    public long getItemId(int position) {
//        return 0;
//    }

}