package com.jpd.web.controller.creator;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.service.FileUploadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/creator/uploadFile")
@Tag(name = "Creator file storage", description = """
        Uploading and deleting the media a creator attaches to their courses. Files live in Firebase
        Storage and each upload counts against the creator's storage quota.

        Upload responses are the plain-text public URL, not JSON.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class FileUploadController {
	@Autowired
	private FileUploadService fileUploadService;

	@Operation(
	    summary = "Upload a PDF",
	    description = """
	        Stores a PDF in the signed-in creator's Firebase Storage folder and returns its public URL as
	        plain text. Use that URL when attaching the document to module content.

	        Rejected if the file is empty, is not an accepted PDF type, or would push the creator past
	        their storage quota.
	        """)
	@ApiResponses({
	    @ApiResponse(responseCode = "200", description = "Uploaded. The body is the public URL as plain text.",
	        content = @Content(mediaType = "text/plain", schema = @Schema(type = "string",
	            example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/pdf%2Fn5-workbook.pdf?alt=media"))),
	    @ApiResponse(responseCode = "400", description = "Firebase returned no URL for the upload, or the upload itself failed (`code: API_ERROR`). Empty body in the former case.",
	        content = @Content),
	    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
	    @ApiResponse(responseCode = "500", description = "No `pdf` part was sent, the file was empty or the wrong type, the storage quota was exceeded, "
	            + "or another unexpected server error occurred.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/savePdf")
	 public ResponseEntity<?> storePDf(
			 @Parameter(description = "The PDF to store.", required = true, schema = @Schema(type = "string", format = "binary"))
			 @RequestParam("pdf") MultipartFile pdf,
			 @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) throws IllegalAccessException, IOException {
	
	 	String creatorId=jwt.getClaimAsString("sub");
	    String a=this.fileUploadService.saveImgIntoFirebase(creatorId,pdf,TypeOfFile.PDF);
	    if(a==null) {return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();}
	    else return ResponseEntity.status(HttpStatus.OK).body(a);
	
	}
	@Operation(
	    summary = "Upload an image",
	    description = """
	        Stores an image in the signed-in creator's Firebase Storage folder and returns its public URL
	        as plain text. Use that URL as a course cover or inside module content.

	        Rejected if the file is empty, is not an accepted image type, or would push the creator past
	        their storage quota.
	        """)
	@ApiResponses({
	    @ApiResponse(responseCode = "200", description = "Uploaded. The body is the public URL as plain text.",
	        content = @Content(mediaType = "text/plain", schema = @Schema(type = "string",
	            example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fn5-cover.jpg?alt=media"))),
	    @ApiResponse(responseCode = "400", description = "Firebase returned no URL for the upload, or the upload itself failed (`code: API_ERROR`). Empty body in the former case.",
	        content = @Content),
	    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
	    @ApiResponse(responseCode = "500", description = "No `img` part was sent, the file was empty or the wrong type, the storage quota was exceeded, "
	            + "or another unexpected server error occurred.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/saveImg")
	public ResponseEntity<?> postMethodName(
											  @Parameter(description = "The image to store.", required = true, schema = @Schema(type = "string", format = "binary"))
											  @RequestParam("img") MultipartFile img,
											  @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) throws IllegalAccessException, IOException {

		String creatorId=jwt.getClaimAsString("sub");
	   String a=this.fileUploadService.saveImgIntoFirebase(creatorId,img,TypeOfFile.IMG);
	   if(a==null) {return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();}
	   else return ResponseEntity.status(HttpStatus.OK).body(a);
	
	}
	@Operation(
	    summary = "Delete an uploaded file",
	    description = """
	        Removes a previously uploaded file from Firebase Storage by its public URL and frees the
	        storage it occupied. The file must belong to the signed-in creator. Irreversible — anything
	        still referencing the URL will break.
	        """)
	@ApiResponses({
	    @ApiResponse(responseCode = "204", description = "File deleted. Empty body.", content = @Content),
	    @ApiResponse(responseCode = "400", description = "The file could not be deleted from Firebase Storage (`code: FILE_UPLOAD_ERROR`).",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
	    @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
	    @ApiResponse(responseCode = "500", description = "`url` was omitted, or another unexpected server error occurred.",
	        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@DeleteMapping("/delete_file")
	public ResponseEntity<?> deleteImage(
			@Parameter(description = "Public URL of the file to delete, exactly as returned by the upload endpoint.", required = true,
					example = "https://firebasestorage.googleapis.com/v0/b/jaen-elearning.appspot.com/o/img%2Fn5-cover.jpg?alt=media")
			@RequestParam("url")String url, @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt)
	{
		String creatorId=jwt.getClaimAsString("sub");
		 this.fileUploadService.deleteFileByUrl(url, creatorId);
		 return ResponseEntity.noContent().build();
	}
	
}
