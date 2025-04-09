package kr.hhplus.be.server.domain.product;

import jakarta.persistence.Entity;

import java.time.LocalDateTime;

public record Product(
        Long id,
        String name,
        int price,
        int stock,
        long categoryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public Product decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("구매 수량은 0보다 커야 합니다.");
        }
        if (this.stock < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }
        return new Product(id, name, price, this.stock - quantity, categoryId, createdAt, LocalDateTime.now());
    }

    public Product increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("재입고 수량은 0보다 커야 합니다.");
        }
        return new Product(id, name, price, this.stock + quantity, categoryId, createdAt, LocalDateTime.now());
    }

    public int getStock() {
        return stock();
    }
}