package com.jpd.web.controller.customer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.config.OpenAPIConfig;
import com.jpd.web.dto.DictionaryImportResultDto;
import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.service.DictionaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/customer/dictionary")
@Tag(name = "Personal dictionary", description = """
        The signed-in customer's private vocabulary list. Every entry is scoped to the `sub` claim of the
        bearer token — one customer can never see or modify another's words.
        """)
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
public class DictionaryController {
    @Autowired
   private DictionaryService dictionaryService;

    @Operation(
        summary = "List the customer's saved words",
        description = "Returns every vocabulary entry belonging to the signed-in customer.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The customer's dictionary. Empty when nothing has been saved.",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = RememberWordDto.class)),
                examples = @ExampleObject(name = "dictionary", value = """
                    [
                      {
                        "rwId": 204,
                        "word": "天気",
                        "meaning": "thời tiết",
                        "description": "Đọc là てんき. Dùng trong hội thoại hằng ngày về thời tiết.",
                        "synonyms": ["気候", "天候"],
                        "example": ["今日はいい天気ですね"],
                        "language": "JAPANESE",
                        "voteCount": 3,
                        "customerEmail": "nguyenthuy010605@gmail.com"
                      }
                    ]"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping()
    public ResponseEntity<List<RememberWordDto>> getDictionary(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt){
        String customerId = jwt.getClaimAsString("sub");
    	     
       List<RememberWordDto> rememberWordDtoList= dictionaryService.getDictionary(customerId);
        return  ResponseEntity.ok(rememberWordDtoList);
    }
    @Operation(
        summary = "Save a new word",
        description = """
            Adds a vocabulary entry to the signed-in customer's dictionary.

            Duplicates are not rejected — saving the same word twice creates two entries. The response
            echoes the submitted body back unchanged, so its `rwId` is whatever you sent, **not** the
            generated identifier. Re-read the dictionary to obtain real ids.
            """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "The word to save. Not validated — the handler does not apply `@Valid`. `rwId`, `voteCount` and `customerEmail` are ignored.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = RememberWordDto.class),
            examples = @ExampleObject(name = "newWord", value = """
                {
                  "word": "天気",
                  "meaning": "thời tiết",
                  "description": "Đọc là てんき. Dùng trong hội thoại hằng ngày về thời tiết.",
                  "synonyms": ["気候", "天候"],
                  "example": ["今日はいい天気ですね"],
                  "language": "JAPANESE"
                }""")))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Word saved. The body is the submitted payload echoed back.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RememberWordDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Malformed or missing body, unknown `language` value, or another unexpected server error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping()
    public ResponseEntity<RememberWordDto> addDictionary(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,@RequestBody RememberWordDto rememberWordDto){
        log.info("Post add new remember word customer {} , remember word :{}",rememberWordDto.getDescription());
        String customerId = jwt.getClaimAsString("sub");
     
        RememberWordDto wordDto= dictionaryService.addRememberWord(customerId,rememberWordDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(wordDto);
    }

    @Operation(
        summary = "Bulk-import words from a JSON file",
        description = """
            Imports many vocabulary entries at once from a JSON file — the export format of the JPD
            Vocabulary browser extension. The file may be either a bare JSON array of entries or an object
            with an `entries` array.

            Each entry needs at least a `word`; `meaning` and `description` fall back to defaults when
            absent. Words already in the customer's dictionary, and words repeated within the file, are
            skipped rather than duplicated — the comparison is on the normalised word.

            A partially successful import still returns 200. Read `importedCount`, `skippedCount` and
            `invalidCount` to see what actually happened. The file must be non-empty and at most 5 MB.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Import finished. Counters describe the outcome.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DictionaryImportResultDto.class),
                examples = @ExampleObject(name = "imported", value = """
                    {
                      "importedCount": 42,
                      "skippedCount": 2,
                      "invalidCount": 1,
                      "skippedWords": ["天気", "学校"],
                      "invalidReasons": ["Một entry không có word"]
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid bearer token.", content = @Content),
        @ApiResponse(responseCode = "500", description = "The file was empty, larger than 5 MB, not readable as JSON, or contained no entries. "
                + "These are raised as `IllegalArgumentException`, which the global handler maps to 500 rather than 400.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DictionaryImportResultDto> importDictionary(
            @Parameter(description = "JSON file of vocabulary entries. Non-empty, maximum 5 MB.", required = true,
                    schema = @Schema(type = "string", format = "binary"))
            @RequestPart("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String customerId = jwt.getClaimAsString("sub");
        DictionaryImportResultDto result = dictionaryService.importJson(customerId, file);
        return ResponseEntity.ok(result);
    }
    @Operation(
        summary = "Delete a saved word",
        description = """
            Removes one vocabulary entry. The entry must belong to the signed-in customer — deleting
            someone else's word is rejected with 401.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Word deleted. Empty body.", content = @Content),
        @ApiResponse(responseCode = "400", description = "`rwId` was not a valid number (`code: TYPE_MISMATCH`).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "The entry belongs to another customer, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "No entry exists with this id, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{rwId}")
    public ResponseEntity<Void> deleteDictionary(
    		@Parameter(description = "Identifier of the entry to delete. Must belong to the caller.", required = true, example = "204")
    		@PathVariable("rwId") long id,@Parameter(hidden = true) @AuthenticationPrincipal
    		Jwt jwt){
        String customerId = jwt.getClaimAsString("sub");
        dictionaryService.deleteRememberWord(customerId,id);
        return ResponseEntity.noContent().build();
    }
    @Operation(
        summary = "Update a saved word",
        description = """
            Replaces a vocabulary entry. The target is identified by `rwId` **in the body**, not in the path,
            and it must belong to the signed-in customer.

            The entry is overwritten wholesale, so send every field you want to keep — omitted fields are
            cleared. The existing `voteCount` is preserved regardless of what the body contains. The
            response echoes the submitted body back.
            """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "The full replacement entry, including the `rwId` of the entry to update. Not validated — the handler does not apply `@Valid`.",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = RememberWordDto.class),
            examples = @ExampleObject(name = "update", value = """
                {
                  "rwId": 204,
                  "word": "天気",
                  "meaning": "thời tiết, khí hậu",
                  "description": "Đọc là てんき. Danh từ, dùng rất phổ biến khi mở đầu hội thoại.",
                  "synonyms": ["気候", "天候"],
                  "example": ["今日はいい天気ですね", "明日の天気はどうですか"],
                  "language": "JAPANESE"
                }""")))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Word updated. The body is the submitted payload echoed back.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RememberWordDto.class))),
        @ApiResponse(responseCode = "401", description = "The entry belongs to another customer, or the bearer token is missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "No entry exists with this `rwId`, the body was malformed, or another unexpected server error occurred.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping
    public ResponseEntity<RememberWordDto> updateDictionary(@Parameter(hidden = true) @AuthenticationPrincipal
    		Jwt jwt,@RequestBody RememberWordDto rememberWordDto){
        String customerId = jwt.getClaimAsString("sub");
       RememberWordDto update = dictionaryService.updateRememberWord(customerId, rememberWordDto);
        return ResponseEntity.ok().body(update);
    }


}
