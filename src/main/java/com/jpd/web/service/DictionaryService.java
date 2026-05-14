package com.jpd.web.service;

import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.RememberWord;
import com.jpd.web.repository.RememberWordRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.RememberTransform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j

public class DictionaryService {
@Autowired
   private RememberWordRepository repository;


   
 
    public List<RememberWordDto> getDictionary(String customerId) {
        List<RememberWord> rememberWords = repository.findAllByCustomerId(customerId);
        return rememberWords.stream().map(e->RememberTransform.toRememberWordDto(e)).toList();
    }
    public RememberWordDto addRememberWord(String customerId,RememberWordDto rememberWordDto) {


                RememberWord rememberWord = RememberTransform.toRememberWord(rememberWordDto);
               
                rememberWord.setCustomerId(customerId);
                repository.save(rememberWord);
                log.info("success to add new remember word {}", rememberWord.getWord());
                return rememberWordDto;

    }
    public RememberWordDto updateRememberWord(String customerId,RememberWordDto rememberWordDto) {
       
       long id=rememberWordDto.getRwId();
       validate(customerId,id);
    	RememberWord rememberWord= repository.findById(rememberWordDto.getRwId()).orElseThrow(()-> new RuntimeException("Remember word not found"));
        //so sanh voi ai nguoi dung
          RememberWord re=RememberTransform.toRememberWord(rememberWordDto);
          re.setId(rememberWord.getId());
          re.setVote(rememberWord.getVote());
          this.repository.save(re);
          return rememberWordDto;
    }
    public void deleteRememberWord(String customerId,long id) {
    	 validate(customerId,id);
  
        repository.deleteById(id);
    }
    private void validate(String customerId ,long id) {

   	 Optional< RememberWord> re=this.repository.findById(id);
   	  if(re.isEmpty())throw new RuntimeException("this id is not exist");
   	if(  re.get().getCustomerId().equals(customerId))
   		throw new UnauthorizedException("you do not own this word");
    }
}