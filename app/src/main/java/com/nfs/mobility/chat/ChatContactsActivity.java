package com.nfs.mobility.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.allyants.notifyme.NotifyMe;
import com.blikoon.roster.R;
import com.mobility.chat.xmpp.ContactRepository;
import com.mobility.chat.xmpp.RosterManager;
import com.mobility.chat.xmpp.model.Contact;

import org.jxmpp.jid.Jid;

import java.util.Collection;
import java.util.List;

/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ChatDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ChatContactsActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */

    private static final String TAG = ChatContactsActivity.class.getSimpleName();
    NotifyMe.Builder notifyMe;
    RosterManager.OnMessageChangeListener messageChangeListener = new RosterManager.OnMessageChangeListener() {
        @Override
        public void onMessageReceived(String fromJID, String newMessage, int totalCount) {
            showNotifications(fromJID, newMessage);
        }

        @Override
        public void onMessageSent(String toJID, String newMessage, int totalCount, boolean success) {

        }

        @Override
        public void onMessageDeleted(String jid, String message, int totalCount) {

        }
    };
    private boolean mTwoPane;
    ContactListActionListener listActionListener = new ContactListActionListener() {
        @Override
        public void startChatWithJidof(final String Jid) {

            if (mTwoPane) {
                Bundle arguments = new Bundle();
                arguments.putString(ChatMessagesFragment.ARG_ITEM_JID, Jid);
                ChatMessagesFragment fragment = new ChatMessagesFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment)
                        .commit();
            } else {
//                Context context = getApplicationContext();//TODO
//                Intent intent = new Intent(context, ChatDetailActivity.class);//TODO
//                intent.putExtra(ChatMessagesFragment.ARG_ITEM_JID, Jid);//TODO
////TODO
//                context.startActivity(intent);//TODO
            }

        }
    };
    private RecyclerView contactsRecyclerView;
    private ContactAdapter mAdapter;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

        notifyMe = new NotifyMe.Builder(getApplicationContext());

        contactsRecyclerView = (RecyclerView) findViewById(R.id.contact_list_online_recycler_view);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;//TODO
        } else {
            mTwoPane = true;//TODO
        }

        ContactRepository model = ContactRepository.getInstance();
        List<Contact> contacts = model.getContacts();

        mAdapter = new ContactAdapter(contacts, listActionListener);
        contactsRecyclerView.setAdapter(mAdapter);

        notifyMe = new NotifyMe.Builder(getApplicationContext());

        RosterManager.getInstance().addOnRosterChangeListener(rosterUpdatesListener);
        RosterManager.getInstance().addOnMessageChangeListener(messageChangeListener);

    }

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
            RosterManager.getInstance().disconnectUser();

            //Finish this activity
            finish();

            //Start login activity for user to login
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);

        }

        return super.onOptionsItemSelected(item);
    }

    //region RosterManager

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        cleanUpRosterListeners();
        RosterManager.getInstance().disconnectUser();
        super.onDestroy();
    }

    void cleanUpRosterListeners() {
        RosterManager.getInstance().removeOnRosterChangeListener(rosterUpdatesListener);
        RosterManager.getInstance().removeOnMessageChangeListener(messageChangeListener);
    }

    @UiThread
    private void updateContactListView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.update(ContactRepository.getInstance().getContacts());
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


        notifyMe.title(title);
        notifyMe.content(content);
//        notifyMe.color(red, green, blue, alpha);//Color of notification header
//        notifyMe.led_color(red, green, blue, alpha);//Color of LED when notification pops up
//        notifyMe.time(Calendar.getInstance().getTime());//The time to popup notification
        notifyMe.delay(1000);//Delay in ms
        notifyMe.key("new_message");
//        notifyMe.large_icon(Int resource);
//        notifyMe.addAction(Intent intent, text); //The action will call the intent when pressed
        notifyMe.build();
    }

    //endregion
}
