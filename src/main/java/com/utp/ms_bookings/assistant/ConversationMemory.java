package com.utp.ms_bookings.assistant;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memoria de conversación en RAM (por correo + conversationId).
 * Guarda solo el texto final de cada turno, con tope de mensajes y expiración.
 */
@Component
public class ConversationMemory {

    private static final int MAX_MESSAGES = 12;          // 6 idas y vueltas
    private static final int MAX_CONVERSATIONS = 500;
    private static final Duration TTL = Duration.ofMinutes(30);

    private record Turn(boolean fromUser, String text) {}

    private static final class Conversation {
        private final Deque<Turn> turns = new ArrayDeque<>();
        private volatile Instant lastUsed = Instant.now();
    }

    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();

    public List<Message> history(String key) {
        Conversation c = conversations.get(key);
        if (c == null) return List.of();

        synchronized (c) {
            if (c.lastUsed.isBefore(Instant.now().minus(TTL))) {
                conversations.remove(key, c);
                return List.of();
            }
            List<Message> out = new ArrayList<>();
            for (Turn t : c.turns) {
                out.add(t.fromUser() ? new UserMessage(t.text()) : new AssistantMessage(t.text()));
            }
            return out;
        }
    }

    public void append(String key, String userText, String assistantText) {
        evictStale();
        Conversation c = conversations.computeIfAbsent(key, k -> new Conversation());
        synchronized (c) {
            c.turns.addLast(new Turn(true, userText));
            c.turns.addLast(new Turn(false, assistantText));
            while (c.turns.size() > MAX_MESSAGES) c.turns.removeFirst();
            c.lastUsed = Instant.now();
        }
    }

    private void evictStale() {
        Instant limit = Instant.now().minus(TTL);
        conversations.entrySet().removeIf(e -> e.getValue().lastUsed.isBefore(limit));

        if (conversations.size() > MAX_CONVERSATIONS) {
            conversations.entrySet().stream()
                    .min(Comparator.comparing(e -> e.getValue().lastUsed))
                    .ifPresent(e -> conversations.remove(e.getKey()));
        }
    }
}