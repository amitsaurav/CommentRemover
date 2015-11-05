package com.github.amitsaurav.commentremover;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility class to strip comments from a given C/C++ code file
 * 
 * Usage:
 *     CommentRemover.removeComments("&lt;Input File&gt;", "&lt;Output File&gt;");
 * 
 * @author Amit Saurav (amitsaurav@gmail.com)
 *
 */
public class CommentRemover {

    public static final char SINGLE_QUOTE = '\'';
    public static final char DOUBLE_QUOTE = '"';
    public static final char COMMENT_MARKER = '/';
    public static final char ESCAPE = '\\';
    public static final char MULTI_LINE_COMMENT = '*';
    public static final char EOL_UNIX = '\n';
    public static final char EOL_MAC = '\r';
    public static final int EOF = -1;

    /**
     * Read an input C/C++ file and generate an output file without comments
     * 
     * @param inputFilename Input file with full path.
     * @param outputFilename Output file with full path.
     */
    public static void removeComments(String inputFilename, String outputFilename) {
        File inputFile = new File(inputFilename);
        File outputFile = new File(outputFilename);

        // try-with-resource automatically handles file handle cleanup 
        try (FileReader reader = new FileReader(inputFile);
             FileWriter writer = new FileWriter(outputFile)) {
            
            for (int character = reader.read(); character != EOF; character = reader.read()) {
                switch (character) {
                case SINGLE_QUOTE:
                case DOUBLE_QUOTE:
                    handleQuotedStrings(reader, writer, character);
                    break;

                case COMMENT_MARKER:
                    // A single slash makes it a candidate for comment evaluation.
                    // If the next character is also a COMMENT_MARKER, that qualifies it as a 
                    // single line comment. If the next character is a MULTI_LINE_COMMENT, that
                    // qualifies if as a multi-line comment.
                    handleComments(reader, writer, character);
                    break;

                default:
                    writer.write(character);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleQuotedStrings(FileReader reader, FileWriter writer, int character)
            throws IOException {
        int startQuote = character;
        int prevCharacter = 0;

        do {
            // Need to store the previously read character to determine if a quote is the correct
            // end quote or is just escaped (\') in a statement.
            prevCharacter = character;
            writer.write(character);
            character = reader.read();
        } while (character != EOF &&
                character != startQuote ||
                prevCharacter == ESCAPE);

        if (character != EOF) {
            // If the previous loop ended with a quote, we didn't get a chance to write it.
            writer.write(character);
        }
    }

    private static void handleComments(FileReader reader, FileWriter writer, int character)
            throws IOException {
        int prevCharacter = character;
        character = reader.read();

        switch (character) {
        case COMMENT_MARKER:
            do {
                character = reader.read();
            } while (character != EOF &&
                    character != EOL_MAC &&
                    character != EOL_UNIX); // Assuming windows EOL \r\n will match one of these.
            break;

        case MULTI_LINE_COMMENT:
            do {
                // Need to store the previously read character to determine if it was '*' if the
                // current character is a slash (/).
                prevCharacter = character;
                character = reader.read();
            } while (character != -1 &&
                    character != COMMENT_MARKER ||
                    prevCharacter != MULTI_LINE_COMMENT);
            break;

        default:
            writer.write(prevCharacter);
            writer.write(character);
        }
    }

    public static void main(String[] args) {
        CommentRemover.removeComments("/tmp/test.c.txt", "/tmp/test.c.new");
    }
}
