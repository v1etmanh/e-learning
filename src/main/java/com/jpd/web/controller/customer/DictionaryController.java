package com.jpd.web.controller.customer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.service.DictionaryService;


import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/customer/dictionary")
public class DictionaryController {
    @Autowired
   private DictionaryService dictionaryService;
    @GetMapping()
    public ResponseEntity<List<RememberWordDto>> getDictionary(@AuthenticationPrincipal Jwt jwt){
        String customerId = jwt.getClaimAsString("sub");
    	     
       List<RememberWordDto> rememberWordDtoList= dictionaryService.getDictionary(customerId);
        return  ResponseEntity.ok(rememberWordDtoList);
    }
    @PostMapping()
    public ResponseEntity<RememberWordDto> addDictionary(@AuthenticationPrincipal Jwt jwt,@RequestBody RememberWordDto rememberWordDto){
        log.info("Post add new remember word customer {} , remember word :{}",rememberWordDto.getDescription());
        String customerId = jwt.getClaimAsString("sub");
     
        RememberWordDto wordDto= dictionaryService.addRememberWord(customerId,rememberWordDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(wordDto);
    }
    @DeleteMapping("/{rwId}")
    public ResponseEntity<Void> deleteDictionary(@PathVariable("rwId") long id,@AuthenticationPrincipal
    		Jwt jwt){
        String customerId = jwt.getClaimAsString("sub");
        dictionaryService.deleteRememberWord(customerId,id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping
    public ResponseEntity<RememberWordDto> updateDictionary(@AuthenticationPrincipal
    		Jwt jwt,@RequestBody RememberWordDto rememberWordDto){
        String customerId = jwt.getClaimAsString("sub");
       RememberWordDto update = dictionaryService.updateRememberWord(customerId, rememberWordDto);
        return ResponseEntity.ok().body(update);
    }


}