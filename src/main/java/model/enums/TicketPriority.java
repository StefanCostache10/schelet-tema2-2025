package model.enums;

public enum TicketPriority {
    LOW, MEDIUM, HIGH, CRITICAL;

    public TicketPriority next() {
        int nextIndex = Math.min(this.ordinal() + 1, TicketPriority.values().length - 1);
        return TicketPriority.values()[nextIndex];
    }
}
