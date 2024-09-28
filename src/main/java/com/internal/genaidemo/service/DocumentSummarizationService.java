package com.internal.genaidemo.service;

import com.google.cloud.documentai.v1.*;

import com.google.cloud.aiplatform.v1.*;


import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@Service
public class DocumentSummarizationService {

    private static final String PROJECT_ID = "sep-rjha-08dec-cts";
    private static final String LOCATION = "us-central1"; // e.g., "us-central1"
    private static final String PROCESSOR_ID = "7381f3650fe0af1";
    private static final String VERTEX_AI_ENDPOINT = "https://us-documentai.googleapis.com/v1/projects/534738955739/locations/us/processors/7381f3650fe0af1:process";


    // Method to summarize the document
    public String summarizeDocument(MultipartFile file) throws IOException {
        // Step 1: Extract text from PDF using Document AI
        String extractedText = extractTextFromDocument(file);

        // Step 2: Summarize the extracted text using Vertex AI (Gemini model)
        return summarizeTextUsingGemini(extractedText);
    }

    // Step 1: Extract text from the PDF document
    private String extractTextFromDocument(MultipartFile file) throws IOException {
        try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create()) {
            ProcessorName processorName = ProcessorName.of(PROJECT_ID, LOCATION, PROCESSOR_ID);
            try (InputStream inputStream = file.getInputStream()) {
                Path tempFile = Files.createTempFile("document", ".pdf");

                byte[] fileBytes = Files.readAllBytes(tempFile);
                ProcessRequest request = ProcessRequest.newBuilder()
                        .setName(processorName.toString())
                        .setRawDocument(RawDocument.newBuilder()
                                .setMimeType("application/pdf")
                                .setContent(com.google.protobuf.ByteString.copyFrom(fileBytes))
                                .build())
                        .build();

                ProcessResponse response = client.processDocument(request);
                return response.getDocument().getText();


            }
        }
    }



    // Step 2: Summarize text using the Gemini model from Vertex AI

    // Method to summarize the document
    public String summarizeTextUsingGemini(String extractedText) throws IOException {
        // Initialize Vertex AI Prediction Service Client
        try (PredictionServiceClient predictionClient = PredictionServiceClient.create()) {
            EndpointName endpoint = EndpointName.of(PROJECT_ID, LOCATION, VERTEX_AI_ENDPOINT);

            // Create input parameters (temperature, maxOutputTokens, etc.)
            Map<String, Value> parametersMap = new HashMap<>();
            parametersMap.put("temperature", Value.newBuilder().setNumberValue(0.5).build());
            parametersMap.put("maxOutputTokens", Value.newBuilder().setNumberValue(100).build());
            parametersMap.put("topP", Value.newBuilder().setNumberValue(0.8).build());
            parametersMap.put("topK", Value.newBuilder().setNumberValue(40).build());

            // Build the `Struct` object for the parameters
            Struct parametersStruct = Struct.newBuilder().putAllFields(parametersMap).build();

            // Create the input content (the extracted text)
            Struct inputContent = Struct.newBuilder()
                    .putFields("content",Value.newBuilder().setStringValue(extractedText).build())
                    .build();
            // Create the PredictRequest object
            PredictRequest predictRequest = PredictRequest.newBuilder()
                    .setEndpoint(endpoint.toString())
                    .addInstances(Value.newBuilder().setStructValue(inputContent).build()) // Add extracted text as input
                    .setParameters(Value.newBuilder().setStructValue(Struct.newBuilder().putAllFields(parametersMap).build()).build()) // Add parameters for prediction
                    .build();

            // Make the prediction request
            PredictResponse response = predictionClient.predict(predictRequest);

            // Get and return the prediction (summary)
            return response.getPredictionsList().get(0).getStructValue().getFieldsMap().get("content").getStringValue();
        }
    }

    public String summarizeDocument1(MultipartFile file) throws IOException {

        Path tempFile = Files.createTempFile("document", ".pdf");
        Files.copy(file.getInputStream(),tempFile,StandardCopyOption.REPLACE_EXISTING);
        String summary = summarizePdf(tempFile.toString());
        Files.delete(tempFile);
        return summary;

    }

    private String summarizePdf(String pdfFilePath) throws IOException {
        try (VertexAI vertexAI = new VertexAI("sep-rjha-08dec-cts", "us-central1")) {

            byte[] pdfData= Files.readAllBytes(Path.of(pdfFilePath));
            GenerativeModel model = new GenerativeModel("gemini-1.5-flash-001", vertexAI);
            com.google.cloud.vertexai.api.GenerateContentResponse response = model.generateContent(
                    ContentMaker.fromMultiModalData(
                            "You are a very professional document summarization specialist.\n"
                                    + "Please summarize the given document in maximum of 10 numbered bullet point.",PartMaker.fromMimeTypeAndData("application/pdf", pdfData)
                    ));

            String output = ResponseHandler.getText(response);
            System.out.println(output);
            return output;
        }
    }
}
