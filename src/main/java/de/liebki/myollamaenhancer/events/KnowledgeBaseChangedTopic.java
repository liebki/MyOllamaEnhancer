package de.liebki.myollamaenhancer.events;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;

public interface KnowledgeBaseChangedTopic {
    Topic<KnowledgeBaseChangedTopic> TOPIC = Topic.create("MOEP.KnowledgeBaseChanged", KnowledgeBaseChangedTopic.class);
    void knowledgeBaseChanged(Project project);
}
