package org.open311.android.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.codeforamerica.open311.internals.parsing.DataParser;
import org.open311.android.R;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MyReportsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MyReportsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyReportsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public MyReportsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MyReportsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MyReportsFragment newInstance(String param1, String param2) {
        MyReportsFragment fragment = new MyReportsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_my_reports, container, false);
        LinearLayout myRequests = (LinearLayout) view.findViewById(R.id.cardview_list);
        try {
            ArrayList<Hashtable> list = loadServiceRequests();

            System.out.println("NUMBER OF STORED SERVICE REQUESTS: " + list.size());

            for (Hashtable record : list) {

                Iterator iterator = record.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }

                File imgFile = null;
                if (record.containsKey(DataParser.MEDIA_URL_TAG)) {
                    String filePath = (String) record.get(DataParser.MEDIA_URL_TAG);
                    imgFile = new File(filePath);
                }

                CardView card = (CardView) LayoutInflater.from(container.getContext())
                        .inflate(R.layout.cardview_request, myRequests, false);

                TextView title = (TextView) card.findViewById(R.id.request_title);
                TextView address = (TextView) card.findViewById(R.id.request_address);
                TextView updated = (TextView) card.findViewById(R.id.request_updated);
                TextView description = (TextView) card.findViewById(R.id.request_description);
                TextView status = (TextView) card.findViewById(R.id.request_status);
                ImageView image = (ImageView) card.findViewById(R.id.request_image);

                if (imgFile == null) {
                    image.setImageDrawable(null);
                    image.setVisibility(View.GONE);
                } else {
                    Picasso.with(getContext()).load(imgFile).fit().centerCrop().into(image);
                    image.setVisibility(View.VISIBLE);
                }

                title.setText((String) record.get(DataParser.SERVICE_NAME_TAG));
                address.setText((String) record.get("address_string"));
                status.setText((String) record.get("onbekend")); // has to be retrieved separately - DataParser.STATUS_TAG
                description.setText((String) record.get(DataParser.DESCRIPTION_TAG));

                Date date = null;
                /*
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                Date updated = null;
                try {
                    updated = df.parse((String) record.get(DataParser.UPDATED_DATETIME_TAG));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (updated != null) {
                    CharSequence elapsedTime = DateUtils.getRelativeTimeSpanString(
                            date.getTime(),
                            (new Date()).getTime(), DateUtils.SECOND_IN_MILLIS
                    );
                    updated.setText(elapsedTime);
                } else {
                    updated.setVisibility(View.GONE);
                }*/
                updated.setVisibility(View.GONE);

                myRequests.addView(card);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        DummyCard cardOne = new DummyCard((CardView) view.findViewById(R.id.dummy_card_1));
        DummyCard cardTwo = new DummyCard((CardView) view.findViewById(R.id.dummy_card_2));

        String mediaUrl = "http://311.baltimorecity.gov/media/baltimore/report/photos/57017f9465c8e77116088369/report.jpg";

        cardOne.title.setText("Overlast");
        cardOne.address.setText("Jan Hollanderstraat 11");
        cardOne.updated.setText("2 uur geleden");
        cardOne.status.setText("gesloten");
        cardOne.description.setText("Luidruchtige hangjongeren drinken alcohol op straat");

        cardOne.image.setImageDrawable(null);
        cardOne.image.setVisibility(View.GONE);

        cardTwo.title.setText("Vervuiling");
        cardTwo.address.setText("Anthony van Leeuwenhoeklaan 30");
        cardTwo.updated.setText("5 uur geleden");
        cardTwo.status.setText("open");
        cardTwo.description.setText("Bankstel gedumpt op kinderspeelplaats");

        Picasso.with(getContext()).load(mediaUrl).fit().centerCrop().into(cardTwo.image);
        cardTwo.image.setVisibility(View.VISIBLE);
        */

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private ArrayList<Hashtable> loadServiceRequests() throws IOException {
        ArrayList<Hashtable> list = new ArrayList<Hashtable>();
        ObjectInputStream file = null;
        try {
            file = new ObjectInputStream(getActivity().openFileInput(""));
            while (true) {
                list.add((Hashtable) file.readObject());
            }
        } catch (EOFException ignored) {
            // as expected
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (OptionalDataException e) {
            e.printStackTrace();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (file != null) file.close();
        }
        return list;
    }

    private class DummyCard {

        TextView title;
        TextView address;
        TextView updated;
        TextView description;
        TextView status;
        ImageView image;

        public DummyCard(CardView card) {
            title = (TextView) card.findViewById(R.id.request_title);
            address = (TextView) card.findViewById(R.id.request_address);
            updated = (TextView) card.findViewById(R.id.request_updated);
            description = (TextView) card.findViewById(R.id.request_description);
            status = (TextView) card.findViewById(R.id.request_status);
            image = (ImageView) card.findViewById(R.id.request_image);
        }
    }
}
