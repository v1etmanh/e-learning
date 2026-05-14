package com.jpd.web.service;


import java.io.IOException;

import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.*;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.CreatorDto;
import com.jpd.web.transform.CreatorTransform;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/*C:\Users\Admin>hookdeck listen 9090 paypal-webhook --path /webhook/paypal

Dashboard
👉 Inspect and replay events: https://dashboard.hookdeck.com?team_id=tm_jpXk88lTVQQb
 * UNCLAIMED*/
@Service
@Slf4j
public class CreatorService {



    @Autowired
    private FireBaseService fireBaseService;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CreatorRepository creatorRepository;

	@Transactional()
	public CreatorDto getAccount(String creatorId) {
		log.info("Retrieving account information for creator {}", creatorId);

		Creator c=this.creatorRepository.findById(creatorId).get();

		return CreatorTransform.transToCreatorDto(c);
	}

	// upload paypalEmail




    public void removeCertificate(String creatorId,String certificateUrl){
		Creator c=this.creatorRepository.findById(creatorId).orElseThrow();
		if(!c.getCertificateUrl().contains(certificateUrl))
				throw new UnauthorizedException("you cannot deletet because you are not own this file");
		this.fireBaseService.deleteImgByUrl(certificateUrl);


	}
   public void upLoadCertificate(String creatorId,MultipartFile multipartFile) throws FileUploadException {
	   Creator c=this.creatorRepository.findById(creatorId).orElseThrow();
	   try {

		     String url =fireBaseService.uploadGlobalCertificate(multipartFile,creatorId);
	        c.getCertificateUrl().add(url);
	        c.setStatus(Status.PENDING);

	        this.creatorRepository.save(c);
	    } catch (IOException e) {
	        throw new FileUploadException("Error uploading certificate", e);
	        // ✅ Giữ lại stacktrace để debug
	    }
   }



}
