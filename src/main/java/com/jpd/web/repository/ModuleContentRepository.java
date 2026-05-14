package com.jpd.web.repository;

import java.util.List;
import java.util.Set;

import com.jpd.web.model.CustomerModuleContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;


public interface ModuleContentRepository extends JpaRepository<ModuleContent, Long> {

@Modifying
@Query("DELETE FROM ModuleContent mc WHERE mc.moduleId = :moduleId")
void deleteByModuleId(@Param("moduleId") Long moduleId);
int deleteByTypeOfContentAndModuleId(TypeOfContent typeOfContent, long moduleId);

ModuleContent  deleteByMcId(Long mcId);
    @Modifying
    @Query("DELETE FROM ModuleContent mc WHERE mc.kahootListFunction.kahootId = :kahootId")
    void deleteByKahootId(@Param("kahootId") Long kahootId);
    List<ModuleContent> findByModuleId(long moduleId);
    @Query("SELECT DISTINCT mc.typeOfContent FROM ModuleContent mc WHERE mc.moduleId = :moduleId")
    Set<TypeOfContent> findTypeOfContentByModuleId(@Param("moduleId") long moduleId);
    List<ModuleContent> findByTypeOfContentAndModuleId(TypeOfContent typeOfContent, long moduleId);

}
