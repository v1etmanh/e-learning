package com.jpd.web.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data

@Entity
public class SemanticData extends SemanticResult{
    @Id
    @GeneratedValue
    private Long semanticDataId;
    private String customerId;
    public SemanticData(SemanticResult result, String customerId) {
        super(
                result.isMatch(),
                result.getSimilarityScore(),
                result.getUserAnswer(),
                result.getExpectedAnswer(),
                result.getFeedback(),
                result.isHasError(),
                result.getErrorMessage()
        );
        this.customerId = customerId;
    }
    @CreationTimestamp
    private LocalDateTime createDate;
     private long moduleId;


}
