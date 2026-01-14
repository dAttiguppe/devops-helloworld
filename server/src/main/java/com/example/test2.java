PactDslJsonBody body = new PactDslJsonBody()
  .minArrayLike("participants", 1,
    new PactDslJsonBody()
      .stringType("role", "ADMIN")
      .stringType("name", "John Doe")
      .stringMatcher("emailaddress", "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", "user@example.com")
      .stringType("partstat", "ACCEPTED")
      .stringType("sentby", "sender@example.com")
    .closeObject(), 5)
  .closeArray();
