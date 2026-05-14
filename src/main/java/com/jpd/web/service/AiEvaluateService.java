package com.jpd.web.service;

import com.jpd.web.model.SemanticData;
import com.jpd.web.repository.SemanticDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.exception.AIHandlerException;
import com.jpd.web.model.Language;
import com.jpd.web.model.SemanticResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class AiEvaluateService {
	@Autowired
	private OpenAIService openAIService;
	@Autowired
	private SemanticDataRepository semanticDataRepository;

// this function check if wheahter where is type of question : speaking passage
// and speaking with picture 
// if speaking with passage , we only need to get transcribe and compare with answer
// else we need to transcribe and convert to emmbeded vector and using aito compare 
	public SemanticResult evaluateSpeaking(MultipartFile audio, String expectedAnswer, String language,String customerId,long moduleId)
			throws Exception {
		LocalDateTime startOfDay = LocalDate.now().atStartOfDay(); // 00:00:00
		LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
		if(semanticDataRepository.countTodayByCustomerId(customerId,startOfDay,endOfDay)>10)throw new AIHandlerException("You can only evaluate 10 times per day");

		validateInputs(audio, expectedAnswer, language);
		String audioS = this.openAIService.speechToText(audio, language);

		SemanticResult result = this.openAIService.compareSemanticWithEmbedding(audioS, expectedAnswer);
         SemanticData data=new SemanticData(result,customerId);
		 data.setModuleId(moduleId);
		 this.semanticDataRepository.save(data);

		return result;
	}

	private void validateInputs(MultipartFile audio, String expectedAnswer, String language) throws AIHandlerException {
		if (audio == null || audio.isEmpty()) {
			throw new AIHandlerException("Audio file is empty");
		}
		if (expectedAnswer == null || expectedAnswer.isBlank()) {
			throw new AIHandlerException("Expected answer cannot be empty");
		}
		if (language == null) {
			throw new AIHandlerException("Language must be specified");
		}
	}

}
