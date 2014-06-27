package com.workshare.msnos.integration;

import java.util.UUID;

import com.workshare.msnos.core.payloads.GenericPayload;
import com.workshare.msnos.core.payloads.PayloadAdapter;
import com.workshare.msnos.soup.json.Json;

public interface IntegrationTestFrame {
    public static final UUID MASTER_CLOUD_UUID = new UUID(000, 987);

    public static final UUID GENERIC_CLOUD_UUID = new UUID(987, 987);
    public static final UUID SECURE_CLOUD_UUID = new UUID(987, 345);
    
    public static class CommandPayload extends PayloadAdapter {

        public enum Command {
            AGENT_JOIN, 
            AGENT_LEAVE, 
            SELF_KILL
        }

        private final Command command;
        
        private CommandPayload(Command command) {
            super();
            this.command = command;
        }

        public Command getCommand() {
            return command;
        }
        
        public static CommandPayload create(Command command) {
            return new CommandPayload(command);
        }

        public static CommandPayload create(GenericPayload payload) {
            return Json.fromJsonTree(payload.getData(), CommandPayload.class);
        }
    }
    
}
