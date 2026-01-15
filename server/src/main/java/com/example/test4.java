DslPart rruleSchema = new PactDslJsonBody()
    .stringType("frequency", "WEEKLY")
    .integerType("count", 12)
    .integerType("interval", 2)
    
    // ✅ CORRECT array syntax
    .minArrayLike("secondlist", 0, PactDslJsonRootValue.integerType(0), 60)
    .minArrayLike("minutelist", 0, PactDslJsonRootValue.integerType(0), 60)
    .minArrayLike("hourlist", 0, PactDslJsonRootValue.integerType(9), 24)
    .minArrayLike("monthdaylist", 0, PactDslJsonRootValue.integerType(15), 31)
    
    // Complex BYDAY objects
    .minArrayLike("daylist", 1,
        new PactDslJsonBody()
            .stringType("day", "MO")
            .integerType("position", -1)
            .closeObject(), 7)
    
    .stringType("weekstartday", "MO")
    .closeObject();
