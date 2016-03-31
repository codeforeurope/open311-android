package org.open311.android.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.open311.android.R;

public class CustomButton extends FrameLayout {

    private int id;
    private int defaultHintTextColor;

    private LayoutInflater inflater;
    private FrameLayout frameView;
    private TextView textView;
    private ImageView imgView;

    public CustomButton(Context context) {
        super(context);
        init();
    }

    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.custom_button, this, true);
        frameView = (FrameLayout) findViewById(R.id.report_button_frame);
        textView = (TextView) findViewById(R.id.report_button_text);
        imgView = (ImageView) findViewById(R.id.report_button_image);
        defaultHintTextColor = textView.getCurrentHintTextColor();
    }

    public CharSequence getText() {
        return textView.getText();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }

    public void setHint(String hint) {
        textView.setHint(hint);
    }

    public void setHint(int resid) {
        textView.setHint(resid);
    }

    public void setIcon(int icon) {
        textView.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
    }

    public void setImageBitmap(Bitmap img) {
        if (img != null) {
            imgView.setImageBitmap(img);
            imgView.setVisibility(ImageView.VISIBLE);
        } else {
            imgView.setImageBitmap(null);
            imgView.setVisibility(ImageView.INVISIBLE);
            textView.setHint(R.string.report_hint_photo);
        }
    }

    public void clear() {
        if (imgView.getVisibility() == ImageView.VISIBLE) {
            setImageBitmap(null);
        }
        textView.setText(null);
        textView.setHintTextColor(defaultHintTextColor);
    }

    public void setError() {
        textView.setHintTextColor(Color.RED);
    }

    public void setOnClickListener(OnClickListener listener) {
        frameView.setOnClickListener(listener);
    }
}
