package de.liebki.myollamaenhancer.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import de.liebki.myollamaenhancer.types.OllamaOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "de.liebki.myollamaenhancer.configuration.MyOllamaEnhancerSettingsService",
        storages = @Storage("MyOllamaEnhancerSettings.xml")
)
public class MyOllamaEnhancerSettingsService implements PersistentStateComponent<MyOllamaEnhancerSettingsService.State> {
    public static final String TITLE = "MyOllamaEnhancer";

    public static class State {
        public String apiEndpoint = "http://localhost:11434/";
        public String ollamaModel = "llama3:8b-instruct-q6_K";
        public int apiTimeout = 120;
        public String promptReadability = OllamaOption.READABILITY.getPrompt();
        public String promptBugs = OllamaOption.BUGS.getPrompt();
        public String promptPerformance = OllamaOption.PERFORMANCE.getPrompt();
        public String promptUnitTests = OllamaOption.UNIT_TESTS.getPrompt();
        public String promptSimplify = OllamaOption.SIMPLIFY.getPrompt();
        public String promptCommentCode = OllamaOption.COMMENT_CODE.getPrompt();
        public String promptFixBroken = OllamaOption.FIX_BROKEN.getPrompt();
        public String promptExplainLogic = "This is a {language} source file.\nExplain the main logic and reasoning of this file for a developer.\nDescribe the key algorithms, control flow, and how the code achieves its goals, highlighting important steps and decisions.\nFocus on how the code works, not just its structure.\nDo not use markdown, symbols, or formatting—just plain text.\nHere is the code:\n{code}";
        public String promptExplainStructure = "This is a {language} source file.\nDescribe the structure and organization of this file for a developer.\nList the main components (such as classes, functions, modules), their relationships, and how data flows between them.\nFocus on how the code is organized, not what it does in detail.\nDo not use markdown, symbols, or formatting—just plain text.\nHere is the code:\n{code}";
        public String promptExplainPurpose = "This is a {language} source file.\nSummarize the purpose and intent of this file.\nExplain what the file is for and its main responsibilities in a few sentences.\nDo not use markdown, symbols, or formatting—just plain text.\nHere is the code:\n{code}";

    }

    private State state = new State();

    public static MyOllamaEnhancerSettingsService getInstance() {
        return ApplicationManager.getApplication().getService(MyOllamaEnhancerSettingsService.class);
    }

    @Nullable
    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public void loadState(@NotNull final State state) {
        this.state = state;
    }

    public String getApiEndpoint() { return this.state.apiEndpoint; }
    public void setApiEndpoint(final String value) {
        this.state.apiEndpoint = value; }

    public String getOllamaModel() { return this.state.ollamaModel; }
    public void setOllamaModel(final String value) {
        this.state.ollamaModel = value; }

    public int getApiTimeout() { return this.state.apiTimeout; }
    public void setApiTimeout(final int value) {
        this.state.apiTimeout = value; }

    public String getPromptReadability() { return this.state.promptReadability; }
    public void setPromptReadability(final String value) {
        this.state.promptReadability = value; }

    public String getPromptBugs() { return this.state.promptBugs; }
    public void setPromptBugs(final String value) {
        this.state.promptBugs = value; }

    public String getPromptPerformance() { return this.state.promptPerformance; }
    public void setPromptPerformance(final String value) {
        this.state.promptPerformance = value; }

    public String getPromptUnitTests() { return this.state.promptUnitTests; }
    public void setPromptUnitTests(final String value) {
        this.state.promptUnitTests = value; }

    public String getPromptSimplify() { return this.state.promptSimplify; }
    public void setPromptSimplify(final String value) {
        this.state.promptSimplify = value; }

    public String getPromptCommentCode() { return this.state.promptCommentCode; }
    public void setPromptCommentCode(final String value) {
        this.state.promptCommentCode = value; }

    public String getPromptFixBroken() { return this.state.promptFixBroken; }
    public void setPromptFixBroken(final String value) {
        this.state.promptFixBroken = value; }

    public String getPromptExplainLogic() { return this.state.promptExplainLogic; }
    public void setPromptExplainLogic(final String value) {
        this.state.promptExplainLogic = value; }

    public String getPromptExplainStructure() { return this.state.promptExplainStructure; }
    public void setPromptExplainStructure(final String value) {
        this.state.promptExplainStructure = value; }

    public String getPromptExplainPurpose() { return this.state.promptExplainPurpose; }
    public void setPromptExplainPurpose(final String value) {
        this.state.promptExplainPurpose = value; }
} 