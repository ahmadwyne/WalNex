package com.example.walnex.auth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.walnex.R;

import java.util.List;

public final class CountryCodeAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<Country> countries;

    public CountryCodeAdapter(Context context, List<Country> countries) {
        this.inflater = LayoutInflater.from(context);
        this.countries = countries;
    }

    @Override
    public int getCount() {
        return countries.size();
    }

    @Override
    public Country getItem(int position) {
        return countries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_country_code_selected, parent, false);
        }

        Country country = countries.get(position);
        TextView flag = view.findViewById(R.id.textCountryFlag);
        TextView label = view.findViewById(R.id.textCountryLabel);

        flag.setText(country.flagEmoji());
        label.setText(country.dialCode);
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_country_code_dropdown, parent, false);
        }

        Country country = countries.get(position);
        TextView flag = view.findViewById(R.id.textCountryFlag);
        TextView name = view.findViewById(R.id.textCountryName);
        TextView dial = view.findViewById(R.id.textCountryDial);

        flag.setText(country.flagEmoji());
        name.setText(country.name);
        dial.setText(country.dialCode);
        return view;
    }
}
