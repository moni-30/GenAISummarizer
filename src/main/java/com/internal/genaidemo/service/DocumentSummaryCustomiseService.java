package com.internal.genaidemo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.aiplatform.v1.EndpointName;
import com.google.cloud.aiplatform.v1.PredictRequest;
import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.documentai.v1.ProcessorName;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.protobuf.ByteString;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

@Service
public class DocumentSummaryCustomiseService {

	@org.springframework.beans.factory.annotation.Value("${google.project-id}")
    private String PROJECT_ID;

	@org.springframework.beans.factory.annotation.Value("${vertex-ai.location}")
    private String LOCATION;

	@org.springframework.beans.factory.annotation.Value("${document-ai.processor-id}")
    private String PROCESSOR_ID;

	@org.springframework.beans.factory.annotation.Value("${vertex-ai.endpoint}")
    private String VERTEX_AI_ENDPOINT;

    // Method to summarize PDF or Word document using Document AI
    public String summarizeDocument(MultipartFile file) throws IOException {
        String extractedText;

        // Check the file type (PDF or Word)
        if (file.getOriginalFilename().endsWith(".pdf")) {
            extractedText = extractTextFromPDF(file);
        } else if (file.getOriginalFilename().endsWith(".docx")) {
            extractedText = extractTextFromWord(file);
        } else {
            throw new IOException("Unsupported file type.");
        }

        // After text extraction, summarize using Vertex AI
        return summarizeTextUsingGemini(extractedText);
    }

    // Method to extract text from PDF using Document AI
    private String extractTextFromPDF(MultipartFile file) throws IOException {
        // Create Document Processor Service Client
        try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create()) {
            ProcessorName processorName = ProcessorName.of(PROJECT_ID, LOCATION, PROCESSOR_ID);

            // Read the file into a byte array
            Path tempFile = Files.createTempFile("document", ".pdf");
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            byte[] fileBytes = Files.readAllBytes(tempFile);

            // Create a ProcessRequest with the raw document
            ProcessRequest request = ProcessRequest.newBuilder()
                .setName(processorName.toString())
                .setRawDocument(
                    RawDocument.newBuilder()
                        .setMimeType("application/pdf") // PDF MimeType
                        .setContent(ByteString.copyFrom(fileBytes))
                        .build()
                )
                .build();

                
            // Process the document
            ProcessResponse response = client.processDocument(request);

            // Extract and return the text from the processed document
            String extractedText = response.getDocument().getText();
            Files.delete(tempFile);  // Clean up the temporary file

            return extractedText;
        }
    }

    // Method to extract text from Word document (.docx)
    private String extractTextFromWord(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            // Extract text from the Word document
            return extractor.getText();
        }
    }

    // Method to summarize the extracted text using Vertex AI (Gemini model)
    private String summarizeTextUsingGemini(String extractedText) throws IOException {
       
    	try (PredictionServiceClient predictionClient = PredictionServiceClient.create()) {
            EndpointName endpoint = EndpointName.of(PROJECT_ID, LOCATION, VERTEX_AI_ENDPOINT);

            Map<String, Value> parametersMap = new HashMap<>();
            parametersMap.put("temperature", Value.newBuilder().setNumberValue(0.5).build());
            parametersMap.put("maxOutputTokens", Value.newBuilder().setNumberValue(100).build());
            parametersMap.put("topP", Value.newBuilder().setNumberValue(0.8).build());
            parametersMap.put("topK", Value.newBuilder().setNumberValue(40).build());

            Struct parametersStruct = Struct.newBuilder().putAllFields(parametersMap).build();
            Struct inputContent = Struct.newBuilder()
                .putFields("content", Value.newBuilder().setStringValue(extractedText).build())
                .build();

            PredictRequest predictRequest = PredictRequest.newBuilder()
                .setEndpoint(endpoint.toString())
                .addInstances(Value.newBuilder().setStructValue(inputContent).build())
                .setParameters(Value.newBuilder().setStructValue(parametersStruct).build())
                .build();

            PredictResponse response = predictionClient.predict(predictRequest);
            return response.getPredictionsList().get(0).getStructValue().getFieldsMap().get("content").getStringValue();
        }
    }
}