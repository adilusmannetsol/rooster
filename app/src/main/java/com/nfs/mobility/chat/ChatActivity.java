package com.nfs.mobility.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


import com.blikoon.roster.R;

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
        mChatView =(ChatView) findViewById(R.id.roster_chat_view);

        mChatView.setOnSentMessageListener(new ChatView.OnSentMessageListener(){
            @Override
            public boolean sendMessage(ChatMessage chatMessage){
                // perform actual message sending
                if (RosterConnectionService.getState().equals(RosterConnection.ConnectionState.CONNECTED)) {
                    Log.d(TAG, "The client is connected to the server,Sending Message");
                    //Send the message to the server

                    Intent intent = new Intent(RosterConnectionService.SEND_MESSAGE);
                    intent.putExtra(RosterConnectionService.BUNDLE_MESSAGE_BODY,
                            mChatView.getTypedMessage());
                    intent.putExtra(RosterConnectionService.BUNDLE_TO, contactJid);

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
                switch (action)
                {
                    case RosterConnectionService.NEW_MESSAGE:
                        String from = intent.getStringExtra(RosterConnectionService.BUNDLE_FROM_JID);
                        String body = intent.getStringExtra(RosterConnectionService.BUNDLE_MESSAGE_BODY);

                        if ( from.equals(contactJid))
                        {
                            ChatMessage chatMessage = new ChatMessage(body,System.currentTimeMillis(), ChatMessage.Type.RECEIVED);
                            mChatView.addMessage(chatMessage);

                        }else
                        {
                            Log.d(TAG,"Got a message from jid :"+from);
                        }

                        return;
                }

            }
        };

        IntentFilter filter = new IntentFilter(RosterConnectionService.NEW_MESSAGE);
        registerReceiver(mBroadcastReceiver,filter);


    }
}
