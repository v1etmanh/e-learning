package com.jpd.web.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.jpd.web.service.utils.CourseMetricsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.WishlistDto;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.WishlistExistException;
import com.jpd.web.model.Course;
import com.jpd.web.model.Wishlist;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.WishlistRepository;
import com.jpd.web.transform.WishlistTransform;

@Service
public class WishlistService {
@Autowired
private WishlistRepository wishlistRepository;

@Autowired
private CourseRepository courseRepository;
@Autowired
private CourseMetricsHelper courseMetricsHelper;
public void addWishlist(String customerId, long courseId) {
	//check wishlist is exist
	//

	Optional<Course>course=this.courseRepository.findById(courseId);
	if(course.isEmpty())throw new CourseNotFoundException(courseId);
 Optional<Wishlist>e =this.wishlistRepository.findByCourse_CourseIdAndCustomerId(courseId, customerId);
 if(e.isPresent())throw new WishlistExistException("you have added this course in your wishlish");
 
 Wishlist w=Wishlist.builder()
		 .customerId(customerId)
		 .course(course.get())
		 .build();
 this.courseMetricsHelper.incrementWishlist(courseId);
 this.wishlistRepository.save(w);
}
public List<WishlistDto> retrieveYourWishlist(String customerId){

	List<Wishlist> wishlists=this.wishlistRepository.findByCustomerId(customerId);
	return wishlists.stream().map(e->WishlistTransform.transformToWishlistDto(e))
			.collect(Collectors.toList());
			
}
}
