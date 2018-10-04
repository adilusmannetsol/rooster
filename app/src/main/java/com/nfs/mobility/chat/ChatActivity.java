package com.nfs.mobility.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.blikoon.roster.R;
import com.mobility.chat.xmpp.RosterConnection;
import com.mobility.chat.xmpp.RosterManager;

import co.intentservice.chatui.ChatView;
import co.intentservice.chatui.models.ChatMessage;


public class ChatActivity extends AppCompatActivity {
    private static final String TAG = ChatActivity.class.getSimpleName();

    private String contactJid;
    private ChatView mChatView;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mChatView = (ChatView) findViewById(R.id.roster_chat_view);

        mChatView.setOnSentMessageListener(new ChatView.OnSentMessageListener() {
            @Override
            public boolean sendMessage(ChatMessage chatMessage) {
                // perform actual message sending
                if (RosterManager.getInstance().getConnectionState().equals(RosterConnection.ConnectionState.CONNECTED)) {
                    Log.d(TAG, "The client is connected to the server,Sending Message");
                    //Send the message to the server

                    Intent intent = new Intent(RosterConnection.SEND_MESSAGE);
                    intent.putExtra(RosterConnection.BUNDLE_MESSAGE_BODY,
                            mChatView.getTypedMessage());
                    intent.putExtra(RosterConnection.BUNDLE_TO, contactJid);

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


//        Intent intent = getIntent();
//        contactJid = intent.getStringExtra("EXTRA_CONTACT_JID");
//        setTitle(contactJid);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case RosterConnection.NEW_MESSAGE:
                        String from = intent.getStringExtra(RosterConnection.BUNDLE_FROM_JID);
                        String body = intent.getStringExtra(RosterConnection.BUNDLE_MESSAGE_BODY);

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

        IntentFilter filter = new IntentFilter(RosterConnection.NEW_MESSAGE);
        registerReceiver(mBroadcastReceiver, filter);


    }
}
