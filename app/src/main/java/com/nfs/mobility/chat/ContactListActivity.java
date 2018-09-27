package com.nfs.mobility.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.blikoon.rooster.R;

import java.util.List;

import co.intentservice.chatui.ChatView;
import co.intentservice.chatui.models.ChatMessage;

public class ContactListActivity extends AppCompatActivity {

    private static final String TAG = ContactListActivity.class.getSimpleName();


    private String contactJid;
    private ChatView mChatView;
    private BroadcastReceiver mBroadcastReceiverNewMessage;
    private BroadcastReceiver mBroadcastReceiverPresenceChanged;
    private BroadcastReceiver mBroadcastReceiverContactsUpdated;

    private RecyclerView contactsRecyclerView;
    private TextView notSelectedTxt;
    private ContactAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        mChatView = (ChatView) findViewById(R.id.rooster_chat_view);
        notSelectedTxt = (TextView) findViewById(R.id.not_selected_chat_account_txt);
        contactsRecyclerView = (RecyclerView) findViewById(R.id.contact_list_online_recycler_view);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        ContactModel model = ContactModel.getInstance();
        List<Contact> contacts = model.getContacts();

        mAdapter = new ContactAdapter(contacts,listActionListener);
        contactsRecyclerView.setAdapter(mAdapter);
    }

    ContactListActionListener listActionListener = new ContactListActionListener() {
        @Override
        public void startChatWithJidof(final String Jid) {

            if (mChatView.getVisibility() != View.VISIBLE) {
                mChatView.setVisibility(View.VISIBLE);
                notSelectedTxt.setVisibility(View.GONE);
            } else {
                mChatView.clearMessages();
            }

            mChatView.setOnSentMessageListener(new ChatView.OnSentMessageListener() {
                @Override
                public boolean sendMessage(ChatMessage chatMessage) {
                    // perform actual message sending
                    if (RoosterConnectionService.getState().equals(RoosterConnection.ConnectionState.CONNECTED)) {
                        Log.d(TAG, "The client is connected to the server,Sending Message");
                        //Send the message to the server

                        Intent intent = new Intent(RoosterConnectionService.SEND_MESSAGE);
                        intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY,
                                mChatView.getTypedMessage());
                        intent.putExtra(RoosterConnectionService.BUNDLE_TO, Jid);

                        sendBroadcast(intent);

                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Client not connected to server ,Message not sent!",
                                Toast.LENGTH_LONG).show();
                    }
                    //message sending ends here
                    return true;
                }
            });

            contactJid = Jid;

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contact_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.rooster_logout) {
            //Disconnect from server
            Log.d(TAG, "Initiating the log out process");
            Intent i1 = new Intent(this, RoosterConnectionService.class);
            stopService(i1);

            //Finish this activity
            finish();

            //Start login activity for user to login
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);

        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiverNewMessage);
        unregisterReceiver(mBroadcastReceiverPresenceChanged);
        unregisterReceiver(mBroadcastReceiverContactsUpdated);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBroadcastReceiverNewMessage = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case RoosterConnectionService.NEW_MESSAGE:
                        String from = intent.getStringExtra(RoosterConnectionService.BUNDLE_FROM_JID);
                        String body = intent.getStringExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY);

                        if (from.equals(contactJid)) {
                            ChatMessage chatMessage = new ChatMessage(body, System.currentTimeMillis(), ChatMessage.Type.RECEIVED);
                            mChatView.addMessage(chatMessage);

                        } else {
                            Log.d(TAG, "Got a message from jid :" + from);
                        }

                        return;
                }

            }
        };

        IntentFilter filterNewMessage = new IntentFilter(RoosterConnectionService.NEW_MESSAGE);
        registerReceiver(mBroadcastReceiverNewMessage, filterNewMessage);

        mBroadcastReceiverPresenceChanged = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case RoosterConnectionService.PRESENCE_CHANGED:
                        String from = intent.getStringExtra(RoosterConnectionService.BUNDLE_FROM_JID);
                        String type = intent.getStringExtra(RoosterConnectionService.BUNDLE_PRESENCE_TYPE);

                        Log.e(TAG, "Presence from: " + from + " updated to " + type);

                        for (Contact contact : ContactModel.getInstance().getContacts()) {
                            if (contact.getJid().equals(from)) {
                                contact.setStatus(type);
                            }
                        }

                        mAdapter.update(ContactModel.getInstance().getContacts());

                        return;
                }

            }
        };

        IntentFilter filterPresenceChanged = new IntentFilter(RoosterConnectionService.PRESENCE_CHANGED);
        registerReceiver(mBroadcastReceiverPresenceChanged, filterPresenceChanged);

        mBroadcastReceiverContactsUpdated = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case RoosterConnectionService.CONTACTS_UPDATED:
                        mAdapter.update(ContactModel.getInstance().getContacts());
                        return;
                }
            }
        };

        IntentFilter filterContactsUpdated = new IntentFilter(RoosterConnectionService.CONTACTS_UPDATED);
        registerReceiver(mBroadcastReceiverContactsUpdated, filterContactsUpdated);


    }

//   private class ContactHolder extends RecyclerView.ViewHolder {
//        private TextView contactTextView;
//        private ImageView contactImageView;
//        private CardView rootItem;
//        private Contact mContact;
//        private boolean isOnline = false;
//        ContactHolder contactHolder = this;
//
//        public ContactHolder(View itemView) {
//            super(itemView);
//
//            rootItem = (CardView) itemView.findViewById(R.id.root_item);
//            contactTextView = (TextView) itemView.findViewById(R.id.contact_jid);
//            contactImageView = (ImageView) itemView.findViewById(R.id.contact_image);
//
//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                    if (mChatView.getVisibility() != View.VISIBLE) {
//                        mChatView.setVisibility(View.VISIBLE);
//                        notSelectedTxt.setVisibility(View.GONE);
//                    } else {
//                        mChatView.clearMessages();
//                    }
//
//
//                    if (itemClicked != null && itemClicked != contactHolder)
//                        itemClicked.rootItem.setCardBackgroundColor(getResources().getColor(R.color.white));
//
//                    itemClicked = contactHolder;
//                    rootItem.setCardBackgroundColor(getResources().getColor(R.color.blue));
//
//                    mChatView.setOnSentMessageListener(new ChatView.OnSentMessageListener() {
//                        @Override
//                        public boolean sendMessage(ChatMessage chatMessage) {
//                            // perform actual message sending
//                            if (RoosterConnectionService.getState().equals(RoosterConnection.ConnectionState.CONNECTED)) {
//                                Log.d(TAG, "The client is connected to the server,Sending Message");
//                                //Send the message to the server
//
//                                Intent intent = new Intent(RoosterConnectionService.SEND_MESSAGE);
//                                intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY,
//                                        mChatView.getTypedMessage());
//                                intent.putExtra(RoosterConnectionService.BUNDLE_TO, mContact.getJid());
//
//                                sendBroadcast(intent);
//
//                            } else {
//                                Toast.makeText(getApplicationContext(),
//                                        "Client not connected to server ,Message not sent!",
//                                        Toast.LENGTH_LONG).show();
//                            }
//                            //message sending ends here
//                            return true;
//                        }
//                    });
//
//                    contactJid = mContact.getJid();
//
//                }
//            });
//        }
//
//
//        public void bindContact(Contact contact) {
//            mContact = contact;
//            if (mContact == null) {
//                Log.d(TAG, "Trying to work on a null Contact object ,returning.");
//                return;
//            }
//            contactTextView.setText(contact.getUserName());
//            if (contact.getStatus().equals(Presence.Type.available.toString())) {
//                contactImageView.setVisibility(View.VISIBLE);
//            } else {
//                contactImageView.setVisibility(View.INVISIBLE);
//
//            }
//        }
//    }
//
//
//    private class ContactAdapter extends RecyclerView.Adapter<ContactHolder> {
//        private List<Contact> mContacts;
//
//
//        public ContactAdapter(List<Contact> contactList) {
//            mContacts = contactList;
//        }
//
//        public void update(List<Contact> contactList) {
//            mContacts = contactList;
//            notifyDataSetChanged();
//        }
//
//        @Override
//        public ContactHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//
//            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
//            View view = layoutInflater
//                    .inflate(R.layout.list_item_contact, parent,
//                            false);
//
//            return new ContactHolder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(ContactHolder holder, int position) {
//            Contact contact = mContacts.get(position);
//            holder.bindContact(contact);
//
//        }
//
//        @Override
//        public int getItemCount() {
//            return mContacts.size();
//        }
//    }
}
