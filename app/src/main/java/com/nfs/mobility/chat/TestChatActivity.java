package com.nfs.mobility.chat;

import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.UiThread;
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

import com.allyants.notifyme.NotifyMe;
import com.blikoon.roster.R;

import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import co.intentservice.chatui.ChatView;
import co.intentservice.chatui.models.ChatMessage;

public class TestChatActivity extends AppCompatActivity {

    private static final String TAG = TestChatActivity.class.getSimpleName();


    private String contactJid;
    private ChatView mChatView;
//    private BroadcastReceiver mBroadcastReceiverNewMessage;
//    private BroadcastReceiver mBroadcastReceiverPresenceChanged;
//    private BroadcastReceiver mBroadcastReceiverContactsUpdated;

    private RecyclerView contactsRecyclerView;
    private TextView notSelectedTxt;
    private ContactAdapter mAdapter;

    NotifyMe.Builder notifyMe;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);


        notifyMe = new NotifyMe.Builder(getApplicationContext());

        mChatView = (ChatView) findViewById(R.id.roster_chat_view);
        notSelectedTxt = (TextView) findViewById(R.id.not_selected_chat_account_txt);
        contactsRecyclerView = (RecyclerView) findViewById(R.id.contact_list_online_recycler_view);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        ContactRepository model = ContactRepository.getInstance();
        List<Contact> contacts = model.getContacts();

        mAdapter = new ContactAdapter(contacts,listActionListener);
        contactsRecyclerView.setAdapter(mAdapter);

        RosterManager.getInstance().enableNotifications(false);
        RosterManager.getInstance().addOnMessageChangeListener(messageChangeListener);
        RosterManager.getInstance().addOnRosterChangeListener(rosterUpdatesListener);
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
                    if (RosterConnectionService.getState().equals(RosterConnection.ConnectionState.CONNECTED)) {
                        Log.d(TAG, "The client is connected to the server,Sending Message");
                        //Send the message to the server

                        Intent intent = new Intent(RosterConnectionService.SEND_MESSAGE);
                        intent.putExtra(RosterConnectionService.BUNDLE_MESSAGE_BODY,
                                mChatView.getTypedMessage());
                        intent.putExtra(RosterConnectionService.BUNDLE_TO, Jid);

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
        if (item.getItemId() == R.id.roster_logout) {
            //Disconnect from server
            Log.d(TAG, "Initiating the log out process");
            Intent i1 = new Intent(this, RosterConnectionService.class);
            stopService(i1);

            RosterManager.getInstance().disconnectUser();

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
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    //region RosterManager
    @Override
    protected void onDestroy() {
        cleanUpRosterListeners();
        RosterManager.getInstance().disconnectUser();
        super.onDestroy();
    }


    void cleanUpRosterListeners() {
        RosterManager.getInstance().removeOnMessageChangeListener(messageChangeListener);
        RosterManager.getInstance().removeOnRosterChangeListener(rosterUpdatesListener);
    }

    @UiThread
    private void updateContactListView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.update(ContactRepository.getInstance().getContacts());
            }
        });
//        mAdapter.update(ContactRepository.getInstance().getContacts());
    }

    @UiThread
    private void updateMessageList() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                List<ChatMessage> chatMessageList = MessageRepository.getInstance().getMessages(contactJid);
                mChatView.clearMessages();
                mChatView.addMessages(new ArrayList<ChatMessage>(chatMessageList));
            }
        });
    }

    void showNotifications(String fromJID, String newMessage) {

        Contact contact = ContactRepository.getInstance().getContact(fromJID);

        String title = contact.getUserName();
        String content = newMessage;
        int red = 0;
        int green = 102;
        int blue = 204;
        int alpha = 255;
        String text = "Mark As Read";


        notifyMe.title(title);
        notifyMe.content(content);
        notifyMe.color(red, green, blue, alpha);//Color of notification header
        notifyMe.led_color(red, green, blue, alpha);//Color of LED when notification pops up
//        notifyMe.time(Calendar.getInstance().getTime());//The time to popup notification
        notifyMe.delay(2000);//Delay in ms
        notifyMe.key("new_message");
//        notifyMe.large_icon(Int resource);
//        notifyMe.addAction(Intent intent, text); //The action will call the intent when pressed
        notifyMe.build();
    }

    RosterManager.OnRosterUpdatesListener rosterUpdatesListener = new RosterManager.OnRosterUpdatesListener() {
        @Override
        public void onChangePresence(String jid, String status) {
            Log.e(TAG, "Presence Changed: " + jid + " , status: " + status);
            updateContactListView();
        }

        @Override
        public void onRemoveContact(Collection<Jid> addresses) {
            Log.e(TAG, "Contact Removed: " + addresses.toString());
            updateContactListView();
        }

        @Override
        public void onUpdateContact(Collection<Jid> addresses) {
            Log.e(TAG, "Contact Updated: " + addresses.toString());
            updateContactListView();
        }

        @Override
        public void onAddContact(Collection<Jid> addresses) {
            Log.e(TAG, "Contact Added: " + addresses.toString());
            updateContactListView();
        }
    };

    RosterManager.OnMessageChangeListener messageChangeListener = new RosterManager.OnMessageChangeListener() {
        @Override
        public void onMessageReceived(String fromJID, String newMessage, int totalCount) {
            Log.e(TAG, "OnMessageChangeListener: onMessageReceived: " + fromJID + " ---> " + newMessage + " ---> " + totalCount);
            updateMessageList();
            showNotifications(fromJID, newMessage);
        }

        @Override
        public void onMessageSent(String toJID, String newMessage, int totalCount, boolean success) {
            Log.e(TAG, "OnMessageChangeListener: onMessageSent: " + toJID + " ---> " + success + " ---> " + totalCount);
            updateMessageList();
        }

        @Override
        public void onMessageDeleted(String jid, String message, int totalCount) {

        }
    };
    //endregion


}
