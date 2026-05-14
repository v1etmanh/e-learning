package com.jpd.web.service;

import java.util.Optional;

import com.jpd.web.repository.CreatorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Module;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.service.utils.ValidationResources;

import jakarta.transaction.Transactional;

@Service
public class ModuleService {
	@Autowired
	private ModuleRepository moduleRepository;
    @Autowired
	private CreatorRepository creatorRepository;
	@Autowired
	private ModuleContentRepository moduleContentRepository;
	@Autowired
	private ValidationResources validationResources;

	public Module createModule(String moduleName, String creatorId, long courseId, long chapterId) {
		validationResources.validateCourseOwnership(courseId, creatorId);
        Chapter chapter = validationResources.validateChapterBelongsToCourse(chapterId, courseId);

		Module module = new Module();

		module.setChapter(chapter);
		module.setTitleOfModule(moduleName);

		return this.moduleRepository.save(module);
	}

	@Transactional
	public void deleteModule(String creatorId, long courseId, long chapterId, long moduleId)
		 {
		
	Module module=validationResources.validateCompleteOwnership(moduleId, chapterId, courseId, creatorId);
		
		moduleContentRepository.deleteByModuleId(module.getModuleId());

		moduleRepository.deleteByModuleId(moduleId);
	}
	public void updateModuleName(String  id, long moduleId,String title) {
	   Creator c=  this.creatorRepository.findById(id).orElseThrow();
	   Optional<Module> m=this.moduleRepository.findById(moduleId);
	   if(m.isEmpty())throw new ModuleNotFoundException(moduleId);
	   if(m.get().getChapter().getCourse().getCreator().getCreatorId()!=id)
		   throw new UnauthorizedException("ko so huu module ");
	   m.get().setTitleOfModule(title);
	   this.moduleRepository.save(m.get());
	}

}
