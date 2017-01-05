package org.open311.android.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.codeforamerica.open311.facade.data.City;
import org.open311.android.MainActivity;
import org.open311.android.R;

import static org.open311.android.helpers.Utils.getSettings;

/**
 * Policy {@link Fragment} subclass.
 */
public class PolicyFragment extends Fragment {
    private static final String LOG_TAG = "PolicyFragment";
    private SharedPreferences settings;

    public PolicyFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSettings(getActivity());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_policy, container, false);
        TextView introTitle = (TextView) view.findViewById(R.id.policy_intro_title);
        TextView introText = (TextView) view.findViewById(R.id.policy_intro_text);
        City currentCity = ((MainActivity) getActivity()).getCurrentCity();
        introTitle.setText(settings.getString("current_city", getString(R.string.policy_intro_title)));


        introText.setText(currentCity.getDescription());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (currentCity.getDescription() != null) {
                introText.setText(Html.fromHtml(currentCity.getDescription().replace("\n","<br />"), Html.FROM_HTML_MODE_LEGACY));
            } else {
                introText.setText(Html.fromHtml(getString(R.string.policy_intro_text).replace("\n","<br />"), Html.FROM_HTML_MODE_LEGACY));
            }
        } else {
            if (currentCity.getDescription() != null) {
                introText.setText(Html.fromHtml(currentCity.getDescription().replace("\n","<br />")));
            } else {
                introText.setText(Html.fromHtml(getString(R.string.policy_intro_text).replace("\n","<br />")));
            }
        }
        return view;
    }

}
