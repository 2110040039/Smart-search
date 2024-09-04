import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

class TNode {
    boolean isEOW;
    TNode[] children;

    public TNode() {
        isEOW = false;
        children = new TNode[26];
    }
}

class Trie {
    TNode root = new TNode();

    void insertWord(String word) {
        TNode temp = root;
        for (char ch : word.toCharArray()) {
            if (ch < 'a' || ch > 'z') {
                continue; // Ignore non-lowercase alphabetic characters
            }
            int idx = ch - 'a';
            if (temp.children[idx] == null) {
                temp.children[idx] = new TNode();
            }
            temp = temp.children[idx];
        }
        temp.isEOW = true;
    }

    List<String> autoSuggest(String prefix) {
        TNode temp = root;
        for (char ch : prefix.toCharArray()) {
            if (ch < 'a' || ch > 'z') {
                return new ArrayList<>(); // No suggestions for invalid prefix
            }
            int idx = ch - 'a';
            if (temp.children[idx] == null) {
                return new ArrayList<>(); // No suggestions
            }
            temp = temp.children[idx];
        }
        List<String> words = new ArrayList<>();
        helper(temp, new StringBuilder(prefix), words);
        Collections.sort(words); // Sort suggestions alphabetically
        return words;
    }

    void helper(TNode root, StringBuilder prefix, List<String> words) {
        if (root.isEOW) {
            words.add(prefix.toString());
        }
        for (int i = 0; i < 26; i++) {
            if (root.children[i] != null) {
                prefix.append((char) (i + 'a'));
                helper(root.children[i], prefix, words);
                prefix.setLength(prefix.length() - 1); // Backtrack
            }
        }
    }
}

public class Main {
    private static JTextField textField;
    private static JList<String> suggestionList;
    private static Trie trie;
    private static int currentIndex = -1; // Current index in the suggestion list

    public static void main(String[] args) {
        trie = new Trie();
        try {
            File f = new File("t.txt");
            Scanner sc = new Scanner(f);
            while (sc.hasNext()) {
                String word = sc.next().toLowerCase();
                trie.insertWord(word);
            }
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Auto Suggest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        textField = new JTextField(20);
        suggestionList = new JList<>();
        JScrollPane scrollPane = new JScrollPane(suggestionList);

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateSuggestions();
            }

            public void removeUpdate(DocumentEvent e) {
                updateSuggestions();
            }

            public void changedUpdate(DocumentEvent e) {
                updateSuggestions();
            }
        });

        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int size = suggestionList.getModel().getSize(); // Get the number of suggestions

                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    // Move down in the list
                    if (currentIndex < size - 1) {
                        currentIndex++;
                        suggestionList.setSelectedIndex(currentIndex);
                        suggestionList.ensureIndexIsVisible(currentIndex);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    // Move up in the list
                    if (currentIndex > 0) {
                        currentIndex--;
                        suggestionList.setSelectedIndex(currentIndex);
                        suggestionList.ensureIndexIsVisible(currentIndex);
                    } else if (currentIndex == 0) {
                        // Edge case: No more suggestions to move up, reset to -1
                        currentIndex = -1;
                        suggestionList.clearSelection();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    // Auto-complete the word when Tab is pressed
                    if (currentIndex >= 0 && currentIndex < size) {
                        String selectedSuggestion = suggestionList.getSelectedValue();
                        if (selectedSuggestion != null) {
                            String[] words = textField.getText().split("\\s+");
                            words[words.length - 1] = selectedSuggestion;
                            textField.setText(String.join(" ", words) + " ");
                            textField.setCaretPosition(textField.getText().length()); // Move cursor to end of text
                            currentIndex = -1; // Reset index
                            updateSuggestions(); // Refresh suggestions
                        }
                    }
                    e.consume(); // Prevent the default tab behavior
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    // Complete the current word and start a new auto-suggest session
                    if (currentIndex >= 0 && currentIndex < size) {
                        String selectedSuggestion = suggestionList.getSelectedValue();
                        if (selectedSuggestion != null) {
                            String[] words = textField.getText().split("\\s+");
                            words[words.length - 1] = selectedSuggestion;
                            textField.setText(String.join(" ", words) + " ");
                            textField.setCaretPosition(textField.getText().length()); // Move cursor to end of text
                            currentIndex = -1; // Reset index
                            updateSuggestions(); // Refresh suggestions
                        }
                    }
                    e.consume(); // Prevent the default space behavior
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // Perform search when Enter is pressed
                    e.consume(); // Consume the event to prevent the default behavior
                    if (currentIndex >= 0 && currentIndex < size) {
                        String selectedSuggestion = suggestionList.getSelectedValue();
                        if (selectedSuggestion != null) {
                            performSearch(selectedSuggestion);
                        }
                    } else {
                        performSearch(textField.getText().trim());
                    }
                    suggestionList.clearSelection();
                }
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(textField, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }

    private static void updateSuggestions() {
        String input = textField.getText().toLowerCase();
        String[] words = input.split("\\s+");
        String lastWord = words[words.length - 1];

        if (lastWord.isEmpty()) {
            suggestionList.setListData(new String[0]);
        } else {
            List<String> suggestions = trie.autoSuggest(lastWord);
            String[] suggestionsArray = suggestions.toArray(new String[0]);
            suggestionList.setListData(suggestionsArray);
            suggestionList.setSelectedIndex(-1); // Deselect all suggestions
        }
    }

    private static void performSearch(String query) {
        if (query != null && !query.isEmpty()) {
            try {
                Desktop.getDesktop().browse(new URI("https://www.google.com/search?q=" + query.replace(" ", "+")));
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        }
    }
}
