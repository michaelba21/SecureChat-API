

package com.securechat.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderDto → Full coverage of equals() & hashCode()") // Test class description
class OrderDtoTest {

    // ────────────────────────────────────────────────
    //  equals(Object) – all branches
    //  Tests for equals() method with full branch coverage
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("equals → same object → true")
    void equals_sameInstance_returnsTrue() {
        // Arrange: create OrderDto instance
        OrderDto dto = new OrderDto();
        dto.productname = "Laptop";
        dto.unitprice = 1299.99;
        dto.quantity = 1;

        // Act & Assert: object should equal itself
        //noinspection EqualsWithItself // IDE warning suppression
        assertThat(dto.equals(dto)).isTrue(); // Reflexive property
    }

    @Test
    @DisplayName("equals → null → false")
    void equals_null_returnsFalse() {
        // Arrange: create OrderDto
        OrderDto dto = new OrderDto();
        
        // Act & Assert: equals with null should return false
        assertThat(dto.equals(null)).isFalse();
    }

    @Test
    @DisplayName("equals → different class → false")
    void equals_differentClass_returnsFalse() {
        // Arrange: OrderDto and String (different class)
        OrderDto dto = new OrderDto();
        String notADto = "I'm a string";

        // Act & Assert: equals with different class should return false
        //noinspection EqualsBetweenInconvertibleTypes // IDE warning suppression
        assertThat(dto.equals(notADto)).isFalse();
    }

    @Test
    @DisplayName("equals → all relevant fields equal → true")
    void equals_allFieldsEqual_returnsTrue() {
        // Arrange: two OrderDtos with identical fields
        OrderDto a = create("Monitor", 249.50, 3);
        OrderDto b = create("Monitor", 249.50, 3);

        // Act & Assert: should be equal (symmetric)
        assertThat(a.equals(b)).isTrue();
        assertThat(b.equals(a)).isTrue(); // Test symmetry property
    }

    @Test
    @DisplayName("equals → productname differs → false")
    void equals_productNameDiffers_returnsFalse() {
        // Arrange: OrderDtos with different product names
        OrderDto a = create("Keyboard", 89.99, 2);
        OrderDto b = create("Mouse",    89.99, 2);

        // Act & Assert: should not be equal
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    @DisplayName("equals → unitprice differs → false")
    void equals_unitPriceDiffers_returnsFalse() {
        // Arrange: OrderDtos with different unit prices
        OrderDto a = create("Headset", 199.00, 1);
        OrderDto b = create("Headset", 189.00, 1);

        // Act & Assert: should not be equal
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    @DisplayName("equals → quantity differs → false")
    void equals_quantityDiffers_returnsFalse() {
        // Arrange: OrderDtos with different quantities
        OrderDto a = create("Cable", 9.99, 5);
        OrderDto b = create("Cable", 9.99, 6);

        // Act & Assert: should not be equal
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    @DisplayName("equals → productname is null in both → true")
    void equals_bothProductNamesNull_returnsTrue() {
        // Arrange: both OrderDtos have null product names
        OrderDto a = create(null, 45.00, 10);
        OrderDto b = create(null, 45.00, 10);

        // Act & Assert: should be equal (both null)
        assertThat(a.equals(b)).isTrue();
    }

    @Test
    @DisplayName("equals → one productname null, other not → false")
    void equals_oneProductNameNull_returnsFalse() {
        // Arrange: one has null product name, other has actual name
        OrderDto a = create(null,       120.00, 1);
        OrderDto b = create("Charger", 120.00, 1);

        // Act & Assert: should not be equal (asymmetric null case)
        assertThat(a.equals(b)).isFalse();
        assertThat(b.equals(a)).isFalse(); // Test both directions
    }

    // ────────────────────────────────────────────────
    //  hashCode()
    //  Tests for hashCode() method
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("hashCode → equal objects have same hash")
    void equalObjects_haveSameHashCode() {
        // Arrange: two equal OrderDtos
        OrderDto a = create("Tablet", 399.99, 2);
        OrderDto b = create("Tablet", 399.99, 2);

        // Act & Assert: equal objects must have equal hash codes
        assertThat(a.hashCode()).isEqualTo(b.hashCode()); // Contract requirement
    }

    @Test
    @DisplayName("hashCode → different productname → different hash (most cases)")
    void differentProductName_usuallyDifferentHash() {
        // Arrange: OrderDtos with different product names
        OrderDto a = create("Phone A", 699.00, 1);
        OrderDto b = create("Phone B", 699.00, 1);

        // Act & Assert: different objects usually have different hash codes
        // Not guaranteed by contract, but very likely → good indicator
        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("hashCode → consistent on same instance")
    void hashCode_isConsistent() {
        // Arrange: single OrderDto instance
        OrderDto dto = create("Router", 89.90, 4);
        
        // Act: get hash code twice
        int first = dto.hashCode();
        int second = dto.hashCode();

        // Assert: hash code should be consistent across calls
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("hashCode → two null productname objects have same hash")
    void hashCode_nullProductName_sameHash() {
        // Arrange: both OrderDtos have null product names
        OrderDto a = create(null, 55.00, 7);
        OrderDto b = create(null, 55.00, 7);

        // Act & Assert: should have same hash code
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    // ────────────────────────────────────────────────
    //  Helpers
    //  Utility method for creating OrderDto instances
    // ────────────────────────────────────────────────

    private OrderDto create(String name, double price, int qty) {
        OrderDto dto = new OrderDto(); // Create new OrderDto
        dto.productname = name;       
        dto.unitprice   = price;       // Set unit price
        dto.quantity    = qty;         
        return dto;                    // Return populated DTO
    }
}