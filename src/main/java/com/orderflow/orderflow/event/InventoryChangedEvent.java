package com.orderflow.orderflow.event;
import java.util.Set;
public record InventoryChangedEvent(Set<Long> productIds) {}
