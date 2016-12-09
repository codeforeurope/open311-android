package org.open311.android.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import org.open311.android.R;

/**
 * Profile {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {
    private static final String LOG_TAG = "ProfileFragment";
    private SharedPreferences settings;
    private EditText inputName, inputEmail, inputPhone;
    private TextInputLayout inputLayoutName, inputLayoutEmail, inputLayoutPhone;
    private FloatingActionButton mSubmitBtn;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getActivity().getPreferences(Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        inputLayoutName = (TextInputLayout) view.findViewById(R.id.input_layout_name);
        inputLayoutEmail = (TextInputLayout) view.findViewById(R.id.input_layout_email);
        inputLayoutPhone = (TextInputLayout) view.findViewById(R.id.input_layout_phone);

        inputName = (EditText) view.findViewById(R.id.input_name);
        inputEmail = (EditText) view.findViewById(R.id.input_email);
        inputPhone = (EditText) view.findViewById(R.id.input_phone);

        inputName.setText(settings.getString("name", null));
        inputEmail.setText(settings.getString("email", null));
        inputPhone.setText(settings.getString("phone", null));

        inputName.addTextChangedListener(new MyTextWatcher(inputName));
        inputEmail.addTextChangedListener(new MyTextWatcher(inputEmail));
        inputPhone.addTextChangedListener(new MyTextWatcher(inputPhone));

        mSubmitBtn = (FloatingActionButton) view.findViewById(R.id.profile_submit);
        mSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitButtonClicked(v);
            }
        });

        return view;
    }

    private void onSubmitButtonClicked(View v) {
        SharedPreferences.Editor editor = settings.edit();
        String result;
        if (!validate()) {
            return;
        }

        editor.putString("name", inputName.getText().toString());
        editor.putString("phone", inputPhone.getText().toString());
        editor.putString("email", inputEmail.getText().toString());

        if (editor.commit()) {
            result = getString(R.string.settings_saved);
        } else {
            result = getString(R.string.error_occurred);
        }
        Snackbar.make(v, result, Snackbar.LENGTH_SHORT)
                .show();
    }

    private boolean validate() {
        if (inputName.getText().toString().trim().isEmpty()) {
            inputLayoutName.setError(getString(R.string.invalid_name));
            mSubmitBtn.setVisibility(View.INVISIBLE);
            return false;
        } else {
            inputLayoutName.setErrorEnabled(false);
        }
        String strEmail = inputEmail.getText().toString().trim();

        if (strEmail.isEmpty() || !isValidEmail(strEmail)) {
            inputLayoutEmail.setError(getString(R.string.invalid_email));
            mSubmitBtn.setVisibility(View.INVISIBLE);
            return false;
        } else {
            inputLayoutEmail.setErrorEnabled(false);
        }
        if (inputPhone.getText().toString().trim().isEmpty()) {
            inputLayoutPhone.setError(getString(R.string.invalid_phone));
            mSubmitBtn.setVisibility(View.INVISIBLE);
            return false;
        } else {
            inputLayoutPhone.setErrorEnabled(false);
        }
        mSubmitBtn.setVisibility(View.VISIBLE);
        return true;
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private class MyTextWatcher implements TextWatcher {

        private View view;

        private MyTextWatcher(View view) {
            this.view = view;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        public void afterTextChanged(Editable editable) {
            validate();
        }
    }
}
