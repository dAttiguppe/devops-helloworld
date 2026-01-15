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



import au.com.dius.pact.consumer.dsl.*;
import au.com.dius.pact.junit5.*;
import static au.com.dius.pact.consumer.dsl.PactDslJsonBody.*;

@Pact(consumer = "CalendarApp", provider = "CalendarAPI")
public RequestResponsePact createFullRRULEPact(PactDslWithProvider builder) {
    
    // Complete iCal RRULE schema
    DslPart fullRRULE = new PactDslJsonBody()
        // REQUIRED fields
        .stringType("frequency", "WEEKLY")  // FREQ: DAILY/WEEKLY/MONTHLY/YEARLY
        
        // TERMINATORS (one of COUNT or UNTIL)
        .integerType("count")               // COUNT=n
        .stringType("until", "20261231T235959Z")  // UNTIL=YYYYMMDD[T]HHMMSS[Z]
        
        // INTERVALS
        .integerType("interval", 1)         // INTERVAL=n
        
        // TIME COMPONENTS (optional arrays, min=0)
        .minArrayLike("secondlist", 0, integerType(0), 60)      // BYSECOND=[0,30]
        .minArrayLike("minutelist", 0, integerType(0), 60)      // BYMINUTE=[0,30]
        .minArrayLike("hourlist", 0, integerType(9), 24)        // BYHOUR=[9,14]
        
        // DATE COMPONENTS (optional arrays, min=0)
        .minArrayLike("monthdaylist", 0, integerType(15), 31)   // BYMONTHDAY=[15,-1]
        .minArrayLike("monthlist", 0, integerType(1), 12)       // BYMONTH=[1,6,12]
        .minArrayLike("yeardaylist", 0, integerType(1), 366)    // BYYEARDAY=[1,100,200]
        .minArrayLike("weeknumberlist", 0, integerType(1), 53)  // BYWEEKNO=[1,52]
        
        // BYDAY with position (e.g., +2TU = 2nd Tuesday)
        .minArrayLike("daylist", 1,
            new PactDslJsonBody()
                .stringMatcher("day", "^[+-]?[1-5]?[A-Z]{2}$", "+2TU")  // MO,TU,WE,-1FR
                .closeObject(), 7)
        
        // BYSETPOS (nth occurrence of qualifying rule)
        .minArrayLike("poslist", 0,
            integerType(1), 366)  // BYSETPOS=[1,3,-1]
        
        // Week start day
        .stringMatcher("weekstartday", "^[A-Z]{2}$", "MO")  // WKST=MO
        
        .closeObject();

    return builder
        .given("complex RRULE recurrence exists")
        .uponReceiving("get full RRULE event")
            .path("/api/calendar/events/123/rrule")
        .willRespondWith()
            .status(200)
            .body(new PactDslJsonBody()
                .stringType("eventId", "evt-123")
                .stringType("title", "Complex Recurring Meeting")
                .stringType("dtstart", "20260115T090000Z")
                .object("rrule", fullRRULE)
            )
        .toPact();
}

