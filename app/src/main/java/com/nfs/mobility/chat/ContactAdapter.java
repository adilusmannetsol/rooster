package com.nfs.mobility.chat;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.blikoon.roster.R;

import org.jivesoftware.smack.packet.Presence;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactHolder> {

    private List<Contact> mContacts;

    private ContactHolder itemClicked = null;

    ContactListActionListener listActionListener;


    public class ContactHolder extends RecyclerView.ViewHolder {
        private TextView contactTextView;
        private ImageView contactImageView;
        private CardView rootItem;
        private Contact mContact;
        private boolean isOnline = false;
        ContactHolder contactHolder = this;

        public ContactHolder(final View itemView) {
            super(itemView);

            rootItem = (CardView) itemView.findViewById(R.id.root_item);
            contactTextView = (TextView) itemView.findViewById(R.id.contact_jid);
            contactImageView = (ImageView) itemView.findViewById(R.id.contact_image);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (itemClicked != null && itemClicked != contactHolder)
                        itemClicked.rootItem.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.white));

                    itemClicked = contactHolder;
                    rootItem.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.blue));

                    listActionListener.startChatWithJidof(mContacts.get(getAdapterPosition()).getJid());
                }
            });
        }


        public void bindContact(Contact contact) {
            mContact = contact;
            if (mContact == null) {
               // Log.d(TAG, "Trying to work on a null Contact object ,returning.");
                return;
            }
            contactTextView.setText(contact.getUserName());
            if (contact.getStatus().equals(Presence.Type.available.toString())) {
                contactImageView.setVisibility(View.VISIBLE);
            } else {
                contactImageView.setVisibility(View.INVISIBLE);

            }
        }
    }



    public ContactAdapter(List<Contact> contactList , ContactListActionListener listActionListener) {
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
        holder.bindContact(contact);

    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }
}