package com.jpd.web.testutil;

import com.jpd.web.model.Creator;
import com.jpd.web.model.Status;

import java.util.ArrayList;
import java.util.List;

public final class CreatorTestDataBuilder {

    private CreatorTestDataBuilder() {
    }

    public static Creator.CreatorBuilder aCreator() {
        return Creator.builder()
                .creatorId("creator-1")
                .fullName("Test Creator")
                .balance(0.0)
                .status(Status.SUCCESS)
                .warningCount(0)
                .reputationScore(100)
                .ban(false)
                .uploadCertificate(false)
                .certificateUrl(new ArrayList<>())
                .courses(new ArrayList<>());
    }

    public static Creator aCreatorWithId(String creatorId) {
        return aCreator().creatorId(creatorId).build();
    }

    public static Creator aCreatorWithCertificates(List<String> urls) {
        return aCreator().certificateUrl(new ArrayList<>(urls)).build();
    }
}
