package de.liebki.myollamaenhancer.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import de.liebki.myollamaenhancer.models.*;
import de.liebki.myollamaenhancer.events.KnowledgeBaseChangedTopic;
import de.liebki.myollamaenhancer.commentgenerator.*;
import de.liebki.myollamaenhancer.js.JavaScriptLanguageHandler;
import de.liebki.myollamaenhancer.php.PHPHandler;
import de.liebki.myollamaenhancer.ts.TypeScriptLanguageHandler;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum DuckDbService {
    ;

    private static Connection conn;
    private static String currentProjectName;

    private static synchronized Connection getConnection(final Project project) throws SQLException {
        try {
            // Explicitly load the DuckDB driver
            Class.forName("org.duckdb.DuckDBDriver");
        } catch (final ClassNotFoundException e) {
            throw new SQLException("Failed to load DuckDB driver. Make sure the DuckDB JDBC dependency is included in your project.", e);
        }
        
        final String projectName = project.getName().strip();
        if (null != conn && projectName.equals(DuckDbService.currentProjectName)) {
            return DuckDbService.conn;
        }
        DuckDbService.closeQuietly();

        final String basePath = project.getBasePath();
        final File projDir = (null != basePath && !basePath.isBlank())
                ? new File(basePath, ".myollamaenhancer/duckdb")
                : null;
        final File homeDir = new File(System.getProperty("user.home"), ".myollamaenhancer/duckdb/" + projectName);

        final File projDb = null != projDir ? new File(projDir, "knowledge_base.duckdb") : null;
        final File homeDb = new File(homeDir, "knowledge_base.duckdb");

        // Decide which DB file to use to avoid split state
        final File chosenDir;
        if (null != projDb && projDb.exists() && homeDb.exists()) {
            // Prefer the larger (likely populated) or newer file
            final long pSize = projDb.length();
            final long hSize = homeDb.length();
            if (pSize != hSize) {
                chosenDir = (pSize > hSize) ? projDir : homeDir;
            } else {
                chosenDir = (projDb.lastModified() >= homeDb.lastModified()) ? projDir : homeDir;
            }
        } else if (null != projDb && projDb.exists()) {
            chosenDir = projDir;
        } else if (homeDb.exists()) {
            chosenDir = homeDir;
        } else {
            // Neither exists; default to project dir when available, else home dir
            chosenDir = (null != projDir) ? projDir : homeDir;
        }

        if (!chosenDir.exists() && !chosenDir.mkdirs()) {
            System.err.println("[DuckDbService] WARNING: Failed to create DuckDB directory: " + chosenDir.getAbsolutePath());
        }
        final File chosenFile = new File(chosenDir, "knowledge_base.duckdb");
        final String dbPath = chosenFile.getAbsolutePath();
        System.out.println("[DuckDbService] Opening DB: " + dbPath +
                (chosenFile.exists() ? " (exists, size=" + chosenFile.length() + ")" : " (new)"));

        try {
            DuckDbService.conn = DriverManager.getConnection("jdbc:duckdb:" + dbPath);
        } catch (final SQLException openEx) {
            final String msg = String.valueOf(openEx.getMessage());
            // Handle WAL replay error for custom index types like HNSW
            if (msg.contains("WAL") && msg.contains("HNSW")) {
                System.err.println("[DuckDbService] WAL replay failed due to HNSW. Attempting to remove WAL and reopen: " + msg);
                final File wal = new File(dbPath + ".wal");
                if (wal.exists()) {
                    final File backup = new File(dbPath + ".wal.bak");
                    if (backup.exists()) backup.delete();
                    final boolean renamed = wal.renameTo(backup);
                    if (!renamed) {
                        // Fall back to delete if rename fails
                        if (!wal.delete()) {
                            System.err.println("[DuckDbService] WARNING: Failed to remove WAL file: " + wal.getAbsolutePath());
                        } else {
                            System.err.println("[DuckDbService] Deleted WAL file: " + wal.getAbsolutePath());
                        }
                    } else {
                        System.err.println("[DuckDbService] Renamed WAL to: " + backup.getAbsolutePath());
                    }
                }
                // Retry opening
                DuckDbService.conn = DriverManager.getConnection("jdbc:duckdb:" + dbPath);
            } else {
                throw openEx;
            }
        }
        DuckDbService.currentProjectName = projectName;
        DuckDbService.initializeSchema(DuckDbService.conn);
        return DuckDbService.conn;
    }

    private static void initializeSchema(final Connection c) {
        try (final Statement st = c.createStatement()) {
            // Sequences and base tables
            st.execute("CREATE SEQUENCE IF NOT EXISTS code_files_id_seq START 1");
            st.execute("CREATE SEQUENCE IF NOT EXISTS code_sections_id_seq START 1");

            st.execute("CREATE TABLE IF NOT EXISTS code_files (\n" +
                    "  id BIGINT PRIMARY KEY DEFAULT nextval('code_files_id_seq'),\n" +
                    "  fileName TEXT,\n" +
                    "  folder TEXT,\n" +
                    "  code TEXT,\n" +
                    "  enabled BOOLEAN DEFAULT TRUE\n" +
                    ")");
            st.execute("CREATE TABLE IF NOT EXISTS code_sections (\n" +
                    "  id BIGINT PRIMARY KEY DEFAULT nextval('code_sections_id_seq'),\n" +
                    "  fileName TEXT,\n" +
                    "  folder TEXT,\n" +
                    "  code TEXT,\n" +
                    "  enabled BOOLEAN DEFAULT TRUE\n" +
                    ")");
            // Reduce WAL dependence after init
            try { st.execute("CHECKPOINT"); } catch (final SQLException ignored) {}
        } catch (final SQLException e) {
            System.err.println("[DuckDbService] ERROR during initializeSchema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkpoint(final Connection c) {
        try (final Statement st = c.createStatement()) { st.execute("CHECKPOINT"); } catch (final SQLException ignored) {}
    }

    private static boolean columnExists(final Connection c, final String table, final String column) {
        final String sql = "SELECT 1 FROM information_schema.columns WHERE table_name = ? AND column_name = ? LIMIT 1";
        try (final PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (final ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (final SQLException e) {
            return false;
        }
    }

    private static void closeQuietly() {
        if (null != conn) {
            try {
                DuckDbService.checkpoint(DuckDbService.conn); } catch (final Exception ignored) {}
            try {
                DuckDbService.conn.close(); } catch (final Exception ignored) {}
            DuckDbService.conn = null;
        }
    }

    private static boolean handleWalHnswError(final SQLException e) {
        final String msg = String.valueOf(e.getMessage());
        if (!(msg.contains("WAL") && msg.contains("HNSW"))) return false;
        // Try to extract the WAL file path which is enclosed in quotes in DuckDB error
        final int start = msg.indexOf('"');
        final int end = (0 <= start) ? msg.indexOf('"', start + 1) : -1;
        if (0 <= start && end > start) {
            final String walPath = msg.substring(start + 1, end);
            final File wal = new File(walPath);
            try {
                if (wal.exists()) {
                    final File backup = new File(walPath + ".bak");
                    if (backup.exists()) backup.delete();
                    final boolean renamed = wal.renameTo(backup);
                    if (!renamed) {
                        if (!wal.delete()) {
                            System.err.println("[DuckDbService] WARNING: Failed to remove WAL file: " + wal.getAbsolutePath());
                        } else {
                            System.err.println("[DuckDbService] Deleted WAL file: " + wal.getAbsolutePath());
                        }
                    } else {
                        System.err.println("[DuckDbService] Renamed WAL to: " + backup.getAbsolutePath());
                    }
                }
            } catch (final Exception ex) {
                System.err.println("[DuckDbService] WARNING: Exception while handling WAL: " + ex.getMessage());
            }
            return true;
        }
        return false;
    }

    private static boolean handleWalMissingOnCommit(final SQLException e) {
        final String msg = String.valueOf(e.getMessage());
        // Example: Failed to commit: Cannot open file "...knowledge_base.duckdb.wal": No such file or directory
        if (!(msg.toLowerCase().contains("wal") && msg.toLowerCase().contains("no such file"))) return false;
        final int start = msg.indexOf('"');
        final int end = (0 <= start) ? msg.indexOf('"', start + 1) : -1;
        if (0 <= start && end > start) {
            final String walPath = msg.substring(start + 1, end);
            try {
                final File wal = new File(walPath);
                final File parent = wal.getParentFile();
                if (null != parent && !parent.exists()) parent.mkdirs();
                if (!wal.exists()) {
                    final boolean created = wal.createNewFile();
                    System.err.println("[DuckDbService] Created missing WAL file (commit): " + wal.getAbsolutePath() + ", ok=" + created);
                }
            } catch (final Exception ex) {
                System.err.println("[DuckDbService] WARNING: Could not create missing WAL file: " + ex.getMessage());
            }
            return true;
        }
        return false;
    }

    // Public API (DuckDB-backed)
    public static RetrievalResult getMatchingCodeFile(final String query, final Project project) throws IOException {
        final String q = null == query ? "" : query.toLowerCase();
        try {
            final Connection c = DuckDbService.getConnection(project);
            final String sql = "SELECT fileName, folder, code FROM code_files " +
                    "WHERE enabled = TRUE AND (LOWER(code) LIKE ? OR LOWER(fileName) LIKE ?) " +
                    "ORDER BY CASE WHEN LOWER(fileName) LIKE ? THEN 0 ELSE 1 END, LENGTH(code) ASC " +
                    "LIMIT 4";
            final List<CodeFile> results = new ArrayList<>();
            try (final PreparedStatement ps = c.prepareStatement(sql)) {
                final String pattern = "%" + q + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
                try (final ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final CodeFile cf = new CodeFile();
                        cf.fileName = rs.getString(1);
                        cf.folder = rs.getString(2);
                        cf.code = rs.getString(3);
                        results.add(cf);
                    }
                }
            }
            if (!results.isEmpty()) {
                return RetrievalResult.fromCodeFile(results.get(0));
            }
            return new RetrievalResult("", new ArrayList<>());
        } catch (final SQLException e) {
            System.err.println("[DuckDbService] getMatchingCodeFile failed: " + e.getMessage());
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    public static RetrievalResult getCodeFileContent(final String fileName, final Project project) {
        try {
            final Connection c = DuckDbService.getConnection(project);
            final String sql = "SELECT fileName, folder, code FROM code_files WHERE enabled = TRUE AND fileName = ? LIMIT 1";
            try (final PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, fileName);
                try (final ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        final CodeFile cf = new CodeFile();
                        cf.fileName = rs.getString(1);
                        cf.folder = rs.getString(2);
                        cf.code = rs.getString(3);
                        return RetrievalResult.fromCodeFile(cf);
                    }
                }
            }
            return new RetrievalResult("", new ArrayList<>());
        } catch (final SQLException e) {
            return new RetrievalResult("", new ArrayList<>());
        }
    }

    public static RetrievalResult getMatchingCodeSection(final String query, final Project project) throws IOException {
        final String q = null == query ? "" : query.toLowerCase();
        try {
            final Connection c = DuckDbService.getConnection(project);
            final String sql = "SELECT fileName, folder, code FROM code_sections " +
                    "WHERE enabled = TRUE AND (LOWER(code) LIKE ? OR LOWER(fileName) LIKE ?) " +
                    "ORDER BY CASE WHEN LOWER(fileName) LIKE ? THEN 0 ELSE 1 END, LENGTH(code) ASC " +
                    "LIMIT 4";
            final List<CodeSection> results = new ArrayList<>();
            try (final PreparedStatement ps = c.prepareStatement(sql)) {
                final String pattern = "%" + q + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
                try (final ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final CodeSection cs = new CodeSection();
                        cs.fileName = rs.getString(1);
                        cs.folder = rs.getString(2);
                        cs.code = rs.getString(3);
                        results.add(cs);
                    }
                }
            }
            if (results.isEmpty()) {
                return new RetrievalResult("", new ArrayList<>());
            }
            // For simplicity, return the best single match
            return RetrievalResult.fromCodeSection(results.get(0));
        } catch (final SQLException e) {
            throw new IOException(e);
        }
    }

    // Embedding utilities removed

    public static boolean isFileInKnowledgeBase(final Project project, final VirtualFile virtualFile) {
        if (null == project || null == virtualFile) return false;
        final String fileName = virtualFile.getName();
        final String folder = null != virtualFile.getParent() ? virtualFile.getParent().getPath() : "";
        try {
            final Connection c = DuckDbService.getConnection(project);
            final String sql = "SELECT 1 FROM code_files WHERE fileName = ? AND folder = ? LIMIT 1";
            try (final PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, fileName);
                ps.setString(2, folder);
                try (final ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (final SQLException e) {
            System.err.println("[DuckDbService] isFileInKnowledgeBase error for fileName=" + fileName + ", folder=" + folder + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static void deleteEntriesForFile(final Project project, final VirtualFile virtualFile) {
        final String fileName = virtualFile.getName();
        final String folder = null != virtualFile.getParent() ? virtualFile.getParent().getPath() : "";
        try {
            final Connection c = DuckDbService.getConnection(project);
            try (final PreparedStatement ps1 = c.prepareStatement("DELETE FROM code_files WHERE fileName=? AND folder=?");
                 final PreparedStatement ps2 = c.prepareStatement("DELETE FROM code_sections WHERE fileName=? AND folder=?")) {
                ps1.setString(1, fileName);
                ps1.setString(2, folder);
                final int cf = ps1.executeUpdate();
                ps2.setString(1, fileName);
                ps2.setString(2, folder);
                final int cs = ps2.executeUpdate();
                System.out.println("[DuckDbService] deleteEntriesForFile fileName=" + fileName + ", folder=" + folder + ": code_files=" + cf + ", code_sections=" + cs);
            }
        } catch (final SQLException ex) {
            System.err.println("[DuckDbService] deleteEntriesForFile error for fileName=" + fileName + ", folder=" + folder + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void reembedIfPresent(final Project project, final VirtualFile virtualFile, final String currentContent)
            throws IOException {
        if (null == project || null == virtualFile) return;
        if (!FileValidationUtils.isValidFile(virtualFile)) return;
        if (!DuckDbService.isFileInKnowledgeBase(project, virtualFile)) return;
        DuckDbService.deleteEntriesForFile(project, virtualFile);
        DuckDbService.addFileToKnowledgeBase(project, virtualFile, currentContent);
        DuckDbService.addMethodsToKnowledgeBase(project, virtualFile, currentContent);
    }

    // Public methods for adding entries (used by actions)
    public static void addFileToKnowledgeBase(final Project project, final VirtualFile virtualFile, final String currentContent)
            throws IOException {
        if (!FileValidationUtils.isValidFile(virtualFile)) return;
        final String fileContent = null != currentContent ? currentContent : new String(virtualFile.contentsToByteArray(), virtualFile.getCharset());
        final String folder = null != virtualFile.getParent() ? virtualFile.getParent().getPath() : "";
        for (int attempt = 0; 2 > attempt; attempt++) {
            try {
                final Connection c = DuckDbService.getConnection(project);
                final String sql = "INSERT INTO code_files(fileName, folder, code, enabled) VALUES(?,?,?,?)";
                try (final PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, virtualFile.getName());
                    ps.setString(2, folder);
                    ps.setString(3, fileContent);
                    ps.setBoolean(4, true);
                    ps.executeUpdate();
                }
                DuckDbService.checkpoint(c);
                break; // success
            } catch (final SQLException e) {
                if (DuckDbService.handleWalMissingOnCommit(e)) {
                    DuckDbService.closeQuietly();
                    continue; // retry once
                }
                throw new IOException(e);
            }
        }
    }

    public static void addMethodsToKnowledgeBase(final Project project, final VirtualFile virtualFile, final String currentContent)
            throws IOException {
        if (!FileValidationUtils.isValidFile(virtualFile)) return;
        final String fileExtension = virtualFile.getExtension();
        final String fileContent = null != currentContent ? currentContent : new String(virtualFile.contentsToByteArray(), virtualFile.getCharset());
        final LanguageHandler handler = DuckDbService.findLanguageHandler(fileExtension);
        if (null == handler) return;
        final List<MethodInfo> methods = handler.findMethods(fileContent);
        final String folder = null != virtualFile.getParent() ? virtualFile.getParent().getPath() : "";
        for (int attempt = 0; 2 > attempt; attempt++) {
            try {
                final Connection c = DuckDbService.getConnection(project);
                for (final MethodInfo method : methods) {
                    final String sql = "INSERT INTO code_sections(fileName, folder, code, enabled) VALUES(?,?,?,?)";
                    try (final PreparedStatement ps = c.prepareStatement(sql)) {
                        ps.setString(1, virtualFile.getName());
                        ps.setString(2, folder);
                        ps.setString(3, method.methodSource);
                        ps.setBoolean(4, true);
                        ps.executeUpdate();
                    }
                }
                DuckDbService.checkpoint(c);
                break; // success
            } catch (final SQLException e) {
                if (DuckDbService.handleWalMissingOnCommit(e)) {
                    DuckDbService.closeQuietly();
                    continue; // retry once
                }
                throw new IOException(e);
            }
        }
    }

    // Removed deprecated embedding-based insertion methods.

    private static LanguageHandler findLanguageHandler(final String fileExtension) {
        if (null == fileExtension) return null;
        return Arrays.asList(
                new JavaLanguageHandler(),
                new PythonLanguageHandler(),
                new CSharpLanguageHandler(),
                new JavaScriptLanguageHandler(),
                new TypeScriptLanguageHandler(),
                new SvelteLanguageHandler(),
                new VueLanguageHandler(),
                new GoLanguageHandler(),
                new RustLanguageHandler(),
                new PHPHandler(),
                new CxxLanguageHandler()
        ).stream().filter(h -> h.isApplicable(fileExtension)).findFirst().orElse(null);
    }

    public static List<CodeFile> getAllCodeFiles(final Project project) {
        final List<CodeFile> list = new ArrayList<>();
        for (int attempt = 0; 2 > attempt; attempt++) {
            try {
                final Connection c = DuckDbService.getConnection(project);
                final String sql = "SELECT id, fileName, folder, code, enabled FROM code_files ORDER BY id";
                try (final Statement st = c.createStatement(); final ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) {
                        final CodeFile cf = new CodeFile();
                        cf.id = rs.getLong(1);
                        cf.fileName = rs.getString(2);
                        cf.folder = rs.getString(3);
                        cf.code = rs.getString(4);
                        cf.enabled = rs.getBoolean(5);
                        list.add(cf);
                    }
                }
                break; // success
            } catch (final SQLException e) {
                System.err.println("[DuckDbService] getAllCodeFiles primary query failed: " + e.getMessage());
                if (DuckDbService.handleWalHnswError(e)) {
                    DuckDbService.closeQuietly();
                    continue; // retry once after cleaning WAL
                }
                // No further fallback
            }
        }
        return list;
    }

    public static List<CodeSection> getAllCodeSections(final Project project) {
        final List<CodeSection> list = new ArrayList<>();
        for (int attempt = 0; 2 > attempt; attempt++) {
            try {
                final Connection c = DuckDbService.getConnection(project);
                final String sql = "SELECT id, fileName, folder, code, enabled FROM code_sections ORDER BY id";
                try (final Statement st = c.createStatement(); final ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) {
                        final CodeSection cs = new CodeSection();
                        cs.id = rs.getLong(1);
                        cs.fileName = rs.getString(2);
                        cs.folder = rs.getString(3);
                        cs.code = rs.getString(4);
                        cs.enabled = rs.getBoolean(5);
                        list.add(cs);
                    }
                }
                break; // success
            } catch (final SQLException e) {
                System.err.println("[DuckDbService] getAllCodeSections primary query failed: " + e.getMessage());
                if (DuckDbService.handleWalHnswError(e)) {
                    DuckDbService.closeQuietly();
                    continue; // retry once after cleaning WAL
                }
                // No further fallback
            }
        }
        return list;
    }
    

    public static void setCodeFileEnabled(final long id, final boolean enabled, final Project project) {
        for (int attempt = 0; 2 > attempt; attempt++) {
            try {
                final Connection c = DuckDbService.getConnection(project);
                try (final PreparedStatement ps = c.prepareStatement("UPDATE code_files SET enabled=? WHERE id=?")) {
                    ps.setBoolean(1, enabled);
                    ps.setLong(2, id);
                    final int updated = ps.executeUpdate();
                    System.out.println("[DuckDbService] setCodeFileEnabled(id=" + id + ", enabled=" + enabled + ", attempt=" + (attempt+1) + ") -> rows=" + updated);
                }
                DuckDbService.checkpoint(c);
                break; // success
            } catch (final SQLException e) {
                if (DuckDbService.handleWalMissingOnCommit(e)) {
                    DuckDbService.closeQuietly();
                    continue; // retry once
                }
                System.err.println("[DuckDbService] setCodeFileEnabled failed: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
    }

    public static void setCodeSectionEnabled(final long id, final boolean enabled, final Project project) {
        for (int attempt = 0; 2 > attempt; attempt++) {
            try {
                final Connection c = DuckDbService.getConnection(project);
                try (final PreparedStatement ps = c.prepareStatement("UPDATE code_sections SET enabled=? WHERE id=?")) {
                    ps.setBoolean(1, enabled);
                    ps.setLong(2, id);
                    final int updated = ps.executeUpdate();
                    System.out.println("[DuckDbService] setCodeSectionEnabled(id=" + id + ", enabled=" + enabled + ", attempt=" + (attempt+1) + ") -> rows=" + updated);
                }
                DuckDbService.checkpoint(c);
                break; // success
            } catch (final SQLException e) {
                if (DuckDbService.handleWalMissingOnCommit(e)) {
                    DuckDbService.closeQuietly();
                    continue; // retry once
                }
                System.err.println("[DuckDbService] setCodeSectionEnabled failed: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
    }

    public static void deleteCodeFile(final long id, final Project project) {
        for (int attempt = 0; 2 > attempt; attempt++) {
            try {
                final Connection c = DuckDbService.getConnection(project);
                try (final PreparedStatement ps = c.prepareStatement("DELETE FROM code_files WHERE id=?")) {
                    ps.setLong(1, id);
                    final int updated = ps.executeUpdate();
                    System.out.println("[DuckDbService] deleteCodeFile(id=" + id + ", attempt=" + (attempt+1) + ") -> rows=" + updated);
                }
                DuckDbService.checkpoint(c);
                break; // success
            } catch (final SQLException e) {
                if (DuckDbService.handleWalMissingOnCommit(e)) {
                    DuckDbService.closeQuietly();
                    continue; // retry once
                }
                System.err.println("[DuckDbService] deleteCodeFile failed: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
    }

    public static void deleteCodeSection(final long id, final Project project) {
        for (int attempt = 0; 2 > attempt; attempt++) {
            try {
                final Connection c = DuckDbService.getConnection(project);
                try (final PreparedStatement ps = c.prepareStatement("DELETE FROM code_sections WHERE id=?")) {
                    ps.setLong(1, id);
                    final int updated = ps.executeUpdate();
                    System.out.println("[DuckDbService] deleteCodeSection(id=" + id + ", attempt=" + (attempt+1) + ") -> rows=" + updated);
                }
                DuckDbService.checkpoint(c);
                break; // success
            } catch (final SQLException e) {
                if (DuckDbService.handleWalMissingOnCommit(e)) {
                    DuckDbService.closeQuietly();
                    continue; // retry once
                }
                System.err.println("[DuckDbService] deleteCodeSection failed: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
    }

    public static void deleteKnowledgeBase(final Project project, final Component parent) {
        final int result = JOptionPane.showConfirmDialog(
                parent,
                "Are you sure you want to delete all knowledge base entries? This cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (JOptionPane.YES_OPTION == result) {
            try {
                if (null != project) {
                    DuckDbService.closeQuietly();
                    // delete possible DB files in both locations (project dir and user-home fallback)
                    final String basePath = project.getBasePath();
                    final File projDir = (null != basePath && !basePath.isBlank())
                            ? new File(basePath, ".MyOllamaEnhancer/duckdb") : null;
                    final File homeDir = new File(System.getProperty("user.home"), ".MyOllamaEnhancer/duckdb/" + project.getName().strip());
                    final File projDb = null != projDir ? new File(projDir, "knowledge_base.duckdb") : null;
                    final File homeDb = new File(homeDir, "knowledge_base.duckdb");
                    if (null != projDb && projDb.exists() && !projDb.delete()) {
                        System.err.println("[DuckDbService] WARNING: Failed to delete DB file: " + projDb.getAbsolutePath());
                    }
                    if (homeDb.exists() && !homeDb.delete()) {
                        System.err.println("[DuckDbService] WARNING: Failed to delete DB file: " + homeDb.getAbsolutePath());
                    }
                    JOptionPane.showMessageDialog(null, "Knowledge base deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    // Notify listeners (e.g., ChatToolWindow) to refresh context after deletion
                    try {
                        ApplicationManager.getApplication().getMessageBus()
                                .syncPublisher(KnowledgeBaseChangedTopic.TOPIC)
                                .knowledgeBaseChanged(project);
                    } catch (final Throwable ignored) { }
                } else {
                    JOptionPane.showMessageDialog(null, "No project found for this settings window.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (final Exception ex) {
                JOptionPane.showMessageDialog(null, "Failed to delete knowledge base: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
