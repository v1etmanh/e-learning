package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Entity
@AllArgsConstructor
@RequiredArgsConstructor
@Data
@EqualsAndHashCode(exclude = {"speakingPictureQuestion"}) 
public class SpeakingPictureListQuestions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "speaking_picture_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) 
    private long id;
    private String question;

    private String answer;
    @ManyToOne
    @JoinColumn(name = "mc_id")
    @JsonBackReference
    @ToString.Exclude
    private SpeakingPictureQuestion speakingPictureQuestion;
}