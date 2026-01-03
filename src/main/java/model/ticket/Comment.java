package model.ticket;

public class Comment {
    private String author;
    private String content;
    private String createdAt;

    public Comment() {}
    public Comment(String author, String content, String createdAt) {
        this.author = author; this.content = content; this.createdAt = createdAt;
    }

    // Getters and Setters (necesari pentru Jackson)
    public String getAuthor() { return author; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
}