@Pact(consumer = "CalendarApp", provider = "CalendarAPI")
public RequestResponsePact createFullRecurrencePact(PactDslWithProvider builder) {
    
    // Full RRULE recurrence schema with all iCal fields
    DslPart rruleSchema = new PactDslJsonBody()
        .stringType("frequency", "WEEKLY")  // DAILY, WEEKLY, MONTHLY, YEARLY, etc.
        .integerType("count", 12)           // Number of occurrences
        .integerType("interval", 2)         // Every Nth interval
        .arrayMinLike("secondlist", 0, integerType(0), 60, "seconds")  // BYSECOND=[0,30]
        .arrayMinLike("minutelist", 0, integerType(0), 60, "minutes")  // BYMINUTE=[0,30]
        .arrayMinLike("hourlist", 0, integerType(9), 24, "hours")      // BYHOUR=[9,14]
        .arrayMinLike("monthdaylist", 0, integerType(15), 31, "monthDays")  // BYMONTHDAY=[15,-1]
        .arrayMinLike("yeardaylist", 0, integerType(1), 366, "yearDays")    // BYYEARDAY=[1,100]
        .arrayMinLike("weeknumberlist", 0, integerType(1), 53, "weekNumbers") // BYWEEKNO=[1,52]
        .arrayMinLike("monthlist", 0, integerType(1), 12, "months")         // BYMONTH=[1,6,12]
        
        // BYDAY with position (e.g., 2nd Tuesday = "2TU")
        .arrayMinLike("poslist", 0, 
            new PactDslJsonBody().integerType("position", 2).stringType("day", "TU").closeObject(), 
            7, "byDayRules")
        
        .stringType("weekstartday", "MO")  // Week start day (MO,TU,etc.)
        .minArrayLike("daylist", 1, 
            new PactDslJsonBody()
                .stringType("day", "MO")
                .integerType("position", -1)  // Last occurrence
                .closeObject(), 7, "days")
        .closeObject();

    return builder
        // Full RRULE calendar response
        .given("recurring event with complex RRULE")
        .uponReceiving("get event with full recurrence rule")
            .path("/api/calendar/events/123/rrule")
        .willRespondWith()
            .status(200)
            .body(new PactDslJsonBody()
                .stringType("eventId", "evt-123")
                .stringType("title", "Weekly Team Sync")
                .object("recurrence", rruleSchema)
            )

        // Simplified weekly recurrence
        .given("simple weekly recurrence")
        .uponReceiving("get simple recurring event")
            .path("/api/calendar/events/456/rrule")
        .willRespondWith()
            .status(200)
            .body(new PactDslJsonBody()
                .stringType("eventId", "evt-456")
                .stringType("title", "Daily Standup")
                .object("recurrence", new PactDslJsonBody()
                    .stringType("frequency", "DAILY")
                    .integerType("interval", 1)
                    .integerType("count", 365)
                    .minArrayLike("daylist", 1, 
                        new PactDslJsonBody().stringType("day", "MO").closeObject(), 7)
                    .stringType("weekstartday", "MO")
                    .closeObject()
                )
            )
        .toPact();
}
