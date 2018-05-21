package pl.lodz.p.it.masi.stp.chatbot.collections;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "logs")
public class ConversationLog {

    private String watsonConversationId;
    private ConversationEndStatusEnum endStatus;
    private List<MessageLog> messagesLogs;

    public ConversationLog(ConversationEndStatusEnum endStatus, List<MessageLog> messagesLogs) {
        this.endStatus = endStatus;
        this.messagesLogs = messagesLogs;
    }

    public ConversationLog() {

    }

    public ConversationEndStatusEnum getEndStatus() {
        return endStatus;
    }

    public void setEndStatus(ConversationEndStatusEnum endStatus) {
        this.endStatus = endStatus;
    }

    public List<MessageLog> getMessagesLogs() {
        return messagesLogs;
    }

    public void setMessagesLogs(List<MessageLog> messagesLogs) {
        this.messagesLogs = messagesLogs;
    }

    public String getWatsonConversationId() {
        return watsonConversationId;
    }

    public void setWatsonConversationId(String watsonConversationId) {
        this.watsonConversationId = watsonConversationId;
    }
}
