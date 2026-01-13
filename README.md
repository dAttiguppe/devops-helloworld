# devops-helloworld
devops-helloworld


import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.session.Expression
import io.gatling.http.request.builder.RawtFileBodyPart // Note: Typo fixed to RawFileBodyPart

// ... (Other imports and Step 1 remain the same) ...

/**
  * Step 2: Custom logic to generate multiple RawFileBodyPart expressions, 
  * referencing files located inside the "data/" resources subdirectory.
  */
val generateMultipartParts = exec(session => {
  // 1. Get the list of file names (e.g., "document_a.pdf") from the session
  val fileNames: List[String] = session("fileNamesList").as[List[String]]

  // 2. Map the list of file names to a sequence of BodyPart expressions
  val bodyParts: Seq[Expression[BodyPart]] = fileNames.map { fileName =>
    // **KEY CHANGE HERE:** Prefix the fileName with "data/" to create the resource path
    val resourcePath = s"data/$fileName"
    
    // Create a RawFileBodyPart for each file name.
    // The first argument ("file") is the multipart form field name.
    // The second argument (resourcePath) is the file path relative to the resources directory.
    RawFileBodyPart("file", resourcePath)
      .fileName(fileName) // Sets the filename header sent to the server (optional, but good practice)
      .asExpression
  }

  // 3. Save the sequence of BodyPart expressions back to the session
  session.set("uploadBodyParts", bodyParts)
})

// ... (Step 3 and Scenario definition remain the same) ...


import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.session.Expression
import io.gatling.http.request.builder.RawtFileBodyPart

import io.circe.parser._ // Import Circe parser for JSON handling
import io.circe.Json

import scala.collection.JavaConverters._

class InternalDataMultipartUploadSimulation extends Simulation {

  // --- Configuration ---
  val httpProtocol = http
    .baseUrl("YOUR_BASE_URL") // e.g., "http://localhost:8080"

  // --- Internal Data ---
  // Define your static JSON array here as a Scala string
  val staticJsonArray: String =
    """
      [
        { "id": 101, "sourceFileName": "document_a.pdf" },
        { "id": 102, "sourceFileName": "image_b.jpg" },
        { "id": 103, "sourceFileName": "data_c.csv" }
      ]
    """
    
  // NOTE: Ensure files like 'document_a.pdf', 'image_b.jpg', etc., exist 
  // in your Gatling resources directory (e.g., src/main/resources/bodies).

  // --- Steps ---
  
  /**
    * Step 1: Parse the static JSON string using Circe and extract the file names.
    */
  val parseAndExtractFileNames = exec(session => {
    // 1. Parse the string into a Circe Json object (using a Try to handle errors)
    val parsedJson: Either[io.circe.Error, Json] = parse(staticJsonArray)

    // 2. Safely extract the file names
    val fileNamesList: List[String] = parsedJson match {
      case Right(json) =>
        json.asArray
          .map { arr =>
            // Map over the array of JSON objects
            arr.flatMap(obj => 
              // Attempt to get the "sourceFileName" field as a String
              obj.hcursor.get[String]("sourceFileName").toOption
            ).toList
          }
          .getOrElse(List.empty) // If it's not an array, return an empty list

      case Left(error) =>
        println(s"Error parsing JSON: $error")
        List.empty
    }

    // 3. Save the resulting list of file names to the session
    session.set("fileNamesList", fileNamesList)
  })

  // ---
  
  /**
    * Step 2: Custom logic to generate multiple RawFileBodyPart expressions.
    * This step is essentially the same as before.
    */
  val generateMultipartParts = exec(session => {
    // 1. Get the list of file names from the session
    val fileNames: List[String] = session("fileNamesList").as[List[String]]

    // 2. Map the list of file names to a sequence of BodyPart expressions
    val bodyParts: Seq[Expression[BodyPart]] = fileNames.map { fileName =>
      // Create a RawFileBodyPart for each file name.
      // "file" is the form field name; fileName is the resource file to load.
      RawFileBodyPart("file", fileName).fileName(fileName).asExpression
    }

    // 3. Save the sequence of BodyPart expressions back to the session
    session.set("uploadBodyParts", bodyParts)
  })

  // ---
  
  /**
    * Step 3: Perform the POST request using the dynamically generated BodyParts.
    */
  val performMultipartUpload = exec(
    http("2 - Multipart File Upload (Internal Data)")
      .post("/api/upload-batch")
      .check(status.is(200))
      // Use the BodyPart Expression stored in the session
      .bodyParts(session => session("uploadBodyParts").as[Seq[Expression[BodyPart]]])
  )

  // --- Scenario Definition ---
  val scn = scenario("Internal Data File Upload Scenario")
    .exec(parseAndExtractFileNames)
    .exec(generateMultipartParts)
    .exec(performMultipartUpload)

  // --- Injecting Users ---
  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}


import au.com.dius.pact.consumer.dsl.*;
import com.fasterxml.jackson.databind.*;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class JsonToPactDslConverter {

    private static final Pattern DATE =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern DATETIME =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}T.*");

    public static PactDslJsonBody convert(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        if (!root.isObject()) {
            throw new IllegalArgumentException("Root JSON must be an object");
        }

        PactDslJsonBody body = new PactDslJsonBody();
        populateObject(body, root);
        return body;
    }

    private static void populateObject(PactDslJsonBody body, JsonNode node) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            addField(body, field.getKey(), field.getValue());
        }
    }

    private static void addField(PactDslJsonBody body, String key, JsonNode value) {

        if (value.isTextual()) {
            handleString(body, key, value.asText());

        } else if (value.isInt() || value.isLong()) {
            body.integerType(key, value.asLong());

        } else if (value.isFloatingPointNumber()) {
            body.decimalType(key, value.decimalValue());

        } else if (value.isBoolean()) {
            body.booleanType(key, value.asBoolean());

        } else if (value.isObject()) {
            body.object(key, o -> populateObject(o, value));

        } else if (value.isArray()) {
            handleArray(body, key, value);

        } else if (value.isNull()) {
            body.nullValue(key);
        }
    }

    private static void handleArray(PactDslJsonBody body, String key, JsonNode array) {
        if (array.isEmpty()) {
            body.array(key);
            return;
        }

        JsonNode first = array.get(0);

        body.minArrayLike(key, 1, a -> {
            if (first.isObject()) {
                populateObject(a, first);
            } else {
                addAnonymousValue(a, first);
            }
        });
    }

    private static void addAnonymousValue(PactDslJsonBody body, JsonNode value) {
        if (value.isTextual()) {
            body.stringType(value.asText());
        } else if (value.isInt() || value.isLong()) {
            body.integerType(value.asLong());
        } else if (value.isFloatingPointNumber()) {
            body.decimalType(value.decimalValue());
        } else if (value.isBoolean()) {
            body.booleanType(value.asBoolean());
        }
    }

    private static void handleString(PactDslJsonBody body, String key, String value) {

        if (isUUID(value)) {
            body.uuid(key, value);

        } else if (DATETIME.matcher(value).matches()) {
            body.datetime(key, "yyyy-MM-dd'T'HH:mm:ss.SSSX", value);

        } else if (DATE.matcher(value).matches()) {
            body.date(key, "yyyy-MM-dd", value);

        } else {
            body.stringType(key, value);
        }
    }

    private static boolean isUUID(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

