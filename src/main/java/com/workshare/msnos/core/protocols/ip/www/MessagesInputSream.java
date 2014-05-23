package com.workshare.msnos.core.protocols.ip.www;

import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;

import com.workshare.msnos.core.Message;
import com.workshare.msnos.core.serializers.WireSerializer;

class MessagesInputSream extends InputStream {
    
    private Queue<Message> messages;
    private String current;
    private int charIndex;
    private WireSerializer serializer;
    
    public MessagesInputSream(WireSerializer serializer, Queue<Message> messages) {
        this.serializer = serializer;
        this.messages = messages;
        loadNextMessage();
    }

    private boolean loadNextMessage() {
        Message message = messages.poll();
        if (message == null)
            return false;

        current = serializer.toText(message);
        charIndex = 0;
        return true;
    }

    @Override
    public int read() throws IOException {
        if (messages == null)
            return -1;
        else if (charIndex == current.length()) {
            if (!loadNextMessage()) 
                messages = null;
            
            return (int) '\n';
        } 
        else
            return (int)current.charAt(charIndex++);
    }
}