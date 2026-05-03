package com.example.walnex.transfer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walnex.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter used for both the Frequent Contacts and All Contacts lists on
 * TransferToActivity.  Each row inflates {@code item_transfer_contact.xml}.
 */
public class TransferContactsAdapter
        extends RecyclerView.Adapter<TransferContactsAdapter.ContactViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(TransferContact contact);
    }

    private final List<TransferContact> contacts = new ArrayList<>();
    private final OnContactClickListener listener;

    public TransferContactsAdapter(OnContactClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<TransferContact> newList) {
        contacts.clear();
        if (newList != null) contacts.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transfer_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        TransferContact contact = contacts.get(position);

        holder.textName.setText(contact.fullName);
        holder.textPhone.setText(contact.phoneE164);

        if (contact.avatarRes != 0) {
            holder.imageAvatar.setImageResource(contact.avatarRes);
        } else {
            holder.imageAvatar.setImageResource(R.drawable.ic_person_avatar);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onContactClick(contact);
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageAvatar;
        final TextView  textName;
        final TextView  textPhone;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.imageContactAvatar);
            textName    = itemView.findViewById(R.id.textContactName);
            textPhone   = itemView.findViewById(R.id.textContactPhone);
        }
    }
}