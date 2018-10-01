package com.nfs.mobility.chat;

import android.content.Context;

import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ContactRepository {

    private static ContactRepository sContactRepo;
    private List<Contact> mContacts = new ArrayList<>();

    public static ContactRepository getInstance()
    {
        if(sContactRepo == null)
        {
            sContactRepo = new ContactRepository();
        }
        return sContactRepo;
    }

    private ContactRepository()
    {
//        mContacts = new ArrayList<>();
       // populateWithInitialContacts(context);

    }

    private void populateWithInitialContacts(Context context)
    {
        //Create the Foods and add them to the list;


//        Contact contact1 = new Contact("testuser2@mobi020.netsolpk.com");
//        mContacts.add(contact1);

    }

    public void setContacts(List<Contact> mContacts) {
        this.mContacts.clear();
        this.mContacts.addAll(mContacts);
    }

    synchronized
    public List<Contact> getContacts()
    {
        Collections.sort(mContacts, new Comparator<Contact>() {
            @Override
            public int compare(Contact contact, Contact t1) {
                return contact.getUserName().compareToIgnoreCase(t1.getUserName());
            }
        });
        return mContacts;
    }

    public Contact getContact(String jid){
        for(Contact contact : mContacts){
            if(contact.getJid().equals(jid)){
                return contact;
            }
        }
        return null;
    }

    public void setStatus(String jid, String status) {
        for (Contact contact : mContacts) {
            if (contact.getJid().equals(jid)) {
                contact.setStatus(status);
            }
        }
        sortContacts();
    }

    private void sortContacts(){

    }

    public void addContact(Contact contact) {
        mContacts.add(contact);
    }

    public void addContacts(Collection<Jid> addresses) {
//        mContacts.add(addresses);
    }

    public void removeContact(Contact contact) {
        mContacts.remove(contact);
    }

    public void removeContact(Collection<Jid> addresses) {
//        mContacts.remove(addresses);
    }

    public void updateContact(Contact updatedContact) {
        for (Contact contact : mContacts) {
            if (contact.getJid().equals(updatedContact.getJid())) {
                contact.setStatus(updatedContact.getStatus());
            }
        }
    }

    public void updateContacts(Collection<Jid> addresses) {
//        mContacts.add(addresses);
    }

    public void cleanUpContacts() {
        mContacts.clear();
    }
}
