package com.nfs.mobility.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.blikoon.rooster.R;

import java.util.List;

import co.intentservice.chatui.models.ChatMessage;

/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ChatDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ChatListActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */

    private static final String TAG = ChatListActivity.class.getSimpleName();

    private boolean mTwoPane;

    private String contactJid;

    private RecyclerView contactsRecyclerView;

    private ContactAdapter mAdapter;

    private BroadcastReceiver mBroadcastReceiverNewMessage;
    private BroadcastReceiver mBroadcastReceiverPresenceChanged;
    private BroadcastReceiver mBroadcastReceiverContactsUpdated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);


        contactsRecyclerView = (RecyclerView) findViewById(R.id.contact_list_online_recycler_view);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;//TODO
        }else {
            mTwoPane = true;//TODO
        }

        ContactModel model = ContactModel.getInstance();
        List<Contact> contacts = model.getContacts();

        mAdapter = new ContactAdapter(contacts,listActionListener);
        contactsRecyclerView.setAdapter(mAdapter);
        }

    ContactListActionListener listActionListener = new ContactListActionListener() {
        @Override
        public void startChatWithJidof(final String Jid) {

//            if (mChatView.getVisibility() != View.VISIBLE) {
//                mChatView.setVisibility(View.VISIBLE);
//                notSelectedTxt.setVisibility(View.GONE);
//            } else {
//                mChatView.clearMessages();
//            }


            if (mTwoPane) {
                Bundle arguments = new Bundle();
                arguments.putString(ChatDetailFragment.ARG_ITEM_JID, Jid);
                ChatDetailFragment fragment = new ChatDetailFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment)
                        .commit();
            } else {
//                Context context = getApplicationContext();//TODO
//                Intent intent = new Intent(context, ChatDetailActivity.class);//TODO
//                intent.putExtra(ChatDetailFragment.ARG_ITEM_JID, Jid);//TODO
////TODO
//                context.startActivity(intent);//TODO
            }


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

                        Log.e(TAG, "NEW_MESSAGE: From: " + from + " Body: " + body);

                        if (from.equals(contactJid)) {
                            ChatMessage chatMessage = new ChatMessage(body, System.currentTimeMillis(), ChatMessage.Type.RECEIVED);
                           // mChatView.addMessage(chatMessage);

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

}
