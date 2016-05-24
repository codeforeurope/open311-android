package org.open311.android.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.open311.android.R;

import java.text.Normalizer;

/**
 * Profile {@link Fragment} subclass.
 *
 */
public class ProfileFragment extends Fragment {

    private SharedPreferences settings;

    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Find a view by id - convenience method
     *
     * @param viewId
     * @return View
     */
    private View findViewById(int viewId) {
        return getActivity().findViewById(viewId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getActivity().getPreferences(Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        EditText name  = (EditText) view.findViewById(R.id.input_name);
        EditText email = (EditText) view.findViewById(R.id.input_email);
        EditText phone = (EditText) view.findViewById(R.id.input_phone);

        name.setText(settings.getString("name", null));
        email.setText(settings.getString("email", null));
        phone.setText(settings.getString("phone", null));

        Button submit = (Button) view.findViewById(R.id.btn_submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitButtonClicked();
            }
        });

        return view;
    }

    private void onSubmitButtonClicked() {
        SharedPreferences.Editor editor = settings.edit();
        String result;
        EditText name = (EditText) findViewById(R.id.input_name);
        EditText email = (EditText) findViewById(R.id.input_email);
        EditText phone = (EditText) findViewById(R.id.input_phone);

        String strName = name.getText().toString();
        String strEmail = email.getText().toString();
        String strPhone = phone.getText().toString();

        // Filter name text. Replace everything that is not a letter or whitespace.
        String pattern = "[^a-zA-Z\\s]";
        name.setText(Normalizer.normalize(strName, Normalizer.Form.NFD).replaceAll(pattern, ""));

        boolean isValid = true;

        if (! strEmail.isEmpty()) {
            if (! isValidEmail(strEmail)) {
                isValid = false;
                email.setError(getString(R.string.invalid_email));
                email.requestFocus();
            } else {
                editor.putString("email", strEmail);
            }
        }

        if (! isValid) return;

        editor.putString("name", strName);
        editor.putString("phone", strPhone);

        if (editor.commit()) {
            result = getString(R.string.settings_saved);
        } else {
            result = getString(R.string.error_occurred);
        }
        Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
    }

    private boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            return Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }
}
