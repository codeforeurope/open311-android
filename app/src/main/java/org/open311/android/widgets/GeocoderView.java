package org.open311.android.widgets;

import android.content.Context;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.widget.AdapterView;

import org.open311.android.adapters.GeocoderAdapter;

/**
 * Created by miblon on 7/15/16.
 */
public class GeocoderView extends SearchView {

    private SearchView.SearchAutoComplete mSearchAutoComplete;
    private Context context;
    public GeocoderView(Context context) {
        super(context);
        this.context = context;
        initialize();
    }

    public GeocoderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public void initialize() {
        mSearchAutoComplete = (SearchAutoComplete) findViewById(android.support.v7.appcompat.R.id.search_src_text);
        final GeocoderAdapter adapter = new GeocoderAdapter(context);
        this.setAdapter(null);
        this.setOnItemClickListener(null);
    }

    @Override
    public void setSuggestionsAdapter(CursorAdapter adapter) {
        // don't let anyone touch this
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        mSearchAutoComplete.setOnItemClickListener(listener);
    }

    public void setAdapter(GeocoderAdapter adapter) {
        mSearchAutoComplete.setAdapter(adapter);
    }

    public void setText(String text) {
        mSearchAutoComplete.setText(text);
    }

}