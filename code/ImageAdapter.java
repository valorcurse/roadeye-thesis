package com.cgi.roadeye.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
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

    public ImageAdapter(Context context, int resourceId, ArrayList<Plate> images) {
        super(context, resourceId, images);
        this.context = context;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View listView;

        listView = inflater.inflate(R.layout.image_list, null);

        Plate plate = getItem(position);

        TextView text = (TextView) listView.findViewById(R.id.textView);
        String runTime = Long.toString(TimeUnit.MILLISECONDS.toSeconds(plate.getEndTime().getTime() - plate.getStartTime().getTime()));
        text.setText(plate.getInformation() + " (" + runTime + " s)");

        // set image based on selected text
        ImageView imageView = (ImageView) listView
                .findViewById(R.id.list_item_image);
        Bitmap image = Utilities.convertMatToBmp(plate.getPlateImage());

        image = Bitmap.createScaledBitmap(image, 180, 90, false);
        imageView.setImageBitmap(image);

        if (plate.isFound())
            listView.setBackgroundColor(Color.GREEN);


        return listView;
    }
}