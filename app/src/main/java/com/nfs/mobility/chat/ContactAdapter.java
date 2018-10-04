package com.nfs.mobility.chat;

import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.blikoon.roster.R;
import com.mobility.chat.xmpp.model.Contact;

import org.jivesoftware.smack.packet.Presence;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactHolder> {

    ContactListActionListener listActionListener;
    private List<Contact> mContacts;
    private int mSelectedPosition = -1;


    public ContactAdapter(List<Contact> contactList, ContactListActionListener listActionListener) {
        this.mContacts = contactList;
        this.listActionListener = listActionListener;
    }

    public void update(List<Contact> contactList) {
        mContacts = contactList;
        notifyDataSetChanged();
    }

    @Override
    public ContactHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater
                .inflate(R.layout.list_item_contact, parent,
                        false);

        return new ContactHolder(view);
    }

    @Override
    public void onBindViewHolder(ContactHolder holder, int position) {
        Contact contact = mContacts.get(position);
        holder.bindContact(contact, position);

    }

    @Override
    public int getItemCount() {
        if (mContacts == null || mContacts.size() < 1)
            return 0;
        return mContacts.size();
    }

    public class ContactHolder extends RecyclerView.ViewHolder {
        ContactHolder contactHolder = this;
        private TextView contactTextView;
        private ImageView contactStatusImg;
        private ImageView contactProfileImg;
        private CardView rootItem;
        private Contact mContact;
        private boolean isOnline = false;

        public ContactHolder(final View itemView) {
            super(itemView);

            rootItem = (CardView) itemView.findViewById(R.id.root_item);
            contactTextView = (TextView) itemView.findViewById(R.id.contact_name);
            contactStatusImg = (ImageView) itemView.findViewById(R.id.contact_status);
            contactProfileImg = (ImageView) itemView.findViewById(R.id.contact_img);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position == -1)
                        return;
                    mSelectedPosition = position;
                    listActionListener.startChatWithJidof(mContacts.get(mSelectedPosition).getJid());
                    notifyDataSetChanged();
                }
            });
        }


        public void bindContact(Contact contact, int position) {
            if (position == mSelectedPosition) {
                rootItem.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.blue));
                contactTextView.setTextColor(itemView.getContext().getResources().getColor(R.color.white));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    contactProfileImg.getDrawable().setTint(itemView.getContext().getResources().getColor(R.color.white));
                }
            } else {
                rootItem.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.white));
                contactTextView.setTextColor(itemView.getContext().getResources().getColor(R.color.black));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    contactProfileImg.getDrawable().setTint(itemView.getContext().getResources().getColor(R.color.blue));
                }
            }
            mContact = contact;
            if (mContact == null) {
                // Log.d(TAG, "Trying to work on a null Contact object ,returning.");
                return;
            }
            contactTextView.setText(contact.getUserName());
            if (contact.getStatus().equals(Presence.Type.available.toString())) {
                contactStatusImg.setVisibility(View.VISIBLE);
            } else {
                contactStatusImg.setVisibility(View.INVISIBLE);

            }
        }
    }
}