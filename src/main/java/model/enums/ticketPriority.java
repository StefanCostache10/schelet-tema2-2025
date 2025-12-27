// model/enums/ticketPriority.java
package model.enums;

public enum ticketPriority {
    LOW, MEDIUM, HIGH, CRITICAL;

    public ticketPriority next() {
        int nextIndex = Math.min(this.ordinal() + 1, ticketPriority.values().length - 1);
        return ticketPriority.values()[nextIndex];
    }
}