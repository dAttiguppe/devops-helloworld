public class OrderResponse {

  // This is the main method that creates the entire response body
  public static PactDslJsonBody getResponse() {
    return new PactDslJsonBody()
      .uuid("orderId")  // This is a simple field, e.g., "orderId": "uuid"
      .minArrayLike("items", 1, itemElement(), 1)  // 'items' is an array with min 1 element, each element follows itemElement
      .minArrayLike("payments", 1, paymentElement(), 1);  // 'payments' is an array with min 1 element, each element follows paymentElement
  }

  // Helper method defining the shape of each item in the 'items' array
  private static PactDslJsonBody itemElement() {
    return new PactDslJsonBody()
      .stringType("sku")  // "sku": "some_string"
      .decimalType("price")  // "price": 10.5
      .integerType("quantity");  // "quantity": 1
  }

  // Helper method defining the shape of each payment in the 'payments' array
  private static PactDslJsonBody paymentElement() {
    return new PactDslJsonBody()
      .stringType("type")  // "type": "CARD"
      .decimalType("amount");  // "amount": 100.0
  }
}
