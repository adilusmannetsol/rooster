package com.nfs.mobility.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
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

import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;
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

        ContactRepository model = ContactRepository.getInstance();
        List<Contact> contacts = model.getContacts();

        mAdapter = new ContactAdapter(contacts,listActionListener);
        contactsRecyclerView.setAdapter(mAdapter);

        RoosterManager.getInstance().addOnMessageChangeListener(messageChangeListener);
        RoosterManager.getInstance().addOnRoosterChangeListener(roosterUpdatesListener);
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
//        mBroadcastReceiverNewMessage = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                String action = intent.getAction();
//                switch (action) {
//                    case RoosterConnectionService.NEW_MESSAGE:
//                        String from = intent.getStringExtra(RoosterConnectionService.BUNDLE_FROM_JID);
//                        String body = intent.getStringExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY);
//
//                        Log.e(TAG, "NEW_MESSAGE: From: " + from + " Body: " + body);
//
//                        if (from.equals(contactJid)) {
//                            ChatMessage chatMessage = new ChatMessage(body, System.currentTimeMillis(), ChatMessage.Type.RECEIVED);
//                            mChatView.addMessage(chatMessage);
//
//                        } else {
//                            Log.d(TAG, "Got a message from jid :" + from);
//                        }
//
//                        return;
//                }
//
//            }
//        };
//
//        IntentFilter filterNewMessage = new IntentFilter(RoosterConnectionService.NEW_MESSAGE);
//        registerReceiver(mBroadcastReceiverNewMessage, filterNewMessage);

//        mBroadcastReceiverPresenceChanged = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                String action = intent.getAction();
//                switch (action) {
//                    case RoosterConnectionService.PRESENCE_CHANGED:
//                        String from = intent.getStringExtra(RoosterConnectionService.BUNDLE_FROM_JID);
//                        String type = intent.getStringExtra(RoosterConnectionService.BUNDLE_PRESENCE_TYPE);
//
//                        Log.e(TAG, "Presence from: " + from + " updated to " + type);
//
//                        for (Contact contact : ContactRepository.getInstance().getContacts()) {
//                            if (contact.getJid().equals(from)) {
//                                contact.setStatus(type);
//                            }
//                        }
//
//                        mAdapter.update(ContactRepository.getInstance().getContacts());
//
//                        return;
//                }
//
//            }
//        };
//
//        IntentFilter filterPresenceChanged = new IntentFilter(RoosterConnectionService.PRESENCE_CHANGED);
//        registerReceiver(mBroadcastReceiverPresenceChanged, filterPresenceChanged);

        mBroadcastReceiverContactsUpdated = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case RoosterConnectionService.CONTACTS_UPDATED:
                        mAdapter.update(ContactRepository.getInstance().getContacts());
                        return;
                }
            }
        };

        IntentFilter filterContactsUpdated = new IntentFilter(RoosterConnectionService.CONTACTS_UPDATED);
        registerReceiver(mBroadcastReceiverContactsUpdated, filterContactsUpdated);

    }

    //region RoosterManager
    @Override
    protected void onDestroy() {
        cleanUpRoosterListeners();
        super.onDestroy();
    }


    void cleanUpRoosterListeners() {
        RoosterManager.getInstance().removeOnMessageChangeListener(messageChangeListener);
        RoosterManager.getInstance().removeOnRoosterChangeListener(roosterUpdatesListener);
    }

    RoosterManager.OnRoosterUpdatesListener roosterUpdatesListener = new RoosterManager.OnRoosterUpdatesListener() {
        @Override
        public void onChangePresence(String jid, String status) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.update(ContactRepository.getInstance().getContacts());
                }
            });
        }

        @Override
        public void onRemoveContact(Collection<Jid> addresses) {

        }

        @Override
        public void onUpdateContact(Collection<Jid> addresses) {

        }

        @Override
        public void onDeleteContact() {

        }

        @Override
        public void onAddContact(Collection<Jid> addresses) {

        }
    };

    RoosterManager.OnMessageChangeListener messageChangeListener = new RoosterManager.OnMessageChangeListener() {
        @Override
        public void onMessageReceived(String fromJID, String newMessage, int totalCount) {
            Log.e(TAG, "OnMessageChangeListener: onMessageReceived: " + fromJID + " ---> " + newMessage + " ---> " + totalCount);
            final String mJID = fromJID;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<ChatMessage> chatMessageList = MessageRepository.getInstance().getMessages(mJID);
                    mChatView.clearMessages();
                    mChatView.addMessages(new ArrayList<ChatMessage>(chatMessageList));
                }
            });
        }

        @Override
        public void onMessageSent(String toJID, String newMessage, int totalCount, boolean success) {
            Log.e(TAG, "OnMessageChangeListener: onMessageSent: " + toJID + " ---> " + success + " ---> " + totalCount);
            if(!success){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mChatView.removeMessage(0);
                    }
                }, 1000);
            }
        }

        @Override
        public void onMessageDeleted(String jid, String message, int totalCount) {

        }
    };
    //endregion


}
