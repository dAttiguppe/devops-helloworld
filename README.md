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



