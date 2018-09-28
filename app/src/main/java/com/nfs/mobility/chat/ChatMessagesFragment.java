package com.nfs.mobility.chat;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.blikoon.roster.R;

import java.util.ArrayList;
import java.util.List;

import co.intentservice.chatui.ChatView;
import co.intentservice.chatui.models.ChatMessage;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link ChatContactsActivity}
 * in two-pane mode (on tablets) or a {@link ChatDetailActivity}
 * on handsets.
 */
public class ChatMessagesFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */

    private static final String TAG = ChatMessagesFragment.class.getSimpleName();

    public static final String ARG_ITEM_JID = "item_jid";

    private String contactJid;

    private ChatView mChatView;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChatMessagesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_JID))
            contactJid = getArguments().getString(ARG_ITEM_JID);

        RosterManager.getInstance().addOnMessageChangeListener(messageChangeListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.item_chat_view, container, false);

        mChatView = rootView.findViewById(R.id.roster_chat_view);

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
                    intent.putExtra(RosterConnectionService.BUNDLE_TO, contactJid);

                    getContext().sendBroadcast(intent);

                } else {
                    Toast.makeText(getContext(),
                            "Client not connected to server ,Message not sent!",
                            Toast.LENGTH_LONG).show();
                }

                return true;
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMessageList();
    }

    @Override
    public void onDestroy() {
        cleanUpRosterListeners();
        super.onDestroy();
    }

    void cleanUpRosterListeners() {
        RosterManager.getInstance().removeOnMessageChangeListener(messageChangeListener);
    }

    private void updateMessageList() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                List<ChatMessage> chatMessageList = MessageRepository.getInstance().getMessages(contactJid);
                mChatView.clearMessages();
                mChatView.addMessages(new ArrayList<ChatMessage>(chatMessageList));
            }
        });
    }

    RosterManager.OnMessageChangeListener messageChangeListener = new RosterManager.OnMessageChangeListener() {
        @Override
        public void onMessageReceived(String fromJID, String newMessage, int totalCount) {
            Log.e(TAG, "OnMessageChangeListener: onMessageReceived: " + fromJID + " ---> " + newMessage + " ---> " + totalCount);
            updateMessageList();
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

}
