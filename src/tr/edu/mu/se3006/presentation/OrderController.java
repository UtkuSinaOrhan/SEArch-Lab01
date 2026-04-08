package tr.edu.mu.se3006.presentation;
import tr.edu.mu.se3006.business.OrderService;

public class OrderController {
    // TODO: Define OrderService dependency and use Constructor Injection
    private OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public void handleUserRequest(Long productId, int quantity) {
        System.out.println(">>> New Request: Product ID=" + productId + ", Quantity=" + quantity);
        // TODO: Call placeOrder inside a try-catch block. 
        // Print "✅ Order Confirmed" on success, or "❌ ERROR: [message]" on failure.
        try {
            orderService.placeOrder(productId, quantity);
            System.out.println("✅ Order Confirmed");
        } catch (IllegalArgumentException e) {
            System.out.println("❌ ERROR: " + e.getMessage());
        }
    }
}
