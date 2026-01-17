package model.ticket;

public final class Comment {
    private String author;
    private String content;
    private String createdAt;

    public Comment() {
    }

    public Comment(final String author, final String content, final String createdAt) {
        this.author = author;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
